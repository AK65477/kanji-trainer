package com.langtrainer.core.srs

import kotlin.math.max
import kotlin.math.min

/**
 * Pure, storage-agnostic session composer.
 *
 * Fixes the "reviews never resurface" problem: every brand-new card is stamped
 * with the import time, the oldest timestamp in the system, so a naive
 * `ORDER BY next_due_at` buries due reviews behind the entire unreviewed new-card
 * backlog. Here we compose a session **reviews-first**, then fill any remaining
 * slots with new cards.
 *
 * Properties:
 *   - Due reviews always take priority, so a card the learner just failed
 *     reappears as soon as it falls due instead of waiting for new cards to run out.
 *   - New intake self-throttles: new cards only occupy slots left over after due
 *     reviews, and are additionally capped by [maxNewCards] so one long session
 *     cannot pile up an unmanageable review load for the next day.
 *
 * Inputs are assumed pre-sorted by the caller (reviews by due time ascending, new
 * cards in introduction order). This function does no sorting; it only slices and
 * concatenates, which keeps it trivially testable without a database.
 */
object SrsSessionPlanner {

    fun <T> plan(
        dueReviews: List<T>,
        newCards: List<T>,
        sessionLimit: Int,
        maxNewCards: Int,
    ): List<T> {
        if (sessionLimit <= 0) return emptyList()

        val reviews = dueReviews.take(sessionLimit)
        val remainingSlots = sessionLimit - reviews.size
        val newAllowed = min(remainingSlots, max(0, maxNewCards))
        return reviews + newCards.take(newAllowed)
    }

    /**
     * Reorders [items] so no two adjacent items share the same [keyOf] value when it
     * can be avoided, while preserving the original order as much as possible.
     *
     * The seed groups several cards for the same target word at consecutive ids, so
     * a session composed in id order can show the same word back-to-back — trivially
     * easy because the answer is still in short-term memory. This spreads such
     * duplicates apart (greedy: always emit the earliest remaining item whose key
     * differs from the one just emitted; fall back to the earliest when every
     * remaining item repeats it, i.e. the collision is unavoidable).
     */
    fun <T, K> spaceOutByKey(items: List<T>, keyOf: (T) -> K): List<T> {
        if (items.size <= 2) return items
        val remaining = ArrayDeque(items)
        val result = ArrayList<T>(items.size)
        var lastKey: K? = null
        var hasLast = false
        while (remaining.isNotEmpty()) {
            val idx = remaining.indexOfFirst { !hasLast || keyOf(it) != lastKey }
            val pickIdx = if (idx >= 0) idx else 0
            val picked = remaining.removeAt(pickIdx)
            result.add(picked)
            lastKey = keyOf(picked)
            hasLast = true
        }
        return result
    }
}
