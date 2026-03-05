package com.aquiles.crosschapp.presentation.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aquiles.crosschapp.presentation.competition.CompetitionHeader
import com.aquiles.crosschapp.presentation.competition.CompetitionSegmentedControl
import com.aquiles.crosschapp.presentation.competition.InfoTab
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aquiles.crosschapp.data.model.Competition
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// Colors Pro
private val ColorGold = Color(0xFFFFD700)
private val ColorSectionLabel = Color.White.copy(alpha = 0.7f)

@Composable
fun FeaturedCompetitionsRow(
    competitions: List<Competition>,
    onCompetitionClick: (String) -> Unit
) {
    if (competitions.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Título de Sección con Ícono
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        ) {
            Icon(
                Icons.Default.Bolt,
                contentDescription = null,
                tint = ColorGold,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "EVENTOS ACTIVOS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = ColorSectionLabel,
                letterSpacing = 1.sp
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(competitions) { comp ->
                ProCompetitionCard(comp) {
                    onCompetitionClick(comp.id)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProCompetitionCard(
    competition: Competition,
    onClick: () -> Unit
) {
    // Animación de escala al presionar
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "cardScale")

    // Estado del Bottom Sheet
    var showSheet by remember { mutableStateOf(false) }

    // Calcular progreso del evento
    val now = Date()
    val daysRemaining = competition.endDate?.let {
        val diff = it.time - now.time
        TimeUnit.MILLISECONDS.toDays(diff).toInt().coerceAtLeast(0)
    }
    val totalDays = if (competition.startDate != null && competition.endDate != null) {
        val diff = competition.endDate!!.time - competition.startDate!!.time
        TimeUnit.MILLISECONDS.toDays(diff).toInt().coerceAtLeast(1)
    } else null
    val progress = if (totalDays != null && daysRemaining != null) {
        1f - (daysRemaining.toFloat() / totalDays.toFloat())
    } else null

    Box(
        modifier = Modifier
            .width(300.dp)
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .clickable(interactionSource = interactionSource, indication = null) { showSheet = true }
    ) {
        // Capa 1: Imagen de fondo o gradiente
        if (!competition.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = competition.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(220.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF1C1C1E), Color(0xFF2C2C2E))
                        )
                    )
            )
        }

        // Capa 2: Overlay oscuro con gradiente
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Capa 3: Borde Glass
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.05f))
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
        )

        // Capa 4: Watermark Trofeo
        Icon(
            imageVector = Icons.Default.EmojiEvents,
            contentDescription = null,
            tint = ColorGold.copy(alpha = 0.06f),
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopEnd)
                .offset(x = 15.dp, y = (-10).dp)
                .rotate(-20f)
        )

        // Capa 5: Contenido
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Badge Tipo
            Surface(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, Color.White.copy(alpha = 0.2f)
                )
            ) {
                Text(
                    text = competition.type.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            // Bottom: Info + Progreso
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = competition.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    maxLines = 1,
                    lineHeight = 24.sp
                )

                // Criterio + Fecha
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = ColorGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = competition.criteria,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(Modifier.weight(1f))

                    competition.endDate?.let { date ->
                        val dateFormat = SimpleDateFormat("dd MMM", Locale.forLanguageTag("es-ES"))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarMonth, null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(3.dp))
                            Text(
                                text = "Hasta ${dateFormat.format(date)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // Barra de Progreso
                if (progress != null && daysRemaining != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (daysRemaining > 0) "$daysRemaining días restantes" else "¡Último día!",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (daysRemaining <= 2) Color(0xFFFF5252) else Color.White.copy(alpha = 0.6f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        LinearProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = if (daysRemaining <= 2) Color(0xFFFF5252) else ColorGold,
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }

                // Botón Expandir
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFFFC5200).copy(0.9f), Color(0xFFFF8C42).copy(0.9f)))
                        )
                        .clickable { showSheet = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Leaderboard, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ver Ranking y Detalle", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }

    // Bottom Sheet con el detalle completo
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            containerColor = Color(0xFF1C1C1E),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            // Reutilizamos directamente los componentes del detalle
            var selectedTab by remember { mutableIntStateOf(0) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.92f)
            ) {
                // Header
                CompetitionHeader(competition)

                Spacer(modifier = Modifier.height(16.dp))

                // Segmented Tabs
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    CompetitionSegmentedControl(
                        selectedIndex = selectedTab,
                        items = listOf("Ranking", "Reglas & Info"),
                        onIndexChanged = { selectedTab = it }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Contenido del tab
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> {
                            // Ranking vacío placeholder – el ViewModel real se conecta desde la pantalla de detalle
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Leaderboard, null, tint = Color.Gray.copy(0.5f), modifier = Modifier.size(48.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("Toca para ver ranking completo", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(Modifier.height(16.dp))
                                    Button(
                                        onClick = { showSheet = false; onClick() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC5200)),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("Abrir Pantalla Completa", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        1 -> InfoTab(competition = competition)
                    }
                }
            }
        }
    }
}
