package io.github.kolod.whiskergrid.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.kolod.whiskergrid.R
import io.github.kolod.whiskergrid.data.SavedGame
import io.github.kolod.whiskergrid.game.Mark
import io.github.kolod.whiskergrid.ui.theme.LocalBoardColors
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkippedScreen(
    games: List<SavedGame>,
    onBack: () -> Unit,
    onOpen: (SavedGame) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.skipped_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(GameIcons.Back, stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        if (games.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.skipped_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            items(games, key = { it.key }) { game ->
                Card(onClick = { onOpen(game) }, modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        BoardPreview(game, Modifier.size(72.dp))
                        Column(Modifier.padding(start = 16.dp)) {
                            val puzzle = game.puzzle
                            Text(
                                stringResource(R.string.game_title, puzzle.size, puzzle.difficulty.label()),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                dateFormat.format(Date(game.created)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                stringResource(R.string.cats_placed, game.marks.count { it == Mark.CAT }, puzzle.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Tiny static picture of a board: region colours and the cats placed so far. */
@Composable
private fun BoardPreview(game: SavedGame, modifier: Modifier = Modifier) {
    val colors = LocalBoardColors.current
    Canvas(modifier) {
        val puzzle = game.puzzle
        val n = puzzle.size
        val cell = size.width / n
        val shape = Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, CornerRadius(6.dp.toPx())))
        }
        clipPath(shape) {
            for (i in 0 until n * n) {
                val topLeft = Offset(puzzle.col(i) * cell, puzzle.row(i) * cell)
                drawRect(colors.regions[puzzle.regions[i] % colors.regions.size], topLeft, Size(cell + 1f, cell + 1f))
                if (game.marks[i] == Mark.CAT) {
                    drawCircle(colors.mark, cell * 0.3f, topLeft + Offset(cell / 2, cell / 2))
                }
            }
        }
    }
}
