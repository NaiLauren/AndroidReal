package com.aquiles.crosschapp.presentation.ui.gamification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aquiles.crosschapp.data.model.User
import com.aquiles.crosschapp.presentation.common.AppBackground
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.viewmodel.AchievementCategory
import com.aquiles.crosschapp.presentation.viewmodel.AchievementDisplayModel
import com.aquiles.crosschapp.presentation.viewmodel.AchievementsState
import com.aquiles.crosschapp.presentation.viewmodel.AchievementsViewModel
import com.aquiles.crosschapp.presentation.viewmodel.CompetitionTrophy
import com.aquiles.crosschapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    user: User,
    totalClassesAttended: Int,
    onBackClick: () -> Unit,
    onNavigateToRules: () -> Unit = {},
    viewModel: AchievementsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(user.id) {
        viewModel.loadAchievements(
            userId = user.id,
            gymId = user.gym_id,
            totalClassesAttended = totalClassesAttended
        )
    }

    AppBackground {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Mis Logros",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Atrás",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f)
                    )
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            when (val s = state) {
                is AchievementsState.Loading -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is AchievementsState.Error -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(s.message, color = ErrorRed, textAlign = TextAlign.Center)
                    }
                }

                is AchievementsState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                    ) {
                        // STATS HEADER
                        item {
                            AchievementsStatsHeader(
                                totalUnlocked = s.totalUnlocked,
                                totalXp = user.xp,
                                trophiesCount = s.trophies.size,
                                onRulesClick = onNavigateToRules
                            )
                        }

                        // TROFEOS DE COMPETENCIAS (si hay)
                        if (s.trophies.isNotEmpty()) {
                            item {
                                TrophiesSection(trophies = s.trophies)
                            }
                        }

                        // SECCIONES DE LOGROS POR CATEGORÍA
                        s.byCategory.forEach { (category, items) ->
                            if (items.isNotEmpty()) {
                                item {
                                    AchievementCategorySection(
                                        category = category,
                                        items = items
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STATS HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AchievementsStatsHeader(
    totalUnlocked: Int,
    totalXp: Int,
    trophiesCount: Int,
    onRulesClick: () -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tres métricas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AchievementStatItem(
                    value = totalUnlocked.toString(),
                    label = "Desbloqueados",
                    color = MaterialTheme.colorScheme.primary
                )
                // Separador vertical
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(48.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                )
                AchievementStatItem(
                    value = totalXp.toString(),
                    label = "XP Total",
                    color = ColorGold
                )
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(48.dp)
                        .background(Color.White.copy(alpha = 0.15f))
                )
                AchievementStatItem(
                    value = trophiesCount.toString(),
                    label = "Trofeos",
                    color = Color(0xFFFF9500)
                )
            }

            // Botón Ver Reglas
            TextButton(
                onClick = onRulesClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    Icons.Default.Map,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Ver Niveles y Cómo Ganar XP",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AchievementStatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = value,
            fontSize = 30.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TROFEOS DE COMPETENCIAS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TrophiesSection(trophies: List<CompetitionTrophy>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = ColorGold, modifier = Modifier.size(18.dp))
            Text(
                "MIS TROFEOS",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 1.sp
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(trophies) { trophy ->
                TrophyChip(trophy = trophy)
            }
        }
    }
}

@Composable
private fun TrophyChip(trophy: CompetitionTrophy) {
    val (medalColor, medalEmoji) = when (trophy.position) {
        1 -> ColorGold to "🥇"
        2 -> ColorSilver to "🥈"
        else -> ColorBronze to "🥉"
    }
    GlassCard(shape = RoundedCornerShape(14.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(medalEmoji, fontSize = 24.sp)
            Column {
                Text(
                    text = trophy.competitionTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 140.dp)
                )
                Text(
                    text = "${trophy.position}° Puesto",
                    style = MaterialTheme.typography.labelSmall,
                    color = medalColor
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECCIÓN DE LOGROS POR CATEGORÍA
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AchievementCategorySection(
    category: AchievementCategory,
    items: List<AchievementDisplayModel>
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Título de categoría
        Text(
            text = category.displayName.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Gray,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp)
        )

        // Grilla adaptativa 3 columnas
        val chunked = items.chunked(3)
        chunked.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { item ->
                    AchievementCard(
                        item = item,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Rellenar columnas vacías si la fila no está completa
                repeat(3 - rowItems.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TARJETA DE LOGRO (paridad iOS AchievementCard)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AchievementCard(
    item: AchievementDisplayModel,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    // Glow pulsante solo si está desbloqueado
    val infiniteTransition = rememberInfiniteTransition(label = "glow_${item.definition.id}")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (item.isUnlocked) 0.55f else 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    val cardBackground = if (item.isUnlocked) Color(0xFF1A1A1A) else Color(0xFF141414)
    val borderGradient = if (item.isUnlocked) {
        Brush.linearGradient(listOf(primaryColor, ColorGold))
    } else {
        Brush.linearGradient(listOf(Color.White.copy(0.08f), Color.Transparent))
    }

    Box(
        modifier = modifier
            .then(
                if (item.isUnlocked) {
                    Modifier.shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = primaryColor.copy(alpha = glowAlpha),
                        spotColor = primaryColor.copy(alpha = glowAlpha)
                    )
                } else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBackground)
                .border(
                    width = 1.dp,
                    brush = borderGradient,
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ÍCONO CON CÍRCULO
            Box(contentAlignment = Alignment.Center) {
                // Fondo del círculo
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (item.isUnlocked)
                                primaryColor.copy(alpha = 0.20f)
                            else
                                Color.Black.copy(alpha = 0.3f)
                        )
                        .then(
                            if (item.isUnlocked) {
                                Modifier.border(
                                    2.dp,
                                    Brush.linearGradient(listOf(primaryColor, ColorGold)),
                                    CircleShape
                                )
                            } else {
                                Modifier.border(1.dp, Color.White.copy(0.1f), CircleShape)
                            }
                        )
                )
                // Emoji o icono Material mapeado
                Text(
                    text = mapIconToEmoji(item.definition.iconName, item.isUnlocked),
                    fontSize = 28.sp
                )
            }

            // TÍTULO
            Text(
                text = item.definition.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (item.isUnlocked) Color.White else Color.Gray,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // BADGE XP
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (item.isUnlocked) primaryColor.copy(alpha = 0.8f)
                        else Color.White.copy(alpha = 0.05f)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (item.isUnlocked) "+${item.definition.xpReward} XP" else "${item.definition.xpReward} XP",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (item.isUnlocked) Color.White else Color.Gray
                )
            }

            // BARRA DE PROGRESO (solo si está bloqueado y tiene progreso)
            if (!item.isUnlocked && item.progressLabel != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Barra
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(item.progress)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(2.dp))
                                .background(primaryColor)
                        )
                    }
                    // Label
                    Text(
                        text = item.progressLabel,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else if (item.isUnlocked) {
                // Espaciador para alinear alturas
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPER: Mapear iconName (string) a emoji visual
// ─────────────────────────────────────────────────────────────────────────────

private fun mapIconToEmoji(iconName: String, isUnlocked: Boolean): String {
    return when (iconName) {
        // Smart achievements
        "wb_sunny", "sunrise" -> "🌅"
        "dark_mode", "moon" -> "🌙"
        "whatshot", "flame" -> "🔥"
        "clean_hands" -> "✨"
        "sports_kabaddi" -> "🥊"
        "calendar_today", "calendar" -> "📅"
        "lunch_dining", "fork" -> "🍽️"
        "sports_soccer" -> "⚽"
        "filter_2", "2_circle" -> "2️⃣"
        // Milestone
        "flag" -> "🚩"
        "local_fire_department", "flame_circle" -> "🔥"
        "weekend", "house" -> "🏠"
        "military_tech" -> "🎖️"
        "shield" -> "🛡️"
        "workspace_premium" -> "💎"
        "auto_awesome", "sparkles" -> "✨"
        // Fallback
        else -> if (isUnlocked) "🏆" else "🔒"
    }
}
