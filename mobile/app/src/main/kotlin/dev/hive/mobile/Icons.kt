package dev.hive.mobile

// The app's icons, drawn as outlines on a 24 grid (the redesign's own, plus a few in the same hand), and Orbie, the
// smiling planet on Home.
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r},${cy} a$r,$r 0 1,0 ${2 * r},0 a$r,$r 0 1,0 ${-2 * r},0"

// outlines: every path stroked with round ends; `filled` paths are filled instead (a dot, a square)
private fun icon(vararg paths: String, width: Float = 1.9f, filled: List<String> = emptyList(), tilt: Float = 0f): ImageVector =
    ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        if (tilt != 0f) addGroup(rotate = tilt, pivotX = 12f, pivotY = 12f)
        paths.forEach { addPath(addPathNodes(it), stroke = SolidColor(Color.Black), strokeLineWidth = width, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) }
        if (tilt != 0f) clearGroup()
        filled.forEach { addPath(addPathNodes(it), fill = SolidColor(Color.Black)) }
    }.build()

object Ic {
    // the four tabs
    val home = ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        addPath(addPathNodes(circle(12f, 12f, 4f)), stroke = SolidColor(Color.Black), strokeLineWidth = 1.9f)
        addGroup(rotate = -20f, pivotX = 12f, pivotY = 12f)
        addPath(addPathNodes("M2,12 a10,4.5 0 1,0 20,0 a10,4.5 0 1,0 -20,0"), stroke = SolidColor(Color.Black), strokeLineWidth = 1.9f)
        clearGroup()
    }.build()
    val threads = icon("M4 5h16v11H9l-5 4z")
    val todos = icon("M8 4h8a4 4 0 0 1 4 4v8a4 4 0 0 1 -4 4H8a4 4 0 0 1 -4 -4V8a4 4 0 0 1 4 -4z", "M8.5 12l2.5 2.5 4.5-5")
    val clock = icon(circle(12f, 12f, 9f), "M12 7v5l3 2")

    val settings = icon(circle(12f, 12f, 3f), "M12 2v3M12 19v3M4.2 4.2l2.1 2.1M17.7 17.7l2.1 2.1M2 12h3M19 12h3M4.2 19.8l2.1-2.1M17.7 6.3l2.1-2.1", width = 1.8f)
    val back = icon("M15 5l-7 7 7 7", width = 2f)
    val next = icon("M9 5l7 7-7 7", width = 2f)
    val down = icon("M5 9l7 7 7-7", width = 2f)
    val plus = icon("M12 5v14M5 12h14", width = 2.4f)
    val send = icon("M12 19V5M6 11l6-6 6 6", width = 2.2f)
    val stop = icon(filled = listOf("M8 6h8a2 2 0 0 1 2 2v8a2 2 0 0 1 -2 2H8a2 2 0 0 1 -2 -2V8a2 2 0 0 1 2 -2z"))
    val attach = icon("M21.44 11.05l-9.19 9.19a6 6 0 0 1 -8.49 -8.49l9.19 -9.19a4 4 0 0 1 5.66 5.66l-9.2 9.19a2 2 0 0 1 -2.83 -2.83l8.49 -8.48")
    val alert = icon("M12 8v5M12 17h.01", circle(12f, 12f, 9f), width = 2f)
    val check = icon("M5 12.5l4.5 4.5L19 7.5", width = 2.6f)
    val close = icon("M18 6L6 18M6 6l12 12", width = 2f)
    val search = icon(circle(11f, 11f, 7f), "M20.5 20.5l-4.2-4.2", width = 2f)
    val goal = icon(circle(12f, 12f, 9f), circle(12f, 12f, 5f), width = 2f, filled = listOf(circle(12f, 12f, 1.5f)))
    val usage = icon("M18 20V10M12 20V4M6 20v-6", width = 2f)
    val running = icon("M22 12h-4l-3 9L9 3l-3 9H2")
    val notes = icon("M14 3H7a2 2 0 0 0 -2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2 -2V8z", "M14 3v5h5M15.5 13h-7M15.5 17h-7")
    val eye = icon("M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7z", circle(12f, 12f, 3f))
    val log = icon("M8 6h12M8 12h12M8 18h12M4 6h.01M4 12h.01M4 18h.01", width = 2f)
    val team = icon("M16 20v-1.5a4 4 0 0 0 -4 -4H6a4 4 0 0 0 -4 4V20", circle(9f, 7.5f, 3.5f), "M22 20v-1.5a4 4 0 0 0 -3 -3.85M15.5 4.2a3.5 3.5 0 0 1 0 6.7")
    val rewind = icon("M3 4v6h6", "M4.5 15a8.5 8.5 0 1 0 2 -8.8L3 10")
    val branch = icon("M6 3v12", circle(18f, 6f, 3f), circle(6f, 18f, 3f), "M18 9a9 9 0 0 1 -9 9")
    val shield = icon("M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z")
    val tool = icon("M5 17l5-5-5-5M12.5 18h6.5", width = 2f)
    val lock = icon("M6 11h12a1 1 0 0 1 1 1v8a1 1 0 0 1 -1 1H6a1 1 0 0 1 -1 -1v-8a1 1 0 0 1 1 -1z", "M8 11V7.5a4 4 0 0 1 8 0V11")
    val power = icon("M18.4 6.6a9 9 0 1 1 -12.8 0M12 2.5v9.5", width = 2f)
    val scan = icon("M3 7.5V5a2 2 0 0 1 2 -2h2.5M16.5 3H19a2 2 0 0 1 2 2v2.5M21 16.5V19a2 2 0 0 1 -2 2h-2.5M7.5 21H5a2 2 0 0 1 -2 -2v-2.5M7 12h10", width = 2f)
    val download = icon("M20 15v3a2 2 0 0 1 -2 2H6a2 2 0 0 1 -2 -2v-3M7.5 10.5L12 15l4.5-4.5M12 15V4", width = 2f)
    val play = icon("M7 4.5v15l12-7.5z")
}

