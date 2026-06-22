package com.langtrainer.core.srs

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SrsSessionPlannerTest {

    private fun reviews(n: Int): List<String> = (1..n).map { "r$it" }
    private fun news(n: Int): List<String> = (1..n).map { "n$it" }

    @Test
    fun `due reviews are served before any new card`() {
        val plan = SrsSessionPlanner.plan(
            dueReviews = reviews(3),
            newCards = news(5),
            sessionLimit = 20,
            maxNewCards = 10,
        )

        // Reviews come first, then new cards fill the rest.
        assertThat(plan.take(3)).containsExactly("r1", "r2", "r3").inOrder()
        assertThat(plan.drop(3)).containsExactly("n1", "n2", "n3", "n4", "n5").inOrder()
    }

    @Test
    fun `new cards fill only the slots left over after reviews`() {
        val plan = SrsSessionPlanner.plan(
            dueReviews = reviews(8),
            newCards = news(10),
            sessionLimit = 12,
            maxNewCards = 10,
        )

        // 8 reviews + 4 new = 12 (session limit), even though more new were available.
        assertThat(plan).hasSize(12)
        assertThat(plan.count { it.startsWith("r") }).isEqualTo(8)
        assertThat(plan.count { it.startsWith("n") }).isEqualTo(4)
    }

    @Test
    fun `reviews alone can fill the whole session and starve new cards`() {
        val plan = SrsSessionPlanner.plan(
            dueReviews = reviews(30),
            newCards = news(10),
            sessionLimit = 20,
            maxNewCards = 10,
        )

        // When the learner is behind on reviews, new cards correctly wait.
        assertThat(plan).hasSize(20)
        assertThat(plan.all { it.startsWith("r") }).isTrue()
    }

    @Test
    fun `new intake is capped even when many slots are free`() {
        val plan = SrsSessionPlanner.plan(
            dueReviews = emptyList(),
            newCards = news(50),
            sessionLimit = 20,
            maxNewCards = 10,
        )

        // No reviews, but new cards are bounded by maxNewCards, not the session limit.
        assertThat(plan).hasSize(10)
        assertThat(plan).isEqualTo(news(10))
    }

    @Test
    fun `empty inputs yield an empty session`() {
        assertThat(
            SrsSessionPlanner.plan(emptyList<String>(), emptyList(), sessionLimit = 20, maxNewCards = 10),
        ).isEmpty()
    }

    @Test
    fun `non-positive session limit yields nothing`() {
        assertThat(
            SrsSessionPlanner.plan(reviews(5), news(5), sessionLimit = 0, maxNewCards = 10),
        ).isEmpty()
    }
}
