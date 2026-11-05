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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rout.max.core.AssistantState
import kotlin.math.cos
import kotlin.math.sin

/**
 * JARVIS-style core: concentric reactor rings with a wireframe human face
 * at the center. Every animation maps to the live AssistantState.
 */
@Composable
fun JarvisCore(state: AssistantState, modifier: Modifier = Modifier, size: Dp = 220.dp) {
    val transition = rememberInfiniteTransition(label = "core")

    val spin by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(if (state == AssistantState.THINKING) 2200 else 14000, easing = LinearEasing)),
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
        val rf = R * 0.52f * breath
        val dim = if (active) 1f else 0.45f

        // --- outer tick ring ---
        val tickCount = 72
        for (i in 0 until tickCount) {
            val a = Math.toRadians((i * 360.0 / tickCount))
            val long = i % 6 == 0
            val r1 = R * (if (long) 0.90f else 0.94f)
            val r2 = R * 0.985f
            drawLine(
                color = glow.copy(alpha = (if (long) 0.55f else 0.28f) * dim),
                start = Offset((c.x + cos(a) * r1).toFloat(), (c.y + sin(a) * r1).toFloat()),
                end = Offset((c.x + cos(a) * r2).toFloat(), (c.y + sin(a) * r2).toFloat()),
                strokeWidth = if (long) 2.5f else 1.5f
            )
        }

        // --- rotating reactor arcs (two counter-rotating segmented arcs) ---
        drawArc(
            color = glow.copy(alpha = 0.85f * dim), startAngle = spin, sweepAngle = 110f,
            useCenter = false, topLeft = Offset(c.x - R * 0.86f, c.y - R * 0.86f),
            size = androidx.compose.ui.geometry.Size(R * 1.72f, R * 1.72f),
            style = Stroke(width = 4f)
        )
        drawArc(
            color = glow.copy(alpha = 0.4f * dim), startAngle = spin + 190f, sweepAngle = 60f,
            useCenter = false, topLeft = Offset(c.x - R * 0.86f, c.y - R * 0.86f),
            size = androidx.compose.ui.geometry.Size(R * 1.72f, R * 1.72f),
            style = Stroke(width = 2.5f)
        )
        drawArc(
            color = glow.copy(alpha = 0.7f * dim), startAngle = -spin * 1.35f + 40f, sweepAngle = 85f,
            useCenter = false, topLeft = Offset(c.x - R * 0.72f, c.y - R * 0.72f),
            size = androidx.compose.ui.geometry.Size(R * 1.44f, R * 1.44f),
            style = Stroke(width = 3f)
        )
        drawArc(
            color = glow.copy(alpha = 0.35f * dim), startAngle = -spin * 1.35f + 220f, sweepAngle = 45f,
            useCenter = false, topLeft = Offset(c.x - R * 0.72f, c.y - R * 0.72f),
            size = androidx.compose.ui.geometry.Size(R * 1.44f, R * 1.44f),
            style = Stroke(width = 2f)
        )

        // --- dashed inner ring around the face ---
        drawCircle(
            color = glow.copy(alpha = 0.5f * dim), radius = rf * 1.28f, center = c,
            style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 13f), spin))
        )

        // --- wireframe human face ---
        val u = rf
        fun px(x: Float) = c.x + x
        fun py(y: Float) = c.y + y

        // Head silhouette: forehead -> temples -> cheeks -> jaw -> chin (mirrored).
        val head = Path().apply {
            moveTo(px(0f), py(-1.00f * u))
            cubicTo(px(0.34f * u), py(-1.00f * u), px(0.62f * u), py(-0.82f * u), px(0.70f * u), py(-0.42f * u))
            cubicTo(px(0.75f * u), py(-0.15f * u), px(0.72f * u), py(0.05f * u), px(0.62f * u), py(0.28f * u))
            cubicTo(px(0.54f * u), py(0.50f * u), px(0.34f * u), py(0.78f * u), px(0f), py(0.94f * u))
            cubicTo(px(-0.34f * u), py(0.78f * u), px(-0.54f * u), py(0.50f * u), px(-0.62f * u), py(0.28f * u))
            cubicTo(px(-0.72f * u), py(0.05f * u), px(-0.75f * u), py(-0.15f * u), px(-0.70f * u), py(-0.42f * u))
            cubicTo(px(-0.62f * u), py(-0.82f * u), px(-0.34f * u), py(-1.00f * u), px(0f), py(-1.00f * u))
            close()
        }

        // Mesh latitude lines across the face.
        fun meshLine(yN: Float, halfW: Float, sag: Float): Path = Path().apply {
            moveTo(px(-halfW * u), py(yN * u))
            cubicTo(px(-halfW * 0.4f * u), py((yN + sag) * u), px(halfW * 0.4f * u), py((yN + sag) * u), px(halfW * u), py(yN * u))
        }
        val mesh = listOf(
            meshLine(-0.72f, 0.52f, -0.06f),
            meshLine(-0.42f, 0.68f, -0.04f),
            meshLine(-0.10f, 0.70f, 0.03f),
            meshLine(0.22f, 0.66f, 0.04f),
            meshLine(0.50f, 0.50f, 0.05f),
            meshLine(0.74f, 0.30f, 0.04f)
        )

        // Center meridian line.
        val meridian = Path().apply {
            moveTo(px(0f), py(-0.98f * u))
            cubicTo(px(0.03f * u), py(-0.4f * u), px(-0.03f * u), py(0.3f * u), px(0f), py(0.92f * u))
        }

        // Angular helmet-style eyes.
        fun eye(sign: Float): Path = Path().apply {
            val ex = 0.33f * sign
            moveTo(px((ex - 0.17f * sign) * u), py(-0.16f * u))
            lineTo(px((ex + 0.15f * sign) * u), py(-0.24f * u))
            lineTo(px((ex + 0.11f * sign) * u), py(-0.09f * u))
            lineTo(px((ex - 0.13f * sign) * u), py(-0.05f * u))
            close()
        }
        val eyeL = eye(-1f)
        val eyeR = eye(1f)

        // Brow arc.
        val brow = Path().apply {
            moveTo(px(-0.52f * u), py(-0.34f * u))
            cubicTo(px(-0.2f * u), py(-0.42f * u), px(0.2f * u), py(-0.42f * u), px(0.52f * u), py(-0.34f * u))
        }

        // Nose.
        val nose = Path().apply {
            moveTo(px(0f), py(-0.02f * u))
            lineTo(px(-0.02f * u), py(0.28f * u))
            lineTo(px(0.06f * u), py(0.32f * u))
        }

        // Mouth - animates while speaking.
        val talk = if (state == AssistantState.SPEAKING) sin(talkPhase) * 0.05f else 0f
        val mouth = Path().apply {
            moveTo(px(-0.24f * u), py(0.56f * u))
            cubicTo(px(-0.08f * u), py((0.58f + talk) * u), px(0.08f * u), py((0.58f + talk) * u), px(0.24f * u), py(0.56f * u))
        }

        // Cheek tech lines.
        val cheeks = Path().apply {
            moveTo(px(0.48f * u), py(0.10f * u)); lineTo(px(0.34f * u), py(0.40f * u))
            moveTo(px(-0.48f * u), py(0.10f * u)); lineTo(px(-0.34f * u), py(0.40f * u))
        }

        // Draw: soft wide glow pass, then crisp pass.
        val soft = Stroke(width = 7f)
        val crisp = Stroke(width = 2.2f)
        val paths = listOf(head, meridian, brow, nose, mouth, cheeks) + mesh + listOf(eyeL, eyeR)
        for (p in paths) drawPath(p, color = glow.copy(alpha = 0.14f * dim), style = soft)
        for (p in mesh) drawPath(p, color = glow.copy(alpha = 0.30f * dim), style = crisp)
        drawPath(head, color = glow.copy(alpha = 0.85f * dim), style = crisp)
        drawPath(meridian, color = glow.copy(alpha = 0.30f * dim), style = crisp)
        drawPath(brow, color = glow.copy(alpha = 0.7f * dim), style = crisp)
        drawPath(nose, color = glow.copy(alpha = 0.7f * dim), style = crisp)
        drawPath(mouth, color = glow.copy(alpha = 0.85f * dim), style = crisp)
        drawPath(cheeks, color = glow.copy(alpha = 0.55f * dim), style = crisp)

        // Eyes: filled glow when active, brighter while listening/speaking.
        val eyeAlpha = when (state) {
            AssistantState.LISTENING, AssistantState.SPEAKING -> 0.75f
            AssistantState.STOPPED -> 0.10f
            else -> 0.40f
        }
        drawPath(eyeL, color = glow.copy(alpha = eyeAlpha))
        drawPath(eyeR, color = glow.copy(alpha = eyeAlpha))
        drawPath(eyeL, color = glow.copy(alpha = 0.9f * dim), style = crisp)
        drawPath(eyeR, color = glow.copy(alpha = 0.9f * dim), style = crisp)
    }
}

/** Thin cyan HUD readout panel: small caps label + monospace value. */
@Composable
fun HudPanel(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaxColors.Glass, RoundedCornerShape(6.dp))
            .border(1.dp, MaxColors.HudBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = MaxColors.HudCyan.copy(alpha = 0.65f), fontSize = 8.sp,
            fontFamily = FontFamily.Monospace, letterSpacing = 2.sp)
        Text(value, color = MaxColors.HudCyan, fontSize = 13.sp,
            fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}

/** Live waveform strip: amplitude and speed follow the assistant state. */
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
    Canvas(modifier = modifier.fillMaxWidth().height(34.dp)) {
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
            val envelope = sin(Math.PI * i / n).toFloat()      // taper at both ends
            val jitter = if (active) sin(phase * 3 + i * 1.7f).toFloat() * 0.35f else 0f
            val y = mid + sin(i * 0.55f + phase).toFloat() * baseAmp * envelope * (1f + jitter)
            drawLine(
                color = MaxColors.HudCyan.copy(alpha = if (active) 0.9f else 0.45f),
                start = Offset(prevX, prevY), end = Offset(x, y), strokeWidth = 2.5f
            )
            prevX = x; prevY = y
        }
    }
}
