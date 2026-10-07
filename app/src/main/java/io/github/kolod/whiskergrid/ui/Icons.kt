package io.github.kolod.whiskergrid.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Small hand-drawn icon set, so the app doesn't need the material icons library. */
object GameIcons {

    /** Cat head with cut-out eyes and nose; tint it with any colour. */
    val Cat: ImageVector = ImageVector.Builder("Cat", 24.dp, 24.dp, 24f, 24f).apply {
        path(fill = SolidColor(Color.Black), pathFillType = PathFillType.EvenOdd) {
            moveTo(4.2f, 2.8f)
            lineTo(9f, 7.1f)
            curveTo(10.9f, 6.6f, 13.1f, 6.6f, 15f, 7.1f)
            lineTo(19.8f, 2.8f)
            curveTo(20.6f, 5.9f, 20.9f, 9f, 20.8f, 12.2f)
            curveTo(20.6f, 17.6f, 16.8f, 21f, 12f, 21f)
            curveTo(7.2f, 21f, 3.4f, 17.6f, 3.2f, 12.2f)
            curveTo(3.1f, 9f, 3.4f, 5.9f, 4.2f, 2.8f)
            close()
            // eyes
            moveTo(7.6f, 12.6f)
            arcToRelative(1.45f, 1.45f, 0f, true, true, 2.9f, 0f)
            arcToRelative(1.45f, 1.45f, 0f, true, true, -2.9f, 0f)
            close()
            moveTo(13.5f, 12.6f)
            arcToRelative(1.45f, 1.45f, 0f, true, true, 2.9f, 0f)
            arcToRelative(1.45f, 1.45f, 0f, true, true, -2.9f, 0f)
            close()
            // nose
            moveTo(10.8f, 15.4f)
            lineTo(13.2f, 15.4f)
            lineTo(12f, 16.8f)
            close()
        }
    }.build()

    val Back: ImageVector = stroked("Back", autoMirror = true) {
        moveTo(19f, 12f); lineTo(5f, 12f)
        moveTo(11f, 6f); lineTo(5f, 12f); lineTo(11f, 18f)
    }

    val Skip: ImageVector = stroked("Skip", autoMirror = true) {
        moveTo(5f, 6f); lineTo(11f, 12f); lineTo(5f, 18f)
        moveTo(12f, 6f); lineTo(18f, 12f); lineTo(12f, 18f)
    }

    val Undo: ImageVector = stroked("Undo", autoMirror = true) {
        moveTo(8f, 5f); lineTo(4f, 9f); lineTo(8f, 13f)
        moveTo(4f, 9f); lineTo(14.5f, 9f)
        curveTo(17.5f, 9f, 20f, 11.5f, 20f, 14.5f)
        curveTo(20f, 17.5f, 17.5f, 20f, 14.5f, 20f)
        lineTo(10f, 20f)
    }

    val Clear: ImageVector = stroked("Clear") {
        moveTo(4f, 7f); lineTo(20f, 7f)
        moveTo(9f, 7f); lineTo(9f, 4f); lineTo(15f, 4f); lineTo(15f, 7f)
        moveTo(6f, 7f); lineTo(7f, 20f); lineTo(17f, 20f); lineTo(18f, 7f)
        moveTo(10f, 11f); lineTo(10f, 16f)
        moveTo(14f, 11f); lineTo(14f, 16f)
    }

    val Help: ImageVector = stroked("Help") {
        moveTo(12f, 3f)
        arcToRelative(9f, 9f, 0f, true, true, 0f, 18f)
        arcToRelative(9f, 9f, 0f, true, true, 0f, -18f)
        moveTo(9.5f, 9.5f)
        curveTo(9.5f, 8f, 10.6f, 7f, 12f, 7f)
        curveTo(13.4f, 7f, 14.5f, 8f, 14.5f, 9.3f)
        curveTo(14.5f, 11.3f, 12f, 11.5f, 12f, 13.5f)
        moveTo(12f, 16.8f); lineTo(12f, 17f)
    }

    private fun stroked(
        name: String,
        autoMirror: Boolean = false,
        block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit,
    ): ImageVector = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f, autoMirror = autoMirror).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        )
    }.build()
}
