package com.aquiles.crosschapp.presentation.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.*
import coil.compose.rememberAsyncImagePainter
import com.aquiles.crosschapp.data.model.PersonalMessage
import com.aquiles.crosschapp.data.model.*
import com.aquiles.crosschapp.presentation.viewmodel.*
import com.aquiles.crosschapp.presentation.viewmodel.FeedTab
import com.aquiles.crosschapp.presentation.viewmodel.FeedUiItem
import com.aquiles.crosschapp.presentation.home.BenchmarkFeedItem as SocialFeedItem // Alias for clarity
import com.aquiles.crosschapp.presentation.viewmodel.NoticeViewModel
import com.aquiles.crosschapp.data.model.GymNotice
import com.aquiles.crosschapp.presentation.components.GlassCard

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.75f)
val LocalPrimaryColor = androidx.compose.runtime.compositionLocalOf { Color(0xFFFC5200) } // Dynamic Color Provider
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    homeViewModel: HomeViewModel = viewModel(),
    adminViewModel: AdminViewModel = viewModel(),
    benchmarkFeedViewModel: BenchmarkFeedViewModel = viewModel(),
    noticeViewModel: NoticeViewModel = viewModel(),
    performanceViewModel: PerformanceViewModel = viewModel(),
    onNavigateToNotifications: () -> Unit,
    onNavigateToMessageArchive: () -> Unit,
    onNavigateToCreateNotice: () -> Unit
) {
    val currentUser by UserSession.currentUser.collectAsState()
    val notificationsState by homeViewModel.notificationsState.collectAsState()
    val pendingRequestsCount by adminViewModel.pendingRequestsCount.collectAsState()
    val personalMessageState by homeViewModel.personalMessageState.collectAsState()
    val notices by noticeViewModel.notices.collectAsState()

    val userClasses by homeViewModel.userClasses.collectAsState()

    // --- DYNAMIC THEMING ---
    val gym by UserSession.currentGym.collectAsState()
    val primaryColor = remember(gym) {
        try {
            if (gym?.primaryColor != null) Color(android.graphics.Color.parseColor(gym!!.primaryColor)) else Color(0xFFFC5200)
        } catch (e: Exception) {
            Color(0xFFFC5200)
        }
    }

    LaunchedEffect(Unit) {
        noticeViewModel.loadNotices() 
        adminViewModel.loadAppConfig()
        adminViewModel.loadActivityImages()
        performanceViewModel.loadInitialData()
    }

    // Social Feed State
    val feedItems by benchmarkFeedViewModel.feedItems.collectAsState()
    val currentTab by benchmarkFeedViewModel.currentTab.collectAsState()
    val availableBenchmarks by benchmarkFeedViewModel.availableBenchmarks.collectAsState()
    val selectedWodFilter by benchmarkFeedViewModel.selectedWodFilter.collectAsState()
    val sortCriteria by benchmarkFeedViewModel.sortCriteria.collectAsState()
    val filteredItems by benchmarkFeedViewModel.filteredItems.collectAsState()

    // --- UI STRUCTURE ---
    val hasUnreadNotifications = notificationsState is NotificationsState.Success &&
            (notificationsState as NotificationsState.Success).notifications.isNotEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        CompositionLocalProvider(LocalPrimaryColor provides primaryColor) {
            Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Inicio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    actions = {
                        // Archivo de Mensajes
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(40.dp)
                                .background(ColorGlassSurface, CircleShape)
                                .clickable { onNavigateToMessageArchive() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MailOutline,
                                contentDescription = "Archivo",
                                tint = LocalPrimaryColor.current
                            )
                        }

                        // Notificaciones
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(40.dp)
                                .background(ColorGlassSurface, CircleShape)
                                .clickable { onNavigateToNotifications() },
                            contentAlignment = Alignment.Center
                        ) {
                             Box {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Notificaciones",
                                    tint = if (hasUnreadNotifications) LocalPrimaryColor.current else Color.White
                                )
                                if (hasUnreadNotifications) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(10.dp)
                                            .background(Color.Red, CircleShape)
                                            .border(1.dp, Color.Black, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                )
            },
            containerColor = Color.Transparent
        ) { localScaffoldPadding ->
            val user = currentUser
            if (user == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = LocalPrimaryColor.current)
                }
            } else {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    HomeScreenContent(
                        user = user,
                        userClasses = userClasses,
                        personalMessageState = personalMessageState,
                        onMarkMessageAsRead = { messageId ->
                            homeViewModel.markPersonalMessageAsRead(messageId)
                        },
                        localScaffoldPadding = localScaffoldPadding,
                        benchmarkFeedViewModel = benchmarkFeedViewModel,
                        notices = notices,
                        onDeleteNotice = { id -> noticeViewModel.deleteNotice(id) },
                        onNavigateToCreateNotice = onNavigateToCreateNotice,
                        feedItems = feedItems,
                        currentTab = currentTab,
                        onTabSelected = { benchmarkFeedViewModel.setTab(it) },
                        availableBenchmarks = availableBenchmarks,
                        selectedWodFilter = selectedWodFilter,
                        onWodFilterSelected = { benchmarkFeedViewModel.setWodFilter(it) },
                        onToggleSort = { benchmarkFeedViewModel.toggleSortOrder() },
                        sortCriteria = sortCriteria
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun HomeScreenContent(
    user: User,
    userClasses: List<GymClass>,
    personalMessageState: PersonalMessageState,
    onMarkMessageAsRead: (String) -> Unit,
    localScaffoldPadding: PaddingValues,
    benchmarkFeedViewModel: BenchmarkFeedViewModel,
    notices: List<GymNotice> = emptyList(),
    onDeleteNotice: (String) -> Unit = {},
    onNavigateToCreateNotice: () -> Unit = {},
    feedItems: List<FeedUiItem> = emptyList(),
    currentTab: FeedTab = FeedTab.TODAY,
    onTabSelected: (FeedTab) -> Unit = {},
    availableBenchmarks: List<String> = emptyList(),
    selectedWodFilter: String? = null,
    onWodFilterSelected: (String?) -> Unit = {},
    onToggleSort: () -> Unit = {},
    sortCriteria: String? = null
) {
    var showRulesDialog by remember { mutableStateOf(false) }
    
    val feedState by benchmarkFeedViewModel.feedState.collectAsState()
    val selectedGenderFilter by benchmarkFeedViewModel.selectedGenderFilter.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = localScaffoldPadding.calculateTopPadding() + 16.dp,
            bottom = 24.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. GREETING (iOS Style)
        item {
            HomeGreetingCard(userName = user.name.split(" ").first())
        }
    
        // 1.5 PERSONAL MESSAGE CARD
        if (personalMessageState is PersonalMessageState.Success) {
            item {
                PersonalMessageCardGlass(
                    message = personalMessageState.message,
                    onAcknowledge = { onMarkMessageAsRead(personalMessageState.message.id) }
                )
            }
        }

        // 2. NEWS CAROUSEL (Muro del Inicio)
        if (notices.isNotEmpty()) {
            item {
                androidx.compose.foundation.pager.HorizontalPager(
                    state = androidx.compose.foundation.pager.rememberPagerState { notices.size },
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    pageSpacing = 16.dp,
                    modifier = Modifier.height(280.dp)
                ) { page ->
                     NoticeBoardCard(
                         notice = notices[page],
                         onDelete = { if (user.isAdmin || user.role == "owner") onDeleteNotice(notices[page].id) },
                         isAdmin = user.isAdmin || user.role == "owner"
                     )
                }
            }
        }

        // 3. TABS (Hoy / Récords)
        item {
            // Segmented Control Style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(ColorGlassSurface, CircleShape)
                    .padding(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    listOf(FeedTab.TODAY to "Hoy \uD83D\uDCC5", FeedTab.RECORDS to "Récords \uD83C\uDFC6").forEach { (tab, label) ->
                        val isSelected = currentTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(if (isSelected) LocalPrimaryColor.current else Color.Transparent)
                                .clickable { onTabSelected(tab) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
        
        // ========== FEED CONTENT ==========

        // TAB HOY y RECORDS: Ambos muestran el Feed Social (Ranking/Resultados)
        // La diferencia de qué items se muestran (solo hoy vs históricos/récords) la maneja el BenchmarkFeedViewModel
        if (currentTab == FeedTab.TODAY || currentTab == FeedTab.RECORDS) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. HEADER & FILTERS
                        Column {
                            // Header con Dropdown
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { /* TODO: Open Sheet */ }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedWodFilter ?: "Filtrar por Entrenamiento", // [User Req: WOD -> Entrenamiento]
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                
                                // Sort Toggle
                                Text(
                                    text = sortCriteria ?: "Ordenar", 
                                    color = LocalPrimaryColor.current, 
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.clickable { onToggleSort() }
                                )
                            }
                            
                            // Chips Scrollable
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedWodFilter == null,
                                        onClick = { onWodFilterSelected(null) },
                                        label = { Text("Todos") },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LocalPrimaryColor.current, labelColor = Color.White)
                                    )
                                }
                                items(availableBenchmarks) { bench ->
                                    FilterChip(
                                        selected = selectedWodFilter == bench,
                                        onClick = { onWodFilterSelected(bench) },
                                        label = { Text(bench) },
                                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LocalPrimaryColor.current, labelColor = Color.White)
                                    )
                                }
                            }
                        }

                        // 2. FEED CONTENT
                        when(feedState) {
                            is FeedState.Loading -> {
                                Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = LocalPrimaryColor.current)
                                } 
                            }
                            is FeedState.Success -> {
                                val allItems = (feedState as FeedState.Success).items
                                
                                if (allItems.isNotEmpty()) {
                                    // Dropdown de Ejercicios (Inside Glass)
                                    val availableBenchmarks by benchmarkFeedViewModel.availableBenchmarks.collectAsState()
                                    var dropdownExpanded by remember { mutableStateOf(false) }
                                    
                                    if (availableBenchmarks.isNotEmpty()) {
                                         Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { dropdownExpanded = true }
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = selectedWodFilter ?: "Seleccionar Ejercicio",
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (selectedWodFilter != null) Color.White else ColorTextSecondary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    imageVector = if (dropdownExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = "Seleccionar ejercicio",
                                                    tint = LocalPrimaryColor.current,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                        
                                        // Dropdown Menu
                                        DropdownMenu(
                                            expanded = dropdownExpanded,
                                            onDismissRequest = { dropdownExpanded = false },
                                            modifier = Modifier.background(Color(0xFF2a2a2a))
                                        ) {
                                             DropdownMenuItem(
                                                text = { Text("Todo (Feed completo)", color = if (selectedWodFilter == null) Color.White else ColorTextSecondary) },
                                                onClick = { benchmarkFeedViewModel.setWodFilter(null); dropdownExpanded = false }
                                             )
                                             HorizontalDivider(color = Color.White.copy(0.1f))
                                             availableBenchmarks.forEach { benchmark ->
                                                 DropdownMenuItem(
                                                     text = { Text(benchmark, color = if (benchmark == selectedWodFilter) Color.White else ColorTextSecondary) },
                                                     onClick = { benchmarkFeedViewModel.setWodFilter(benchmark); dropdownExpanded = false }
                                                 )
                                             }
                                        }
                                    }
                                    
                                    // UI Premium del Ranking (Sort & Gender)
                                    AnimatedVisibility(visible = selectedWodFilter != null) {
                                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            // Badge Sort
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
                                            // Gender Chips
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("Filtrar:", color = ColorTextSecondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.CenterVertically))
                                                SuggestionChip(onClick = { benchmarkFeedViewModel.setGenderFilter(null) }, label = { Text("Todos") }, colors = SuggestionChipDefaults.suggestionChipColors(containerColor = if(selectedGenderFilter == null) LocalPrimaryColor.current else Color.Transparent))
                                                SuggestionChip(onClick = { benchmarkFeedViewModel.setGenderFilter("male") }, label = { Text("Hombre") }, colors = SuggestionChipDefaults.suggestionChipColors(containerColor = if(selectedGenderFilter == "male") LocalPrimaryColor.current else Color.Transparent))
                                                SuggestionChip(onClick = { benchmarkFeedViewModel.setGenderFilter("female") }, label = { Text("Mujer") }, colors = SuggestionChipDefaults.suggestionChipColors(containerColor = if(selectedGenderFilter == "female") LocalPrimaryColor.current else Color.Transparent))
                                            }
                                        }
                                    }

                                    // EMPTY STATE check
                                    if (feedItems.isEmpty()) {
                                        Text("No hay resultados.", style = MaterialTheme.typography.bodyMedium, color = ColorTextSecondary)
                                    } else {
                                        // LIST / PODIUM
                                        if (selectedWodFilter != null && feedItems.size >= 3) {
                                             // PODIUM ROW
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                verticalAlignment = Alignment.Bottom
                                            ) {
                                                PodiumCard(feedItems[1], 2, MedalColor.Silver, false)
                                                PodiumCard(feedItems[0], 1, MedalColor.Gold, true)
                                                PodiumCard(feedItems[2], 3, MedalColor.Bronze, false)
                                            }
                                            // REMAINING LIST
                                            if (feedItems.size > 3) {
                                                feedItems.drop(3).forEach { feedItem ->
                                                    SocialFeedItem(
                                                        item = feedItem, 
                                                        rankingPosition = feedItems.indexOf(feedItem) + 1,
                                                        onLongClick = { if (user.isAdmin || user.role == "owner") benchmarkFeedViewModel.toggleVerification(feedItem.id, feedItem.isVerified) }
                                                    )
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                }
                                            }
                                        } else {
                                            // NORMAL LIST (No Podium)
                                            feedItems.forEach { feedItem ->
                                                 SocialFeedItem(
                                                    item = feedItem, 
                                                    rankingPosition = if(selectedWodFilter != null) feedItems.indexOf(feedItem) + 1 else null,
                                                    onLongClick = { if (user.isAdmin || user.role == "owner") benchmarkFeedViewModel.toggleVerification(feedItem.id, feedItem.isVerified) }
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                            }
                                        }
                                    }

                                } else {
                                     Text("No hay resultados.", style = MaterialTheme.typography.bodyMedium, color = ColorTextSecondary)
                                }
                            }
                            is FeedState.Error -> {
                                Text((feedState as FeedState.Error).message, color = MaterialTheme.colorScheme.error)
                            }
                            else -> {}
                        }
                    }
                }
            }
        }

        // Espacio final padding
        item { Spacer(modifier = Modifier.height(50.dp)) }
    }

    // Trigger Load
    LaunchedEffect(Unit) {
        benchmarkFeedViewModel.loadFeed()
    }

    if (showRulesDialog) {
        GamificationRulesScreen(onDismiss = { showRulesDialog = false })
    }
}

