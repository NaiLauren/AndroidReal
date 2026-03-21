package com.aquiles.crosschapp.presentation.competition

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.aquiles.crosschapp.data.model.Competition
import com.aquiles.crosschapp.data.model.RankingEntry
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.components.CompetitionSegmentedControl
import com.aquiles.crosschapp.presentation.viewmodel.StudentCompetitionViewModel
import com.aquiles.crosschapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.Person

// --- UI Constants ---
// Replaced by global tokens in Color.kt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun StudentCompetitionDetailScreen(
    competitionId: String,
    navController: NavController,
    vm: StudentCompetitionViewModel = viewModel()
) {
    val competition by vm.competition.collectAsState()
    val ranking by vm.ranking.collectAsState()
    val myEntry by vm.myEntry.collectAsState()
    val enrolledUsers by vm.enrolledUsers.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Ranking, 1 = Info

    LaunchedEffect(competitionId) {
        vm.loadCompetition(competitionId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.height(72.dp),
                title = { Text("Competencia", fontWeight = FontWeight.Bold, color = Color.White) }, // Generic title, detail is in Header
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f))
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Background Gradient (Subtle)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1A1A2E), // Dark Blue/Purple base
                                Color(0xFF000000)
                            )
                        )
                    )
            )
            
            if (isLoading && competition == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFFFC5200))
            } else if (competition != null) {
                val comp = competition!!
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // --- HEADER ---
                    CompetitionHeader(comp)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // --- TABS (Segmented Control) ---
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        CompetitionSegmentedControl(
                            selectedIndex = selectedTab,
                            items = listOf("Ranking", "Reglas & Info"),
                            onIndexChanged = { selectedTab = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // --- CONTENT ---
                    Box(modifier = Modifier.weight(1f)) {
                        when (selectedTab) {
                            0 -> RankingTab(
                                ranking = ranking,
                                myEntry = myEntry,
                                enrolledUsers = enrolledUsers,
                                onReactionClick = { resultId, emotion ->
                                    vm.toggleReaction(resultId, emotion)
                                }
                            )
                            1 -> InfoTab(competition = comp)
                        }
                    }
                }
            } else {
                Text("Error cargando competencia", color = Color.Red, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun CompetitionHeader(competition: Competition) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFC5200).copy(alpha = 0.1f),
                        Color.Transparent
                    )
                )
            )
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = competition.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Type Badge
                Surface(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = CircleShape
                ) {
                    Text(
                        text = competition.type.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                
                // Status
                val isActive = competition.isActive
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(if(isActive) SuccessGreen else ErrorRed, CircleShape))
                    Text(
                        text = if(isActive) "ACTIVO" else "FINALIZADO", 
                        style = MaterialTheme.typography.labelSmall, 
                        fontWeight = FontWeight.Bold,
                        color = if(isActive) SuccessGreen else ErrorRed
                    )
                }
            }
            
            if (!competition.prizeDescription.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, null, tint = ColorGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = competition.prizeDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

// CompetitionSegmentedControl moved to shared components

@Composable
fun RankingTab(
    ranking: List<RankingEntry>, 
    myEntry: RankingEntry?,
    enrolledUsers: List<com.aquiles.crosschapp.data.model.User>,
    onReactionClick: (String, String) -> Unit
) {
    val resultsByUser = ranking.associateBy { it.userId }
    val usersWithoutResult = enrolledUsers.filter { user -> !resultsByUser.containsKey(user.id) }

    if (ranking.isEmpty() && usersWithoutResult.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Leaderboard, null, tint = Color.Gray.copy(0.5f), modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text("Aún no hay resultados", style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text("Sé el primero en participar.", color = Color.Gray)
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // My Position Section
            if (myEntry != null) {
                item {
                    Text(
                        text = "TU POSICIÓN",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFC5200),
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                    RankingItemCard(entry = myEntry, isMe = true)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "TABLA GENERAL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                }
            }

            // General List
            itemsIndexed(ranking) { _, entry ->
                RankingItemCard(
                    entry = entry, 
                    isMe = entry.userId == myEntry?.userId,
                    onReactionClick = { emotion ->
                        onReactionClick(entry.resultId, emotion)
                    },
                    currentUserId = myEntry?.userId
                )
            }

            // Participantes sin resultado
            if (usersWithoutResult.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "AÚN NO CARGARON RESULTADO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                    )
                }
                
                itemsIndexed(usersWithoutResult) { _, user ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(0.1f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!user.profileImageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = user.profileImageUrl,
                                        contentDescription = "Avatar",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.Person, null, tint = Color.White)
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${user.name} ${user.lastName}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Sin resultado",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RankingItemCard(
    entry: RankingEntry,
    isMe: Boolean = false,
    onReactionClick: ((String) -> Unit)? = null,
    currentUserId: String? = null
) {
    val rankColor = when (entry.rank) {
        1 -> ColorGold
        2 -> ColorSilver
        3 -> ColorBronze
        else -> Color.White
    }
    
    val borderColor = if (isMe) Color(0xFFFC5200).copy(0.5f) else Color.Transparent
    val bgModifier = if (isMe) Modifier.background(Color(0xFFFC5200).copy(0.1f), RoundedCornerShape(24.dp)) else Modifier

    GlassCard(
        modifier = Modifier.fillMaxWidth().then(bgModifier),
        shape = RoundedCornerShape(24.dp)
    ) {
        if (isMe) {
            Box(modifier = Modifier.matchParentSize().border(1.dp, borderColor, RoundedCornerShape(24.dp)))
        }

        Column {
            // --- HEADER ---
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank / Avatar Badge
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(if(entry.rank <= 3) rankColor.copy(alpha = 0.2f) else Color.White.copy(0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (!entry.userProfileImageUrl.isNullOrBlank()) {
                         AsyncImage(
                             model = entry.userProfileImageUrl,
                             contentDescription = "Avatar",
                             modifier = Modifier.fillMaxSize().clip(CircleShape),
                             contentScale = ContentScale.Crop
                         )
                    } else {
                         Text(
                             text = "${entry.rank}",
                             fontWeight = FontWeight.Bold,
                             color = if (entry.rank <= 3) rankColor else Color.White
                         )
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Name & Badge
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Text(
                             text = entry.userName,
                             fontWeight = FontWeight.SemiBold,
                             fontSize = 15.sp,
                             color = if(isMe) Color(0xFFFC5200) else Color.White,
                             maxLines = 1
                         )
                         if (entry.validationStatus == "approved") {
                             Spacer(modifier = Modifier.width(4.dp))
                             VerifiedBadge()
                         }
                    }
                     entry.userLevel?.let {
                         if (it.isNotBlank()) Text(it, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                     }
                }
                
                // Score
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = entry.scoreDisplay,
                        fontWeight = FontWeight.Black,
                        fontSize = 32.sp,
                        color = Color.White
                    )
                }
            }
            
            // --- BOTTOM GRADIENT & EMOJIS ---
            // Simulating image background to match design, or just keeping the bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.6f))))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                 EmojiReactionBar(reactions = entry.reactions ?: emptyMap(), currentUserId = currentUserId, onReactionClick = onReactionClick)
            }
        }
    }
}

@Composable
fun VerifiedBadge() {
    // Escudo destellante
    val infiniteTransition = rememberInfiniteTransition()
    val shineOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart)
    )
    
    Box(
        modifier = Modifier
            .background(Color(0xFF00E5FF).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .graphicsLayer {
                 shadowElevation = 8f
                 spotShadowColor = Color(0xFF00E5FF)
                 ambientShadowColor = Color(0xFF00E5FF)
            }
    ) {
         Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Bolt, contentDescription = "Validado", tint = Color(0xFF00E5FF), modifier = Modifier.size(10.dp))
              Text("Validado", color = Color(0xFF00E5FF), fontWeight = FontWeight.Black, fontSize = 9.sp)
         }
         // Shine Overlay
         Spacer(
             modifier = Modifier
                 .matchParentSize()
                 .background(
                     Brush.linearGradient(
                         colors = listOf(Color.Transparent, Color.White.copy(0.8f), Color.Transparent),
                         start = androidx.compose.ui.geometry.Offset(shineOffset * 100f, 0f),
                         end = androidx.compose.ui.geometry.Offset((shineOffset + 1f) * 100f, 100f)
                     )
                 )
         )
    }
}

