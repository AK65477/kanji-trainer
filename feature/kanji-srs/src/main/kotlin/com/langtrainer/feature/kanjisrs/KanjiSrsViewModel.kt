package com.langtrainer.feature.kanjisrs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.langtrainer.core.model.ReviewOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class KanjiSrsViewModel @Inject constructor(
    private val repository: KanjiSrsRepository,
    private val resumeStore: KanjiSessionResumeStore,
) : ViewModel() {

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Captured per card just before showing options. Used to compute responseMs. */
    private var cardShownAtMs: Long = 0L

    /** Set when [onPause] fires while a card is awaiting answer. */
    private var wasBackgrounded: Boolean = false

    private var sessionId: Long? = null

    /** Which deck the current session is drawing from. Drives the name-only banner. */
    private var deck: Deck = Deck.GENERAL

    fun startSession(deck: Deck = Deck.GENERAL, limit: Int = SESSION_DEFAULT_LIMIT) {
        this.deck = deck
        viewModelScope.launch {
            _state.value = UiState.Loading
            val now = System.currentTimeMillis()
            val saved = resumeStore.load(deck)
            val resumedCards = saved?.let { repository.fetchCardsByIds(it.cardIds) }.orEmpty()
            val resumeIndex = saved?.nextIndex ?: 0
            val canResume = resumedCards.isNotEmpty() && resumeIndex in resumedCards.indices
            val cards = if (canResume) resumedCards else repository.fetchSession(limit = limit, nowEpochMs = now, deck = deck)
            val startIndex = if (canResume) resumeIndex else 0
            _state.value = if (cards.isEmpty()) {
                resumeStore.clear(deck)
                UiState.Empty(isNameDeck = deck == Deck.NAME)
            } else {
                sessionId = repository.startTrainingSession(nowEpochMs = now)
                rememberResumePosition(cards = cards, nextIndex = startIndex)
                // Start the response timer as the first card is emitted. Otherwise
                // a fast tap before onCardRendered runs leaves cardShownAtMs at 0L,
                // making responseMs null -> the answer is misclassified as ABANDONED
                // and the first card silently skips. onCardRendered refines this.
                cardShownAtMs = System.currentTimeMillis()
                wasBackgrounded = false
                UiState.Reviewing(
                    cards = cards,
                    index = startIndex,
                    shuffledChoices = makeChoices(cards[startIndex]),
                    cardSummary = SessionSummary.empty(),
                    isNameDeck = deck == Deck.NAME,
                )
            }
        }
    }

    /**
     * Called once the card UI is fully rendered. Used as t0 for response timing.
     */
    fun onCardRendered(nowEpochMs: Long = System.currentTimeMillis()) {
        cardShownAtMs = nowEpochMs
        wasBackgrounded = false
    }

    fun onAppPaused() {
        wasBackgrounded = true
    }

    fun onAnswer(choice: String) {
        submit(isCorrectOverride = null, choice = choice, unsure = false, userPick = choice)
    }

    /**
     * Honest "I don't know" ??bypasses 4-choice gambling. Always classifies as LAPSED.
     */
    fun onUnsure() {
        submit(isCorrectOverride = false, choice = null, unsure = true, userPick = null)
    }

    private fun submit(
        isCorrectOverride: Boolean?,
        choice: String?,
        unsure: Boolean,
        userPick: String?,
    ) {
        val now = System.currentTimeMillis()
        val current = _state.value as? UiState.Reviewing ?: return
        // Re-entry guard: a slow DB write must not be re-fired by a second tap.
        if (current.isSubmitting) return
        val card = current.cards.getOrNull(current.index) ?: return

        val responseMs = if (cardShownAtMs > 0L) now - cardShownAtMs else null
        val isCorrect = isCorrectOverride ?: (choice == card.correctReading)
        // Snapshot wasBackgrounded at tap time. Reading it inside the coroutine
        // races with a post-tap onPause and could misclassify a valid answer.
        val backgroundedAtSubmit = wasBackgrounded

        // Mark in-flight before launching so concurrent taps see the guard.
        _state.value = current.copy(isSubmitting = true)

        viewModelScope.launch {
            val outcome = repository.submitReview(
                card = card,
                responseMs = responseMs,
                isCorrect = isCorrect,
                wasBackgrounded = backgroundedAtSubmit,
                nowEpochMs = now,
                wasUnsure = unsure,
            )
            val updatedSummary = current.cardSummary.update(outcome)

            // Finalise session progress on every answer so leaving mid-session
            // still records the turns done so far (and updates the duration).
            sessionId?.let { repository.recordSessionProgress(it, now, updatedSummary.totalAttempts) }
            if (outcome.countsForSrsUpdate) {
                rememberResumePosition(cards = current.cards, nextIndex = current.index + 1)
            }

            // For all counted outcomes ??pause briefly to expose post-answer feedback
            // (?묐떟?쒓컙 + bucket). For LAPSED also reveal correct answer (existing).
            // Screen auto-advances non-LAPSED after 1.5s; LAPSED requires tap.
            // Also show a notice for ABANDONED / BACKGROUNDED so a skipped card
            // explains itself instead of silently advancing. Only a spurious
            // EARLY_TAP falls through to a silent advance.
            val isSkipNotice = outcome == ReviewOutcome.ABANDONED ||
                outcome == ReviewOutcome.BACKGROUNDED
            if (outcome.countsForSrsUpdate || isSkipNotice) {
                val showAnswer = outcome == ReviewOutcome.LAPSED || isSkipNotice
                _state.value = current.copy(
                    cardSummary = updatedSummary,
                    isSubmitting = false,
                    revealedCorrect = if (showAnswer) card.correctReading else null,
                    revealedUserPick = if (outcome == ReviewOutcome.LAPSED) userPick else null,
                    revealedOutcome = outcome,
                    revealedResponseMs = responseMs,
                )
                cardShownAtMs = 0L
                wasBackgrounded = false
                return@launch
            }

            advance(current, updatedSummary)
        }
    }

    /** Called when the user has read the revealed correct answer and taps "?ㅼ쓬". */
    fun onContinueAfterReveal() {
        val current = _state.value as? UiState.Reviewing ?: return
        if (current.revealedOutcome == null) return
        viewModelScope.launch {
            advance(
                current.copy(
                    revealedCorrect = null,
                    revealedUserPick = null,
                    revealedOutcome = null,
                    revealedResponseMs = null,
                ),
                current.cardSummary,
            )
        }
    }

    private suspend fun advance(current: UiState.Reviewing, nextSummary: SessionSummary) {
        val nextIndex = current.index + 1
        val finished = nextIndex >= current.cards.size
        _state.value = if (finished) {
            // Final progress was already persisted by the last answer's
            // recordSessionProgress; just detach so nothing else mutates it.
            resumeStore.clear(deck)
            sessionId = null
            UiState.Finished(nextSummary)
        } else {
            rememberResumePosition(cards = current.cards, nextIndex = nextIndex)
            current.copy(
                index = nextIndex,
                shuffledChoices = makeChoices(current.cards[nextIndex]),
                cardSummary = nextSummary,
                isSubmitting = false,
                revealedCorrect = null,
                revealedUserPick = null,
                revealedOutcome = null,
                revealedResponseMs = null,
            )
        }
        // Start the next card's response timer immediately (refined later by
        // onCardRendered). Never leave it at 0L while a tappable card is shown,
        // or a fast tap would be mis-timed as null -> ABANDONED -> silent skip.
        cardShownAtMs = if (finished) 0L else System.currentTimeMillis()
        wasBackgrounded = false
    }

    fun restart() {
        _state.value = UiState.Idle
        startSession(deck = deck)
    }

    private fun rememberResumePosition(cards: List<CardForReview>, nextIndex: Int) {
        resumeStore.save(deck = deck, cardIds = cards.map { it.cardId }, nextIndex = nextIndex)
    }
    private fun makeChoices(card: CardForReview): List<String> =
        (card.distractors + card.correctReading).shuffled()

    sealed interface UiState {
        data object Idle : UiState
        data object Loading : UiState
        data class Empty(val isNameDeck: Boolean = false) : UiState
        data class Reviewing(
            val cards: List<CardForReview>,
            val index: Int,
            val shuffledChoices: List<String>,
            val cardSummary: SessionSummary,
            /** True when this session is the name-only (雅뷴릫?? deck. Drives the
             *  "?щ엺 ?대쫫 ?꾩슜" banner. */
            val isNameDeck: Boolean = false,
            /** True while a submitted answer is being persisted. Buttons are disabled
             *  in the screen to prevent double-tap duplicates while the coroutine runs. */
            val isSubmitting: Boolean = false,
            /** Non-null when a LAPSED outcome occurred (紐⑤Ⅴ寃좎뼱??OR wrong pick):
             *  holds the correct reading to display as a learning beat before
             *  advancing. SRS already recorded LAPSED; advance gated on [next] tap. */
            val revealedCorrect: String? = null,
            /** What the user actually tapped (when they picked a wrong distractor).
             *  Null for the "紐⑤Ⅴ寃좎뼱?? path. Used to highlight the mistake in reveal. */
            val revealedUserPick: String? = null,
            /** Counted outcome of the just-submitted card. UI shows a brief time
             *  + bucket pill. For non-LAPSED outcomes the screen auto-advances; for
             *  LAPSED the user taps "?ㅼ쓬". Null = normal answering phase. */
            val revealedOutcome: ReviewOutcome? = null,
            val revealedResponseMs: Long? = null,
        ) : UiState
        data class Finished(val summary: SessionSummary) : UiState
    }

    data class SessionSummary(
        val mastered: Int,
        val slow: Int,
        val sluggish: Int,
        val lapsed: Int,
        val noisyAttempts: Int, // EARLY_TAP + BACKGROUNDED + ABANDONED
    ) {
        val totalCounted: Int get() = mastered + slow + sluggish + lapsed
        val totalAttempts: Int get() = totalCounted + noisyAttempts

        fun update(outcome: ReviewOutcome): SessionSummary = when (outcome) {
            ReviewOutcome.MASTERED -> copy(mastered = mastered + 1)
            ReviewOutcome.SLOW -> copy(slow = slow + 1)
            ReviewOutcome.SLUGGISH -> copy(sluggish = sluggish + 1)
            ReviewOutcome.LAPSED -> copy(lapsed = lapsed + 1)
            ReviewOutcome.EARLY_TAP,
            ReviewOutcome.ABANDONED,
            ReviewOutcome.BACKGROUNDED -> copy(noisyAttempts = noisyAttempts + 1)
        }

        companion object {
            fun empty() = SessionSummary(0, 0, 0, 0, 0)
        }
    }

    companion object {
        const val SESSION_DEFAULT_LIMIT = 20
    }
}


