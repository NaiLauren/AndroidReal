package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.CircularProgressIndicator
import com.aquiles.crosschapp.presentation.components.GlassCard
import java.util.Calendar

@Composable
fun HomeGreetingCard(userName: String, profileImageUrl: String? = null) {
    val greeting = getGreetingMessage()
    val emoji = getGreetingEmoji()
    val motivation = "Listo para darlo todo hoy. Revisa los Horarios y reserva tu clase."

    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box {
            // Watermark decorativo
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.04f),
                modifier = Modifier
                    .size(140.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 20.dp, y = 20.dp)
                    .rotate(-25f)
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
                        modifier = Modifier.weight(1f) // Para que el texto tome el espacio disponible
                    )
                    
                    // Foto de Perfil Cricular
                    Box(
                        modifier = Modifier
                            .size(70.dp) // Tamaño generoso para destacar
                            .clip(CircleShape)
                            .background(Color.Gray.copy(alpha = 0.3f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        SubcomposeAsyncImage(
                            model = profileImageUrl,
                            contentDescription = "Foto de perfil",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            loading = { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary) },
                            error = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(35.dp)) }
                        )
                    }
                }

                // Separador con Gradiente
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.2f),
                                    Color.White.copy(alpha = 0.2f),
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

