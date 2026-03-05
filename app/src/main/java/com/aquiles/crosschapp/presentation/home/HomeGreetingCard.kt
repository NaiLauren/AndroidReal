package com.aquiles.crosschapp.presentation.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.aquiles.crosschapp.presentation.components.FloatingParticlesBackground
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.components.animatedGlowGradient
import com.aquiles.crosschapp.presentation.components.pulsingGlow
import java.util.Calendar

@Composable
fun HomeGreetingCard(userName: String, profileImageUrl: String? = null) {
    val greeting = getGreetingMessage()
    val emoji = getGreetingEmoji()
    val motivation = "Listo para darlo todo hoy. Revisa los Horarios y reserva tu clase."

    // Animación de entrada del texto (cuenta regresiva suave)
    val textVisible = remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { textVisible.value = true }

    val infiniteTransition = rememberInfiniteTransition(label = "greetingCard")

    // Rotación suave del watermark
    val watermarkRotation by infiniteTransition.animateFloat(
        initialValue = -25f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "watermarkRot"
    )

    // Pulso de escala del watermark
    val watermarkScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "watermarkScale"
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
            .animatedGlowGradient(
                color1 = Color(0xFFFC5200),
                color2 = Color(0xFF8B00FF),
                durationMs = 6000
            ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box {
            // Partículas flotantes dentro de la card
            FloatingParticlesBackground(
                modifier = Modifier.matchParentSize(),
                particleColor = Color(0xFFFC5200),
                particleCount = 14
            )

            // Watermark decorativo animado
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.05f),
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 20.dp, y = 20.dp)
                    .rotate(watermarkRotation)
                    .scale(watermarkScale)
            )

            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header: Saludo y Foto de Perfil
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Normal
                            )) {
                                append("$greeting,\n")
                            }
                            withStyle(style = SpanStyle(
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold
                            )) {
                                append(userName)
                            }
                            withStyle(style = SpanStyle(fontSize = 32.sp)) {
                                append(" $emoji")
                            }
                        },
                        lineHeight = 38.sp,
                        modifier = Modifier.weight(1f)
                    )

                    // Avatar con aura pulsante
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .pulsingGlow(
                                color = Color(0xFFFC5200),
                                minAlpha = 0.2f,
                                maxAlpha = 0.55f,
                                durationMs = 1800
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(Color(0xFFFC5200).copy(0.3f), Color.Gray.copy(0.3f))
                                    ),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            SubcomposeAsyncImage(
                                model = profileImageUrl,
                                contentDescription = "Foto de perfil",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop,
                                loading = {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                },
                                error = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(35.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // Separador con Gradiente animado
                val glowAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.15f,
                    targetValue = 0.45f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dividerAlpha"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFFC5200).copy(alpha = glowAlpha),
                                    Color.White.copy(alpha = glowAlpha),
                                    Color(0xFFFC5200).copy(alpha = glowAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Mensaje motivacional
                Text(
                    text = motivation,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = 22.sp
                )
            }
        }
    }
}

private fun getGreetingMessage(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Buenos días"
        in 12..19 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}

private fun getGreetingEmoji(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..8 -> "🌅"
        in 9..11 -> "☀️"
        in 12..17 -> "💪"
        in 18..19 -> "🌇"
        else -> "🌙"
    }
}
