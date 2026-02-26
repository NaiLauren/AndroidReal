package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.aquiles.crosschapp.ui.theme.LocalPrimaryColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.aquiles.crosschapp.presentation.components.GlassCard

// --- CONSTANTS ---
// --- CONSTANTS ---
// ColorPrimaryAction removed in favor of LocalPrimaryColor.current
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)

@Composable
fun GamificationRulesScreen(
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1C1C1E).copy(alpha = 0.95f),
                            Color(0xFF000000).copy(alpha = 0.98f)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(BorderStroke(1.dp, ColorBorder), RoundedCornerShape(24.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, null, tint = ColorTextPrimary, modifier = Modifier.size(18.dp))
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
                            "Gana XP asistiendo a clases y completando logros.\nTu nivel refleja tu constancia.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorTextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                        )
                    }

                    item {
                        LevelTableSection()
                    }

                    item {
                        RecurringBonusesSection()
                    }

                    item {
                        AchievementsSection()
                    }

                    item {
                        MilestonesSection()
                    }
                }
            }
        }
    }
}

@Composable
fun LevelTableSection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GamificationSectionTitle("NIVELES Y RECOMPENSAS")

        GamificationLevelRow("Novato", "0 - 349 XP", null)
        GamificationLevelRow("Constante", "350 - 999 XP", Icons.Default.MilitaryTech, "Medalla Bronce", Color(0xFFCD7F32))
        GamificationLevelRow("Atleta", "1000 - 2999 XP", Icons.Default.FitnessCenter, "Icono Pesas", Color.Cyan)
        GamificationLevelRow("RX", "3000 - 9999 XP", Icons.Default.WorkspacePremium, "Medalla Oro", Color(0xFFFFD700))
        GamificationLevelRow("Elite", "10000+ XP", Icons.Default.EmojiEvents, "Corona Elite", Color(0xFFFFD700))
    }
}

@Composable
fun RecurringBonusesSection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GamificationSectionTitle("BONOS DE CLASE (RECURRENTES)")
        Text("Estos puntos se suman extra CADA VEZ que cumples la condición.", style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)

        GlassCard(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                GamificationPointRow(Icons.AutoMirrored.Filled.DirectionsRun, "Asistencia Base", "+10 XP")
                GamificationDivider()
                GamificationPointRow(Icons.Default.LocalFireDepartment, "Finde (Sáb/Dom)", "+75 XP", Color(0xFFFF9500))
                GamificationDivider()
                GamificationPointRow(Icons.Default.RocketLaunch, "Comienzo Fuerte (Lun)", "+50 XP", Color(0xFF2196F3))
                GamificationDivider()
                GamificationPointRow(Icons.Default.WbTwilight, "Madrugador (<8 AM)", "+30 XP", Color(0xFFFFC107))
                GamificationDivider()
                GamificationPointRow(Icons.Default.NightsStay, "Cierre del Día (>20 PM)", "+30 XP", Color(0xFF673AB7))
            }
        }
    }
}

@Composable
fun AchievementsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GamificationSectionTitle("LOGROS ESPECIALES (MEDALLAS)")
        Text("Se desbloquean una sola vez y quedan en tu perfil.", style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)

        GlassCard(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                GamificationAchievementRow(Icons.Default.WbSunny, "Madrugador", "Entrenar antes de las 9 AM", "+50 XP")
                GamificationDivider()
                GamificationAchievementRow(Icons.Default.DarkMode, "Ave Nocturna", "Cerrando el box (>20hs)", "+50 XP")
                GamificationDivider()
                GamificationAchievementRow(Icons.Default.LocalFireDepartment, "Semana de Fuego", "5 clases en 7 días", "+100 XP")
                GamificationDivider()
                GamificationAchievementRow(Icons.Default.Favorite, "Domingo Santo", "Entrenar un domingo", "+75 XP")
                GamificationDivider()
                GamificationAchievementRow(Icons.Default.Restaurant, "Hora del Almuerzo", "Entrenar 12-14hs", "+40 XP")
                GamificationDivider()
                GamificationAchievementRow(Icons.Default.LooksTwo, "Doble Turno", "2 clases el mismo día", "+200 XP")
            }
        }
    }
}

@Composable
fun MilestonesSection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GamificationSectionTitle("HITOS DE CONSTANCIA")

        GlassCard(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                GamificationPointRow(Icons.Default.Flag, "Primer Paso (1 Clase)", "+20 XP")
                GamificationDivider()
                GamificationPointRow(Icons.Default.LooksOne, "Calentando (5 Clases)", "+50 XP")
                GamificationDivider()
                GamificationPointRow(Icons.Default.Shield, "Lealtad (50 Clases)", "+200 XP")
                GamificationDivider()
                GamificationPointRow(Icons.Default.Stars, "Titán (100 Clases)", "+500 XP")
                GamificationDivider()
                GamificationPointRow(Icons.Default.AutoAwesome, "Deidad (300 Clases)", "+2000 XP")
            }
        }
    }
}

// --- COMPONENTS ---

@Composable
private fun GamificationSectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = LocalPrimaryColor.current)
}

@Composable
private fun GamificationDivider() {
    HorizontalDivider(color = ColorBorder, modifier = Modifier.padding(vertical = 12.dp))
}

@Composable
private fun GamificationLevelRow(name: String, range: String, icon: ImageVector?, rewardName: String? = null, iconTint: Color? = null) {
    val effectiveTint = iconTint ?: LocalPrimaryColor.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, color = ColorTextPrimary, style = MaterialTheme.typography.titleMedium)
                Text(range, style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)
            }

            if (icon != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (rewardName != null) {
                        Text(
                            rewardName,
                            style = MaterialTheme.typography.labelSmall,
                            color = effectiveTint.copy(alpha = 0.9f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = effectiveTint,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GamificationPointRow(icon: ImageVector, label: String, points: String, pointsColor: Color = Color(0xFF4CAF50)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = ColorTextPrimary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = ColorTextPrimary, modifier = Modifier.weight(1f))
        Text(points, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = pointsColor)
    }
}

@Composable
private fun GamificationAchievementRow(icon: ImageVector, title: String, description: String, points: String) {
     Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(LocalPrimaryColor.current.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = LocalPrimaryColor.current, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
            Text(description, style = MaterialTheme.typography.labelSmall, color = ColorTextSecondary)
        }
        Text(points, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50)) // Green
    }
}
