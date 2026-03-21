package com.aquiles.crosschapp.presentation.home.BenchmarkFeedItem

// IMPORTACIONES (las últimas corregidas)
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.viewmodel.FeedItemType
import com.aquiles.crosschapp.presentation.viewmodel.FeedUiItem
import com.aquiles.crosschapp.ui.theme.LocalPrimaryColor

// --- CONSTANTES DE DISEÑO ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.45f)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
// Color especial para Desafíos Globales, no depende de la marca
private val ColorGlobalChallenge = Color(0xFFFFD700) // Oro
private val ColorVerified = Color(0xFF1DA1F2)
private val ColorRx = Color(0xFFFF9500)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BenchmarkFeedItem(
    item: FeedUiItem,
    rankingPosition: Int? = null,
    currentUserId: String? = null,
    onReactionClick: ((String) -> Unit)? = null,
    onLongClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    // --- GESTIÓN DE COLOR DE MARCA A PRUEBA DE NULOS ---
    // Usa el color de marca si está disponible, si no, un naranja por defecto.
    val brandColor = LocalPrimaryColor.current ?: Color(0xFFFC5200)

    // --- SISTEMA DE ICONOS Y COLORES DINÁMICOS (CON NOMBRES CORRECTOS) ---
    val contentTypeInfo = when (item.type) {
        FeedItemType.BENCHMARK -> ContentTypeInfo(Icons.Default.EmojiEvents, brandColor, "RÉCORD")
        FeedItemType.CLASS -> ContentTypeInfo(Icons.Default.SportsMma, brandColor, "CLASE") // CORREGIDO a CLASS
        FeedItemType.COMPETITION -> ContentTypeInfo(Icons.Default.Flag, brandColor, "COMPETENCIA")
        FeedItemType.GLOBAL -> ContentTypeInfo(Icons.Default.Public, ColorGlobalChallenge, "GLOBAL") // CORREGIDO a GLOBAL
        else -> ContentTypeInfo(Icons.Default.HelpOutline, brandColor, "ACTIVIDAD")
    }

    val borderBrush = if (item.type == FeedItemType.GLOBAL) { // CORREGIDO a GLOBAL
        Brush.linearGradient(listOf(ColorGlobalChallenge, ColorGlobalChallenge.copy(alpha = 0.2f)))
    } else {
        Brush.linearGradient(listOf(contentTypeInfo.color.copy(alpha = 0.6f), contentTypeInfo.color.copy(alpha = 0.1f)))
    }

    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick
                ),
            borderBrush = borderBrush
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // --- CABECERA: QUIÉN y QUÉ ---
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // --- BANDA DE COLOR IZQUIERDA ---
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight(0.7f)
                            .background(
                                Brush.verticalGradient(
                                    listOf(contentTypeInfo.color, contentTypeInfo.color.copy(alpha = 0.2f))
                                ),
                                RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50)
                            )
                    )
                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = item.userName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ColorTextPrimary
                            )
                            if (item.isVerified) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.CheckCircle, null, tint = ColorVerified, modifier = Modifier.size(16.dp))
                            }
                            rankingPosition?.let { pos ->
                                Spacer(modifier = Modifier.width(6.dp))
                                RankingBadgePill(position = pos, brandColor = brandColor)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Icono y Tipo de Contenido
                            Surface(
                                color = contentTypeInfo.color.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.offset(y = (-2).dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = contentTypeInfo.icon,
                                        contentDescription = null,
                                        tint = contentTypeInfo.color,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = contentTypeInfo.label,
                                        color = contentTypeInfo.color,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            val levelColor = getFeedLevelColor(item.userLevel, brandColor)
                            FeedBadgePill(text = item.userLevel.uppercase(), color = levelColor)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Tiempo relativo
                        val timeAgo = item.date?.let {
                            android.text.format.DateUtils.getRelativeTimeSpanString(
                                it.time, System.currentTimeMillis(), android.text.format.DateUtils.MINUTE_IN_MILLIS
                            )
                        } ?: "Reciente"
                        Text(
                            text = timeAgo.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorTextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    // --- ZONA DERECHA: ACCIONES ---
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (!item.videoUrl.isNullOrBlank()) {
                                IconButton(onClick = { uriHandler.openUri(item.videoUrl) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.PlayCircle, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(24.dp))
                                }
                            }

                            val (statusIcon, statusColor) = when (item.validationStatus) {
                                "approved" -> Icons.Default.CheckCircle to Color(0xFF34C759)
                                "rejected" -> Icons.Default.Cancel to Color(0xFFFF3B30)
                                "pending" -> Icons.Default.Schedule to Color(0xFFFF9500)
                                else -> null to null
                            }
                            statusIcon?.let {
                                Icon(it, null, tint = statusColor ?: Color.Gray, modifier = Modifier.size(20.dp))
                            }
                        }

                        // Badge RX/SCALED
                        Surface(
                            color = if(item.isRx) ColorRx.copy(alpha = 0.2f) else brandColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (item.isRx) "RX" else "SCALED",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if(item.isRx) ColorRx else brandColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = ColorBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // --- CUERPO: EL RESULTADO ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = ColorTextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = item.score,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = contentTypeInfo.color
                        )
                    }
                }

                // --- BARRA DE REACCIONES MEJORADA ---
                if (onReactionClick != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    EnhancedReactionBar(
                        reactions = item.reactions,
                        currentUserId = currentUserId,
                        brandColor = brandColor,
                        onReactionClick = onReactionClick
                    )
                }
            }
        }
    }
}

