package com.langtrainer.core.model

/**
 * The full output of [SrsEngine.apply].
 *
 * - [outcome]/[responseMs] are echoed for persistence into ReviewLog.
 * - [newState] is the post-attempt SrsState; the caller writes it back.
 * - [kanjiMasteryDeltas] is a map of kanjiId → qualifying-card-count delta (+1, 0, -1)
 *   that the caller applies to KanjiMastery rows. The recompute of is_mastered uses
 *   [SrsConfig.masteryMinCards].
 */
data class ReviewResult(
    val outcome: ReviewOutcome,
    val responseMs: Long?,
    val newState: SrsState,
    val kanjiMasteryDeltas: Map<Long, Int>,
)
