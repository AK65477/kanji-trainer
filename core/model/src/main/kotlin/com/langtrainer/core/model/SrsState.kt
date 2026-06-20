package com.langtrainer.core.model

/**
 * Card-level SRS state. Pure value object — no DB/Android dependencies.
 *
 * Mirrors the SrsState Room entity but lives in :core:model so :core:srs can stay
 * Android-free.
 */
data class SrsState(
    val cardId: Long,
    val bucket: Bucket,
    val consecutiveMastered: Int,
    val nextDueAtEpochMs: Long,
    val intervalDays: Int,
    val easeFactor: Double,
) {
    /**
     * Qualifying = consecutive Mastered streak has reached [threshold].
     * Threshold-injected so callers honor [SrsConfig.qualifyingStreak] overrides
     * instead of being locked to the compile-time constant.
     */
    fun isQualifying(threshold: Int): Boolean = consecutiveMastered >= threshold

    companion object {
        fun newCard(cardId: Long, nowEpochMs: Long): SrsState = SrsState(
            cardId = cardId,
            bucket = Bucket.NEW,
            consecutiveMastered = 0,
            nextDueAtEpochMs = nowEpochMs,
            intervalDays = 0,
            easeFactor = SrsConfig.INITIAL_EASE_FACTOR,
        )
    }
}
