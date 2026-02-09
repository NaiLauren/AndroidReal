package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.viewmodel.FeedUiItem

private val ColorPrimaryAction = Color(0xFFFC5200)

enum class MedalColor {
    Gold, Silver, Bronze
}

@Composable
fun PodiumCard(
    result: FeedUiItem,
    position: Int,
    medalColor: MedalColor,
    isLarger: Boolean = false
) {
    val size = if (isLarger) 140.dp else 110.dp
    val borderBrush = when (medalColor) {
        MedalColor.Gold -> Brush.linearGradient(
            colors = listOf(Color(0xFFFFD700), Color(0xFFFFB340))
        )
        MedalColor.Silver -> Brush.linearGradient(
            colors = listOf(Color(0xFFC0C0C0), Color(0xFFE8E8E8))
        )
        MedalColor.Bronze -> Brush.linearGradient(
            colors = listOf(Color(0xFFCD7F32), Color(0xFFB87333))
        )
    }
    
    // GlassCard acts as container
    GlassCard(
        modifier = Modifier.width(size)
        // .border logic could be added here if GlassCard supported it, or via wrapper.
        // For now, let's just use GlassCard style. Or maybe wrap content.
    ) {
         Box(modifier = Modifier.matchParentSize().border(2.dp, borderBrush, RoundedCornerShape(20.dp))) // Add border manually inside

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Medalla emoji
            val medalEmoji = when (position) {
                1 -> "🥇"
                2 -> "🥈"
                3 -> "🥉"
                else -> "🏅"
            }
            Text(
                text = medalEmoji,
                style = MaterialTheme.typography.headlineMedium
            )
            
            // Foto de perfil con borde del color de medalla
            AsyncImage(
                model = result.userProfileImageUrl?.ifEmpty { null },
                contentDescription = null,
                modifier = Modifier
                    .size(if (isLarger) 60.dp else 50.dp)
                    .clip(CircleShape)
                    .border(2.dp, borderBrush, CircleShape)
            )
            
            // Nombre
            Text(
                text = result.userName.split(" ").firstOrNull() ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
            
            // Score
            Text(
                text = result.score,
                style = if (isLarger) 
                    MaterialTheme.typography.titleLarge 
                else 
                    MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ColorPrimaryAction
            )
            
            // RX Badge
            if (result.isRx) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = ColorPrimaryAction.copy(0.2f),
                    border = BorderStroke(1.dp, ColorPrimaryAction)
                ) {
                    Text(
                        text = "RX",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorPrimaryAction,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
