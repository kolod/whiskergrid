package io.github.kolod.whiskergrid.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import io.github.kolod.whiskergrid.game.Mark
import io.github.kolod.whiskergrid.game.Puzzle
import io.github.kolod.whiskergrid.ui.theme.LocalBoardColors

/**
 * The playing field. A tap cycles a cell (empty → × → cat); dragging across cells paints or erases
 * crosses. Nothing here ever hints whether a mark is right.
 */
@Composable
fun Board(
    puzzle: Puzzle,
    marks: List<Mark>,
    enabled: Boolean,
    onTap: (Int) -> Unit,
    onDragStart: (Int) -> Unit,
    onDragOver: (Int) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalBoardColors.current
    val cat = rememberVectorPainter(GameIcons.Cat)
    val n = puzzle.size
    val tap by rememberUpdatedState(onTap)
    val dragStart by rememberUpdatedState(onDragStart)
    val dragOver by rememberUpdatedState(onDragOver)
    val dragEnd by rememberUpdatedState(onDragEnd)

    Canvas(
        modifier
            .aspectRatio(1f)
            .pointerInput(n, enabled) {
                if (!enabled) return@pointerInput
                fun cellAt(p: Offset): Int? {
                    val cellSize = size.width.toFloat() / n
                    val col = (p.x / cellSize).toInt()
                    val row = (p.y / cellSize).toInt()
                    return if (p.x >= 0 && p.y >= 0 && row < n && col < n) row * n + col else null
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val start = cellAt(down.position) ?: return@awaitEachGesture
                    var last = start
                    var dragging = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val cell = cellAt(change.position)
                        if (cell != null && cell != last) {
                            if (!dragging) {
                                dragging = true
                                dragStart(start)
                            }
                            dragOver(cell)
                            last = cell
                        }
                        change.consume()
                    }
                    if (dragging) dragEnd() else tap(start)
                }
            },
    ) {
        val cell = size.width / n
        val corner = CornerRadius(12.dp.toPx())
        val outline = androidx.compose.ui.graphics.Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, corner))
        }
        clipPath(outline) {
            for (i in 0 until n * n) {
                drawRect(
                    color = colors.regions[puzzle.regions[i] % colors.regions.size],
                    topLeft = Offset(puzzle.col(i) * cell, puzzle.row(i) * cell),
                    size = Size(cell + 1f, cell + 1f),
                )
            }
            val thin = 1.dp.toPx()
            for (k in 1 until n) {
                drawLine(colors.grid, Offset(k * cell, 0f), Offset(k * cell, size.height), thin)
                drawLine(colors.grid, Offset(0f, k * cell), Offset(size.width, k * cell), thin)
            }
            val thick = 3.dp.toPx()
            for (i in 0 until n * n) {
                val r = puzzle.row(i)
                val c = puzzle.col(i)
                if (c < n - 1 && puzzle.regions[i] != puzzle.regions[i + 1]) {
                    drawLine(
                        colors.border, Offset((c + 1) * cell, r * cell), Offset((c + 1) * cell, (r + 1) * cell),
                        thick, StrokeCap.Square,
                    )
                }
                if (r < n - 1 && puzzle.regions[i] != puzzle.regions[i + n]) {
                    drawLine(
                        colors.border, Offset(c * cell, (r + 1) * cell), Offset((c + 1) * cell, (r + 1) * cell),
                        thick, StrokeCap.Square,
                    )
                }
            }
        }
        drawRoundRect(colors.border, cornerRadius = corner, style = Stroke(3.dp.toPx()))

        val crossStroke = (cell * 0.07f).coerceAtLeast(2.dp.toPx())
        for (i in marks.indices) {
            val left = puzzle.col(i) * cell
            val top = puzzle.row(i) * cell
            when (marks[i]) {
                Mark.EMPTY -> Unit
                Mark.CROSS -> {
                    val a = cell * 0.36f
                    val b = cell * 0.64f
                    val tint = colors.mark.copy(alpha = 0.55f)
                    drawLine(tint, Offset(left + a, top + a), Offset(left + b, top + b), crossStroke, StrokeCap.Round)
                    drawLine(tint, Offset(left + b, top + a), Offset(left + a, top + b), crossStroke, StrokeCap.Round)
                }
                Mark.CAT -> {
                    val inset = cell * 0.12f
                    translate(left + inset, top + inset) {
                        with(cat) {
                            draw(Size(cell - 2 * inset, cell - 2 * inset), colorFilter = ColorFilter.tint(colors.mark))
                        }
                    }
                }
            }
        }
    }
}
