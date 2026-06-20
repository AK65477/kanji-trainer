package com.langtrainer.core.model

/**
 * One review attempt as captured by the UI, before SRS classification.
 *
 * - [responseMs] is null when the attempt was abandoned or backgrounded
 *   (in that case [isCorrect] must also be null).
 * - [linkedKanjiIds] is the set of kanji IDs reached via CardKanjiLink for the
 *   reviewed card. Passed in so [SrsEngine] can compute KanjiMastery deltas without
 *   reaching back into the database.
 */
data class ReviewAttempt(
    val cardId: Long,
    val linkedKanjiIds: Set<Long>,
    val isCorrect: Boolean?,
    val responseMs: Long?,
    val wasBackgrounded: Boolean,
    val nowEpochMs: Long,
    /**
     * User explicitly tapped "모르겠어요" — bypass response-time/correctness
     * classification and treat as LAPSED. Protects KPI integrity from the
     * 4-choice false-positive rate (25% on blind guesses).
     */
    val wasUnsure: Boolean = false,
)
