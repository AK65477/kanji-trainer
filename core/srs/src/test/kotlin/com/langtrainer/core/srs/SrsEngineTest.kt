package com.langtrainer.core.srs

import com.google.common.truth.Truth.assertThat
import com.langtrainer.core.model.Bucket
import com.langtrainer.core.model.ReviewAttempt
import com.langtrainer.core.model.ReviewOutcome
import com.langtrainer.core.model.SrsConfig
import com.langtrainer.core.model.SrsState
import org.junit.Test

class SrsEngineTest {

    private val engine = SrsEngine()
    private val now = 1_700_000_000_000L
    private val msPerDay = 24L * 60L * 60L * 1000L

    @Test
    fun `new card mastered fast moves to 1 day interval and bucket Mastered`() {
        val state = SrsState.newCard(cardId = 1L, nowEpochMs = now)
        val attempt = attempt(responseMs = 1200, correct = true)

        val r = engine.apply(state, attempt)

        assertThat(r.outcome).isEqualTo(ReviewOutcome.MASTERED)
        assertThat(r.newState.bucket).isEqualTo(Bucket.MASTERED)
        assertThat(r.newState.intervalDays).isEqualTo(1)
        assertThat(r.newState.consecutiveMastered).isEqualTo(1)
        assertThat(r.newState.easeFactor).isEqualTo(SrsConfig.INITIAL_EASE_FACTOR)
        assertThat(r.newState.nextDueAtEpochMs).isEqualTo(now + msPerDay)
        // First mastered, not yet qualifying (needs 3 consecutive)
        assertThat(r.kanjiMasteryDeltas).isEmpty()
    }

    @Test
    fun `third consecutive mastered triggers kanji mastery delta`() {
        var state = SrsState.newCard(cardId = 1L, nowEpochMs = now)
        repeat(2) {
            state = engine.apply(state, attempt(responseMs = 1000, correct = true)).newState
        }
        assertThat(state.consecutiveMastered).isEqualTo(2)
        assertThat(state.isQualifying(SrsConfig.QUALIFYING_STREAK)).isFalse()

        val r = engine.apply(state, attempt(responseMs = 800, correct = true))

        assertThat(r.outcome).isEqualTo(ReviewOutcome.MASTERED)
        assertThat(r.newState.consecutiveMastered).isEqualTo(3)
        assertThat(r.newState.isQualifying(SrsConfig.QUALIFYING_STREAK)).isTrue()
        assertThat(r.kanjiMasteryDeltas).containsExactly(10L, 1, 20L, 1)
    }

    @Test
    fun `lapse from qualifying decrements all linked kanji`() {
        var state = SrsState.newCard(cardId = 1L, nowEpochMs = now)
        repeat(3) {
            state = engine.apply(state, attempt(responseMs = 1000, correct = true)).newState
        }
        assertThat(state.isQualifying(SrsConfig.QUALIFYING_STREAK)).isTrue()

        val r = engine.apply(state, attempt(responseMs = 1200, correct = false))

        assertThat(r.outcome).isEqualTo(ReviewOutcome.LAPSED)
        assertThat(r.newState.bucket).isEqualTo(Bucket.LAPSED)
        assertThat(r.newState.consecutiveMastered).isEqualTo(0)
        assertThat(r.newState.intervalDays).isEqualTo(1)
        assertThat(r.newState.easeFactor)
            .isEqualTo(SrsConfig.INITIAL_EASE_FACTOR - SrsConfig.LAPSE_EASE_PENALTY)
        assertThat(r.kanjiMasteryDeltas).containsExactly(10L, -1, 20L, -1)
    }

    @Test
    fun `slow response keeps ease factor but resets streak`() {
        var state = SrsState.newCard(cardId = 1L, nowEpochMs = now)
        state = engine.apply(state, attempt(responseMs = 800, correct = true)).newState
        assertThat(state.consecutiveMastered).isEqualTo(1)

        val r = engine.apply(state, attempt(responseMs = 2500, correct = true))

        assertThat(r.outcome).isEqualTo(ReviewOutcome.SLOW)
        assertThat(r.newState.bucket).isEqualTo(Bucket.SLOW)
        assertThat(r.newState.consecutiveMastered).isEqualTo(0)
        assertThat(r.newState.easeFactor).isEqualTo(SrsConfig.INITIAL_EASE_FACTOR)
        // interval: state.intervalDays was 1, * 1.2 = 1.2 ??rounds to 1
        assertThat(r.newState.intervalDays).isEqualTo(1)
    }

    @Test
    fun `sluggish on new card keeps it in same session (interval 0)`() {
        val state = SrsState.newCard(cardId = 1L, nowEpochMs = now)

        val r = engine.apply(state, attempt(responseMs = 4500, correct = true))

        assertThat(r.outcome).isEqualTo(ReviewOutcome.SLUGGISH)
        assertThat(r.newState.bucket).isEqualTo(Bucket.SLUGGISH)
        assertThat(r.newState.intervalDays).isEqualTo(0)
        assertThat(r.newState.nextDueAtEpochMs).isEqualTo(now)
    }

    @Test
    fun `abandoned attempt leaves state unchanged`() {
        val state = SrsState.newCard(cardId = 1L, nowEpochMs = now)

        val r = engine.apply(state, attempt(responseMs = null, correct = null))

        assertThat(r.outcome).isEqualTo(ReviewOutcome.ABANDONED)
        assertThat(r.newState).isEqualTo(state)
        assertThat(r.kanjiMasteryDeltas).isEmpty()
    }

