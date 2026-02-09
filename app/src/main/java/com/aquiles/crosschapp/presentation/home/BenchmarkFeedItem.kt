package com.aquiles.crosschapp.presentation.home

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.aquiles.crosschapp.data.model.BenchmarkResult
import java.util.Date
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.viewmodel.FeedUiItem
import com.aquiles.crosschapp.presentation.viewmodel.FeedTab // Imports required for refactor

// --- CONSTANTS MATCHING HOMESCREEN ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.75f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
private val ColorVerified = Color(0xFF1DA1F2) // Light Blue like verification badge
private val ColorRx = ColorPrimaryAction
private val ColorScaled = Color(0xFF2196F3)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BenchmarkFeedItem(
    item: FeedUiItem,
    rankingPosition: Int? = null,
    onLongClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {}, 
                    onLongClick = onLongClick
                ),
            // Border is handled by GlassCard internally, but we can override if verified logic is critical
            // GlassCard doesn't expose border override easily. 
            // We can wrap content or accept the glass border. 
            // The item.isVerified logic added a blue border. 
            // Let's rely on GlassCard for now or implement a wrapper if needed.
            // Actually, let's keep it simple: GlassCard is better than custom border for consistency.
            // If verified needs highlighting, use the badge.
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // --- HEADER ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Avatar (Existing logic...)
                    if (!item.userProfileImageUrl.isNullOrBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(item.userProfileImageUrl),
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Gray),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.Gray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.userName.take(1).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
    
                    Spacer(modifier = Modifier.width(12.dp))
    
                    // Name and Info
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            
                            Text(
                                text = item.userName, // FeedUiItem already combining/formatting name if needed or valid
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ColorTextPrimary
                            )
                            
                            // Verification Badge
                            if (item.isVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verificado",
                                    tint = ColorVerified,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
    
                        // Level Badge
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FeedBadgePill(text = item.userLevel.uppercase(), color = getFeedLevelColor(item.userLevel))
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // Relative Time
                            val timeAgo = item.date?.let { 
                                DateUtils.getRelativeTimeSpanString(it.time, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
                            } ?: "Reciente"
                            
                            Text(
                                text = timeAgo.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
    
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = ColorBorder)
                Spacer(modifier = Modifier.height(12.dp))
    
                // --- BODY ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Completó",
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorTextSecondary
                        )
                        Text(
                            text = item.title, // [CHANGED] benchmarkName -> title
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black, // Extra Bold
                            color = ColorTextPrimary
                        )
                    }
    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = item.score,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isRx) ColorRx else ColorScaled
                        )
                        
                        Surface(
                            color = (if (item.isRx) ColorRx else ColorScaled).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (item.isRx) "RX" else "SCALED",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (item.isRx) ColorRx else ColorScaled
                            )
                        }
                    }
                }
            }
        }

        // RANKING BADGE OVERLAY
        if (rankingPosition != null) {
            val badgeColor = when(rankingPosition) {
                1 -> Color(0xFFFFD700) // Gold
                2 -> Color(0xFFC0C0C0) // Silver
                3 -> Color(0xFFCD7F32) // Bronze
                else -> ColorPrimaryAction
            }
            
            Surface(
                color = badgeColor,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
                    .size(32.dp),
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "#$rankingPosition",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

// RENAMED TO FeedBadgePill
@Composable
private fun FeedBadgePill(text: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = Color.White
        )
    }
}

// RENAMED TO getFeedLevelColor
private fun getFeedLevelColor(level: String): Color {
    return when(level.uppercase()) {
        "NOVATO" -> Color.Gray
        "CONSTANTE" -> Color(0xFF2196F3) // Blue
        "ATLETA" -> Color(0xFF4CAF50) // Green
        "RX" -> ColorPrimaryAction // Orange
        "ELITE" -> Color(0xFF9C27B0) // Purple
        else -> Color.Gray
    }
}
