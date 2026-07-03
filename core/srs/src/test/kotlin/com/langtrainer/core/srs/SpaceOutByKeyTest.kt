package com.langtrainer.core.srs

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SpaceOutByKeyTest {

    private fun <T> hasAdjacentDuplicate(items: List<T>, keyOf: (T) -> Any?): Boolean =
        items.zipWithNext().any { (a, b) -> keyOf(a) == keyOf(b) }

    @Test
    fun `separates an adjacent same-key pair`() {
        val input = listOf("a1", "a2", "b", "c")
        val out = SrsSessionPlanner.spaceOutByKey(input) { it.first() }
        assertThat(hasAdjacentDuplicate(out) { it.first() }).isFalse()
    }

    @Test
    fun `preserves the exact multiset of items`() {
        val input = listOf("a1", "a2", "b", "c", "a3", "b2")
        val out = SrsSessionPlanner.spaceOutByKey(input) { it.first() }
        assertThat(out.sorted()).isEqualTo(input.sorted())
    }

    @Test
    fun `leaves an already-spread list unchanged`() {
        val input = listOf("a", "b", "c", "d")
        val out = SrsSessionPlanner.spaceOutByKey(input) { it }
        assertThat(out).isEqualTo(input)
    }

    @Test
    fun `keeps original order as much as possible`() {
        // a a b c  ->  a b a c : the second 'a' is pushed just past 'b', nothing more.
        val out = SrsSessionPlanner.spaceOutByKey(listOf("a1", "a2", "b", "c")) { it.first() }
        assertThat(out).isEqualTo(listOf("a1", "b", "a2", "c"))
    }

    @Test
    fun `returns lists of size two unchanged even if duplicated`() {
        val input = listOf("a1", "a2")
        assertThat(SrsSessionPlanner.spaceOutByKey(input) { it.first() }).isEqualTo(input)
    }

    @Test
    fun `keeps unavoidable duplicates instead of dropping them`() {
        val input = listOf("a1", "a2", "a3")
        val out = SrsSessionPlanner.spaceOutByKey(input) { it.first() }
        assertThat(out).containsExactlyElementsIn(input)
        assertThat(out).hasSize(3)
    }

    @Test
    fun `handles empty and single-element lists`() {
        assertThat(SrsSessionPlanner.spaceOutByKey(emptyList<String>()) { it }).isEmpty()
        assertThat(SrsSessionPlanner.spaceOutByKey(listOf("x")) { it }).containsExactly("x")
    }
}
