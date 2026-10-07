package io.github.kolod.whiskergrid.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GeneratorTest {

    @Test
    fun everySizeAndDifficultyIsUniqueAndNeedsNoGuessing() {
        for (size in Generator.MIN_SIZE..Generator.MAX_SIZE) {
            for (difficulty in Difficulty.entries) {
                repeat(3) { seed ->
                    val started = System.nanoTime()
                    val puzzle = Generator(Random(seed * 100 + size)).generate(size, difficulty)
                    val ms = (System.nanoTime() - started) / 1_000_000
                    println("size=$size $difficulty seed=$seed -> ${puzzle.difficulty} in ${ms}ms")

                    val cats = BooleanArray(size * size).also { c ->
                        puzzle.solution.forEachIndexed { row, col -> c[row * size + col] = true }
                    }
                    assertTrue("solution must satisfy the rules", puzzle.isSolvedBy(cats))
                    assertEquals("exactly one solution", 1, countSolutions(puzzle))

                    val solver = LogicSolver(size, puzzle.regions)
                    assertTrue("solvable by logic", solver.solve(puzzle.difficulty).solved)
                    if (puzzle.difficulty > Difficulty.EASY) {
                        val easier = Difficulty.entries[puzzle.difficulty.ordinal - 1]
                        assertFalse("not solvable at an easier level", solver.solve(easier).solved)
                    }
                }
            }
        }
    }

    @Test
    fun encodeRoundTrip() {
        val puzzle = Generator(Random(1)).generate(6, Difficulty.MEDIUM)
        val copy = Puzzle.decode(puzzle.encode())!!
        assertEquals(puzzle.regions.toList(), copy.regions.toList())
        assertEquals(puzzle.solution.toList(), copy.solution.toList())
        assertEquals(puzzle.difficulty, copy.difficulty)
    }

    @Test
    fun keyIgnoresRegionColours() {
        val puzzle = Generator(Random(2)).generate(7, Difficulty.EASY)
        val swapped = IntArray(puzzle.regions.size) { (puzzle.regions[it] + 3) % puzzle.size }
        val recoloured = Puzzle(puzzle.size, swapped, puzzle.solution, puzzle.difficulty)
        assertEquals(puzzle.key, recoloured.key)
        val other = Generator(Random(3)).generate(7, Difficulty.EASY)
        assertTrue(puzzle.key != other.key)
    }

    private fun countSolutions(p: Puzzle): Int {
        val n = p.size
        val usedCol = BooleanArray(n)
        val usedRegion = BooleanArray(n)
        val cols = IntArray(n)
        fun go(row: Int): Int {
            if (row == n) return 1
            var total = 0
            for (c in 0 until n) {
                val region = p.regions[row * n + c]
                if (usedCol[c] || usedRegion[region]) continue
                if (row > 0 && kotlin.math.abs(cols[row - 1] - c) <= 1) continue
                usedCol[c] = true; usedRegion[region] = true; cols[row] = c
                total += go(row + 1)
                usedCol[c] = false; usedRegion[region] = false
                if (total > 1) return total
            }
            return total
        }
        return go(0)
    }
}
