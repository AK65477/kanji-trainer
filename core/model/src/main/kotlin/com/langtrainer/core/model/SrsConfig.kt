package com.langtrainer.core.model

/**
 * SRS tuning constants. Exposed as a value object so settings UI can later override them
 * without changing core logic.
 *
 * Defaults reflect the values agreed in DESIGN.md §5.
 */
data class SrsConfig(
    val masteredMaxResponseMs: Long = DEFAULT_MASTERED_MS,
    val slowMaxResponseMs: Long = DEFAULT_SLOW_MS,
    val abandonAfterMs: Long = DEFAULT_ABANDON_MS,
    val ignoreFirstMs: Long = DEFAULT_IGNORE_FIRST_MS,
    val initialEaseFactor: Double = INITIAL_EASE_FACTOR,
    val minEaseFactor: Double = MIN_EASE_FACTOR,
    val lapseEasePenalty: Double = LAPSE_EASE_PENALTY,
    val slowIntervalMultiplier: Double = SLOW_INTERVAL_MULTIPLIER,
    val sluggishIntervalMultiplier: Double = SLUGGISH_INTERVAL_MULTIPLIER,
    val qualifyingStreak: Int = QUALIFYING_STREAK,
    val masteryMinCards: Int = MASTERY_MIN_CARDS,
) {
    companion object {
        const val DEFAULT_MASTERED_MS = 2000L
        const val DEFAULT_SLOW_MS = 4000L
        // A real tap is always a real answer, so this only needs to catch a card
        // the user genuinely walked away from. 5s was far too eager (deliberate
        // reading easily exceeds it), making slow taps silently skip as ABANDONED.
        const val DEFAULT_ABANDON_MS = 30000L
        const val DEFAULT_IGNORE_FIRST_MS = 100L

        const val INITIAL_EASE_FACTOR = 2.5
        const val MIN_EASE_FACTOR = 1.3
        const val LAPSE_EASE_PENALTY = 0.15

        const val SLOW_INTERVAL_MULTIPLIER = 1.2
        const val SLUGGISH_INTERVAL_MULTIPLIER = 1.0

        /** consecutive_mastered required for a card to count toward KanjiMastery. */
        const val QUALIFYING_STREAK = 3

        /** A kanji is mastered when this many of its cards are qualifying. */
        const val MASTERY_MIN_CARDS = 2

        val Default: SrsConfig = SrsConfig()
    }
}
