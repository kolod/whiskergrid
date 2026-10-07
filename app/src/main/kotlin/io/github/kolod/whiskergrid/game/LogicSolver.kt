package io.github.kolod.whiskergrid.game

/**
 * Solves a board the way a person would: only with deductions, never by guessing.
 *
 * Every deduction is sound, so if the solver finishes, the solution is unique and a player
 * can reach it without trial and error. Techniques by difficulty:
 * - EASY: a row/column/region with one possible cell; a unit whose candidates lie in a single
 *   other unit ("this region lives in one row"); a cell whose cat would wipe out a whole unit.
 * - MEDIUM: the same confinement reasoning for pairs of units.
 * - HARD: confinement for any number of units.
 */
class LogicSolver(private val size: Int, private val regions: IntArray) {

    class Result(val solved: Boolean, val hardest: Difficulty, val unresolved: Int)

    private val cells = size * size

    // Units 0 until size are rows, then columns, then regions.
    private val units: Array<IntArray> = Array(size * 3) { u ->
        val idx = u % size
        when (u / size) {
            0 -> IntArray(size) { idx * size + it }
            1 -> IntArray(size) { it * size + idx }
            else -> (0 until cells).filter { regions[it] == idx }.toIntArray()
        }
    }
    private val attacked: Array<IntArray> = Array(cells) { attackedBy(size, regions, it) }

    private val candidate = BooleanArray(cells)
    private val unitDone = BooleanArray(size * 3)
    private var placedCount = 0
    private var broken = false

    // Scratch for the "would wipe out a unit" check.
    private val hitStamp = IntArray(cells)
    private var stamp = 0

    fun solve(maxLevel: Difficulty): Result {
        candidate.fill(true)
        unitDone.fill(false)
        placedCount = 0
        broken = false
        var hardest = Difficulty.EASY

        while (!broken && placedCount < size) {
            if (singles() || confinement(1) || wipeOut()) continue
            if (maxLevel >= Difficulty.MEDIUM && confinement(2)) {
                hardest = maxOf(hardest, Difficulty.MEDIUM)
                continue
            }
            if (maxLevel >= Difficulty.HARD && (3 until size).any { confinement(it) }) {
                hardest = Difficulty.HARD
                continue
            }
            break
        }
        val solved = !broken && placedCount == size
        return Result(solved, hardest, candidate.count { it })
    }

    private fun place(cell: Int) {
        candidate[cell] = false
        for (other in attacked[cell]) candidate[other] = false
        unitDone[cell / size] = true
        unitDone[size + cell % size] = true
        unitDone[2 * size + regions[cell]] = true
        placedCount++
    }

    /** A unit with exactly one candidate left gets its cat. */
    private fun singles(): Boolean {
        for (u in units.indices) {
            if (unitDone[u]) continue
            var last = -1
            var count = 0
            for (cell in units[u]) if (candidate[cell]) { last = cell; count++ }
            if (count == 0) { broken = true; return false }
            if (count == 1) { place(last); return true }
        }
        return false
    }

    /** A candidate whose cat would leave some other unit with no candidates can't hold a cat. */
    private fun wipeOut(): Boolean {
        var changed = false
        for (cell in 0 until cells) {
            if (!candidate[cell]) continue
            stamp++
            for (other in attacked[cell]) hitStamp[other] = stamp
            for (u in units.indices) {
                if (unitDone[u] || cell in units[u]) continue
                if (units[u].all { !candidate[it] || hitStamp[it] == stamp }) {
                    candidate[cell] = false
                    changed = true
                    break
                }
            }
        }
        return changed
    }

    private fun kindOf(kind: Int, cell: Int) = when (kind) {
        0 -> cell / size
        1 -> cell % size
        else -> regions[cell]
    }

    /**
     * If the candidates of k open units of one kind fit inside k units of another kind, those k
     * units are used up by them, so their other cells are ruled out.
     */
    private fun confinement(k: Int): Boolean {
        for (a in 0..2) for (b in 0..2) {
            if (a == b) continue
            val open = (0 until size).filter { !unitDone[a * size + it] }
            if (k >= open.size) continue
            if (combinations(open, k) { chosen -> confine(a, b, chosen, k) }) return true
        }
        return false
    }

    private fun confine(a: Int, b: Int, chosen: IntArray, k: Int): Boolean {
        var mask = 0L
        for (idx in chosen) for (cell in units[a * size + idx]) {
            if (candidate[cell]) mask = mask or (1L shl kindOf(b, cell))
        }
        val count = java.lang.Long.bitCount(mask)
        if (count < k) { broken = true; return false }
        if (count > k) return false
        var changed = false
        for (cell in 0 until cells) {
            if (!candidate[cell]) continue
            if (mask and (1L shl kindOf(b, cell)) == 0L) continue
            if (kindOf(a, cell) in chosen) continue
            candidate[cell] = false
            changed = true
        }
        return changed
    }

    /** Calls [action] for every k-subset of [items]; stops and returns true once it returns true. */
    private fun combinations(items: List<Int>, k: Int, action: (IntArray) -> Boolean): Boolean {
        val chosen = IntArray(k)
        fun go(start: Int, depth: Int): Boolean {
            if (broken) return false
            if (depth == k) return action(chosen)
            for (i in start..items.size - (k - depth)) {
                chosen[depth] = items[i]
                if (go(i + 1, depth + 1)) return true
            }
            return false
        }
        return go(0, 0)
    }
}
