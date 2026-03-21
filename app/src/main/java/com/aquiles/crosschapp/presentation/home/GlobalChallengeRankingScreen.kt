package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.background
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalChallengeRankingScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    adminViewModel: AdminViewModel = viewModel()
) {
    val globalRanking by adminViewModel.globalChallengeRanking.collectAsState()
    val isLoading by adminViewModel.isLoadingRanking.collectAsState()
    var selectedFilter by remember { mutableStateOf("all") } // "all", "week", "month"

    LaunchedEffect(Unit) {
        adminViewModel.loadGlobalChallengeRanking()
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
                    PodiumView(results = filteredResults.take(3))
                    Spacer(Modifier.height(24.dp))
                }

                // Rest of ranking
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    itemsIndexed(filteredResults.drop(3)) { index, result ->
                        LeaderboardCard(
                            rank = index + 4,
                            result = result,
                            dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PodiumView(results: List<ChallengeResult>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd Place
        if (results.size >= 2) {
            PodiumPosition(
                position = 2,
                result = results[1],
                height = 140.dp,
                modifier = Modifier.weight(1f)
            )
        }

        // 1st Place (taller)
        if (results.size >= 1) {
            PodiumPosition(
                position = 1,
                result = results[0],
                height = 180.dp,
                modifier = Modifier.weight(1f)
            )
        }

        // 3rd Place
        if (results.size >= 3) {
            PodiumPosition(
                position = 3,
                result = results[2],
                height = 100.dp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun PodiumPosition(
    position: Int,
    result: ChallengeResult,
    height: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val medal = when (position) {
        1 -> "🥇"
        2 -> "🥈"
        else -> "🥉"
    }

    val backgroundColor = when (position) {
        1 -> Color(0xFFFFD700).copy(alpha = 0.2f)
        2 -> Color(0xFFC0C0C0).copy(alpha = 0.2f)
        else -> Color(0xFFCD7F32).copy(alpha = 0.2f)
    }

    val borderColor = when (position) {
        1 -> Color(0xFFFFD700)
        2 -> Color(0xFFC0C0C0)
        else -> Color(0xFFCD7F32)
    }

    GlassCard(
        modifier = modifier
            .fillMaxHeight(1f)
            .background(backgroundColor),
        borderColor = borderColor,
        backgroundColor = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(medal, fontSize = 28.sp)
            Text(
                "${result.numericScore}",
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                fontSize = 16.sp,
                maxLines = 1
            )
            Text(
                "${result.userName}\n${result.userLastName}",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
