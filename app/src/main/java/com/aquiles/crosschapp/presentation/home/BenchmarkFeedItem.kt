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
    item: BenchmarkResult,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {}, // No action on single click for now
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (item.isVerified) ColorVerified.copy(alpha = 0.5f) else ColorBorder),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // --- HEADER ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar
                if (!item.userProfileImageUrl.isNullOrBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(item.userProfileImageUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Gray), // Fallback color
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
                            text = "${item.userName} ${item.userLastName}",
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
                        // RENAMED FUNCTION CALL
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
                        text = item.benchmarkName,
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
