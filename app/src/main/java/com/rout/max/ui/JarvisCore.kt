package com.rout.max.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rout.max.core.AssistantState
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private fun headPath(cx: Float, cy: Float, u: Float): Path = Path().apply {
    moveTo(cx, cy - 1.00f * u)
    cubicTo(cx + 0.34f * u, cy - 1.00f * u, cx + 0.62f * u, cy - 0.82f * u, cx + 0.70f * u, cy - 0.42f * u)
    cubicTo(cx + 0.75f * u, cy - 0.15f * u, cx + 0.72f * u, cy + 0.05f * u, cx + 0.62f * u, cy + 0.28f * u)
    cubicTo(cx + 0.54f * u, cy + 0.50f * u, cx + 0.34f * u, cy + 0.78f * u, cx, cy + 0.94f * u)
    cubicTo(cx - 0.34f * u, cy + 0.78f * u, cx - 0.54f * u, cy + 0.50f * u, cx - 0.62f * u, cy + 0.28f * u)
    cubicTo(cx - 0.72f * u, cy + 0.05f * u, cx - 0.75f * u, cy - 0.15f * u, cx - 0.70f * u, cy - 0.42f * u)
    cubicTo(cx - 0.62f * u, cy - 0.82f * u, cx - 0.34f * u, cy - 1.00f * u, cx, cy - 1.00f * u)
    close()
}

/** Approx half-width of the head silhouette at normalized height yN. */
private fun headHalfW(yN: Float): Float {
    val t = 1f - (yN * 0.98f) * (yN * 0.98f)
    return 0.72f * sqrt(if (t > 0.04f) t else 0.04f)
}

/**
 * JARVIS-style core: reactor rings around a dense wireframe human face with
 * grid backdrop and scan line. All motion maps to the live AssistantState.
 */
