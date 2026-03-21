package com.aquiles.crosschapp.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================================
// PARTÍCULAS FLOTANTES
// ============================================================

private data class Particle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val speedY: Float,
    val speedX: Float,
    val alpha: Float,
    val phase: Float   // para el movimiento sinusoidal
)

@Composable
fun FloatingParticlesBackground(
    modifier: Modifier = Modifier,
    particleColor: Color = Color(0xFFFC5200),
    particleCount: Int = 24
) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    val particles = remember {
        List(particleCount) {
            Particle(
                x = (0..100).random() / 100f,
                y = (0..100).random() / 100f,
                radius = (2..5).random().toFloat(),
                speedY = (1..3).random() / 3000f,
                speedX = ((1..3).random() - 2) / 5000f,
                alpha = (2..7).random() / 10f,
                phase = (0..628).random() / 100f
            )
        }
    }

    Canvas(modifier = modifier) {
        particles.forEach { p ->
            val t = time
            val currentY = ((p.y - p.speedY * t) % 1f + 1f) % 1f
            val sineOffsetX = sin(t * 0.01f + p.phase) * 0.03f
            val currentX = (p.x + sineOffsetX + p.speedX * t).let { x ->
                ((x % 1f) + 1f) % 1f
            }

            // Fade in/out near top/bottom
            val fadeAlpha = when {
                currentY < 0.1f -> currentY / 0.1f
                currentY > 0.85f -> (1f - currentY) / 0.15f
                else -> 1f
            } * p.alpha

            drawCircle(
                color = particleColor.copy(alpha = fadeAlpha),
                radius = p.radius,
                center = Offset(currentX * size.width, currentY * size.height)
            )
        }
    }
}

// ============================================================
// GRADIENTE ANIMADO EN BACKGROUND DE CARDS
// ============================================================

@Composable
fun Modifier.animatedGlowGradient(
    color1: Color = Color(0xFFFC5200),
    color2: Color = Color(0xFF8B00FF),
    durationMs: Int = 5000
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    return this.drawBehind {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val offsetX = cos(phase) * cx * 0.3f
        val offsetY = sin(phase) * cy * 0.3f

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    color1.copy(alpha = 0.18f),
                    color2.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(cx + offsetX, cy + offsetY),
                radius = size.width * 0.8f
            )
        )
    }
}

// ============================================================
// AURA PULSANTE (para avatar)
// ============================================================

@Composable
fun Modifier.pulsingGlow(
    color: Color = Color(0xFFFC5200),
    minAlpha: Float = 0.25f,
    maxAlpha: Float = 0.6f,
    durationMs: Int = 1800
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    val radiusScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    return this.drawBehind {
        val radius = (size.minDimension / 2f) * radiusScale
        // Outer glow rings
        for (i in 3 downTo 1) {
            drawCircle(
                color = color.copy(alpha = alpha * (0.3f / i)),
                radius = radius + (i * 10f),
                style = Stroke(width = 4f)
            )
        }
        drawCircle(
            color = color.copy(alpha = alpha * 0.4f),
            radius = radius + 4f
        )
    }
}

// ============================================================
// SHIMMER (para loading)
// ============================================================

@Composable
fun Modifier.shimmerEffect(
    baseColor: Color = Color(0xFF2C2C2E),
    highlightColor: Color = Color(0xFF3C3C3E)
): Modifier {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    return this.drawBehind {
        val shimmerX = size.width * shimmerTranslate
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    baseColor,
                    baseColor,
                    highlightColor,
                    highlightColor.copy(alpha = 0.9f),
                    highlightColor,
                    baseColor,
                    baseColor
                ),
                start = Offset(shimmerX - size.width, 0f),
                end = Offset(shimmerX + size.width, size.height)
            )
        )
    }
}

// ============================================================
// ANILLO XP ANIMADO (para Perfil)
// ============================================================

@Composable
fun AnimatedXpRing(
    progress: Float,
    xp: Int,
    level: String,
    ringSize: Dp = 130.dp,
    trackColor: Color = Color.White.copy(alpha = 0.1f),
    progressColor: Color = Color(0xFFFC5200),
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "xpProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "xpGlint")
    val glintAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glintAngle"
    )

    val sweepAngle = animatedProgress * 360f

    Canvas(modifier = modifier.then(Modifier.size(ringSize))) {
        val strokeWidth = 12.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
        val arcSize = androidx.compose.ui.geometry.Size(diameter, diameter)

        // Track
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Progress gradient arc
        drawArc(
            brush = Brush.sweepGradient(
                colorStops = arrayOf(
                    0f to progressColor,
                    0.5f to Color(0xFFFFD700),
                    1f to progressColor
                )
            ),
            startAngle = -90f,
            sweepAngle = sweepAngle.coerceAtLeast(2f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Glint dot at the end of the arc
        if (sweepAngle > 5f) {
            val endAngleRad = Math.toRadians((-90f + sweepAngle).toDouble())
            val radius = diameter / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val dotCenter = Offset(
                (center.x + radius * cos(endAngleRad)).toFloat(),
                (center.y + radius * sin(endAngleRad)).toFloat()
            )
            drawCircle(color = Color.White, radius = strokeWidth / 2f, center = dotCenter)
            drawCircle(color = progressColor, radius = strokeWidth / 3f, center = dotCenter)
        }
    }
}

// ============================================================
// CONTADOR ANIMADO DE NÚMEROS
// ============================================================

@Composable
fun animatedCounter(target: Int, durationMs: Int = 1200): State<Int> {
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(target) {
        animatable.animateTo(
            targetValue = target.toFloat(),
            animationSpec = tween(durationMs, easing = FastOutSlowInEasing)
        )
    }
    return remember { derivedStateOf { animatable.value.toInt() } }
}
