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
}
