package com.langtrainer.core.model

enum class ReviewOutcome {
    MASTERED,
    SLOW,
    SLUGGISH,
    LAPSED,
    ABANDONED,
    BACKGROUNDED,

    /**
     * Tap landed before [SrsConfig.ignoreFirstMs] elapsed from card render.
     * Treated as a misfire (state unchanged) but logged distinctly so debug stats
     * don't conflate it with "app went to background".
     */
    EARLY_TAP;

    val countsForSrsUpdate: Boolean
        get() = this == MASTERED || this == SLOW || this == SLUGGISH || this == LAPSED

    val isCorrect: Boolean?
        get() = when (this) {
            MASTERED, SLOW, SLUGGISH -> true
            LAPSED -> false
            ABANDONED, BACKGROUNDED, EARLY_TAP -> null
        }
}
