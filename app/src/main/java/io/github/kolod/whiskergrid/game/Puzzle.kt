package io.github.kolod.whiskergrid.game

enum class Difficulty { EASY, MEDIUM, HARD }

/**
 * A board of [size]×[size] cells split into [size] regions.
 * Cells are indexed row-major: `cell = row * size + col`.
 *
 * @property regions region id (0 until size) for every cell.
 * @property solution column of the cat in every row.
 */
class Puzzle(
    val size: Int,
    val regions: IntArray,
    val solution: IntArray,
    val difficulty: Difficulty,
) {
    init {
        require(regions.size == size * size)
        require(solution.size == size)
    }

    /**
     * Identifies the board regardless of colours: regions renumbered in reading order. The
     * regions alone fix the (unique) solution, so equal keys mean the same puzzle.
     */
    val key: String by lazy {
        val relabel = IntArray(size) { -1 }
        var next = 0
        regions.joinToString("") { region ->
            if (relabel[region] == -1) relabel[region] = next++
            relabel[region].digitToChar().toString()
        }
    }

    fun row(cell: Int) = cell / size
    fun col(cell: Int) = cell % size

    fun encode(): String =
        listOf(size, difficulty.name, regions.joinToString(","), solution.joinToString(","))
            .joinToString(";")

    companion object {
        fun decode(text: String): Puzzle? = runCatching {
            val (size, difficulty, regions, solution) = text.split(";")
            Puzzle(
                size = size.toInt(),
                regions = regions.split(",").map(String::toInt).toIntArray(),
                solution = solution.split(",").map(String::toInt).toIntArray(),
                difficulty = Difficulty.valueOf(difficulty),
            )
        }.getOrNull()
    }
}

/** Cells that would be ruled out by a cat on [cell]: same row, column, region and the 8 neighbours. */
fun Puzzle.attackedBy(cell: Int): IntArray = attackedBy(size, regions, cell)

internal fun attackedBy(size: Int, regions: IntArray, cell: Int): IntArray {
    val r = cell / size
    val c = cell % size
    val out = ArrayList<Int>()
    for (other in 0 until size * size) {
        if (other == cell) continue
        val orow = other / size
        val ocol = other % size
        val near = kotlin.math.abs(orow - r) <= 1 && kotlin.math.abs(ocol - c) <= 1
        if (orow == r || ocol == c || regions[other] == regions[cell] || near) out += other
    }
    return out.toIntArray()
}

/** True when [cats] (one flag per cell) is a complete, rule-abiding placement. */
fun Puzzle.isSolvedBy(cats: BooleanArray): Boolean {
    val placed = cats.indices.filter { cats[it] }
    if (placed.size != size) return false
    if (placed.map(::row).toSet().size != size) return false
    if (placed.map(::col).toSet().size != size) return false
    if (placed.map { regions[it] }.toSet().size != size) return false
    for (a in placed) for (b in placed) {
        if (a < b && kotlin.math.abs(row(a) - row(b)) <= 1 && kotlin.math.abs(col(a) - col(b)) <= 1) return false
    }
    return true
}
