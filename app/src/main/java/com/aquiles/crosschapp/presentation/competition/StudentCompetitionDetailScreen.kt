package com.aquiles.crosschapp.presentation.competition

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Leaderboard
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
    val isLoading by viewModel.isLoading.collectAsState()
    
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Ranking, 1 = Info

    LaunchedEffect(competitionId) {
        viewModel.loadCompetition(competitionId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(competition?.title ?: "Competencia", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            
            // Background Image/Blur could go here behind content
            
            if (isLoading && competition == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFFFC5200))
            } else if (competition != null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Tabs
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            if (selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    color = Color(0xFFFC5200)
                                )
                            }
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Ranking") },
                            icon = { Icon(Icons.Default.Leaderboard, null) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Info") },
                            icon = { Icon(Icons.Default.Info, null) }
                        )
                    }
                    
                    // Content
                    when (selectedTab) {
                        0 -> RankingTab(ranking = ranking)
                        1 -> InfoTab(competition = competition!!)
                    }
                }
            } else {
                Text("Error cargando competencia", color = Color.Red, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun RankingTab(ranking: List<RankingEntry>) {
    if (ranking.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aún no hay resultados.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top 3 Podium (Optional fanciness, for now list)
            
            itemsIndexed(ranking) { index, entry ->
                RankingItemCard(entry, index)
            }
        }
    }
}

@Composable
fun RankingItemCard(entry: RankingEntry, index: Int) {
    val rankColor = when (entry.rank) {
        1 -> ColorGold
        2 -> ColorSilver
        3 -> ColorBronze
        else -> Color.White
    }
    
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(rankColor.copy(alpha = 0.2f), CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${entry.rank}",
                    fontWeight = FontWeight.Bold,
                    color = if (entry.rank <= 3) rankColor else Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Avatar
            if (!entry.userProfileImageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = entry.userProfileImageUrl,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(40.dp).background(Color.Gray, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(entry.userName.take(1).uppercase(), color = Color.White)
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Name & Level
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.userName, fontWeight = FontWeight.Bold, color = Color.White)
                if (!entry.userLevel.isNullOrBlank()) {
                     Text(entry.userLevel, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
            
            // Score
            Text(
                text = entry.scoreDisplay,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFFFC5200)
            )
        }
    }
}

@Composable
fun InfoTab(competition: Competition) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale("es", "ES"))
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        GlassCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Detalles del Evento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(16.dp))
                
                InfoRow("Tipo", competition.type)
                InfoRow("Criterio", competition.criteria)
                
                competition.startDate?.let {
                    InfoRow("Inicio", dateFormat.format(it))
                }
                competition.endDate?.let {
                    InfoRow("Fin", dateFormat.format(it))
                }
                
                if (competition.xpReward != null) {
                    InfoRow("Premio XP", "+${competition.xpReward} XP")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (!competition.description.isNullOrBlank()) {
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Descripción", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(competition.description, color = Color.White.copy(alpha = 0.8f))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (!competition.prizeDescription.isNullOrBlank()) {
            GlassCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, null, tint = ColorGold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Premios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(competition.prizeDescription, color = Color.White.copy(alpha = 0.8f))
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
