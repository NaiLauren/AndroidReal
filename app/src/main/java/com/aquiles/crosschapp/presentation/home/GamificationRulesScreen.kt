package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// --- CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.95f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)

@Composable
fun GamificationRulesScreen(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Full screen ish
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = ColorGlassSurface,
            border = BorderStroke(1.dp, ColorBorder)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                        Icon(Icons.Default.Close, null, tint = ColorTextPrimary)
                    }
                    Text(
                        text = "Sistema de Niveles",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                HorizontalDivider(color = ColorBorder)

                LazyColumn(
                    contentPadding = PaddingValues(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        Text(
                            "Gana XP asistiendo a clases y completando logros. Tu nivel refleja tu constancia.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorTextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        LevelTableSection()
                    }

                    item {
                        ActionPointsSection()
                    }
                }
            }
        }
    }
}

@Composable
fun LevelTableSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("NIVELES Y RECOMPENSAS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ColorPrimaryAction)

        LevelRow("Novato", "0 - 99 XP", null)
        LevelRow("Constante", "100 - 499 XP", "military_tech", "Medalla Bronce")
        LevelRow("Atleta", "500 - 1499 XP", "fitness_center", "Icono Pesas")
        LevelRow("RX", "1500 - 4999 XP", "workspace_premium", "Medalla Oro")
        LevelRow("Elite", "5000+ XP", "emoji_events", "Corona Elite")
    }
}

@Composable
fun LevelRow(name: String, range: String, iconName: String?, rewardName: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Bold, color = ColorTextPrimary, style = MaterialTheme.typography.titleMedium)
            Text(range, style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)
        }
        
        if (iconName != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (rewardName != null) {
                    Text(rewardName, style = MaterialTheme.typography.labelSmall, color = ColorPrimaryAction, modifier = Modifier.padding(end = 8.dp))
                }
                Icon(
                    imageVector = getIconByName(iconName),
                    contentDescription = null,
                    tint = if(name == "Elite") Color(0xFFFFD700) else ColorPrimaryAction, // Gold for Elite
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ActionPointsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("CÓMO GANAR PUNTOS", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ColorPrimaryAction)

        PointRow(Icons.Default.DirectionsRun, "Asistencia a Clase", "+10 XP")
        PointRow(Icons.Default.Flag, "Primer Paso", "+20 XP")
        PointRow(Icons.Default.LocalFireDepartment, "Racha Semanal (5 clases)", "+100 XP")
        PointRow(Icons.Default.MilitaryTech, "Hito: 50 Clases", "+200 XP")
    }
}

@Composable
fun PointRow(icon: ImageVector, label: String, points: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = ColorTextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = ColorTextPrimary, modifier = Modifier.weight(1f))
        Text(points, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
    }
}

// Helper simple para iconos de demo
fun getIconByName(name: String): ImageVector {
    return when(name) {
        "military_tech" -> Icons.Default.MilitaryTech
        "fitness_center" -> Icons.Default.FitnessCenter
        "workspace_premium" -> Icons.Default.WorkspacePremium
        "emoji_events" -> Icons.Default.EmojiEvents
        else -> Icons.Default.Star
    }
}
