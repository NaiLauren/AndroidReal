package com.aquiles.crosschapp.presentation.home

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.aquiles.crosschapp.data.model.PersonalMessage
import com.aquiles.crosschapp.data.model.*
import com.aquiles.crosschapp.presentation.viewmodel.*
import com.aquiles.crosschapp.presentation.viewmodel.FeedTab
import com.aquiles.crosschapp.presentation.viewmodel.FeedUiItem
import com.aquiles.crosschapp.presentation.viewmodel.NoticeViewModel
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.common.AppBackground
import com.aquiles.crosschapp.ui.theme.LocalPrimaryColor

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.65f)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)

// --- NUEVO: Paleta de colores para tipos de contenido ---
private val ColorBenchmark = Color(0xFFFFD700) // Dorado
private val ColorClass = Color(0xFF34C759) // Verde
private val ColorCompetition = Color(0xFFFF3B30) // Rojo
private val ColorChallenge = Color(0xFF007AFF) // Azul

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    homeViewModel: HomeViewModel = viewModel(),
    adminViewModel: AdminViewModel = viewModel(),
    noticeViewModel: NoticeViewModel = viewModel(),
    performanceViewModel: PerformanceViewModel = viewModel(),
    benchmarkFeedViewModel: BenchmarkFeedViewModel = viewModel(),
    onNavigateToNotifications: () -> Unit,
    onNavigateToMessageArchive: () -> Unit,
    onNavigateToCreateNotice: () -> Unit,
    onNavigateToCompetition: (String) -> Unit,
    onNavigateToWall: () -> Unit = {}
) {
    val currentUser by UserSession.currentUser.collectAsState()
    val notificationsState by homeViewModel.notificationsState.collectAsState()
    val pendingRequestsCount by adminViewModel.pendingRequestsCount.collectAsState()
    val personalMessageState by homeViewModel.personalMessageState.collectAsState()
    val notices by noticeViewModel.notices.collectAsState()
    val activeCompetitions by homeViewModel.activeCompetitions.collectAsState()
    val feedItems by benchmarkFeedViewModel.feedItems.collectAsState()
    val currentFeedTab by benchmarkFeedViewModel.currentTab.collectAsState()
    val activeCompetitionsForFeed by benchmarkFeedViewModel.activeCompetitions.collectAsState()
    val userClasses by homeViewModel.userClasses.collectAsState()

    // --- DYNAMIC THEMING ---
    val primaryColor = LocalPrimaryColor.current

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            noticeViewModel.loadNotices()
            adminViewModel.loadAppConfig()
            adminViewModel.loadActivityImages()
            performanceViewModel.loadInitialData()
            benchmarkFeedViewModel.setTab(FeedTab.TODAY)
        }
    }

    // --- UI STRUCTURE ---
    val hasUnreadNotifications = notificationsState is NotificationsState.Success &&
            (notificationsState as NotificationsState.Success).notifications.isNotEmpty()

    AppBackground {
        CompositionLocalProvider(LocalPrimaryColor provides primaryColor) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        modifier = Modifier.height(72.dp),
                        title = {
                            Text(
                                "Inicio",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = ColorTextPrimary
                            )
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f)
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        HomeScreenContent(
                            user = user,
                            userClasses = userClasses,
                            personalMessageState = personalMessageState,
                            onMarkMessageAsRead = { messageId -> homeViewModel.markPersonalMessageAsRead(messageId) },
                            localScaffoldPadding = localScaffoldPadding,
                            notices = notices,
                            onDeleteNotice = { id -> noticeViewModel.deleteNotice(id) },
                            onNavigateToCreateNotice = onNavigateToCreateNotice,
                            activeCompetitions = activeCompetitions,
                            onNavigateToCompetition = onNavigateToCompetition,
                            onNavigateToWall = onNavigateToWall
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
    notices: List<GymNotice> = emptyList(),
    onDeleteNotice: (String) -> Unit = {},
    onNavigateToCreateNotice: () -> Unit = {},
    activeCompetitions: List<com.aquiles.crosschapp.data.model.Competition> = emptyList(),
    onNavigateToCompetition: (String) -> Unit = {},
    onNavigateToWall: () -> Unit = {}
) {
    var showRulesDialog by remember { mutableStateOf(false) }

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
            HomeGreetingCard(
                userName = user.name.split(" ").first(),
                profileImageUrl = user.profileImageUrl,
                primaryColor = LocalPrimaryColor.current
            )
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

        // 1.8 ACTIVE COMPETITIONS
        if (activeCompetitions.isNotEmpty()) {
            item {
                FeaturedCompetitionsRow(
                    competitions = activeCompetitions,
                    onCompetitionClick = onNavigateToCompetition
                )
            }
        }

        // 2. NEWS CAROUSEL
        if (notices.isNotEmpty()) {
            item {
                HorizontalPager(
                    state = rememberPagerState { notices.size },
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    pageSpacing = 16.dp,
                    modifier = Modifier.height(280.dp)
                ) { page ->
                    NoticeBoardCard(
                        notice = notices[page],
                        onDelete = { if (user.isAdmin) onDeleteNotice(notices[page].id) },
                        isAdmin = user.isAdmin
                    )
                }
            }
        }

        // 3. WIDGET MURO DE RESULTADOS (reemplaza el social feed en Home)
        item {
            MuroDeResultadosWidget(onClick = onNavigateToWall)
        }

        // Espacio final
        item {
            Spacer(modifier = Modifier.height(50.dp))
        }
    }

    if (showRulesDialog) {
        GamificationRulesScreen(onDismiss = { showRulesDialog = false })
    }
}

