package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.aquiles.crosschapp.data.model.Trophy
import com.aquiles.crosschapp.data.model.CompetitionPodium
import com.aquiles.crosschapp.data.model.PodiumEntry
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.viewmodel.HallOfFameViewModel
import com.aquiles.crosschapp.presentation.viewmodel.UserSession
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HallOfFameScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: HallOfFameViewModel = viewModel()
) {
    val trophies by viewModel.trophies.collectAsState()
    val podiums by viewModel.podiums.collectAsState()
    val isLoadingTrophies by viewModel.isLoadingTrophies.collectAsState()
    val isLoadingPodiums by viewModel.isLoadingPodiums.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0=Trofeos, 1=Podios

    LaunchedEffect(Unit) {
        val gymId = UserSession.currentUser.value?.gym_id ?: return@LaunchedEffect
        viewModel.loadTrophies(gymId)
        viewModel.loadPodiums(gymId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(72.dp),
                title = {
                    Column {
                        Text(
                            "MURO DE RESULTADOS",
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                        Text(
                            "Podios. Récords. Reacciones.",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f)
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Segmented Control
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .background(
                        color = Color.White.opacity(0.07f),
                        shape = RoundedCornerShape(50)
                    )
                    .border(
                        width = 1.dp,
                        color = Color.White.opacity(0.1f),
                        shape = RoundedCornerShape(50)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                TabButton(
                    text = "🏆 Mis Trofeos",
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    text = "🏁 Eventos",
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.weight(1f)
                )
            }

            // Content
            when (selectedTab) {
                0 -> TrophiesTab(
                    trophies = trophies,
                    isLoading = isLoadingTrophies
                )
                1 -> PodiumsTab(
                    podiums = podiums,
                    isLoading = isLoadingPodiums
                )
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Void,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected)
                Color(0xFFFC5200)
            else
                Color.Transparent,
            contentColor = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 16.dp)
    ) {
        Text(
            text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun TrophiesTab(
    trophies: List<Trophy>,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFFC5200))
        }
    } else if (trophies.isEmpty()) {
        EmptyTrophiesState()
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Medal summary
            item {
                MedalSummaryBar(trophies)
            }

            items(trophies) { trophy ->
                TrophyCard(trophy)
            }
        }
    }
}

@Composable
private fun EmptyTrophiesState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🏆", fontSize = 64.sp, color = Color.Gray.copy(alpha = 0.3f))
        Spacer(Modifier.height(20.dp))
        Text(
            "Aún sin trofeos",
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 20.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Participa en las próximas competencias\ny conquista el podio.",
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MedalSummaryBar(trophies: List<Trophy>) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(1 to "🥇", 2 to "🥈", 3 to "🥉").forEach { (rank, emoji) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(emoji, fontSize = 28.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${trophies.count { it.rank == rank }}",
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 24.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        when (rank) {
                            1 -> "Oro"
                            2 -> "Plata"
                            else -> "Bronce"
                        },
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun TrophyCard(trophy: Trophy) {
    val colorGradient = when (trophy.rank) {
        1 -> listOf(Color(0xFFFFD700), Color(0xFFB8860B))
        2 -> listOf(Color(0xFFC0C0C0), Color(0xFF708090))
        else -> listOf(Color(0xFFCD7F32), Color(0xFF8B4513))
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Medal with glow
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        brush = Brush.radialGradient(colorGradient),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when (trophy.rank) {
                        1 -> "🥇"
                        2 -> "🥈"
                        else -> "🥉"
                    },
                    fontSize = 28.sp
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    when (trophy.rank) {
                        1 -> "PRIMER LUGAR"
                        2 -> "SEGUNDO LUGAR"
                        else -> "TERCER LUGAR"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = colorGradient[0]
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    trophy.competitionName,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${trophy.score}",
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 20.sp
                )
                if (trophy.isRx) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "RX",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFC5200),
                        modifier = Modifier
                            .background(
                                Color(0xFFFC5200).copy(alpha = 0.15f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PodiumsTab(
    podiums: List<CompetitionPodium>,
    isLoading: Boolean
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFFFC5200))
        }
    } else if (podiums.isEmpty()) {
        EmptyPodiumsState()
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(podiums) { podium ->
                PodiumCard(podium)
            }
        }
    }
}

@Composable
private fun EmptyPodiumsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.EmojiEvents,
            contentDescription = null,
            modifier = Modifier.size(60.dp),
            tint = Color.Gray.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Aún no hay competencias finalizadas",
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Los podios históricos aparecerán aquí.",
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun PodiumCard(podium: CompetitionPodium) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column {
            // Header
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🏛️", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        podium.competitionName,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        dateFormatter.format(podium.endDate),
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }

                podium.prizeDescription?.let { prize ->
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎁", fontSize = 12.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            prize,
                            fontSize = 12.sp,
                            color = Color.Yellow.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Divider(color = Color.White.opacity(0.08f))

            // Podium visual
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // 2nd
                podium.winners.find { it.rank == 2 }?.let { silver ->
                    PodiumColumn(silver, 70.dp, Modifier.weight(1f))
                }

                // 1st
                podium.winners.find { it.rank == 1 }?.let { gold ->
                    PodiumColumn(gold, 100.dp, Modifier.weight(1f))
                }

                // 3rd
                podium.winners.find { it.rank == 3 }?.let { bronze ->
                    PodiumColumn(bronze, 50.dp, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PodiumColumn(
    entry: PodiumEntry,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val rankEmoji = mapOf(1 to "🥇", 2 to "🥈", 3 to "🥉")
    val rankColors = mapOf(
        1 to listOf(Color(0xFFFFD700), Color(0xFFB8860B)),
        2 to listOf(Color(0xFFC0C0C0), Color(0xFF708090)),
        3 to listOf(Color(0xFFCD7F32), Color(0xFF8B4513))
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .border(2.dp, rankColors[entry.rank]?.first() ?: Color.Gray, CircleShape)
        ) {
            if (entry.profileImageUrl != null) {
                AsyncImage(
                    model = entry.profileImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.linearGradient(rankColors[entry.rank] ?: listOf(Color.Gray))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        entry.userName.take(1).uppercase(),
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }

        // Name
        Text(
            entry.userName.split(" ").firstOrNull() ?: entry.userName,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1
        )

        // Score
        Text(
            "${entry.score}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = Color.White.copy(alpha = 0.8f)
        )

        // Medal
        Text(rankEmoji[entry.rank] ?: "🏅", fontSize = 24.sp)

        // Podium base
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(
                    brush = Brush.verticalGradient(
                        rankColors[entry.rank]?.map { it.copy(alpha = 0.6f) } ?: listOf(Color.Gray)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                "${entry.rank}°",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