// --- DATA CLASS PARA INFO DE TIPO DE CONTENIDO ---
private data class ContentTypeInfo(
    val icon: ImageVector,
    val color: Color,
    val label: String
)

// --- COMPONENTES DE UI REFINADOS ---

// --- COMPONENTES DE UI REFINADOS Y FORTALECIDOS ---

@Composable
private fun RankingBadgePill(position: Int, brandColor: Color?) {
    // Aseguramos un color no nulo
    val safeBrandColor = brandColor ?: Color(0xFFFC5200)
    val badgeColor = when (position) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> safeBrandColor
    }

    // Reestructurado para mayor robustez
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(badgeColor.copy(alpha = 0.2f))
            .border(BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f)), RoundedCornerShape(50))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "#$position",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = badgeColor
        )
    }
}

@Composable
private fun FeedBadgePill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            color = color
        )
    }
}

private fun getFeedLevelColor(level: String, primaryColor: Color?): Color {
    // Aseguramos un color no nulo
    val safePrimaryColor = primaryColor ?: Color(0xFFFC5200)
    return when(level.uppercase()) {
        "NOVATO" -> Color.Gray
        "CONSTANTE" -> safePrimaryColor
        "ATLETA" -> safePrimaryColor
        "RX" -> safePrimaryColor
        "ELITE" -> safePrimaryColor
        else -> Color.Gray
    }
}

// --- BARRA DE REACCIONES CON ANIMACIÓN Y COLOR DE MARCA (FORTALECIDA) ---
@Composable
private fun EnhancedReactionBar(
    reactions: Map<String, String>,
    currentUserId: String?,
    brandColor: Color?, // Lo aceptamos como nullable aquí
    onReactionClick: (String) -> Unit
) {
    // Nos aseguramos de tener un color no nulo DENTRO del componente
    val safeBrandColor = brandColor ?: Color(0xFFFC5200)
    val emojis = listOf("🔥", "💪", "👏", "⚡")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        emojis.forEach { emoji ->
            val count = reactions.values.count { it == emoji }
            val myReaction = currentUserId?.let { reactions[it] }
            val isSelected = myReaction == emoji

            val scale = animateFloatAsState(
                targetValue = if (isSelected) 1.1f else 1f,
                animationSpec = tween(durationMillis = 200), label = "reaction_scale"
            )

            // Usamos el safeBrandColor para todo
            Surface(
                color = if (isSelected) safeBrandColor.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f),
                shape = CircleShape,
                border = if (isSelected) BorderStroke(1.5.dp, safeBrandColor) else null,
                modifier = Modifier
                    .scale(scale.value)
                    .clickable { onReactionClick(emoji) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = emoji,
                        fontSize = 16.sp,
                        modifier = Modifier.offset(y = (-1).dp)
                    )
                    if (count > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) safeBrandColor else ColorTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