@Composable
fun JarvisCore(state: AssistantState, modifier: Modifier = Modifier, size: Dp = 200.dp) {
    val transition = rememberInfiniteTransition(label = "core")

    val spin by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(if (state == AssistantState.THINKING) 2200 else 16000, easing = LinearEasing)),
        label = "spin"
    )
    val breath by transition.animateFloat(
        initialValue = 0.97f, targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            tween(
                when (state) {
                    AssistantState.SPEAKING -> 800
                    AssistantState.LISTENING -> 1200
                    AssistantState.THINKING -> 1000
                    AssistantState.EXECUTING -> 1400
                    AssistantState.STOPPED -> 3600
                    AssistantState.IDLE -> 2400
                }, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath"
    )
    val talkPhase by transition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing)),
        label = "talk"
    )
    val scan by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3400, easing = LinearEasing), RepeatMode.Reverse),
        label = "scan"
    )

    val active = state != AssistantState.STOPPED
    val glow = when (state) {
        AssistantState.IDLE -> MaxColors.HudCyan
        AssistantState.LISTENING -> MaxColors.HudCyanBright
        AssistantState.THINKING -> MaxColors.HudBlue
        AssistantState.SPEAKING -> MaxColors.HudCyanBright
        AssistantState.EXECUTING -> MaxColors.HudBlue
        AssistantState.STOPPED -> Color(0xFF4A4A52)
    }

    Canvas(modifier = modifier.size(size)) {
        val c = center
        val R = this.size.minDimension / 2f * 0.97f
        val u = R * 0.50f * breath
        val dim = if (active) 1f else 0.45f

        // --- faint HUD grid backdrop behind everything ---
        val gridStep = this.size.minDimension / 14f
        var gx = 0f
        while (gx <= this.size.width) {
            drawLine(glow.copy(alpha = 0.05f * dim), Offset(gx, 0f), Offset(gx, this.size.height), 1f)
            gx += gridStep
        }
        var gy = 0f
        while (gy <= this.size.height) {
            drawLine(glow.copy(alpha = 0.05f * dim), Offset(0f, gy), Offset(this.size.width, gy), 1f)
            gy += gridStep
        }

        // --- outer tick ring ---
        val tickCount = 72
        for (i in 0 until tickCount) {
            val a = Math.toRadians((i * 360.0 / tickCount))
            val long = i % 6 == 0
            val r1 = R * (if (long) 0.90f else 0.94f)
            val r2 = R * 0.985f
            drawLine(
                color = glow.copy(alpha = (if (long) 0.55f else 0.26f) * dim),
                start = Offset((c.x + cos(a) * r1).toFloat(), (c.y + sin(a) * r1).toFloat()),
                end = Offset((c.x + cos(a) * r2).toFloat(), (c.y + sin(a) * r2).toFloat()),
                strokeWidth = if (long) 2.5f else 1.5f
            )
        }

        // --- counter-rotating reactor arcs ---
        drawArc(color = glow.copy(alpha = 0.85f * dim), startAngle = spin, sweepAngle = 110f, useCenter = false,
            topLeft = Offset(c.x - R * 0.86f, c.y - R * 0.86f), size = Size(R * 1.72f, R * 1.72f), style = Stroke(width = 4f))
        drawArc(color = glow.copy(alpha = 0.38f * dim), startAngle = spin + 190f, sweepAngle = 60f, useCenter = false,
            topLeft = Offset(c.x - R * 0.86f, c.y - R * 0.86f), size = Size(R * 1.72f, R * 1.72f), style = Stroke(width = 2.5f))
        drawArc(color = glow.copy(alpha = 0.70f * dim), startAngle = -spin * 1.35f + 40f, sweepAngle = 85f, useCenter = false,
            topLeft = Offset(c.x - R * 0.72f, c.y - R * 0.72f), size = Size(R * 1.44f, R * 1.44f), style = Stroke(width = 3f))
        drawArc(color = glow.copy(alpha = 0.32f * dim), startAngle = -spin * 1.35f + 220f, sweepAngle = 45f, useCenter = false,
            topLeft = Offset(c.x - R * 0.72f, c.y - R * 0.72f), size = Size(R * 1.44f, R * 1.44f), style = Stroke(width = 2f))

        // --- dashed ring around the face ---
        drawCircle(color = glow.copy(alpha = 0.5f * dim), radius = u * 1.30f, center = c,
            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 12f), spin)))

        val head = headPath(c.x, c.y, u)
        val soft = Stroke(width = 7f)
        val crisp = Stroke(width = 2f)
        val fine = Stroke(width = 1.2f)

        // --- dense wireframe mesh, clipped inside the head silhouette ---
        clipPath(head) {
            // latitude lines
            var yN = -0.92f
            while (yN <= 0.92f) {
                val hw = headHalfW(yN)
                val p = Path().apply {
                    moveTo(c.x - hw * u, c.y + yN * u)
                    cubicTo(c.x - hw * 0.4f * u, c.y + (yN + 0.03f) * u,
                        c.x + hw * 0.4f * u, c.y + (yN + 0.03f) * u, c.x + hw * u, c.y + yN * u)
                }
                drawPath(p, glow.copy(alpha = 0.30f * dim), style = fine)
                yN += 0.115f
            }
            // longitude lines
            for (k in listOf(-0.55f, -0.35f, -0.17f, 0f, 0.17f, 0.35f, 0.55f)) {
                val p = Path().apply {
                    moveTo(c.x + k * 0.75f * u, c.y - 0.95f * u)
                    cubicTo(c.x + k * 1.0f * u, c.y - 0.3f * u,
                        c.x + k * 1.0f * u, c.y + 0.3f * u, c.x + k * 0.55f * u, c.y + 0.85f * u)
                }
                drawPath(p, glow.copy(alpha = 0.30f * dim), style = fine)
            }
            // scan line sweeping over the face
            val sy = c.y - u + scan * 2f * u
            drawRect(glow.copy(alpha = if (active) 0.16f else 0.05f),
                topLeft = Offset(c.x - u, sy - 6f), size = Size(2f * u, 12f))
            drawLine(glow.copy(alpha = if (active) 0.55f else 0.15f),
                Offset(c.x - u, sy), Offset(c.x + u, sy), 2f)
        }

        // --- facial features on top of the mesh ---
        // double brow
        for (dy in listOf(0f, 0.045f)) {
            val brow = Path().apply {
                moveTo(c.x - 0.52f * u, c.y + (-0.34f + dy) * u)
                cubicTo(c.x - 0.2f * u, c.y + (-0.42f + dy) * u, c.x + 0.2f * u, c.y + (-0.42f + dy) * u, c.x + 0.52f * u, c.y + (-0.34f + dy) * u)
            }
            drawPath(brow, glow.copy(alpha = 0.65f * dim), style = crisp)
        }
        // almond eyes with inner lid line
        fun eye(sign: Float, inset: Float, lift: Float): Path = Path().apply {
            val ex = 0.33f * sign
            moveTo(c.x + (ex - (0.17f - inset) * sign) * u, c.y + (-0.15f + lift) * u)
            cubicTo(c.x + (ex - 0.06f * sign) * u, c.y + (-0.26f + lift) * u,
                c.x + (ex + 0.06f * sign) * u, c.y + (-0.26f + lift) * u, c.x + (ex + (0.15f - inset) * sign) * u, c.y + (-0.20f + lift) * u)
            cubicTo(c.x + (ex + 0.06f * sign) * u, c.y + (-0.08f + lift) * u,
                c.x + (ex - 0.06f * sign) * u, c.y + (-0.07f + lift) * u, c.x + (ex - (0.17f - inset) * sign) * u, c.y + (-0.15f + lift) * u)
            close()
        }
        val eyeAlpha = when (state) {
            AssistantState.LISTENING, AssistantState.SPEAKING -> 0.70f
            AssistantState.STOPPED -> 0.08f
            else -> 0.35f
        }
        for (s in listOf(-1f, 1f)) {
            drawPath(eye(s, 0f, 0f), glow.copy(alpha = eyeAlpha))
            drawPath(eye(s, 0f, 0f), glow.copy(alpha = 0.9f * dim), style = crisp)
            drawPath(eye(s, 0.05f, 0.02f), glow.copy(alpha = 0.5f * dim), style = fine)
        }
        // nose: bridge sides + nostrils
        val nose = Path().apply {
            moveTo(c.x - 0.05f * u, c.y - 0.05f * u); lineTo(c.x - 0.09f * u, c.y + 0.26f * u); lineTo(c.x - 0.02f * u, c.y + 0.31f * u)
            moveTo(c.x + 0.05f * u, c.y - 0.05f * u); lineTo(c.x + 0.09f * u, c.y + 0.26f * u); lineTo(c.x + 0.02f * u, c.y + 0.31f * u)
            moveTo(c.x - 0.10f * u, c.y + 0.24f * u); lineTo(c.x - 0.13f * u, c.y + 0.28f * u)
            moveTo(c.x + 0.10f * u, c.y + 0.24f * u); lineTo(c.x + 0.13f * u, c.y + 0.28f * u)
        }
        drawPath(nose, glow.copy(alpha = 0.7f * dim), style = crisp)
        // lips: upper M + lower; mouth animates while speaking
        val talk = if (state == AssistantState.SPEAKING) sin(talkPhase) * 0.045f else 0f
        val lips = Path().apply {
            moveTo(c.x - 0.24f * u, c.y + (0.52f + talk) * u)
            cubicTo(c.x - 0.10f * u, c.y + (0.47f + talk) * u, c.x - 0.04f * u, c.y + (0.50f + talk) * u, c.x, c.y + (0.52f + talk) * u)
            cubicTo(c.x + 0.04f * u, c.y + (0.50f + talk) * u, c.x + 0.10f * u, c.y + (0.47f + talk) * u, c.x + 0.24f * u, c.y + (0.52f + talk) * u)
            moveTo(c.x - 0.16f * u, c.y + (0.58f + talk) * u)
            cubicTo(c.x - 0.05f * u, c.y + (0.64f + talk) * u, c.x + 0.05f * u, c.y + (0.64f + talk) * u, c.x + 0.16f * u, c.y + (0.58f + talk) * u)
        }
        drawPath(lips, glow.copy(alpha = 0.8f * dim), style = crisp)
        // cheekbones + jaw accents + chin arc + ears + temples
        val detail = Path().apply {
            moveTo(c.x - 0.60f * u, c.y + 0.02f * u); cubicTo(c.x - 0.45f * u, c.y + 0.14f * u, c.x - 0.35f * u, c.y + 0.22f * u, c.x - 0.25f * u, c.y + 0.30f * u)
            moveTo(c.x + 0.60f * u, c.y + 0.02f * u); cubicTo(c.x + 0.45f * u, c.y + 0.14f * u, c.x + 0.35f * u, c.y + 0.22f * u, c.x + 0.25f * u, c.y + 0.30f * u)
            moveTo(c.x - 0.44f * u, c.y + 0.52f * u); lineTo(c.x - 0.30f * u, c.y + 0.70f * u)
            moveTo(c.x + 0.44f * u, c.y + 0.52f * u); lineTo(c.x + 0.30f * u, c.y + 0.70f * u)
            moveTo(c.x - 0.14f * u, c.y + 0.72f * u); cubicTo(c.x - 0.05f * u, c.y + 0.78f * u, c.x + 0.05f * u, c.y + 0.78f * u, c.x + 0.14f * u, c.y + 0.72f * u)
            moveTo(c.x - 0.71f * u, c.y - 0.10f * u); cubicTo(c.x - 0.78f * u, c.y - 0.02f * u, c.x - 0.78f * u, c.y + 0.08f * u, c.x - 0.70f * u, c.y + 0.14f * u)
            moveTo(c.x + 0.71f * u, c.y - 0.10f * u); cubicTo(c.x + 0.78f * u, c.y - 0.02f * u, c.x + 0.78f * u, c.y + 0.08f * u, c.x + 0.70f * u, c.y + 0.14f * u)
            moveTo(c.x - 0.62f * u, c.y - 0.62f * u); lineTo(c.x - 0.48f * u, c.y - 0.70f * u)
            moveTo(c.x + 0.62f * u, c.y - 0.62f * u); lineTo(c.x + 0.48f * u, c.y - 0.70f * u)
        }
        drawPath(detail, glow.copy(alpha = 0.55f * dim), style = crisp)

        // head outline: glow pass + crisp pass
        drawPath(head, glow.copy(alpha = 0.14f * dim), style = soft)
        drawPath(head, glow.copy(alpha = 0.9f * dim), style = crisp)
    }
}

