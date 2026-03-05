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
import androidx.compose.material3.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.aquiles.crosschapp.data.model.Competition
import com.aquiles.crosschapp.data.model.RankingEntry
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.viewmodel.StudentCompetitionViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.layout.height

// --- UI Constants ---
private val ColorGold = Color(0xFFFFD700)
private val ColorSilver = Color(0xFFC0C0C0)
private val ColorBronze = Color(0xFFCD7F32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentCompetitionDetailScreen(
    competitionId: String,
    navController: NavController,
    viewModel: StudentCompetitionViewModel = viewModel()
) {
    val competition by viewModel.competition.collectAsState()
    val ranking by viewModel.ranking.collectAsState()
    val myEntry by viewModel.myEntry.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Ranking, 1 = Info

    LaunchedEffect(competitionId) {
        viewModel.loadCompetition(competitionId)
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
                            0 -> RankingTab(ranking = ranking, myEntry = myEntry)
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
                    Box(modifier = Modifier.size(6.dp).background(if(isActive) Color.Green else Color.Red, CircleShape))
                    Text(
                        text = if(isActive) "ACTIVO" else "FINALIZADO", 
                        style = MaterialTheme.typography.labelSmall, 
                        fontWeight = FontWeight.Bold,
                        color = if(isActive) Color.Green else Color.Red
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

@Composable
fun CompetitionSegmentedControl(
    selectedIndex: Int,
    items: List<String>,
    onIndexChanged: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, label ->
                val isSelected = selectedIndex == index
                val animatedColor by animateColorAsState(
                    targetValue = if (isSelected) Color(0xFFFC5200) else Color.Transparent,
                    label = "tabColor"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                    label = "tabText"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(animatedColor)
                        .clickable { onIndexChanged(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = textColor,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun RankingTab(ranking: List<RankingEntry>, myEntry: RankingEntry?) {
    if (ranking.isEmpty()) {
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
                RankingItemCard(entry = entry, isMe = entry.userId == myEntry?.userId)
            }
        }
    }
}

@Composable
fun RankingItemCard(entry: RankingEntry, isMe: Boolean = false) {
    val rankColor = when (entry.rank) {
        1 -> ColorGold
        2 -> ColorSilver
        3 -> ColorBronze
        else -> Color.White
    }
    
    val borderColor = if (isMe) Color(0xFFFC5200).copy(0.5f) else Color.Transparent
    val bgModifier = if (isMe) Modifier.background(Color(0xFFFC5200).copy(0.1f), RoundedCornerShape(16.dp)) else Modifier

    GlassCard(
        modifier = Modifier.fillMaxWidth().then(bgModifier),
        shape = RoundedCornerShape(16.dp)
    ) {
        // Overlay for Border if Me
        if (isMe) {
            Box(modifier = Modifier.matchParentSize().border(1.dp, borderColor, RoundedCornerShape(16.dp)))
        }

        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(if(entry.rank <= 3) rankColor.copy(alpha = 0.2f) else Color.White.copy(0.1f), CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${entry.rank}",
                    fontWeight = FontWeight.Bold,
                    color = if (entry.rank <= 3) rankColor else Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.userName,
                    fontWeight = if(isMe) FontWeight.Bold else FontWeight.Medium,
                    color = if(isMe) Color(0xFFFC5200) else Color.White,
                    maxLines = 1
                )
                 entry.userLevel?.let {
                     if (it.isNotBlank()) Text(it, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                 }
            }
            
            // Score
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.scoreDisplay,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                // Assuming RX/Scaled isn't directly in RankingEntry yet, but if it were:
                // if (entry.isRx) Badge("RX")
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

