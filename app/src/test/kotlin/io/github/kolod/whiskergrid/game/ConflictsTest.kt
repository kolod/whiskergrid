package io.github.kolod.whiskergrid.game

import org.junit.Assert.assertEquals
import org.junit.Test

class ConflictsTest {

    // 5×5, one region per row except that cells (0,4) and (4,0) swap regions:
    // row 0: 0 0 0 0 4
    // row 4: 0 4 4 4 4
    private val puzzle = Puzzle(
        size = 5,
        regions = IntArray(25) { it / 5 }.also {
            it[4] = 4
            it[20] = 0
        },
        solution = intArrayOf(0, 2, 4, 1, 3),
        difficulty = Difficulty.EASY,
    )

    private fun conflicting(vararg cats: Pair<Int, Int>): Set<Pair<Int, Int>> {
        val flags = BooleanArray(25)
        for ((r, c) in cats) flags[r * 5 + c] = true
        val out = puzzle.conflicts(flags)
        return out.indices.filter { out[it] }.map { it / 5 to it % 5 }.toSet()
    }

    @Test
    fun sameRow() = assertEquals(setOf(0 to 0, 0 to 4), conflicting(0 to 0, 0 to 4))

    @Test
    fun sameColumn() = assertEquals(setOf(0 to 2, 3 to 2), conflicting(0 to 2, 3 to 2))

    @Test
    fun sameRegion() = assertEquals(setOf(0 to 4, 4 to 2), conflicting(0 to 4, 4 to 2))

    @Test
    fun touchingDiagonally() = assertEquals(setOf(1 to 1, 2 to 2), conflicting(1 to 1, 2 to 2))

    @Test
    fun onlyTheOffendingCatsAreFlagged() =
        assertEquals(setOf(2 to 0, 2 to 4), conflicting(0 to 2, 2 to 0, 2 to 4))

    @Test
    fun solutionHasNoConflicts() {
        val cats = BooleanArray(25)
        puzzle.solution.forEachIndexed { r, c -> cats[r * 5 + c] = true }
        assertEquals(emptySet<Pair<Int, Int>>(), conflicting(*puzzle.solution.mapIndexed { r, c -> r to c }.toTypedArray()))
        assertEquals(true, puzzle.isSolvedBy(cats))
    }
}