/** Thin cyan HUD readout panel: small caps label + monospace value (REAL data only). */
@Composable
fun HudPanel(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaxColors.Glass, RoundedCornerShape(3.dp))
            .border(1.dp, MaxColors.HudBorder, RoundedCornerShape(3.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = MaxColors.HudCyan.copy(alpha = 0.65f), fontSize = 8.sp,
            fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        Text(value, color = MaxColors.HudCyan, fontSize = 13.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

/** Faint full-screen HUD grid backdrop for screens. */
@Composable
fun HudGridBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val step = this.size.minDimension / 12f
        var x = 0f
        while (x <= this.size.width) {
            drawLine(MaxColors.HudCyan.copy(alpha = 0.03f), Offset(x, 0f), Offset(x, this.size.height), 1f)
            x += step
        }
        var y = 0f
        while (y <= this.size.height) {
            drawLine(MaxColors.HudCyan.copy(alpha = 0.03f), Offset(0f, y), Offset(this.size.width, y), 1f)
            y += step
        }
    }
}

/** Decorative animated line graph panel (state animation, not a measurement). */
@Composable
fun HudLineGraph(state: AssistantState, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "lg")
    val phase by t.animateFloat(0f, (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(if (state == AssistantState.SPEAKING || state == AssistantState.LISTENING) 1600 else 5000, easing = LinearEasing)), "p")
    Column(modifier = modifier
        .background(MaxColors.Glass, RoundedCornerShape(3.dp))
        .border(1.dp, MaxColors.HudBorder, RoundedCornerShape(3.dp))
        .padding(4.dp)) {
        Canvas(modifier = Modifier.fillMaxWidth().height(34.dp)) {
            val n = 26
            var px = 0f; var py = this.size.height / 2f
            for (i in 1..n) {
                val x = this.size.width * i / n
                val y = this.size.height / 2f +
                    (sin(i * 0.9f + phase) * 0.5f + sin(i * 0.37f - phase * 1.3f) * 0.5f) * this.size.height * 0.32f
                drawLine(MaxColors.HudCyan.copy(alpha = 0.7f), Offset(px, py), Offset(x, y), 2f)
                px = x; py = y
            }
        }
    }
}