    @Test
    fun `backgrounded attempt leaves state unchanged`() {
        val state = SrsState.newCard(cardId = 1L, nowEpochMs = now)

        val r = engine.apply(state, attempt(responseMs = 2000, correct = true, backgrounded = true))

        assertThat(r.outcome).isEqualTo(ReviewOutcome.BACKGROUNDED)
        assertThat(r.newState).isEqualTo(state)
    }

    @Test
    fun `response above abandon threshold is abandoned`() {
        val state = SrsState.newCard(cardId = 1L, nowEpochMs = now)

        val r = engine.apply(state, attempt(responseMs = 35000, correct = true))

        assertThat(r.outcome).isEqualTo(ReviewOutcome.ABANDONED)
        assertThat(r.newState).isEqualTo(state)
    }

    @Test
    fun `response under 100ms is classified as EARLY_TAP`() {
        val state = SrsState.newCard(cardId = 1L, nowEpochMs = now)

        val r = engine.apply(state, attempt(responseMs = 50, correct = true))

        assertThat(r.outcome).isEqualTo(ReviewOutcome.EARLY_TAP)
        assertThat(r.newState).isEqualTo(state)
        assertThat(r.kanjiMasteryDeltas).isEmpty()
    }
    @Test
    fun `wrong answer under 100ms is still lapsed`() {
        val state = SrsState.newCard(cardId = 1L, nowEpochMs = now)

        val r = engine.apply(state, attempt(responseMs = 50, correct = false))

        assertThat(r.outcome).isEqualTo(ReviewOutcome.LAPSED)
        assertThat(r.newState.bucket).isEqualTo(Bucket.LAPSED)
    }

    @Test
    fun `qualifyingStreak override moves mastery threshold (config-driven)`() {
        // Override: a card qualifies after just 2 consecutive Mastered.
        val tunedEngine = SrsEngine(SrsConfig(qualifyingStreak = 2))
        var state = SrsState.newCard(cardId = 1L, nowEpochMs = now)

        // First Mastered: not yet qualifying.
        val first = tunedEngine.apply(state, attempt(responseMs = 800, correct = true))
        state = first.newState
        assertThat(first.kanjiMasteryDeltas).isEmpty()

        // Second Mastered: now qualifying under override; default engine would still wait.
        val second = tunedEngine.apply(state, attempt(responseMs = 800, correct = true))
        assertThat(second.newState.consecutiveMastered).isEqualTo(2)
        assertThat(second.kanjiMasteryDeltas).containsExactly(10L, 1, 20L, 1)
        assertThat(second.newState.isQualifying(threshold = 2)).isTrue()
        assertThat(second.newState.isQualifying(threshold = 3)).isFalse()
    }

    @Test
    fun `ease factor floor is respected after many lapses`() {
        var state = SrsState(
            cardId = 1L, bucket = Bucket.MASTERED, consecutiveMastered = 0,
            nextDueAtEpochMs = now, intervalDays = 5,
            easeFactor = SrsConfig.MIN_EASE_FACTOR + 0.05,
        )

        // Two lapses in a row ??should clamp at MIN_EASE_FACTOR.
        repeat(2) {
            state = engine.apply(state, attempt(responseMs = 1000, correct = false)).newState
        }

        assertThat(state.easeFactor).isEqualTo(SrsConfig.MIN_EASE_FACTOR)
    }

    @Test
    fun `mastered card interval grows by ease factor`() {
        var state = SrsState(
            cardId = 1L, bucket = Bucket.MASTERED, consecutiveMastered = 3,
            nextDueAtEpochMs = now, intervalDays = 4,
            easeFactor = SrsConfig.INITIAL_EASE_FACTOR,
        )

        val r = engine.apply(state, attempt(responseMs = 900, correct = true))

        // 4 * 2.5 = 10
        assertThat(r.newState.intervalDays).isEqualTo(10)
        assertThat(r.newState.consecutiveMastered).isEqualTo(4)
        // Already qualifying before AND after ??no delta
        assertThat(r.kanjiMasteryDeltas).isEmpty()
    }

    @Test
    fun `qualifying card that slows down un-qualifies and decrements kanji`() {
        val state = SrsState(
            cardId = 1L, bucket = Bucket.MASTERED, consecutiveMastered = 5,
            nextDueAtEpochMs = now, intervalDays = 10,
            easeFactor = SrsConfig.INITIAL_EASE_FACTOR,
        )

        val r = engine.apply(state, attempt(responseMs = 2500, correct = true))

        assertThat(r.outcome).isEqualTo(ReviewOutcome.SLOW)
        assertThat(r.newState.isQualifying(SrsConfig.QUALIFYING_STREAK)).isFalse()
        assertThat(r.kanjiMasteryDeltas).containsExactly(10L, -1, 20L, -1)
    }

    private fun attempt(
        responseMs: Long?,
        correct: Boolean?,
        backgrounded: Boolean = false,
        linkedKanjiIds: Set<Long> = setOf(10L, 20L),
    ): ReviewAttempt = ReviewAttempt(
        cardId = 1L,
        linkedKanjiIds = linkedKanjiIds,
        isCorrect = correct,
        responseMs = responseMs,
        wasBackgrounded = backgrounded,
        nowEpochMs = now,
    )
}

