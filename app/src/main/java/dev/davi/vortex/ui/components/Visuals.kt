package dev.davi.vortex.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import dev.davi.vortex.ui.theme.VX
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Fundo animado: orbes de luz neon flutuando + grade sutil.
 * A animação é lida só na fase de desenho, então não recompõe a tela.
 */
@Composable
fun VortexBackground(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "bg")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing)),
        label = "phase",
    )
    Canvas(modifier.fillMaxSize()) {
        drawRect(VX.Bg)
        orb(size.width * (0.15f + 0.18f * cos(phase)), size.height * (0.12f + 0.06f * sin(phase)), size.width * 0.95f, VX.Violet, 0.30f)
        orb(size.width * (0.90f + 0.10f * sin(phase * 1.3f)), size.height * (0.42f + 0.10f * cos(phase)), size.width * 0.80f, VX.Cyan, 0.20f)
        orb(size.width * (0.30f + 0.20f * sin(phase * 0.7f)), size.height * (0.92f + 0.05f * cos(phase * 1.7f)), size.width * 0.90f, VX.Magenta, 0.14f)

        // Grade futurista que some em direção ao meio da tela
        val step = 44.dp.toPx()
        val fadeEnd = size.height * 0.55f
        var x = 0f
        while (x <= size.width) {
            drawLine(
                brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.045f), Color.Transparent), endY = fadeEnd),
                start = Offset(x, 0f), end = Offset(x, fadeEnd), strokeWidth = 1f,
            )
            x += step
        }
        var y = 0f
        while (y <= fadeEnd) {
            val a = 0.045f * (1f - y / fadeEnd)
            drawLine(Color.White.copy(alpha = a), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += step
        }
    }
}

private fun DrawScope.orb(cx: Float, cy: Float, radius: Float, color: Color, alpha: Float) {
    val c = Offset(cx, cy)
    drawCircle(
        brush = Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center = c, radius = radius),
        radius = radius,
        center = c,
    )
}

/** Logo: anel neon girando com uma seta de download no centro. */
@Composable
fun LogoMark(modifier: Modifier = Modifier, animated: Boolean = true) {
    val t = rememberInfiniteTransition(label = "logo")
    val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "rot")
    Canvas(modifier) {
        val stroke = size.minDimension * 0.09f
        val r = size.minDimension / 2 - stroke
        rotate(if (animated) rot else 0f) {
            drawCircle(
                brush = Brush.sweepGradient(listOf(VX.Cyan, VX.Violet, VX.Magenta, VX.Cyan), center),
                radius = r,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        val cx = center.x
        val cy = center.y
        val a = r * 0.55f
        val arrow = Path().apply {
            moveTo(cx, cy - a)
            lineTo(cx, cy + a * 0.85f)
            moveTo(cx - a * 0.6f, cy + a * 0.25f)
            lineTo(cx, cy + a * 0.85f)
            lineTo(cx + a * 0.6f, cy + a * 0.25f)
        }
        drawPath(
            arrow,
            brush = Brush.verticalGradient(listOf(VX.Cyan, VX.Violet), startY = cy - a, endY = cy + a),
            style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/** Anel de "escaneando" usado enquanto o link é analisado. */
@Composable
fun ScanningRing(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "scan")
    val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "r")
    val pulse by t.animateFloat(
        0.55f, 1f,
        infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p",
    )
    Canvas(modifier) {
        val stroke = size.minDimension * 0.08f
        val r = size.minDimension / 2 - stroke
        drawCircle(Color.White.copy(alpha = 0.06f), radius = r, style = Stroke(stroke))
        rotate(rot) {
            drawArc(
                brush = Brush.sweepGradient(listOf(Color.Transparent, VX.Violet, VX.Cyan), center),
                startAngle = 0f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = Offset(center.x - r, center.y - r),
                size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        drawCircle(
            Brush.radialGradient(listOf(VX.Cyan.copy(alpha = 0.9f), Color.Transparent), center, r * 0.5f * pulse),
            radius = r * 0.5f * pulse,
        )
    }
}
