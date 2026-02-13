package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aquiles.crosschapp.data.model.GymNotice
import com.aquiles.crosschapp.data.model.NoticePriority
import com.aquiles.crosschapp.data.model.NoticeType
import com.google.firebase.Timestamp
import com.aquiles.crosschapp.presentation.components.GlassCard
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable



@Composable
fun NoticeBoardCard(
    notice: GymNotice,
    isAdmin: Boolean = false,
    onDelete: (() -> Unit)? = null
) {
    val gymPrimaryColor = try { 
        Color(android.graphics.Color.parseColor(com.aquiles.crosschapp.presentation.viewmodel.UserSession.currentGym.value?.primaryColor ?: "#FC5200")) 
    } catch (e: Exception) { 
        Color(0xFFFC5200) 
    }

    val tintColor = when (notice.priority) {
        NoticePriority.HIGH, NoticePriority.PINNED -> Color(0xFFFF6B35).copy(alpha = 0.15f)
        else -> Color.Transparent // Includes null (iOS notices)
    }

    val borderColor = when (notice.priority) {
        NoticePriority.HIGH, NoticePriority.PINNED -> Color(0xFFFF6B35).copy(alpha = 0.5f)
        else -> Color.White.copy(0.1f) // Includes null (iOS notices)
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(260.dp) // Fixed height for immersive look
    ) {
        val hasImage = !notice.imageUrl.isNullOrEmpty()

        // 1. Background Image (if exists)
        if (hasImage) {
             AsyncImage(
                model = notice.imageUrl,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
            
            // 2. Scrim (Gradient Overlay for readability)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f),
                                Color.Black.copy(alpha = 0.9f)
                            ),
                            startY = 100f // Start gradient earlier
                        )
                    )
            )
        }

        // 3. Content Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header (Icon & Priority)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Icon (with background for visibility)
                 Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black.copy(0.4f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (notice.type) {
                            NoticeType.CLASS_CANCELLED -> Icons.Default.Cancel
                            NoticeType.EVENT -> Icons.Default.EmojiEvents
                            NoticeType.ACHIEVEMENT -> Icons.Default.Star
                            NoticeType.PRICE_CHANGE -> Icons.Default.AttachMoney
                            NoticeType.GENERAL -> Icons.Default.Campaign
                            NoticeType.ANNOUNCEMENT -> Icons.Default.Campaign
                            null -> Icons.Default.Campaign // iOS notices without type
                        },
                        contentDescription = null,
                        tint = gymPrimaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (isAdmin && onDelete != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color.Black.copy(0.4f), androidx.compose.foundation.shape.CircleShape)
                            .clickable { onDelete() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Bottom Content (Title, Message, Date)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = notice.title ?: "Novedades",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2
                )
                
                if (notice.message.isNotEmpty()) {
                    Text(
                        text = notice.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(0.9f),
                        maxLines = 3,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = getRelativeTimeString(notice.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(0.6f)
                    )
                    if (notice.authorName.isNotEmpty()) {
                        Text(
                            text = " • ${notice.authorName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(0.6f)
                        )
                    }
                }
            }
        }
    }
}

fun getRelativeTimeString(timestamp: Timestamp?): String {
    if (timestamp == null) return "Reciente"
    
    val now = System.currentTimeMillis()
    val diff = now - timestamp.toDate().time
    
    return when {
        diff < 60_000 -> "Ahora"
        diff < 3600_000 -> "${diff / 60_000} min"
        diff < 86400_000 -> "${diff / 3600_000} h"
        else -> "${diff / 86400_000} días"
    }
}