// =====================================================
// COMPONENTES UI (GLASS STYLE)
// =====================================================

@Composable
fun UserClassItem(gymClass: GymClass) {
    val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    val timeStr = gymClass.dateTime?.let { timeFormat.format(it) } ?: "--:--"
    
    // Style matching iOS "Neon" card
    val stripColor = try { Color(android.graphics.Color.parseColor(gymClass.hexColor)) } catch(e:Exception) { LocalPrimaryColor.current }

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Time Column
            Text(
                text = timeStr,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = LocalPrimaryColor.current,
                modifier = Modifier.width(50.dp)
            )
            
            // Vertical Color Strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(stripColor)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gymClass.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = ColorTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(12.dp), tint = ColorTextSecondary)
                    Spacer(Modifier.width(4.dp))
                    Text(gymClass.coachName, style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)
                }
            }
            
            // Status Icon (e.g. Registered Check)
            Icon(Icons.Default.CheckCircle, null, tint = Color.Green, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun PersonalMessageCardGlass(message: PersonalMessage, onAcknowledge: () -> Unit) {
    val context = LocalContext.current

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(LocalPrimaryColor.current.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = LocalPrimaryColor.current)
                }
                Column {
                    Text(
                        text = "Mensaje del Coach",
                        style = MaterialTheme.typography.titleSmall,
                        color = ColorTextSecondary
                    )
                    Text(
                        text = message.sender_name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary
                    )
                }
            }

            HorizontalDivider(color = ColorBorder)

            if (message.content.isNotBlank()) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ColorTextPrimary
                )
            }

            when (message.attachmentType) {
                "image" -> {
                    Image(
                        painter = rememberAsyncImagePainter(message.attachmentUrl),
                        contentDescription = "Imagen adjunta",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentScale = ContentScale.Crop
                    )
                }
                "pdf" -> {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(message.attachmentUrl))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, ColorBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = LocalPrimaryColor.current)
                        Spacer(Modifier.size(8.dp))
                        Text("Ver Rutina Adjunta (PDF)", color = ColorTextPrimary)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onAcknowledge) {
                    Text("Marcar como Leído", color = LocalPrimaryColor.current, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}