package com.langtrainer.feature.kanjisrs

import com.langtrainer.core.database.dao.SessionDao
import com.langtrainer.core.database.dao.SentenceCardDao
import com.langtrainer.core.database.dao.SrsDao
import com.langtrainer.core.database.entity.ReviewLogEntity
import com.langtrainer.core.database.entity.SessionEntity
import com.langtrainer.core.database.entity.SrsStateEntity
import com.langtrainer.core.model.Bucket
import com.langtrainer.core.model.ReviewAttempt
import com.langtrainer.core.model.ReviewOutcome
import com.langtrainer.core.model.SessionCategory
import com.langtrainer.core.model.SrsConfig
import com.langtrainer.core.model.SrsState
import com.langtrainer.core.srs.SrsEngine
import com.langtrainer.core.srs.SrsSessionPlanner
import javax.inject.Inject
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class KanjiSrsRepository @Inject constructor(
    private val srsDao: SrsDao,
    private val cardDao: SentenceCardDao,
    private val sessionDao: SessionDao,
    private val srsConfig: SrsConfig,
    private val srsEngine: SrsEngine,
) {

    /**
     * Builds one study session **reviews-first**: due reviews are served before any
     * brand-new card, then new cards fill the remaining slots (capped by
     * [SrsConfig.maxNewCardsPerSession]). This is what lets a just-failed card
     * resurface on its due date instead of waiting behind the whole new-card
     * backlog. See [SrsSessionPlanner].
     */
    suspend fun fetchSession(
        limit: Int,
        nowEpochMs: Long,
        deck: Deck = Deck.GENERAL,
    ): List<CardForReview> {
        val maxNew = srsConfig.maxNewCardsPerSession
        val dueReviews = when (deck) {
            Deck.GENERAL -> srsDao.getDueReviewsGeneral(nowEpochMs = nowEpochMs, limit = limit)
            Deck.NAME -> srsDao.getDueReviewsNameOnly(nowEpochMs = nowEpochMs, limit = limit)
        }
        val newCards = when (deck) {
            Deck.GENERAL -> srsDao.getNewCardsGeneral(limit = maxNew)
            Deck.NAME -> srsDao.getNewCardsNameOnly(limit = maxNew)
        }
        val planned = SrsSessionPlanner.plan(
            dueReviews = dueReviews,
            newCards = newCards,
            sessionLimit = limit,
            maxNewCards = maxNew,
        ).mapNotNull { state -> state.toCardForReview() }
        // The seed groups some same-word cards at consecutive ids, so an id-ordered
        // session can show the same target word back-to-back — trivially easy. Spread
        // duplicates apart while keeping the reviews-first composition.
        return SrsSessionPlanner.spaceOutByKey(planned) { it.targetText }
    }

    suspend fun fetchCardsByIds(cardIds: List<Long>): List<CardForReview> =
        cardIds.mapNotNull { cardId ->
            val state = srsDao.getState(cardId) ?: return@mapNotNull null
            state.toCardForReview()
        }

    suspend fun submitReview(
        card: CardForReview,
        responseMs: Long?,
        isCorrect: Boolean?,
        wasBackgrounded: Boolean,
        nowEpochMs: Long,
        wasUnsure: Boolean = false,
    ): ReviewOutcome {
        val attempt = ReviewAttempt(
            cardId = card.cardId,
            linkedKanjiIds = card.linkedKanjiIds,
            isCorrect = isCorrect,
            responseMs = responseMs,
            wasBackgrounded = wasBackgrounded,
            nowEpochMs = nowEpochMs,
            wasUnsure = wasUnsure,
        )
        val result = srsEngine.apply(card.state, attempt)

        srsDao.applyReview(
            stateEntity = result.newState.toEntity(),
            log = ReviewLogEntity(
                cardId = card.cardId,
                shownAtEpochMs = nowEpochMs,
                outcome = result.outcome,
                responseMs = result.responseMs,
                isCorrect = isCorrect,
            ),
            kanjiDeltas = result.kanjiMasteryDeltas,
            masteryMinCards = srsConfig.masteryMinCards,
            nowEpochMs = nowEpochMs,
        )
        return result.outcome
    }

    suspend fun startTrainingSession(nowEpochMs: Long): Long {
        // Discard any session left open by a previous abrupt exit (e.g. process
        // death before the first answer). Otherwise a stale open row could
        // accumulate; it also used to permanently block new session tracking.
        sessionDao.getOpenSession(SessionCategory.KANJI_SRS)?.let { sessionDao.delete(it) }
        return sessionDao.insert(
            SessionEntity(
                category = SessionCategory.KANJI_SRS,
                startedAtEpochMs = nowEpochMs,
                endedAtEpochMs = null,
                turnsCompleted = null,
            ),
        )
    }

    /**
     * Finalises the session's progress after each answered card. Writing
     * [nowEpochMs] to ended_at on every turn means the row always reflects the
     * latest state, so leaving the screen at any point preserves partial credit
     * without relying on a fragile teardown callback.
     */
    suspend fun recordSessionProgress(sessionId: Long, nowEpochMs: Long, turnsCompleted: Int) {
        val current = sessionDao.getById(sessionId) ?: return
        sessionDao.update(
            current.copy(
                endedAtEpochMs = nowEpochMs,
                turnsCompleted = turnsCompleted,
            ),
        )
    }

    private suspend fun SrsStateEntity.toCardForReview(): CardForReview? {
        val card = cardDao.getById(cardId) ?: return null
        val linked = cardDao.getLinkedKanjiIds(cardId).toSet()
        val distractors: List<String> = Json.decodeFromString(
            ListSerializer(String.serializer()), card.distractorsJson,
        )
        return CardForReview(
            state = toDomain(),
            cardId = card.id,
            sentenceJp = card.sentenceJp,
            targetText = card.targetText,
            targetStart = card.targetStart,
            targetEnd = card.targetEnd,
            correctReading = card.correctReading,
            distractors = distractors,
            translationEn = card.translationEn,
            translationKo = card.translationKo,
            linkedKanjiIds = linked,
        )
    }
    private fun SrsStateEntity.toDomain(): SrsState = SrsState(
        cardId = cardId,
        bucket = currentBucket,
        consecutiveMastered = consecutiveMastered,
        nextDueAtEpochMs = nextDueAtEpochMs,
        intervalDays = intervalDays,
        easeFactor = easeFactor,
    )

    private fun SrsState.toEntity(): SrsStateEntity = SrsStateEntity(
        cardId = cardId,
        currentBucket = bucket,
        consecutiveMastered = consecutiveMastered,
        nextDueAtEpochMs = nextDueAtEpochMs,
        intervalDays = intervalDays,
        easeFactor = easeFactor,
    )
}

/**
 * Which study track a review session draws from.
 *
 * [GENERAL] — the JLPT/common-kanji deck (excludes name-only kanji).
 * [NAME] — the 人名用 deck: only kanji that appear almost solely in personal names.
 */
enum class Deck { GENERAL, NAME }

data class CardForReview(
    val state: SrsState,
    val cardId: Long,
    val sentenceJp: String,
    val targetText: String,
    val targetStart: Int,
    val targetEnd: Int,
    val correctReading: String,
    val distractors: List<String>,
    val translationEn: String?,
    val translationKo: String?,
    val linkedKanjiIds: Set<Long>,
)



