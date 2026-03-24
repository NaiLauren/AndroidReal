package com.aquiles.crosschapp.presentation.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.presentation.home.BenchmarkFeedItem.BenchmarkFeedItem
import com.aquiles.crosschapp.presentation.viewmodel.BenchmarkFeedViewModel
import com.aquiles.crosschapp.presentation.viewmodel.FeedState
import com.aquiles.crosschapp.presentation.viewmodel.FeedTab
import com.aquiles.crosschapp.presentation.viewmodel.UserSession
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.viewmodel.FeedUiItem
import com.aquiles.crosschapp.presentation.viewmodel.FeedItemType
import com.aquiles.crosschapp.data.model.Competition
import com.aquiles.crosschapp.data.model.GlobalChallenge
import com.aquiles.crosschapp.ui.theme.LocalPrimaryColor
import kotlin.math.roundToInt

// --- ESTADO PARA EL INDICADOR DESLIZANTE ---
@Stable
class TabRowState {
    var indicatorOffset: MutableState<Float> = mutableStateOf(0f)
    var indicatorWidth: MutableState<Float> = mutableStateOf(0f)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallOfResultsScreen(
    navController: NavController,
    benchmarkFeedViewModel: BenchmarkFeedViewModel = viewModel()
) {
    val feedState by benchmarkFeedViewModel.feedState.collectAsState()
    val feedItems by benchmarkFeedViewModel.feedItems.collectAsState()
    val currentTab by benchmarkFeedViewModel.currentTab.collectAsState()
    val availableBenchmarks by benchmarkFeedViewModel.availableBenchmarks.collectAsState()
    val selectedWodFilter by benchmarkFeedViewModel.selectedWodFilter.collectAsState()
    val selectedGenderFilter by benchmarkFeedViewModel.selectedGenderFilter.collectAsState()
    val sortCriteria by benchmarkFeedViewModel.sortCriteria.collectAsState()
    val activeCompetitions by benchmarkFeedViewModel.activeCompetitions.collectAsState()
    val allGlobalChallenges by benchmarkFeedViewModel.allGlobalChallenges.collectAsState()
    val currentUser = UserSession.currentUser.collectAsState().value
    val primaryColor = LocalPrimaryColor.current ?: Color(0xFFFC5200 // Color por defecto por si acaso
    )

    var selectedItemForValidation by remember { mutableStateOf<FeedUiItem?>(null) }

    val tabs = listOf(
        FeedTab.TODAY to "📅 Hoy",
        FeedTab.RECORDS to "🏆 Récords",
        FeedTab.CHALLENGES to "🌎 Desafíos",
        FeedTab.EVENTS to "🏛️ Eventos"
    )

    LaunchedEffect(Unit) {
        benchmarkFeedViewModel.loadFeed()
    }

    Scaffold(
        containerColor = Color(0xFF0A0A0A),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Muro de Resultados",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp
                        )
                        Text(
                            "Podios · Récords · Reacciones",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = primaryColor
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f)
                )
            )
        }
    ) { paddingVals ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVals)
                .background(Color(0xFF0A0A0A)),
            contentPadding = PaddingValues(bottom = 100.dp, top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ======================================================
            // NUEVAS TABS CON INDICADOR DESLIZANTE
            // ======================================================
            item {
                // Usamos un remember para que el estado del indicador no se reinicie en recomposiciones
                val tabRowState = remember { TabRowState() }

                // Obtenemos el índice de la tab seleccionada
                val selectedIndex = tabs.indexOfFirst { it.first == currentTab }.coerceAtLeast(0)

                // Contenedor principal que albergará tanto las tabs como el indicador
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    // --- EL INDICADOR DESLIZANTE ---
                    // Se animará a la posición de la tab seleccionada
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(tabRowState.indicatorOffset.value.roundToInt(), 0) }
                            .size(
                                width = tabRowState.indicatorWidth.value.dp,
                                height = 4.dp
                            )
                            .background(
                                color = primaryColor,
                                shape = RoundedCornerShape(50)
                            )
                    )

                    // --- LAS TABS (ahora sin fondo de color individual) ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.08f))
                            .padding(4.dp)
                    ) {
                        tabs.forEachIndexed { index, (tab, label) ->
                            val isSelected = currentTab == tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .clickable { benchmarkFeedViewModel.setTab(tab) }
                                    .onGloballyPositioned { coordinates ->
                                        // Si esta tab está seleccionada, actualizamos la posición y ancho del indicador
                                        if (isSelected) {
                                            tabRowState.indicatorOffset.value = coordinates.size.width * index.toFloat() / tabs.size
                                            tabRowState.indicatorWidth.value = coordinates.size.width.toFloat() / tabs.size
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = if (isSelected) primaryColor else Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // ======================================================
            // FILTROS (solo Récords)
            // ======================================================
            if (currentTab == FeedTab.RECORDS) {
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (availableBenchmarks.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 4.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedWodFilter == null,
                                        onClick = { benchmarkFeedViewModel.setWodFilter(null) },
                                        label = { Text("Todo") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = primaryColor,
                                            labelColor = Color.White,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                                items(availableBenchmarks) { bench ->
                                    FilterChip(
                                        selected = selectedWodFilter == bench,
                                        onClick = { benchmarkFeedViewModel.setWodFilter(bench) },
                                        label = { Text(bench) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = primaryColor,
                                            labelColor = Color.White,
                                            selectedLabelColor = Color.White
                                        )
                                    )
                                }
                            }
                        }

                        if (selectedWodFilter != null) {
                            sortCriteria?.let { criteria ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { benchmarkFeedViewModel.toggleSortOrder() }
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color(0xFFFF6B35), Color(0xFFFFB340))
                                            ),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "🏆 $selectedWodFilter • ${criteria.replace("🏆 ", "").replace("⏱️ ", "")}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(Icons.Default.SwapVert, null, tint = Color.White.copy(0.7f))
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Género:",
                                    color = Color.White.copy(0.6f),
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                                listOf(null to "Todos", "male" to "Hombres", "female" to "Mujeres").forEach { (filter, label) ->
                                    SuggestionChip(
                                        onClick = { benchmarkFeedViewModel.setGenderFilter(filter) },
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (selectedGenderFilter == filter)
                                                primaryColor.copy(0.2f) else Color.Transparent
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ======================================================
            // CONTENIDO
            // ======================================================
            if (currentTab == FeedTab.EVENTS) {
                // Separar competencias activas vs finalizadas
                val activeComps = activeCompetitions.filter { it.resolveStatus() == com.aquiles.crosschapp.data.model.CompetitionStatus.ONGOING }
                val finishedComps = activeCompetitions.filter { it.resolveStatus() == com.aquiles.crosschapp.data.model.CompetitionStatus.FINISHED }.sortedByDescending { it.endDate }
                
                if (activeComps.isEmpty() && finishedComps.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("🏁", fontSize = 56.sp)
                                Text(
                                    "Sin eventos",
                                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp
                                )
                                Text(
                                    "Próximamente habrán nuevas competencias.",
                                    color = Color.White.copy(0.5f), fontSize = 14.sp
                                )
                            }
                        }
                    }
                } else {
                    // SECCIÓN: ACTIVAS
                    if (activeComps.isNotEmpty()) {
                        item {
                            Text(
                                "🔥 ACTIVAS AHORA",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                            )
                        }
                        items(activeComps) { competition ->
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clickable { competition.id?.let { navController.navigate("student_competition/$it") } },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(competition.type.uppercase(), style = MaterialTheme.typography.labelSmall, color = primaryColor, fontWeight = FontWeight.Black)
                                        competition.status?.let { status ->
                                            Box(modifier = Modifier.background(primaryColor.copy(0.15f), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                                Text(status.uppercase(), color = primaryColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Text(competition.title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                    if (competition.description.isNotBlank()) {
                                        Text(competition.description, color = Color.White.copy(0.7f), fontSize = 14.sp, maxLines = 3)
                                    }
                                }
                            }
                        }
                    }
                    
                    // SECCIÓN: FINALIZADAS
                    if (finishedComps.isNotEmpty()) {
                        item {
                            Text("📜 HISTORIAL", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))
                        }
                        items(finishedComps) { competition ->
                            GlassCard(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clickable { competition.id?.let { navController.navigate("student_competition/$it") } },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(competition.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("${competition.type} • Finalizada", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                                    Text("🏆 Ver resultados finales", color = primaryColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            } else if (currentTab == FeedTab.CHALLENGES) {
                // ======================================================
                // SECCIÓN: DESAFÍOS GLOBALES (LISTA INTERACTIVA)
                // ======================================================
                if (allGlobalChallenges.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("🌎", fontSize = 56.sp)
                            Text(
                                "Sin desafíos actuales",
                                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp
                            )
                            Text(
                                "Próximamente habrán nuevos desafíos globales.",
                                color = Color.White.copy(0.5f), fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(allGlobalChallenges) { challenge ->
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clickable { navController.navigate("challenge_ranking/${challenge.id}") },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        if (challenge.isGlobal) "🌎 GLOBAL" else "🏠 LOCAL",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = primaryColor,
                                        fontWeight = FontWeight.Black
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(primaryColor.copy(0.15f), RoundedCornerShape(20.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            "VER RANKING",
                                            color = primaryColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    challenge.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 20.sp
                                )
                                if (challenge.description.isNotBlank()) {
                                    Text(
                                        challenge.description,
                                        color = Color.White.copy(0.7f),
                                        fontSize = 14.sp,
                                        maxLines = 2
                                    )
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("🏆", fontSize = 14.sp)
                                    Text(
                                        "Compite con toda la comunidad",
                                        color = primaryColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // SECCIÓN: FEED NORMAL (TODAY, RECORDS)
                when (feedState) {
                    is FeedState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = primaryColor)
                            }
                        }
                    }
                    is FeedState.Error -> {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    (feedState as FeedState.Error).message,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                    is FeedState.Success -> {
                        if (feedItems.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            if (currentTab == FeedTab.TODAY) "🔥" else "📊",
                                            fontSize = 56.sp
                                        )
                                        Text(
                                            if (currentTab == FeedTab.TODAY) "¡Sé el primero hoy! 🚀" else "¡El Muro está vacío!",
                                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp
                                        )
                                        Text(
                                            if (currentTab == FeedTab.TODAY) "Nadie ha registrado resultados todavía." else "Sé el primero en completar un Benchmark.",
                                            color = Color.White.copy(0.5f), fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(feedItems) { index, feedItem ->
                                BenchmarkFeedItem(
                                    item = feedItem,
                                    rankingPosition = if (currentTab == FeedTab.RECORDS && selectedWodFilter != null) index + 1 else null,
                                    currentUserId = currentUser?.id,
                                    onReactionClick = { emotion ->
                                        benchmarkFeedViewModel.toggleReaction(feedItem.id, feedItem.type, emotion)
                                    },
                                    onLongClick = {
                                        if (currentUser?.isAdmin == true || currentUser?.role == "owner") {
                                            selectedItemForValidation = feedItem
                                        }
                                    }
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        }

        // --- VALIDATION DIALOG ---
        val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
        val item = selectedItemForValidation
        if (item != null) {
            AlertDialog(
                onDismissRequest = { selectedItemForValidation = null },
                title = { Text("Validar Resultado", color = Color.White) },
                text = {
                    Column {
                        Text("Usuario: ${item.userName}", color = Color.White.copy(alpha = 0.8f))
                        Text("Resultado: ${item.score}", color = Color.White.copy(alpha = 0.8f))
                        if (!item.videoUrl.isNullOrBlank()) {
                            TextButton(onClick = {
                                uriHandler.openUri(item.videoUrl!!)
                            }) {
                                Text("Video disponible ✅", color = Color(0xFFFF9500))
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            benchmarkFeedViewModel.updateValidationStatus(item.id, item.type, "approved")
                            selectedItemForValidation = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759))
                    ) { Text("Aprobar") }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            benchmarkFeedViewModel.updateValidationStatus(item.id, item.type, "rejected")
                            selectedItemForValidation = null
                        }
                    ) { Text("Rechazar", color = Color(0xFFFF3B30)) }
                },
                containerColor = Color(0xFF1C1C1E),
                textContentColor = Color.White
            )
        }
    }
}

// ======================================================
// WIDGET PREMIUM: Acceso al Muro desde HomeScreen
// ======================================================
@Composable
fun MuroDeResultadosWidget(onClick: () -> Unit) {
    val primaryColor = LocalPrimaryColor.current ?: Color(0xFFFC5200)

    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    val goldOffset by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gold_float"
    )
    val silverOffset by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "silver_float"
    )
    val bronzeOffset by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bronze_float"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0xFFFF9500).copy(alpha = glowAlpha),
                spotColor = Color(0xFFFF9500).copy(alpha = glowAlpha)
            )
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1C1C1E).copy(alpha = 0.9f),
                        Color(0xFF2C2020).copy(alpha = 0.9f)
                    )
                )
            )
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Muro de Resultados",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp
                )
                Text(
                    "Podios · Récords · Reacciones",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("Ver todo", color = primaryColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text("→", color = primaryColor, fontSize = 12.sp)
                }
            }

            Box(
                modifier = Modifier.size(width = 110.dp, height = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "🥈",
                    fontSize = 30.sp,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .offset(y = silverOffset.dp)
                )
                Text(
                    "🥉",
                    fontSize = 26.sp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(y = bronzeOffset.dp)
                )
                Text(
                    "🥇",
                    fontSize = 42.sp,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = goldOffset.dp)
                )
            }
        }
    }
}