// =========================================================
// HOME SOCIAL FEED SECTION (REIMPLEMENTADO)
// =========================================================
@Composable
fun HomeSocialFeedSection(
    feedItems: List<FeedUiItem>,
    currentFeedTab: FeedTab,
    activeCompetitions: List<com.aquiles.crosschapp.data.model.Competition>,
    isAdmin: Boolean,
    onTabSelected: (FeedTab) -> Unit,
    onToggleReaction: (String, FeedItemType, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Segmented Tab Control — solo Hoy y Récords
        val tabs = listOf(FeedTab.TODAY to "Hoy 📅", FeedTab.RECORDS to "Récords 🏆")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(Color.White.copy(alpha = 0.08f))
                .padding(4.dp)
        ) {
            tabs.forEach { (tab, label) ->
                val isSelected = currentFeedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(if (isSelected) LocalPrimaryColor.current else Color.Transparent)
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Content
        if (feedItems.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (currentFeedTab == FeedTab.TODAY) "Nadie ha subido resultados hoy aún." else "No hay resultados publicados.",
                        color = ColorTextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            feedItems.take(5).forEach { item ->
                EnhancedFeedItemCard(item = item, onToggleReaction = { emotion ->
                    onToggleReaction(item.id, item.type, emotion)
                })
            }
        }
    }
}

// =========================================================
// NUEVO COMPONENTE DE TARJETA MEJORADO
// =========================================================
@Composable
fun EnhancedFeedItemCard(item: FeedUiItem, onToggleReaction: (String) -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // --- CABECERA CON AVATAR Y TIPO DE CONTENIDO ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(
                    modifier = Modifier.size(40.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(LocalPrimaryColor.current.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!item.userProfileImageUrl.isNullOrBlank()) {
                        coil.compose.AsyncImage(
                            model = item.userProfileImageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        androidx.compose.material3.Icon(
                            Icons.Default.Person, null, tint = LocalPrimaryColor.current, modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.userName, color = ColorTextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)

                    val (typeIcon, typeColor) = when (item.type) {
                        FeedItemType.BENCHMARK    -> Icons.Default.EmojiEvents   to ColorBenchmark
                        FeedItemType.CLASS        -> Icons.Default.FitnessCenter to ColorClass
                        FeedItemType.COMPETITION  -> Icons.Default.Flag          to ColorCompetition
                        FeedItemType.GLOBAL       -> Icons.Default.Public        to ColorChallenge
                        else                      -> Icons.Default.QuestionMark  to LocalPrimaryColor.current
                    }
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = null,
                            tint = typeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(item.title, color = typeColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                    }
                }
                // Score
                Column(horizontalAlignment = Alignment.End) {
                    Text(item.score, color = LocalPrimaryColor.current, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                    if (item.isRx) {
                        Surface(
                            modifier = Modifier.clip(androidx.compose.foundation.shape.CircleShape),
                            color = Color(0xFFFF9500)
                        ) {
                            Text(" RX ", color = Color.Black, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                        }
                    }
                }
            }

            // --- NUEVO SISTEMA DE REACCIONES COMPACTO Y ELEGANTE ---
            val emojiList = listOf("🔥", "💪", "👏", "⚡")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Contador total de reacciones
                val totalReactions = item.reactions.values.size
                Text(
                    text = "$totalReactions reacciones",
                    color = ColorTextSecondary,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 4.dp)
                )

                // Fila de iconos de reacción
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    emojiList.forEach { emoji ->
                        val count = item.reactions.values.count { it == emoji }
                        if (count > 0) {
                            // Solo mostramos las reacciones que existen
                            Surface(
                                modifier = Modifier
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .clickable { onToggleReaction(emoji) },
                                color = LocalPrimaryColor.current.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "$emoji $count",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorTextPrimary
                                )
                            }
                        } else {
                            // Icono sutil para añadir una reacción
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .clickable { onToggleReaction(emoji) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = emoji,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
fun PersonalMessageCardGlass(message: PersonalMessage, onAcknowledge: () -> Unit) {
    val context = LocalContext.current
    val avatarRingBrush = Brush.sweepGradient(
        listOf(
            LocalPrimaryColor.current,
            Color.White,
            LocalPrimaryColor.current.copy(alpha = 0.1f),
            LocalPrimaryColor.current
        )
    )

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Premium
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar Holográfico del Coach
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .border(BorderStroke(2.dp, avatarRingBrush), CircleShape)
                        .background(LocalPrimaryColor.current.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = LocalPrimaryColor.current, modifier = Modifier.size(24.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mensaje Especial",
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalPrimaryColor.current,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = message.sender_name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = ColorTextPrimary
                    )
                }
            }

            HorizontalDivider(color = ColorBorder, modifier = Modifier.padding(vertical = 4.dp))

            // Message Body
            if (message.content.isNotBlank()) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ColorTextSecondary,
                    lineHeight = 24.sp
                )
            }

            // Media / Attachments
            when (message.attachmentType) {
                "image" -> {
                    Image(
                        painter = rememberAsyncImagePainter(message.attachmentUrl),
                        contentDescription = "Imagen adjunta",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
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
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        border = BorderStroke(1.dp, ColorBorder),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = LocalPrimaryColor.current)
                        Spacer(Modifier.size(12.dp))
                        Text("Ver Documento Adjunto", color = ColorTextPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Button
            Button(
                onClick = onAcknowledge,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LocalPrimaryColor.current),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Entendido", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun UserClassItem(gymClass: GymClass) {
    val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    val timeStr = gymClass.dateTime?.let { timeFormat.format(it) } ?: "--:--"
    val isCompetition = gymClass.classType == "COMPETITION"
    val stripColor = try {
        Color(android.graphics.Color.parseColor(gymClass.hexColor))
    } catch (e: Exception) {
        LocalPrimaryColor.current
    }
    val compBrush = Brush.verticalGradient(listOf(Color(0xFFFFD700), Color(0xFFFFA500)))

    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Time
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
                    .then(if (isCompetition) Modifier.background(compBrush) else Modifier.background(stripColor))
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
                    Icon(
                        if (isCompetition) Icons.Default.EmojiEvents else Icons.Default.Person,
                        null,
                        modifier = Modifier.size(12.dp),
                        tint = if (isCompetition) Color(0xFFFFD700) else ColorTextSecondary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (isCompetition) "Evento" else gymClass.coachName,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isCompetition) Color(0xFFFFD700) else ColorTextSecondary,
                        fontWeight = if (isCompetition) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            // Status Icon (e.g. Registered Check)
            Icon(Icons.Default.CheckCircle, null, tint = Color.Green, modifier = Modifier.size(24.dp))
        }
    }
}