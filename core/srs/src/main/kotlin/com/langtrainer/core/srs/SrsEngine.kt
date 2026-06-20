package com.langtrainer.core.srs

import com.langtrainer.core.model.Bucket
import com.langtrainer.core.model.ReviewAttempt
import com.langtrainer.core.model.ReviewOutcome
import com.langtrainer.core.model.ReviewResult
import com.langtrainer.core.model.SrsConfig
import com.langtrainer.core.model.SrsState
import kotlin.math.max

/**
 * Pure-function SRS classifier and updater.
 *
 * Single entry point: [apply]. Given the current [SrsState] and a [ReviewAttempt],
 * returns a [ReviewResult] containing:
 *   - the classified [ReviewOutcome]
 *   - the post-attempt [SrsState] (caller writes back)
 *   - per-kanji deltas for KanjiMastery aggregation
 *
 * Algorithm reflects DESIGN.md 짠5 (v3.1).
 *
 * This class is stateless. Inject [SrsConfig] for tunable thresholds; the default
 * pulls the constants from [SrsConfig.Default].
 */
class SrsEngine(
    private val config: SrsConfig = SrsConfig.Default,
) {

    fun apply(state: SrsState, attempt: ReviewAttempt): ReviewResult {
        val outcome = classify(attempt)

        // Non-counting outcomes: state unchanged, no kanji mastery delta.
        if (!outcome.countsForSrsUpdate) {
            return ReviewResult(
                outcome = outcome,
                responseMs = attempt.responseMs,
                newState = state,
                kanjiMasteryDeltas = emptyMap(),
            )
        }

        val wasQualifyingBefore = state.isQualifying(config.qualifyingStreak)
        val newState = updateState(state, outcome, attempt.nowEpochMs)
        val isQualifyingAfter = newState.isQualifying(config.qualifyingStreak)

        val deltas: Map<Long, Int> = when {
            !wasQualifyingBefore && isQualifyingAfter ->
                attempt.linkedKanjiIds.associateWith { +1 }
            wasQualifyingBefore && !isQualifyingAfter ->
                attempt.linkedKanjiIds.associateWith { -1 }
            else -> emptyMap()
        }

        return ReviewResult(
            outcome = outcome,
            responseMs = attempt.responseMs,
            newState = newState,
            kanjiMasteryDeltas = deltas,
        )
    }

    private fun classify(attempt: ReviewAttempt): ReviewOutcome {
        // Honest "I don't know" trumps everything else ??treat as LAPSED so the
        // card returns soon and ease factor decreases. Stops blind-guess false
        // positives from corrupting Mastered KPI.
        if (attempt.wasUnsure) return ReviewOutcome.LAPSED

        // App was sent to background / screen locked during a pending review.
        if (attempt.wasBackgrounded) return ReviewOutcome.BACKGROUNDED

        // No tap within the abandon window.
        val ms = attempt.responseMs
        if (ms == null || ms >= config.abandonAfterMs) return ReviewOutcome.ABANDONED

        val correct = attempt.isCorrect
            ?: return ReviewOutcome.ABANDONED // null isCorrect with a response is a contract violation; treat as no-op

        if (!correct) return ReviewOutcome.LAPSED

        // Spurious early tap (< ignoreFirstMs after card appeared). Keep this only
        // for correct taps; a wrong tap is still a real lapse that should reveal
        // the answer instead of silently advancing.
        if (ms < config.ignoreFirstMs) return ReviewOutcome.EARLY_TAP

        return when {
            ms < config.masteredMaxResponseMs -> ReviewOutcome.MASTERED
            ms < config.slowMaxResponseMs -> ReviewOutcome.SLOW
            else -> ReviewOutcome.SLUGGISH
        }
    }

    private fun updateState(
        state: SrsState,
        outcome: ReviewOutcome,
        nowEpochMs: Long,
    ): SrsState {
        val isNew = state.bucket == Bucket.NEW

        val (nextIntervalDays, nextEase, nextStreak) = when (outcome) {
            ReviewOutcome.MASTERED -> {
                val interval = if (isNew) 1 else max(1, (state.intervalDays * state.easeFactor).roundToIntHalfUp())
                Triple(interval, state.easeFactor, state.consecutiveMastered + 1)
            }
            ReviewOutcome.SLOW -> {
                val interval = if (isNew) 1 else max(1, (state.intervalDays * config.slowIntervalMultiplier).roundToIntHalfUp())
                Triple(interval, state.easeFactor, 0)
            }
            ReviewOutcome.SLUGGISH -> {
                val interval = if (isNew) 0 else max(1, (state.intervalDays * config.sluggishIntervalMultiplier).roundToIntHalfUp())
                Triple(interval, state.easeFactor, 0)
            }
            ReviewOutcome.LAPSED -> {
                val ease = max(config.minEaseFactor, state.easeFactor - config.lapseEasePenalty)
                Triple(1, ease, 0)
            }
            else -> error("Non-counting outcome reached updateState: $outcome")
        }

        return state.copy(
            bucket = outcome.toBucket(),
            consecutiveMastered = nextStreak,
            nextDueAtEpochMs = nowEpochMs + nextIntervalDays.toLong() * MILLIS_PER_DAY,
            intervalDays = nextIntervalDays,
            easeFactor = nextEase,
        )
    }

    private fun ReviewOutcome.toBucket(): Bucket = when (this) {
        ReviewOutcome.MASTERED -> Bucket.MASTERED
        ReviewOutcome.SLOW -> Bucket.SLOW
        ReviewOutcome.SLUGGISH -> Bucket.SLUGGISH
        ReviewOutcome.LAPSED -> Bucket.LAPSED
        else -> error("Non-counting outcome has no Bucket: $this")
    }

    private fun Double.roundToIntHalfUp(): Int =
        kotlin.math.floor(this + 0.5).toInt()

    companion object {
        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
    }
}