// Orbie: the planet with its face, a tilted ring and two sparkles, and one blue moon per working thread (three at
// most), as the redesign draws it (a 150 grid).
@Composable
fun Orbie(size: Dp = 132.dp, moons: Int = 0) {
    val words = "Orbie, smiling" + when (moons.coerceIn(0, 3)) { 0 -> ""; 1 -> ", with one moon for a working thread"; else -> ", with ${moons.coerceIn(0, 3)} moons for the working threads" }
    val spark1 = androidx.compose.runtime.remember { PathParser().parsePathString("M22 40l2 5 5 2-5 2-2 5-2-5-5-2 5-2z").toPath() }
    val spark2 = androidx.compose.runtime.remember { PathParser().parsePathString("M128 22l1.5 3.5 3.5 1.5-3.5 1.5-1.5 3.5-1.5-3.5-3.5-1.5 3.5-1.5z").toPath() }
    val smile = androidx.compose.runtime.remember { PathParser().parsePathString("M65 86q10 8 20 0").toPath() }
    Canvas(Modifier.size(size).semantics { contentDescription = words }) {
        scale(this.size.width / 150f, pivot = Offset.Zero) {
            drawPath(spark1, C.accent, alpha = 0.8f)
            drawPath(spark2, C.hi, alpha = 0.7f)
            rotate(-16f, pivot = Offset(75f, 78f)) { drawOval(Color(0xFF2E354A), topLeft = Offset(7f, 54f), size = Size(136f, 48f), style = Stroke(1.5f)) }
            drawCircle(C.accent, 34f, Offset(75f, 76f))
            drawCircle(Color(0xFFFFD58A), 34f, Offset(75f, 76f), alpha = 0.5f, style = Stroke(2f))
            drawOval(C.bg, topLeft = Offset(58.5f, 66f), size = Size(9f, 12f))
            drawOval(C.bg, topLeft = Offset(82.5f, 66f), size = Size(9f, 12f))
            drawPath(smile, C.bg, style = Stroke(3f, cap = StrokeCap.Round))
            drawCircle(Color(0xFFE8894A), 4f, Offset(56f, 83f), alpha = 0.55f)
            drawCircle(Color(0xFFE8894A), 4f, Offset(94f, 83f), alpha = 0.55f)
            val at = listOf(Triple(12f, 94f, 7f), Triple(138f, 60f, 6f), Triple(112f, 112f, 5f))
            at.take(moons.coerceIn(0, 3)).forEach { (x, y, r) -> drawCircle(C.work, r, Offset(x, y)) }
        }
    }
}