/** Decorative equalizer bars (state animation). */
@Composable
fun HudBars(state: AssistantState, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "bars")
    val phase by t.animateFloat(0f, (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(if (state == AssistantState.SPEAKING) 500 else 2000, easing = LinearEasing)), "p")
    Canvas(modifier = modifier.fillMaxWidth().height(44.dp)) {
        val bars = 5
        val bw = this.size.width / (bars * 2f)
        for (i in 0 until bars) {
            val amp = (sin(phase + i * 1.3f) * 0.5f + 0.5f)
            val h = this.size.height * (0.25f + 0.7f * amp)
            drawRect(
                MaxColors.HudCyan.copy(alpha = 0.55f),
                topLeft = Offset(bw * (i * 2f + 0.5f), this.size.height - h),
                size = Size(bw, h)
            )
        }
    }
}

/** Decorative mini gauge: slow sweeping arc + tick (state animation). */
@Composable
fun HudGauge(state: AssistantState, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "gauge")
    val sweep by t.animateFloat(0f, 360f,
        infiniteRepeatable(tween(if (state == AssistantState.THINKING) 1800 else 6000, easing = LinearEasing)), "s")
    Canvas(modifier = modifier.size(34.dp)) {
        val r = this.size.minDimension / 2f - 2f
        drawCircle(color = MaxColors.HudCyan.copy(alpha = 0.25f), radius = r, center = center, style = Stroke(2f))
        drawArc(color = MaxColors.HudCyan.copy(alpha = 0.8f), startAngle = sweep, sweepAngle = 70f, useCenter = false,
            topLeft = Offset(center.x - r, center.y - r), size = Size(r * 2, r * 2), style = Stroke(3f))
        drawCircle(color = MaxColors.HudCyan.copy(alpha = 0.5f), radius = r * 0.35f, center = center, style = Stroke(1.5f))
    }
}

