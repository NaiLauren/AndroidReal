package com.aquiles.crosschapp.presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.aquiles.crosschapp.data.model.BenchmarkResult
import com.aquiles.crosschapp.data.model.BenchmarkWod
import com.aquiles.crosschapp.presentation.viewmodel.LeaderboardState
import com.aquiles.crosschapp.presentation.viewmodel.LeaderboardViewModel
import com.aquiles.crosschapp.presentation.viewmodel.LeaderboardTab
import com.aquiles.crosschapp.presentation.viewmodel.UserSession

// --- DESIGN TOKENS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.45f)
private val ColorGlassHighlight = Color(0xFF2C2C2E).copy(alpha = 0.5f)
private val ColorPrimaryGold = Color(0xFFFFD700)
private val ColorPrimarySilver = Color(0xFFC0C0C0)
private val ColorPrimaryBronze = Color(0xFFCD7F32)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorAccent = Color(0xFF64B5F6) // Light Blue for generic accent

@Composable
fun LeaderboardScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    leaderboardViewModel: LeaderboardViewModel = viewModel()
) {
    val leaderboardState by leaderboardViewModel.leaderboardState.collectAsState()
    val availableBenchmarks by leaderboardViewModel.availableBenchmarks.collectAsState()
    val selectedBenchmark by leaderboardViewModel.selectedBenchmark.collectAsState()
    val selectedGender by leaderboardViewModel.selectedGenderFilter.collectAsState()

    val selectedCategory by leaderboardViewModel.selectedCategoryFilter.collectAsState()
    val currentTab by leaderboardViewModel.currentTab.collectAsState()

    var showBenchmarkSelector by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Fallback background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp) // Adjust based on scaffold
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            // HEADER
            LeaderboardHeader(navController)

            Spacer(Modifier.height(16.dp))

            // TABS SECTION
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ColorGlassSurface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                 listOf(LeaderboardTab.BENCHMARK to "Benchmarks", LeaderboardTab.XP to "Ranking XP").forEach { (tab, label) ->
                    val isSelected = currentTab == tab
                     Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ColorAccent else Color.Transparent)
                            .clickable { leaderboardViewModel.setTab(tab) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if(isSelected) Color.White else ColorTextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                 }
            }

            Spacer(Modifier.height(16.dp))

            // FILTERS SECTION (Only for Benchmarks)
            if (currentTab == LeaderboardTab.BENCHMARK) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    // Benchmark Selector Button
                    GlassButton(
                        text = selectedBenchmark?.name ?: "Seleccionar Benchmark",
                        icon = Icons.Default.Search,
                        onClick = { showBenchmarkSelector = true }
                    )
    
                    Spacer(Modifier.height(12.dp))
    
                    // Gender & Category Filters Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Gender Filter
                        GlassFilterChip(
                            label = mapGenderLabel(selectedGender),
                            selected = false, // Always acts as trigger for menu in full impl, here toggle
                            modifier = Modifier.weight(1f),
                            onClick = { 
                                // Simple cycle for MVP: ALL -> Male -> Female -> ALL
                                val next = when(selectedGender) {
                                    "ALL" -> "Male"
                                    "Male" -> "Female"
                                    else -> "ALL"
                                }
                                leaderboardViewModel.setGenderFilter(next)
                            }
                        )
                        
                        // Category Filter
                        GlassFilterChip(
                            label = if(selectedCategory == "ALL") "Todas CAT" else selectedCategory,
                            selected = selectedCategory != "ALL",
                            modifier = Modifier.weight(1f),
                            onClick = {
                                // Cycle: ALL -> RX -> SCALED -> ALL
                                val next = when(selectedCategory) {
                                    "ALL" -> "RX"
                                    "RX" -> "SCALED"
                                    else -> "ALL"
                                }
                                leaderboardViewModel.setCategoryFilter(next)
                            }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(16.dp))

            // LEADERBOARD LIST
            when (val state = leaderboardState) {
                is LeaderboardState.Loading -> {
                     Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                         CircularProgressIndicator(color = ColorAccent)
                     }
                }
                is LeaderboardState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.message}", color = Color.Red)
                    }
                }
                is LeaderboardState.Success -> {
                    if (state.rankings.isEmpty()) {
                         Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.EmojiEvents, null, tint = ColorTextSecondary, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Aún no hay registros.", color = ColorTextSecondary)
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            itemsIndexed(state.rankings) { index, result ->
                                LeaderboardItem(
                                    rank = index + 1,
                                    result = result,
                                    isCurrentUser = result.userId == UserSession.getCurrentUserId()
                                )
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
    }

    // Modal Sheet for Benchmark Selection could go here, or simple dialog
    if (showBenchmarkSelector) {
        AlertDialog(
            onDismissRequest = { showBenchmarkSelector = false },
            containerColor = ColorGlassSurface,
            title = { Text("Seleccionar Evaluación", color = ColorTextPrimary) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(availableBenchmarks) { bench ->
                        TextButton(
                            onClick = { 
                                leaderboardViewModel.selectBenchmark(bench)
                                showBenchmarkSelector = false 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Text(
                                bench.name, 
                                color = if(bench.id == selectedBenchmark?.id) ColorAccent else ColorTextPrimary,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        HorizontalDivider(color = Color.White.copy(alpha=0.1f))
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun LeaderboardHeader(navController: NavController) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ColorTextPrimary)
        }
        Text(
            "Ranking",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = ColorTextPrimary
        )
    }
}

@Composable
fun LeaderboardItem(
    rank: Int,
    result: BenchmarkResult,
    isCurrentUser: Boolean
) {
    val medalColor = when(rank) {
        1 -> ColorPrimaryGold
        2 -> ColorPrimarySilver
        3 -> ColorPrimaryBronze
        else -> Color.Transparent
    }
    
    val rankText = if (rank <= 3) "" else "#$rank"
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if(isCurrentUser) ColorAccent.copy(alpha = 0.2f) else ColorGlassSurface),
        border = if(isCurrentUser) BorderStroke(1.dp, ColorAccent.copy(alpha = 0.5f)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // RANK INDICATOR
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                if (rank <= 3) {
                    Icon(
                        Icons.Default.EmojiEvents, 
                        contentDescription = "Rank $rank",
                        tint = medalColor,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Text(rankText, color = ColorTextSecondary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            // AVATAR
            if (result.userProfileImageUrl.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(result.userProfileImageUrl),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = ColorTextSecondary)
                }
            }
            
            Spacer(Modifier.width(12.dp))
            
            // INFO
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${result.userName} ${result.userLastName}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    result.userLevel.ifBlank { "Atleta" },
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTextSecondary
                )
            }
            
            // SCORE
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    result.score,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorAccent
                )
                if (result.isRx) {
                     Text("RX", style = MaterialTheme.typography.labelSmall, color = ColorPrimaryGold)
                }
            }
        }
    }
}

@Composable
fun GlassFilterChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if(selected) ColorAccent.copy(alpha = 0.3f) else ColorGlassHighlight,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, if(selected) ColorAccent else Color.White.copy(alpha = 0.1f)),
        modifier = modifier.height(40.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = ColorTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun GlassButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = ColorGlassHighlight,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = ColorTextSecondary)
            Spacer(Modifier.width(12.dp))
            Text(text, color = ColorTextPrimary, modifier = Modifier.weight(1f))
            Icon(Icons.Default.FilterList, null, tint = ColorTextSecondary) // End icon hint
        }
    }
}

private fun mapGenderLabel(key: String): String {
    return when(key) {
        "ALL" -> "Todos (Género)"
        "Male" -> "Hombres"
        "Female" -> "Mujeres"
        else -> key
    }
}
