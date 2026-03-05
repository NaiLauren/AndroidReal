package com.aquiles.crosschapp.presentation.social

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.presentation.viewmodel.BenchmarkFeedViewModel
import com.aquiles.crosschapp.presentation.viewmodel.FeedState
import com.aquiles.crosschapp.presentation.viewmodel.FeedTab
import com.aquiles.crosschapp.presentation.viewmodel.UserSession
import com.aquiles.crosschapp.ui.theme.LocalPrimaryColor
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle
import com.aquiles.crosschapp.LocalHazeState

private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SocialFeedScreen(
    navController: NavController,
    benchmarkFeedViewModel: BenchmarkFeedViewModel,
    innerPadding: PaddingValues
) {
    val hazeState = LocalHazeState.current
    val feedState by benchmarkFeedViewModel.feedState.collectAsState()
    val feedItems by benchmarkFeedViewModel.feedItems.collectAsState()
    val currentTab by benchmarkFeedViewModel.currentTab.collectAsState()
    val availableBenchmarks by benchmarkFeedViewModel.availableBenchmarks.collectAsState()
    val selectedWodFilter by benchmarkFeedViewModel.selectedWodFilter.collectAsState()
    val selectedGenderFilter by benchmarkFeedViewModel.selectedGenderFilter.collectAsState()
    val sortCriteria by benchmarkFeedViewModel.sortCriteria.collectAsState()
    val activeCompetitions by benchmarkFeedViewModel.activeCompetitions.collectAsState()

    val currentUser = UserSession.currentUser.collectAsState().value

    LaunchedEffect(Unit) {
        benchmarkFeedViewModel.loadFeed()
    }

    Scaffold(
        containerColor = Color.Transparent, // El Haze viene desde el Host
        topBar = {
            TopAppBar(
                title = { Text("Comunidad", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingVals ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 100.dp), // Espacio para el BottomNav
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // --- TABS (Integrated Segmented Control) ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(4.dp)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        listOf(
                            FeedTab.TODAY to "Hoy \uD83D\uDCC5",
                            FeedTab.RECORDS to "Récords \uD83C\uDFC6",
                            FeedTab.EVENTS to "Eventos \uD83C\uDFC5"
                        ).forEach { (tab, label) ->
                            val isSelected = currentTab == tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(if (isSelected) LocalPrimaryColor.current else Color.Transparent)
                                    .clickable { benchmarkFeedViewModel.setTab(tab) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }


            // --- HEADER FILTERS ---
            item {
                if (currentTab == FeedTab.RECORDS) {
                    Column {
                        Text(
                            text = selectedWodFilter ?: "Filtrar por Entrenamiento",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedWodFilter == null,
                                    onClick = { benchmarkFeedViewModel.setWodFilter(null) },
                                    label = { Text("Todos") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LocalPrimaryColor.current, labelColor = Color.White)
                                )
                            }
                            items(availableBenchmarks) { bench ->
                                FilterChip(
                                    selected = selectedWodFilter == bench,
                                    onClick = { benchmarkFeedViewModel.setWodFilter(bench) },
                                    label = { Text(bench) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LocalPrimaryColor.current, labelColor = Color.White)
                                )
                            }
                        }

                        AnimatedVisibility(visible = selectedWodFilter != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                sortCriteria?.let { criteria ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { benchmarkFeedViewModel.toggleSortOrder() }
                                            .background(Brush.horizontalGradient(listOf(Color(0xFFFF6B35), Color(0xFFFFB340))), RoundedCornerShape(12.dp))
                                            .padding(horizontal = 16.dp, vertical = 12.dp)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("ORDENADO POR: ${criteria.replace("🏆 ", "").replace("⏱️ ", "")}", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                            Icon(Icons.Default.SwapVert, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Filtrar:", color = ColorTextSecondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterVertically))
                                    SuggestionChip(onClick = { benchmarkFeedViewModel.setGenderFilter(null) }, label = { Text("Todos") }, colors = SuggestionChipDefaults.suggestionChipColors(containerColor = if(selectedGenderFilter == null) LocalPrimaryColor.current else Color.Transparent))
                                    SuggestionChip(onClick = { benchmarkFeedViewModel.setGenderFilter("male") }, label = { Text("Masculino") }, colors = SuggestionChipDefaults.suggestionChipColors(containerColor = if(selectedGenderFilter == "male") LocalPrimaryColor.current else Color.Transparent))
                                    SuggestionChip(onClick = { benchmarkFeedViewModel.setGenderFilter("female") }, label = { Text("Femenino") }, colors = SuggestionChipDefaults.suggestionChipColors(containerColor = if(selectedGenderFilter == "female") LocalPrimaryColor.current else Color.Transparent))
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Actividad Reciente (Hoy)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // --- FEED CONTENT ---
            if (currentTab == FeedTab.EVENTS) {
                if (activeCompetitions.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            androidx.compose.foundation.layout.Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("\uD83C\uDFC1", fontSize = 48.sp)
                                Text("Sin eventos activos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("Pr\u00f3ximamente habr\u00e1n nuevas competencias.", color = ColorTextSecondary, fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    items(activeCompetitions) { competition ->
                        com.aquiles.crosschapp.presentation.components.GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        ) {
                            androidx.compose.foundation.layout.Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(competition.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                if (competition.description.isNotBlank()) {
                                    Text(competition.description, color = ColorTextSecondary, fontSize = 14.sp, maxLines = 2)
                                }
                                competition.prizeDescription?.let { prize ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("\uD83C\uDFC6", fontSize = 14.sp)
                                        Text(prize, color = LocalPrimaryColor.current, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
            when (feedState) {
                is FeedState.Loading -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = LocalPrimaryColor.current)
                        }
                    }
                }
                is FeedState.Error -> {
                    item {
                        Text((feedState as FeedState.Error).message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is FeedState.Success -> {
                    if (feedItems.isEmpty()) {
                        item {
                            Text("No hay resultados aún.", style = MaterialTheme.typography.bodyMedium, color = ColorTextSecondary)
                        }
                    } else {
                        items(feedItems) { feedItem ->
                            com.aquiles.crosschapp.presentation.home.BenchmarkFeedItem.BenchmarkFeedItem(
                                item = feedItem,
                                rankingPosition = if (currentTab == FeedTab.RECORDS && selectedWodFilter != null) feedItems.indexOf(feedItem) + 1 else null,
                                currentUserId = currentUser?.id,
                                onReactionClick = { emotion: String ->
                                    benchmarkFeedViewModel.toggleReaction(feedItem.id, feedItem.type, emotion)
                                },
                                onLongClick = {
                                    if (currentUser?.isAdmin == true || currentUser?.role == "owner") {
                                        benchmarkFeedViewModel.toggleVerification(feedItem.id, feedItem.isVerified)
                                    }
                                }
                            )
                        }
                    }
                }
                else -> {}
            }
            } // end Events else branch
        }
    }
}
