package io.github.kolod.whiskergrid.ui

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.kolod.whiskergrid.data.GameStore
import io.github.kolod.whiskergrid.data.SavedGame
import io.github.kolod.whiskergrid.game.Difficulty
import io.github.kolod.whiskergrid.game.Generator
import io.github.kolod.whiskergrid.game.Mark
import io.github.kolod.whiskergrid.game.Puzzle
import io.github.kolod.whiskergrid.game.isSolvedBy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class GameState(
    val puzzle: Puzzle? = null,
    val marks: List<Mark> = emptyList(),
    val solved: Boolean = false,
    val generating: Boolean = false,
    val canUndo: Boolean = false,
)

data class Settings(val size: Int = 7, val difficulty: Difficulty = Difficulty.EASY)

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("game", Context.MODE_PRIVATE)
    private val store = GameStore(app)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _game = MutableStateFlow(store.current?.toState() ?: GameState())
    val game: StateFlow<GameState> = _game.asStateFlow()

    private val _skipped = MutableStateFlow(store.skipped())
    /** Unfinished boards the player moved away from; they can be picked up again. */
    val skipped: StateFlow<List<SavedGame>> = _skipped.asStateFlow()

    private val history = ArrayDeque<List<Mark>>()
    private var generation: Job? = null

    /** What a drag does to the cells it passes over; decided by the cell it started on. */
    private var dragPaint: Mark? = null

    /** The cell whose × was put by the latest action, a tap; a second tap there turns it into a cat. */
    private var crossTappedOn: Int? = null

    /** The board for the next "new game", generated ahead of time for [nextFor]. */
    private var next: Deferred<Puzzle>? = null
    private var nextFor: Settings? = null

    init {
        val saved = prefs.getString(KEY_NEXT, null)?.let(Puzzle::decode)
        val settings = _settings.value
        if (saved != null && saved.size == settings.size && saved.difficulty == settings.difficulty &&
            saved.key !in store.keys()
        ) {
            next = CompletableDeferred(saved)
            nextFor = settings
        } else {
            prefetch()
        }
    }

    fun updateSettings(settings: Settings) {
        val capped = settings.copy(
            difficulty = minOf(settings.difficulty, Generator.maxDifficulty(settings.size)),
        )
        _settings.value = capped
        prefs.edit {
            putInt(KEY_SIZE, capped.size)
            putString(KEY_DIFFICULTY, capped.difficulty.name)
        }
        prefetch()
    }

    /** Starts generating the next board for the current settings unless one is already on its way. */
    private fun prefetch() {
        val settings = _settings.value
        if (nextFor == settings && next?.isCancelled == false) return
        next?.cancel()
        nextFor = settings
        val played = store.keys()
        next = viewModelScope.async(Dispatchers.Default) {
            val generator = Generator()
            // A repeat is astronomically rare on big boards but quite possible on 5×5.
            var candidate: Puzzle
            var tries = 0
            do {
                candidate = generator.generate(settings.size, settings.difficulty) { isActive }
            } while (candidate.key in played && ++tries < MAX_REPEAT_TRIES)
            prefs.edit { putString(KEY_NEXT, candidate.encode()) }
            candidate
        }
    }

    fun newGame() {
        generation?.cancel()
        history.clear()
        store.flush()
        prefetch()
        generation = viewModelScope.launch {
            var ready = next!!
            // Normally the board is already waiting; show the spinner only if it is not.
            if (!ready.isCompleted) _game.value = GameState(generating = true)
            var puzzle = ready.await()
            if (puzzle.key in store.keys()) {
                // Someone else took it meanwhile (e.g. a resumed copy); make another one.
                nextFor = null
                prefetch()
                ready = next!!
                _game.value = GameState(generating = true)
                puzzle = ready.await()
            }
            next = null
            nextFor = null
            prefs.edit { remove(KEY_NEXT) }
            history.clear()
            val cells = puzzle.size * puzzle.size
            open(SavedGame(puzzle, List(cells) { Mark.EMPTY }, solved = false, System.currentTimeMillis()))
            prefetch()
        }
    }

    /** Continues a previously skipped board where the player left it. */
    fun resume(game: SavedGame) {
        generation?.cancel()
        store.flush()
        history.clear()
        open(game)
    }

    private fun open(game: SavedGame) {
        crossTappedOn = null
        store.open(game)
        _game.value = game.toState()
        _skipped.value = store.skipped()
    }

    private fun SavedGame.toState() = GameState(puzzle = puzzle, marks = marks, solved = solved)

    fun tap(cell: Int) {
        val current = _game.value.marks.getOrNull(cell) ?: return
        val next = when (current) {
            Mark.EMPTY -> Mark.CROSS
            Mark.CROSS -> Mark.CAT
            Mark.CAT -> Mark.EMPTY
        }
        // Placing a cat takes two taps (empty → × → cat); undo should take it back in one step,
        // so the second tap joins the step of the first one if nothing happened in between.
        val joinLast = next == Mark.CAT && crossTappedOn == cell
        change(joinLast) { it[cell] = next }
        crossTappedOn = if (next == Mark.CROSS) cell else null
    }

    fun dragStart(cell: Int) {
        val start = _game.value.marks.getOrNull(cell) ?: return
        dragPaint = if (start == Mark.CROSS) Mark.EMPTY else Mark.CROSS
        change { paint(it, cell) }
    }

    fun dragOver(cell: Int) {
        if (dragPaint == null) return
        val marks = _game.value.marks.toMutableList()
        if (!paint(marks, cell)) return
        // The whole drag is one undo step, recorded in dragStart.
        apply(marks)
    }

    fun dragEnd() {
        dragPaint = null
    }

    /** Crosses go only onto empty cells and are erased only from crosses; cats stay put. */
    private fun paint(marks: MutableList<Mark>, cell: Int): Boolean {
        val paint = dragPaint ?: return false
        val from = if (paint == Mark.CROSS) Mark.EMPTY else Mark.CROSS
        if (marks.getOrNull(cell) != from) return false
        marks[cell] = paint
        return true
    }

    fun undo() {
        crossTappedOn = null
        val previous = history.removeLastOrNull() ?: return
        apply(previous)
    }

    fun clearBoard() {
        if (_game.value.marks.all { it == Mark.EMPTY }) return
        change { it.fill(Mark.EMPTY) }
    }

    /** Applies [edit] as a new undo step, or as part of the last one when [joinLast] is set. */
    private fun change(joinLast: Boolean = false, edit: (MutableList<Mark>) -> Unit) {
        crossTappedOn = null
        val state = _game.value
        if (state.puzzle == null || state.solved) return
        val marks = state.marks.toMutableList()
        edit(marks)
        if (marks == state.marks) return
        if (!joinLast || history.isEmpty()) {
            history.addLast(state.marks)
            if (history.size > MAX_UNDO) history.removeFirst()
        }
        apply(marks)
    }

    private fun apply(marks: List<Mark>) {
        _game.update { state ->
            val puzzle = state.puzzle ?: return@update state
            val cats = BooleanArray(marks.size) { marks[it] == Mark.CAT }
            val solved = puzzle.isSolvedBy(cats)
            // A finished board shows every free cell crossed out.
            val shown = if (solved) marks.map { if (it == Mark.CAT) it else Mark.CROSS } else marks
            state.copy(marks = shown, solved = solved, canUndo = history.isNotEmpty())
        }
        save()
    }

    private fun save() {
        val state = _game.value
        if (state.puzzle == null) return
        store.updateCurrent(state.marks, state.solved)
    }

    override fun onCleared() {
        store.flush()
    }

    private fun loadSettings(): Settings {
        val size = prefs.getInt(KEY_SIZE, Settings().size)
            .coerceIn(Generator.MIN_SIZE, Generator.MAX_SIZE)
        val difficulty = prefs.getString(KEY_DIFFICULTY, null)
            ?.let { name -> Difficulty.entries.firstOrNull { it.name == name } }
            ?: Difficulty.EASY
        return Settings(size, minOf(difficulty, Generator.maxDifficulty(size)))
    }

    private companion object {
        const val KEY_SIZE = "size"
        const val KEY_DIFFICULTY = "difficulty"
        const val KEY_NEXT = "next"
        const val MAX_UNDO = 500
        const val MAX_REPEAT_TRIES = 50
    }
}
