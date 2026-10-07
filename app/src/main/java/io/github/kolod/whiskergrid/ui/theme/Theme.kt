package io.github.kolod.whiskergrid.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/** Colours of the board itself; region colours must stay distinct in both themes. */
@Immutable
data class BoardColors(
    val regions: List<Color>,
    val mark: Color,
    val grid: Color,
    val border: Color,
)

private val LightBoard = BoardColors(
    regions = listOf(
        Color(0xFFF6B8B3), Color(0xFFF9D49B), Color(0xFFFFF0A3), Color(0xFFC7E6A5), Color(0xFF9FDCCB),
        Color(0xFFA9CDF2), Color(0xFFC9B9F0), Color(0xFFF2B8DC), Color(0xFFDCC5A6), Color(0xFFD0D9DE),
    ),
    mark = Color(0xFF2B2320),
    grid = Color(0x33000000),
    border = Color(0xFF3A302C),
)

private val DarkBoard = BoardColors(
    regions = listOf(
        Color(0xFF8A4A45), Color(0xFF8C6530), Color(0xFF7D7334), Color(0xFF4F7838), Color(0xFF2F7464),
        Color(0xFF3A6290), Color(0xFF5E4E92), Color(0xFF8A4672), Color(0xFF735E44), Color(0xFF56636B),
    ),
    mark = Color(0xFFF7EFEA),
    grid = Color(0x40FFFFFF),
    border = Color(0xFF0E0B0A),
)

val LocalBoardColors = staticCompositionLocalOf { LightBoard }

@Composable
fun WhiskerGridTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme(primary = Color(0xFFF2B880), secondary = Color(0xFFC9B9F0))
        else -> lightColorScheme(primary = Color(0xFF8A5300), secondary = Color(0xFF5E4E92))
    }
    MaterialTheme(colorScheme = scheme) {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalBoardColors provides if (dark) DarkBoard else LightBoard,
            content = content,
        )
    }
}
