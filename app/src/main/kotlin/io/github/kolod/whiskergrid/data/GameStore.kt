package io.github.kolod.whiskergrid.data

import android.content.Context
import android.util.AtomicFile
import io.github.kolod.whiskergrid.game.Mark
import io.github.kolod.whiskergrid.game.Puzzle
import io.github.kolod.whiskergrid.game.isSolvedBy
import java.io.File

class SavedGame(
    val puzzle: Puzzle,
    val marks: List<Mark>,
    val solved: Boolean,
    val created: Long,
) {
    val key: String get() = puzzle.key

    fun copy(marks: List<Mark> = this.marks, solved: Boolean = this.solved) =
        SavedGame(puzzle, marks, solved, created)
}

/**
 * Every board the player has been given, solved or not. Used so new boards never repeat and
 * unfinished ones can be picked up again.
 *
 * The full history lives in a file that is rewritten only when the set of games changes. Marks of
 * the game being played change on every tap, so they are kept separately in preferences and laid
 * over the history on load.
 */
class GameStore(context: Context) {

    private val file = AtomicFile(File(context.filesDir, "games.txt"))
    private val prefs = context.getSharedPreferences("game", Context.MODE_PRIVATE)
    private val games = LinkedHashMap<String, SavedGame>()

    var currentKey: String? = prefs.getString(KEY_CURRENT, null)
        private set

    init {
        load()
        migrateLegacyGame()
        val current = currentKey?.let(games::get)
        val marks = prefs.getString(KEY_MARKS, null)?.let(::decodeMarks)
        if (current != null && marks != null && marks.size == current.marks.size) {
            games[current.key] = current.copy(marks = marks)
        }
    }

    val current: SavedGame? get() = currentKey?.let(games::get)

    /** Keys of every board ever handed out. */
    fun keys(): Set<String> = games.keys.toSet()

    /** Unsolved games other than the current one, newest first. */
    fun skipped(): List<SavedGame> =
        games.values.filter { !it.solved && it.key != currentKey }.sortedByDescending { it.created }

    /** Adds or replaces [game] and makes it the current one. */
    fun open(game: SavedGame) {
        games[game.key] = game
        currentKey = game.key
        prefs.edit().putString(KEY_CURRENT, game.key).remove(KEY_MARKS).apply()
        persist()
    }

    /** Cheap per-move save of the current game's marks. */
    fun updateCurrent(marks: List<Mark>, solved: Boolean) {
        val game = current ?: return
        val wasSolved = game.solved
        games[game.key] = game.copy(marks = marks, solved = solved)
        prefs.edit().putString(KEY_MARKS, encodeMarks(marks)).apply()
        if (solved != wasSolved) persist()
    }

    /** Writes the current marks into the history file as well, e.g. before switching games. */
    fun flush() = persist()

    private fun persist() {
        val out = file.startWrite()
        try {
            out.bufferedWriter().apply {
                for (game in games.values) {
                    write(
                        listOf(
                            game.puzzle.encode(), encodeMarks(game.marks),
                            if (game.solved) "1" else "0", game.created.toString(),
                        ).joinToString("\t"),
                    )
                    write("\n")
                }
                flush()
            }
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
            throw e
        }
    }

    private fun load() {
        val text = runCatching { file.readFully().decodeToString() }.getOrNull() ?: return
        for (line in text.lineSequence()) {
            val parts = line.split("\t")
            if (parts.size != 4) continue
            val puzzle = Puzzle.decode(parts[0]) ?: continue
            val marks = decodeMarks(parts[1]).takeIf { it.size == puzzle.size * puzzle.size } ?: continue
            games[puzzle.key] = SavedGame(puzzle, marks, parts[2] == "1", parts[3].toLongOrNull() ?: 0L)
        }
    }

    /** Version 1.0 kept a single game in preferences. */
    private fun migrateLegacyGame() {
        val legacy = prefs.getString(LEGACY_PUZZLE, null) ?: return
        prefs.edit().remove(LEGACY_PUZZLE).apply()
        val puzzle = Puzzle.decode(legacy) ?: return
        val marks = prefs.getString(KEY_MARKS, null)?.let(::decodeMarks)
            ?.takeIf { it.size == puzzle.size * puzzle.size }
            ?: List(puzzle.size * puzzle.size) { Mark.EMPTY }
        val solved = puzzle.isSolvedBy(BooleanArray(marks.size) { marks[it] == Mark.CAT })
        open(SavedGame(puzzle, marks, solved, created = System.currentTimeMillis()))
        prefs.edit().putString(KEY_MARKS, encodeMarks(marks)).apply()
    }

    private fun encodeMarks(marks: List<Mark>) = marks.joinToString("") { it.ordinal.toString() }

    private fun decodeMarks(text: String) =
        text.map { Mark.entries.getOrNull(it.digitToIntOrNull() ?: -1) ?: Mark.EMPTY }

    private companion object {
        const val KEY_CURRENT = "current"
        const val KEY_MARKS = "marks"
        const val LEGACY_PUZZLE = "puzzle"
    }
}