@Composable
private fun EmojiReactionBar(
    reactions: Map<String, String>,
    currentUserId: String?,
    onReactionClick: ((String) -> Unit)?
) {
    val fireCount = reactions.values.count { it == "fire" }
    val flexCount = reactions.values.count { it == "flex" }
    val clapCount = reactions.values.count { it == "clap" }
    
    val myReaction = currentUserId?.let { reactions[it] }
    
    // Glassmorphic pill
    Row(
        modifier = Modifier
            .background(Color.White.copy(0.1f), RoundedCornerShape(50))
            .border(1.dp, Color.White.copy(0.15f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EmojiButton("🔥", fireCount, myReaction == "fire") { onReactionClick?.invoke("fire") }
        EmojiButton("💪", flexCount, myReaction == "flex") { onReactionClick?.invoke("flex") }
        EmojiButton("👏", clapCount, myReaction == "clap") { onReactionClick?.invoke("clap") }
    }
}

@OptIn(androidx.compose.animation.ExperimentalAnimationApi::class)
@Composable
private fun EmojiButton(emoji: String, count: Int, isSelected: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    
    // Debounce state to prevent rapid clicks
    var lastClickTime by remember { mutableStateOf(0L) }
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.3f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    )
    
    val bgColor = if (isSelected) Color(0xFFFC5200).copy(alpha = 0.3f) else Color.Transparent
    
    Row(
        modifier = Modifier
            .scale(scale)
            .background(bgColor, CircleShape)
            .clickable(
                 interactionSource = interactionSource,
                 indication = null,
                 onClick = {
                     val now = System.currentTimeMillis()
                     if (now - lastClickTime > 500) { // 500ms debounce
                         lastClickTime = now
                         haptic.performHapticFeedback(HapticFeedbackType.LongPress) // Medium feeling
                         onClick()
                     }
                 }
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 24.sp) // Big 44px approx bounding box via padding
        if (count > 0) {
            Spacer(modifier = Modifier.width(4.dp))
            AnimatedContent(
                targetState = count,
                label = "count",
                transitionSpec = {
                    (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                }
            ) { targetCount ->
                 Text(
                     text = targetCount.toString(),
                     color = if (isSelected) Color(0xFFFC5200) else Color.White.copy(0.8f),
                     fontWeight = FontWeight.Bold,
                     fontSize = 14.sp
                 )
            }
        }
    }
}

@Composable
fun InfoTab(competition: Competition) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("es-ES"))
    
    // We wrap Scroll logic inside a Box or use LazyColumn for the whole tab if needed, 
    // but here sticking to Column in Box is fine for limited content.
    // Better: use LazyColumn to be scroll-safe.
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Reglas del Juego", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    InfoRow("Criterio", competition.criteria)
                    InfoRow("Validación", "Por Coach") // Hardcoded or mapped if validation type in model
                    
                     Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Duración", color = Color.Gray)
                        Row {
                            Icon(Icons.Default.CalendarMonth, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            val start = competition.startDate?.let { dateFormat.format(it) } ?: "?"
                            val end = competition.endDate?.let { dateFormat.format(it) } ?: "?"
                            Text("$start - $end", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        
        if (!competition.description.isNullOrBlank()) {
            item {
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = Color(0xFFFC5200))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Descripción", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(competition.description, color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        }
        
        if (competition.xpReward != null) {
             item {
                GlassCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = ColorGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Recompensas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("+${competition.xpReward} XP para el ganador", color = Color.White.copy(alpha = 0.8f))
                        if (!competition.prizeDescription.isNullOrBlank()) {
                             Text(competition.prizeDescription, color = Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
             }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