/** Decorative static bar rows, like data list lines. */
@Composable
fun HudBarRows(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(30.dp)) {
        val widths = listOf(0.9f, 0.6f, 0.75f)
        for (i in 0..2) {
            val y = this.size.height * (i + 0.5f) / 3f
            drawLine(MaxColors.HudCyan.copy(alpha = 0.35f),
                Offset(0f, y), Offset(this.size.width * widths[i], y), 2.5f)
        }
    }
}

/** Live waveform strip: animation tied to assistant state (not an audio measurement). */
@Composable
fun WaveformStrip(state: AssistantState, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            tween(if (state == AssistantState.SPEAKING || state == AssistantState.LISTENING) 900 else 3200,
                easing = LinearEasing)),
        label = "phase"
    )
    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(34.dp)) {
            val w = this.size.width
            val h = this.size.height
            val mid = h / 2f
            val active = state == AssistantState.SPEAKING || state == AssistantState.LISTENING
            val baseAmp = when (state) {
                AssistantState.SPEAKING -> h * 0.34f
                AssistantState.LISTENING -> h * 0.28f
                AssistantState.THINKING -> h * 0.16f
                AssistantState.EXECUTING -> h * 0.16f
                AssistantState.STOPPED -> h * 0.04f
                AssistantState.IDLE -> h * 0.10f
            }
            val n = 72
            var prevX = 0f
            var prevY = mid
            for (i in 1..n) {
                val x = w * i / n
                val envelope = sin(Math.PI * i / n).toFloat()
                val jitter = if (active) sin(phase * 3 + i * 1.7f).toFloat() * 0.35f else 0f
                val y = mid + sin(i * 0.55f + phase).toFloat() * baseAmp * envelope * (1f + jitter)
                drawLine(
                    color = MaxColors.HudCyan.copy(alpha = if (active) 0.9f else 0.45f),
                    start = Offset(prevX, prevY), end = Offset(x, y), strokeWidth = 2.5f
                )
                prevX = x; prevY = y
            }
        }
        Text("ACTIVITY - state animation", color = MaxColors.TextSecondary, fontSize = 7.sp,
            fontFamily = FontFamily.Monospace, letterSpacing = 2.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}
