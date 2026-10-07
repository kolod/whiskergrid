package io.github.kolod.whiskergrid.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.kolod.whiskergrid.R
import io.github.kolod.whiskergrid.game.Mark
import io.github.kolod.whiskergrid.game.Difficulty
import io.github.kolod.whiskergrid.game.Generator
import kotlinx.coroutines.delay

private const val SOLVED_MESSAGE_DELAY_MS = 700L

@Composable
fun Difficulty.label(): String = stringResource(
    when (this) {
        Difficulty.EASY -> R.string.difficulty_easy
        Difficulty.MEDIUM -> R.string.difficulty_medium
        Difficulty.HARD -> R.string.difficulty_hard
    },
)

@Composable
fun HomeScreen(
    settings: Settings,
    canContinue: Boolean,
    skippedCount: Int,
    onSettings: (Settings) -> Unit,
    onNewGame: () -> Unit,
    onContinue: () -> Unit,
    onSkipped: () -> Unit,
) {
    var showRules by rememberSaveable { mutableStateOf(false) }
    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                GameIcons.Cat, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp),
            )
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(top = 8.dp, bottom = 32.dp),
            )
            Column(Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
                Text(stringResource(R.string.board_size), style = MaterialTheme.typography.titleMedium)
                Column(Modifier.padding(top = 8.dp, bottom = 24.dp)) {
                    (Generator.MIN_SIZE..Generator.MAX_SIZE).chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (size in row) {
                                FilterChip(
                                    selected = settings.size == size,
                                    onClick = { onSettings(settings.copy(size = size)) },
                                    label = {
                                        Text(
                                            stringResource(R.string.size_value, size),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }

                Text(stringResource(R.string.difficulty), style = MaterialTheme.typography.titleMedium)
                val maxDifficulty = Generator.maxDifficulty(settings.size)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Difficulty.entries.forEachIndexed { index, difficulty ->
                        SegmentedButton(
                            selected = settings.difficulty == difficulty,
                            onClick = { onSettings(settings.copy(difficulty = difficulty)) },
                            enabled = difficulty <= maxDifficulty,
                            shape = SegmentedButtonDefaults.itemShape(index, Difficulty.entries.size),
                            label = { Text(difficulty.label()) },
                        )
                    }
                }
                if (maxDifficulty < Difficulty.HARD) {
                    Text(
                        stringResource(R.string.hard_unavailable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                Spacer(Modifier.height(32.dp))
                Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.new_game))
                }
                if (canContinue) {
                    OutlinedButton(onClick = onContinue, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text(stringResource(R.string.continue_game))
                    }
                }
                if (skippedCount > 0) {
                    OutlinedButton(onClick = onSkipped, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text(stringResource(R.string.skipped_button, skippedCount))
                    }
                }
                TextButton(
                    onClick = { showRules = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.rules))
                }
            }
        }
    }
    if (showRules) RulesDialog { showRules = false }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    state: GameState,
    onBack: () -> Unit,
    onNewGame: () -> Unit,
    onTap: (Int) -> Unit,
    onDragStart: (Int) -> Unit,
    onDragOver: (Int) -> Unit,
    onDragEnd: () -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onSkip: () -> Unit,
) {
    var showRules by rememberSaveable { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    val puzzle = state.puzzle
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (puzzle != null && !state.generating) {
                        Text(stringResource(R.string.game_title, puzzle.size, puzzle.difficulty.label()))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(GameIcons.Back, stringResource(R.string.back)) }
                },
                actions = {
                    if (puzzle != null && !state.generating && !state.solved) {
                        TextButton(onClick = onSkip) {
                            Text(stringResource(R.string.skip))
                            Icon(GameIcons.Skip, null, Modifier.padding(start = 4.dp).size(18.dp))
                        }
                    }
                    IconButton(onClick = { showRules = true }) { Icon(GameIcons.Help, stringResource(R.string.rules)) }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            if (state.generating || puzzle == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.generating), Modifier.padding(top = 16.dp))
                }
                return@Box
            }
            Column(
                Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Board(
                    puzzle = puzzle,
                    marks = state.marks,
                    enabled = !state.solved,
                    onTap = { cell ->
                        if (state.marks.getOrNull(cell) == Mark.CROSS) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        onTap(cell)
                    },
                    onDragStart = onDragStart,
                    onDragOver = onDragOver,
                    onDragEnd = onDragEnd,
                    modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth().weight(1f, fill = false),
                )
                // Controls and the "solved" message share one slot whose height is the taller of the
                // two, so the board never moves when one replaces the other.
                var showSolved by remember(puzzle) { mutableStateOf(state.solved) }
                LaunchedEffect(puzzle, state.solved) {
                    if (state.solved && !showSolved) {
                        // Let the player see the board fill with crosses before the message shows up.
                        delay(SOLVED_MESSAGE_DELAY_MS)
                        showSolved = true
                    }
                }
                val controlsAlpha by animateFloatAsState(if (state.solved) 0f else 1f, label = "controls")
                val solvedAlpha by animateFloatAsState(if (showSolved) 1f else 0f, label = "solved")
                Box(Modifier.padding(top = 20.dp), contentAlignment = Alignment.TopCenter) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.alpha(controlsAlpha),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedButton(onClick = onUndo, enabled = !state.solved && state.canUndo) {
                                Icon(GameIcons.Undo, null, Modifier.size(18.dp))
                                Text(stringResource(R.string.undo), Modifier.padding(start = 8.dp))
                            }
                            OutlinedButton(
                                onClick = onClear,
                                enabled = !state.solved && state.marks.any { it != Mark.EMPTY },
                            ) {
                                Icon(GameIcons.Clear, null, Modifier.size(18.dp))
                                Text(stringResource(R.string.clear), Modifier.padding(start = 8.dp))
                            }
                        }
                        Text(
                            stringResource(R.string.controls_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                    SolvedMessage(
                        enabled = showSolved,
                        onNewGame = onNewGame,
                        onMenu = onBack,
                        modifier = Modifier.alpha(solvedAlpha),
                    )
                }
            }
        }
    }
    if (showRules) RulesDialog { showRules = false }
}

@Composable
private fun SolvedMessage(enabled: Boolean, onNewGame: () -> Unit, onMenu: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.solved_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 16.dp)) {
            OutlinedButton(onClick = onMenu, enabled = enabled) { Text(stringResource(R.string.to_menu)) }
            Button(onClick = onNewGame, enabled = enabled) { Text(stringResource(R.string.play_again)) }
        }
    }
}

@Composable
fun RulesDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rules_title)) },
        text = { Text(stringResource(R.string.rules_text)) },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) } },
    )
}
