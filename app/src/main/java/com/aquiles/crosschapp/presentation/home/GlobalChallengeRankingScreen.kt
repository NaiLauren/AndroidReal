package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.data.model.ChallengeResult
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Path

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalChallengeRankingScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    challengeId: String,
    adminViewModel: AdminViewModel = viewModel()
) {
    val globalRanking by adminViewModel.globalChallengeRanking.collectAsState()
    val gymLogos by adminViewModel.gymLogos.collectAsState()
    val isLoading by adminViewModel.isLoadingRanking.collectAsState()
    var selectedFilter by remember { mutableStateOf("all") } // "all", "week", "month"

    LaunchedEffect(challengeId) {
        adminViewModel.loadGlobalChallengeRanking(challengeId)
    }

    val filteredResults = remember(globalRanking, selectedFilter) {
        when (selectedFilter) {
            "week" -> {
                val oneWeekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
                globalRanking.filter { it.date?.time ?: 0 >= oneWeekAgo }
            }
            "month" -> {
                val oneMonthAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000)
                globalRanking.filter { it.date?.time ?: 0 >= oneMonthAgo }
            }
            else -> globalRanking
        }
    }.sortedByDescending { it.numericScore }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(72.dp),
                title = {
                    Column {
                        Text(
                            "🏆 Ranking Global",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                        Text(
                            "Top Desafíos",
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
                .padding(horizontal = 16.dp)
        ) {
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == "all",
                    onClick = { selectedFilter = "all" },
                    label = { Text("Todo el Tiempo") }
                )
                FilterChip(
                    selected = selectedFilter == "week",
                    onClick = { selectedFilter = "week" },
                    label = { Text("Esta Semana") }
                )
                FilterChip(
                    selected = selectedFilter == "month",
                    onClick = { selectedFilter = "month" },
                    label = { Text("Este Mes") }
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFC5200))
                }
            } else if (filteredResults.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("No hay resultados en este período", color = Color.Gray)
                    }
                }
            } else {
                // Top 3 Podium
                if (filteredResults.size >= 1) {
                    PodiumView(
                        results = filteredResults.take(3),
                        gymLogos = gymLogos
                    )
                    Spacer(Modifier.height(24.dp))
                }

                // Rest of ranking
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    itemsIndexed(filteredResults.drop(3)) { index, result ->
                        LeaderboardCard(
                            rank = index + 4,
                            result = result,
                            gymLogo = gymLogos[result.gym_id],
                            dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PodiumView(results: List<ChallengeResult>, gymLogos: Map<String, String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd Place
        if (results.size >= 2) {
            PodiumPosition(
                position = 2,
                result = results[1],
                gymLogo = gymLogos[results[1].gym_id],
                modifier = Modifier.weight(1f)
            )
        }

        // 1st Place (taller)
        if (results.size >= 1) {
            PodiumPosition(
                position = 1,
                result = results[0],
                gymLogo = gymLogos[results[0].gym_id],
                modifier = Modifier.weight(1.2f)
            )
        }

        // 3rd Place
        if (results.size >= 3) {
            PodiumPosition(
                position = 3,
                result = results[2],
                gymLogo = gymLogos[results[2].gym_id],
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun PodiumPosition(
    position: Int,
    result: ChallengeResult,
    gymLogo: String?,
    modifier: Modifier = Modifier
) {
    val heightScale = when (position) {
        1 -> 0.95f
        2 -> 0.75f
        else -> 0.6f
    }

    val medalIcon = when (position) {
        1 -> "👑"
        2 -> "🥈"
        else -> "🥉"
    }

    val baseColor = when (position) {
        1 -> Color(0xFFFFD700) // Gold
        2 -> Color(0xFFC0C0C0) // Silver
        else -> Color(0xFFCD7F32) // Bronze
    }

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // User Profile Image with Medal
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier
                    .size(if (position == 1) 70.dp else 60.dp)
                    .border(2.dp, baseColor, CircleShape)
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
            ) {
                AsyncImage(
                    model = result.userProfileImage.ifBlank { "https://via.placeholder.com/150" },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Text(
                medalIcon,
                fontSize = if (position == 1) 22.sp else 18.sp,
                modifier = Modifier.offset(x = 4.dp, y = (-4).dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        // Podium Pillar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(heightScale)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            baseColor.copy(alpha = 0.4f),
                            baseColor.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(baseColor, Color.Transparent)
                    ),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    result.numericScore.toString(),
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = if (position == 1) 20.sp else 16.sp
                )
                Text(
                    result.userName,
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.weight(1f))
                
                // Gym Logo at the bottom of the pillar
                if (gymLogo != null) {
                    AsyncImage(
                        model = gymLogo,
                        contentDescription = "Gym Logo",
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}

@Composable
fun LeaderboardCard(
    rank: Int,
    result: ChallengeResult,
    gymLogo: String?,
    dateFormat: java.text.SimpleDateFormat
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Text(
                text = "#$rank",
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.width(40.dp)
            )

            // User Info
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AsyncImage(
                    model = result.userProfileImage.ifBlank { "https://via.placeholder.com/150" },
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                
                Column {
                    Text(
                        "${result.userName} ${result.userLastName}",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (gymLogo != null) {
                            AsyncImage(
                                model = gymLogo,
                                contentDescription = "Gym Logo",
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(
                            result.facilityName,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Score
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = result.score,
                    color = Color(0xFFFC5200),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
                if (result.isRx) {
                    Text(
                        "RX",
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
