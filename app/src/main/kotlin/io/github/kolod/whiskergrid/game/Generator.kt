package io.github.kolod.whiskergrid.game

import kotlin.random.Random

/**
 * Builds boards that [LogicSolver] can finish at exactly the requested difficulty.
 *
 * A valid cat placement is chosen first, regions are grown around the cats, and then the region
 * borders are nudged one cell at a time until the board needs the requested techniques and
 * nothing beyond them.
 */
class Generator(private val random: Random = Random.Default) {

    fun generate(size: Int, requested: Difficulty, isActive: () -> Boolean = { true }): Puzzle {
        require(size in MIN_SIZE..MAX_SIZE)
        val difficulty = minOf(requested, maxDifficulty(size))
        var fallback: Puzzle? = null
        for (attempt in 0 until MAX_RESTARTS) {
            check(isActive()) { "Generation cancelled" }
            val solution = randomSolution(size)
            val regions = growRegions(size, solution)
            val found = climb(size, solution, regions, difficulty, isActive)
            if (found != null) return found
            val solved = LogicSolver(size, regions).solve(difficulty)
            if (solved.solved && (fallback == null || solved.hardest > fallback.difficulty)) {
                fallback = Puzzle(size, regions, solution, solved.hardest)
            }
        }
        // Practically unreachable; still better to hand out a slightly easier board than nothing.
        return fallback ?: generate(size, Difficulty.EASY, isActive)
    }

    private fun climb(
        size: Int,
        solution: IntArray,
        regions: IntArray,
        target: Difficulty,
        isActive: () -> Boolean,
    ): Puzzle? {
        val catCells = BooleanArray(size * size).also { cats ->
            solution.forEachIndexed { row, col -> cats[row * size + col] = true }
        }
        var score = score(size, regions, target)
        for (step in 0 until MAX_STEPS) {
            if (score == 0) return Puzzle(size, regions.copyOf(), solution, target)
            if (step % 32 == 0 && !isActive()) return null
            val cell = random.nextInt(size * size)
            if (catCells[cell]) continue
            val neighbours = orthogonal(size, cell).filter { regions[it] != regions[cell] }
            if (neighbours.isEmpty()) continue
            val from = regions[cell]
            val to = regions[neighbours.random(random)]
            regions[cell] = to
            if (!isConnected(size, regions, from)) {
                regions[cell] = from
                continue
            }
            val next = score(size, regions, target)
            if (next <= score) score = next else regions[cell] = from
        }
        return null
    }

    /** 0 = exactly the target difficulty; otherwise lower is closer. */
    private fun score(size: Int, regions: IntArray, target: Difficulty): Int {
        val solver = LogicSolver(size, regions)
        val atTarget = solver.solve(target)
        if (!atTarget.solved) return 2 + atTarget.unresolved
        if (target == Difficulty.EASY) return 0
        val easier = Difficulty.entries[target.ordinal - 1]
        return if (solver.solve(easier).solved) 1 else 0
    }

    /** One cat per row and column, no two touching (diagonally adjacent rows differ by ≥ 2). */
    private fun randomSolution(size: Int): IntArray {
        val cols = IntArray(size)
        val used = BooleanArray(size)
        fun fill(row: Int): Boolean {
            if (row == size) return true
            for (col in (0 until size).shuffled(random)) {
                if (used[col] || (row > 0 && kotlin.math.abs(cols[row - 1] - col) <= 1)) continue
                cols[row] = col
                used[col] = true
                if (fill(row + 1)) return true
                used[col] = false
            }
            return false
        }
        check(fill(0))
        return cols
    }

    /** Every cat seeds a region; unclaimed cells join a random adjacent region until none remain. */
    private fun growRegions(size: Int, solution: IntArray): IntArray {
        val regions = IntArray(size * size) { -1 }
        solution.forEachIndexed { row, col -> regions[row * size + col] = row }
        var left = size * size - size
        while (left > 0) {
            val cell = random.nextInt(size * size)
            if (regions[cell] != -1) continue
            val owned = orthogonal(size, cell).filter { regions[it] != -1 }
            if (owned.isEmpty()) continue
            regions[cell] = regions[owned.random(random)]
            left--
        }
        // Region ids follow the rows of their cats; shuffle so colours don't run top to bottom.
        val relabel = (0 until size).shuffled(random)
        return IntArray(size * size) { relabel[regions[it]] }
    }

    private fun orthogonal(size: Int, cell: Int): List<Int> {
        val r = cell / size
        val c = cell % size
        return buildList(4) {
            if (r > 0) add(cell - size)
            if (r < size - 1) add(cell + size)
            if (c > 0) add(cell - 1)
            if (c < size - 1) add(cell + 1)
        }
    }

    private fun isConnected(size: Int, regions: IntArray, region: Int): Boolean {
        val start = regions.indexOfFirst { it == region }
        if (start == -1) return false
        val seen = BooleanArray(size * size)
        val stack = ArrayDeque<Int>().apply { add(start) }
        seen[start] = true
        var reached = 0
        while (stack.isNotEmpty()) {
            val cell = stack.removeLast()
            reached++
            for (n in orthogonal(size, cell)) {
                if (!seen[n] && regions[n] == region) {
                    seen[n] = true
                    stack.add(n)
                }
            }
        }
        return reached == regions.count { it == region }
    }

    companion object {
        const val MIN_SIZE = 5
        const val MAX_SIZE = 10

        /** Small boards are too tight for the HARD techniques to ever be needed. */
        fun maxDifficulty(size: Int) = if (size <= 5) Difficulty.MEDIUM else Difficulty.HARD
        private const val MAX_RESTARTS = 200
        private const val MAX_STEPS = 600
    }
}
