package io.github.kolod.whiskergrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.kolod.whiskergrid.ui.GameScreen
import io.github.kolod.whiskergrid.ui.GameViewModel
import io.github.kolod.whiskergrid.ui.HomeScreen
import io.github.kolod.whiskergrid.ui.SkippedScreen
import io.github.kolod.whiskergrid.ui.theme.WhiskerGridTheme

private enum class Screen { HOME, GAME, SKIPPED }

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            WhiskerGridTheme {
                val settings by viewModel.settings.collectAsStateWithLifecycle()
                val game by viewModel.game.collectAsStateWithLifecycle()
                val skipped by viewModel.skipped.collectAsStateWithLifecycle()
                var screen by rememberSaveable { mutableStateOf(Screen.HOME) }
                if (screen != Screen.HOME) BackHandler { screen = Screen.HOME }

                when (screen) {
                    Screen.GAME -> GameScreen(
                        state = game,
                        onBack = { screen = Screen.HOME },
                        onNewGame = viewModel::newGame,
                        onTap = viewModel::tap,
                        onDragStart = viewModel::dragStart,
                        onDragOver = viewModel::dragOver,
                        onDragEnd = viewModel::dragEnd,
                        onUndo = viewModel::undo,
                        onClear = viewModel::clearBoard,
                        onSkip = viewModel::newGame,
                    )
                    Screen.SKIPPED -> SkippedScreen(
                        games = skipped,
                        onBack = { screen = Screen.HOME },
                        onOpen = {
                            viewModel.resume(it)
                            screen = Screen.GAME
                        },
                    )
                    Screen.HOME -> HomeScreen(
                        settings = settings,
                        canContinue = game.puzzle != null && !game.solved && !game.generating,
                        skippedCount = skipped.size,
                        onSettings = viewModel::updateSettings,
                        onNewGame = {
                            viewModel.newGame()
                            screen = Screen.GAME
                        },
                        onContinue = { screen = Screen.GAME },
                        onSkipped = { screen = Screen.SKIPPED },
                    )
                }
            }
        }
    }
}
