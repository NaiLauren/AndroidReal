package com.aquiles.crosschapp.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.aquiles.crosschapp.data.model.*
import com.aquiles.crosschapp.presentation.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil
import androidx.compose.foundation.layout.height
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.components.GlassAlertDialog
import com.aquiles.crosschapp.ui.theme.LocalPrimaryColor

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.65f)
private val ColorDialogSurface = Color(0xFF1C1C1E).copy(alpha = 0.70f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
private val ColorError = Color(0xFFEF5350)
private val ColorBackgroundGradientStart = Color(0xFF000000)
private val ColorBackgroundGradientEnd = Color(0xFF121212)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceScreen(
    innerPadding: PaddingValues,
    performanceViewModel: PerformanceViewModel,
    userTrainingViewModel: UserTrainingViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateToLeaderboard: () -> Unit = {}
) {
    val benchmarkState by performanceViewModel.benchmarkRecordsState.collectAsState()
    val dailyWodState by performanceViewModel.dailyWodRecordsState.collectAsState()
    val achievementsState by performanceViewModel.achievementsState.collectAsState()
    val chartState by performanceViewModel.attendanceChartState.collectAsState()
    val globalBenchmarkState by performanceViewModel.globalBenchmarkRecordsState.collectAsState()
    val currentUser by UserSession.currentUser.collectAsState()
    
    var showingTrainingConfig by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        if (currentUser != null && benchmarkState is PerformanceState.Idle) {
            performanceViewModel.loadInitialData()
        }
    }

    // --- UI STRUCTURE ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        if (currentUser == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = ColorPrimaryAction) }
        } else {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        modifier = Modifier.height(72.dp),
                        title = { Text("Mi Rendimiento", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f))
                    )
                },
                containerColor = Color.Transparent
            ) { localScaffoldPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = localScaffoldPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Spacer(Modifier.height(4.dp))

                    // 0. ENTRENAMIENTO SEMANAL
                    TrainingScheduleSummarySectionGlass(
                        viewModel = userTrainingViewModel,
                        onConfigure = { showingTrainingConfig = true }
                    )

                    // 1. GRÁFICO DE ASISTENCIA
                    AttendanceChartSectionGlass(
                        state = chartState,
                        onTimeRangeSelected = { performanceViewModel.onTimeRangeSelected(it) }
                    )

                    // 2. RÉCORDS UNIFICADOS (iOS Parity Redesign)
                    UnifiedPerformanceRecordsCard(
                        wodRecordsState = dailyWodState,
                        benchmarkRecordsState = benchmarkState,
                        globalBenchmarkRecordsState = globalBenchmarkState,
                        viewModel = performanceViewModel
                    )

                    // 3. LOGROS
                    Column {
                        SectionLabel("LOGROS DESBLOQUEADOS")
                        AchievementsSectionGlass(state = achievementsState)
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }

            // XP Reward Popup Overlay — Mejorado
            val xpMessage by userTrainingViewModel.xpRewardMessage.collectAsState()
            AnimatedVisibility(
                visible = xpMessage != null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 96.dp)
                    .zIndex(10f),
                enter = slideInVertically(initialOffsetY = { -it }, animationSpec = spring()) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }, animationSpec = tween(300)) + fadeOut()
            ) {
                if (xpMessage != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFFF9800), Color(0xFFFC5200))
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(50))
                            .padding(horizontal = 28.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⚡", fontSize = 18.sp)
                            Text(
                                text = xpMessage!!,
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
            
            if (showingTrainingConfig) {
                ModalBottomSheet(
                    onDismissRequest = { showingTrainingConfig = false },
                    containerColor = ColorDialogSurface,
                    scrimColor = Color.Black.copy(alpha = 0.5f),
                    dragHandle = { BottomSheetDefaults.DragHandle(color = ColorTextSecondary) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 32.dp)
                    ) {
                        TrainingScheduleSectionGlass(viewModel = userTrainingViewModel)
                    }
                }
            }
        }
    }
}

// =====================================================
// COMPONENTES UI (GLASS STYLE)
// =====================================================

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = ColorTextSecondary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        letterSpacing = 1.sp
    )
}

@Composable
fun AttendanceChartSectionGlass(
    state: AttendanceChartUiState,
    onTimeRangeSelected: (TimeRange) -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Asistencia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
                // Icono decorativo pequeño
                Icon(Icons.Default.BarChart, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.height(16.dp))

            GlassSegmentedControl(
                selectedRange = state.selectedRange,
                onRangeSelected = onTimeRangeSelected
            )

            Spacer(Modifier.height(24.dp))

            Box(Modifier.fillMaxWidth().height(160.dp), Alignment.Center) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                } else {
                    when(val data = state.data) {
                        is AttendanceData.BarChartData -> {
                            if (data.values.all { it == 0f }) NoDataPlaceholder()
                            else GlassBarChart(data.values, data.labels, Modifier.fillMaxSize())
                        }
                        is AttendanceData.WeeklySummaryData -> {
                            if (data.days.none { it.attended }) NoDataPlaceholder()
                            else WeeklyAttendanceSummary(data.days)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassSegmentedControl(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit
) {
    val ranges = mapOf(TimeRange.WEEK to "Semana", TimeRange.MONTH to "Mes", TimeRange.YEAR to "Año")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(Color.Black.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ranges.forEach { (range, label) ->
            val isSelected = range == selectedRange
            val bgColor = if (isSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent
            val textColor = if (isSelected) ColorTextPrimary else ColorTextSecondary

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(bgColor)
                    .clickable { onRangeSelected(range) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun GlassBarChart(data: List<Float>, labels: List<String>, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val barColor = MaterialTheme.colorScheme.primary
    val axisColor = ColorTextSecondary

    Canvas(modifier = modifier) {
        val chartHeight = size.height - 20.dp.toPx()
        val maxValue = ceil(data.maxOrNull()?.coerceAtLeast(1f) ?: 1f)

        // Ajuste dinámico de ancho
        val totalSpacing = (data.size + 1) * 8.dp.toPx() // Espacio entre barras
        val barWidth = (size.width - totalSpacing) / data.size

        data.forEachIndexed { index, value ->
            val barHeight = chartHeight * (value / maxValue)
            val x = (index * (barWidth + 8.dp.toPx())) + 8.dp.toPx()
            val y = chartHeight - barHeight

            // Barra
            if (barHeight > 0) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
            }

            // Etiqueta Eje X (Solo mostrar algunas si son muchas)
            val shouldDrawLabel = data.size <= 7 || index % 2 == 0
            if (shouldDrawLabel) {
                val label = labels.getOrNull(index) ?: ""
                val textLayout = textMeasurer.measure(label, style = TextStyle(color = axisColor, fontSize = 10.sp))
                drawText(
                    textLayout,
                    topLeft = Offset(x + (barWidth / 2) - (textLayout.size.width / 2), chartHeight + 6.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun NoDataPlaceholder() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.BarChart, null, tint = ColorTextSecondary.copy(alpha = 0.3f), modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text("Sin datos", color = ColorTextSecondary.copy(alpha = 0.5f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun WeeklyAttendanceSummary(days: List<WeeklyAttendanceDay>) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        days.forEach { day ->
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (day.attended) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f))
                        .border(1.dp, if (day.attended) Color.Transparent else ColorBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (day.attended) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = day.dayName.take(1),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.attended) MaterialTheme.colorScheme.primary else ColorTextSecondary
                )
            }
        }
    }
}

// =====================================================
// RÉCORDS UNIFICADOS (3 PESTAÑAS)
// =====================================================

private data class DisplayRecord(
    val name: String, val date: Date?, val score: String, val isRx: Boolean,
    val notes: String, val description: String, val uniqueId: String
)

enum class PerformanceTab { CLASSES, BENCHMARKS }

@Composable
fun UnifiedPerformanceRecordsCard(
    wodRecordsState: PerformanceState,
    benchmarkRecordsState: PerformanceState,
    globalBenchmarkRecordsState: PerformanceState,
    viewModel: PerformanceViewModel
) {
    var selectedTab by remember { mutableStateOf(PerformanceTab.CLASSES) }
    val primaryColor = LocalPrimaryColor.current

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header con Pestañas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.03f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PerformanceTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    val label = when(tab) {
                        PerformanceTab.CLASSES -> "MIS CLASES"
                        PerformanceTab.BENCHMARKS -> "MIS BENCHMARKS"
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { selectedTab = tab }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                            color = if (isSelected) primaryColor else Color.White.copy(alpha = 0.4f),
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            // Contenido dinámico según pestaña
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    PerformanceTab.CLASSES -> {
                        RecordColumnInsideGlass(
                            state = wodRecordsState,
                            label = "Mis Marcas",
                            viewModel = viewModel,
                            isBenchmark = false,
                            showHeaderLabel = false
                        )
                    }
                    PerformanceTab.BENCHMARKS -> {
                        RecordColumnInsideGlass(
                            state = benchmarkRecordsState,
                            label = "Mis Récords",
                            viewModel = viewModel,
                            isBenchmark = true,
                            showHeaderLabel = false
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordColumnInsideGlass(
    state: PerformanceState,
    label: String,
    viewModel: PerformanceViewModel,
    isBenchmark: Boolean,
    modifier: Modifier = Modifier,
    headerAction: (@Composable () -> Unit)? = null,
    showHeaderLabel: Boolean = true
) {
    var showHistory by remember { mutableStateOf(false) }

    if (showHistory && state is PerformanceState.Success) {
        RecordHistoryDialog(
            records = state.records,
            label = label,
            onDismiss = { showHistory = false },
            onDelete = { record ->
                viewModel.deleteRecord(record, isBenchmark)
                // If list becomes empty, dialog stays but shows empty state
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable { 
                if (state is PerformanceState.Success && state.records.isNotEmpty()) {
                    showHistory = true 
                }
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
            // Header con ícono historial
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showHeaderLabel) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape)
                        )
                        Text(
                            text = label.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                    }
                }
                
                headerAction?.invoke()
                
                if (headerAction == null && state is PerformanceState.Success && state.records.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.History, 
                        contentDescription = "Ver Historial",
                        tint = ColorTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Content
            when (state) {
                is PerformanceState.Loading, PerformanceState.Idle -> {
                    Box(Modifier.fillMaxWidth(), Alignment.Center) { 
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) 
                    }
                }
                is PerformanceState.Error -> {
                    Text(
                        text = "Error al cargar",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorError,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                is PerformanceState.Success -> {
                    if (state.records.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.FitnessCenter, null, tint = ColorTextSecondary.copy(0.25f), modifier = Modifier.size(24.dp))
                            Text(
                                text = "Sin marcas aún",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorTextSecondary.copy(alpha = 0.5f),
                                fontStyle = FontStyle.Italic,
                            )
                        }
                    } else {
                        val latestRecord = state.records.first()
                        val data = extractRecordData(latestRecord)

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            // Nombre del WOD (label pequeño)
                            Text(
                                text = data.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // Score grande y prominente (iOS parity)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = data.score,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (data.isRx) {
                                    Box(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primary.copy(0.12f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "RX",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}

private fun extractRecordData(record: PerformanceRecord): DisplayRecord {
    return when (val res = record.result) {
        is WodResult -> DisplayRecord(
            name = (record.wodDetails as? Wod)?.title ?: "WOD",
            date = res.date, score = res.score, isRx = res.isRx, notes = res.notes,
            description = (record.wodDetails as? Wod)?.description ?: "", uniqueId = res.wodId
        )
        is BenchmarkResult -> DisplayRecord(
            name = (record.wodDetails as? BenchmarkWod)?.name ?: (record.wodDetails as? GlobalChallenge)?.name ?: res.benchmarkName,
            date = res.date, score = res.score, isRx = res.isRx, notes = res.notes,
            description = (record.wodDetails as? BenchmarkWod)?.description ?: (record.wodDetails as? GlobalChallenge)?.description ?: "", uniqueId = res.benchmarkId
        )
        is ChallengeResult -> DisplayRecord(
            name = (record.wodDetails as? GlobalChallenge)?.name ?: (record.wodDetails as? BenchmarkWod)?.name ?: res.challengeName,
            date = res.date, score = res.score, isRx = res.isRx, notes = res.notes,
            description = (record.wodDetails as? GlobalChallenge)?.description ?: (record.wodDetails as? BenchmarkWod)?.description ?: "", uniqueId = res.challengeId
        )
        else -> DisplayRecord("Error", null, "", false, "", "", "")
    }
}

// --- ACHIEVEMENTS ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AchievementsSectionGlass(state: AchievementState) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        when (state) {
            is AchievementState.Loading, AchievementState.Idle -> Box(Modifier.padding(24.dp).fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            is AchievementState.Error -> Text(state.message, color = ColorError, modifier = Modifier.padding(16.dp))
            is AchievementState.Success -> {
                if (state.achievements.isEmpty()) {
                    Box(Modifier.padding(32.dp).fillMaxWidth(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.EmojiEvents, null, tint = ColorTextSecondary.copy(alpha = 0.3f), modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Sin logros aún", color = ColorTextSecondary.copy(alpha = 0.5f))
                        }
                    }
                } else {
                    FlowRow(
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        maxItemsInEachRow = 4
                    ) {
                        state.achievements.forEach { AchievementItem(it) }
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementItem(achievement: AchievementUiModel) {
    var showDialog by remember { mutableStateOf(false) }
    val isUnlocked = achievement.isUnlocked

    val icon = when (achievement.iconName) {
        // Iconos Numéricos existentes
        "flag" -> Icons.Default.Flag
        "local_fire_department" -> Icons.Default.LocalFireDepartment
        "star" -> Icons.Default.Star
        "military_tech" -> Icons.Default.MilitaryTech
        "shield" -> Icons.Default.Shield
        "workspace_premium" -> Icons.Default.WorkspacePremium
        "auto_awesome" -> Icons.Default.AutoAwesome
        "weekend" -> Icons.Default.Weekend

        // --- NUEVOS ICONOS INTELIGENTES ---
        "wb_sunny" -> Icons.Default.WbSunny
        "dark_mode" -> Icons.Default.DarkMode
        "whatshot" -> Icons.Default.Whatshot
        
        // Gamification 2.0
        "sports_kabaddi", "weekend_warrior" -> Icons.Default.FitnessCenter // Representing tough weekend training
        "calendar_today", "never_skip_monday" -> Icons.Default.CalendarToday
        "lunch_dining", "lunch_crew" -> Icons.Default.Restaurant // Lunch
        "sports_soccer", "hat_trick" -> Icons.Default.SportsSoccer
        "filter_2", "double_trouble" -> Icons.Default.Filter2
        "clean_hands", "clean_sunday" -> Icons.Default.Spa // Clean/Holy Sunday

        else -> Icons.Default.EmojiEvents
    }

    // Estilos según estado
    val iconTint = if (isUnlocked) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f)
    val bgBorder = if (isUnlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)
    val bgAlpha = if (isUnlocked) 0.4f else 0.1f

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp).clickable { showDialog = true }) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(Color.Black.copy(alpha = bgAlpha), CircleShape)
                .border(1.dp, bgBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(28.dp))
            if (!isUnlocked) {
                Icon(Icons.Default.Lock, null, tint = ColorTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(12.dp).align(Alignment.BottomEnd).padding(4.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            achievement.title,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            textAlign = TextAlign.Center,
            color = if(isUnlocked) ColorTextSecondary else ColorTextSecondary.copy(alpha = 0.5f),
            lineHeight = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }

    if (showDialog) {
        GlassAlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(achievement.title, color = ColorTextPrimary)
                    if (!isUnlocked) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "BLOQUEADO",
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorError,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(achievement.description, color = ColorTextSecondary, textAlign = TextAlign.Center)
                    if (isUnlocked && achievement.unlockedAt != null) {
                        Spacer(Modifier.height(8.dp))
                        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("es")).format(achievement.unlockedAt)
                        Text("Conseguido el: $dateStr", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
                    } else if (!isUnlocked) {
                        Spacer(Modifier.height(8.dp))
                        Text("Gana +${achievement.xpReward} XP", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD700))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(if (isUnlocked) "Genial" else "Entendido", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// --- HISTORY COMPONENTS (NEW) ---

@Composable
fun RecordHistoryDialog(
    records: List<PerformanceRecord>,
    label: String,
    onDismiss: () -> Unit,
    onDelete: (PerformanceRecord) -> Unit
) {
    GlassAlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Historial de $label",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ColorTextPrimary
            )
        },
        text = {
            if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("No hay registros guardados.", color = ColorTextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp), // Limit height
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(records) { record ->
                        RecordHistoryItem(record = record, onDelete = { onDelete(record) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun RecordHistoryItem(
    record: PerformanceRecord,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val data = extractRecordData(record)
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("es")) }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = data.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary
                    )
                    data.date?.let {
                        Text(
                            text = dateFormat.format(it),
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorTextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = data.score,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (data.isRx) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "RX",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Expanded Details
            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = ColorBorder)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (data.description.isNotBlank()) {
                         Text(
                            text = data.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorTextSecondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (data.notes.isNotBlank()) {
                        Text(
                            text = "Notas: ${data.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = ColorTextSecondary.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Delete Button
                   Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                       TextButton(
                           onClick = onDelete,
                           colors = ButtonDefaults.textButtonColors(contentColor = ColorError)
                       ) {
                           Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp))
                           Spacer(modifier = Modifier.width(4.dp))
                           Text("Eliminar registro")
                       }
                   }
                }
            }
        }
    }
}

// =====================================================
// SMART DAILY TIMELINE (GAMIFIED UI)
// =====================================================

@Composable
fun TrainingScheduleSummarySectionGlass(viewModel: UserTrainingViewModel, onConfigure: () -> Unit) {
    val intentions by viewModel.userIntentions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentWeekday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    val todayIntentions = intentions.filter { it.dayOfWeek == currentWeekday }.sortedBy { it.time }

    // Nombres cortos de los días L-D (semana empieza lunes, Calendar usa Dom=1)
    val dayNames = listOf("D","L","M","M","J","V","S")
    // Días del alfabeto calendario: 1=Dom, 2=Lun … 7=Sab
    val calDays = listOf(1,2,3,4,5,6,7)

    // Animación de borde angular tipo iOS
    val infiniteTransition = rememberInfiniteTransition(label = "todayBorder")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )

    val dayOfWeekName = remember {
        val cal = Calendar.getInstance()
        val sdf = java.text.SimpleDateFormat("EEEE", java.util.Locale.forLanguageTag("es-ES"))
        sdf.format(cal.time).replaceFirstChar { it.uppercase() }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF).copy(alpha = 0.15f),
                        Color(0xFF1C1C1E).copy(alpha = 0.56f),
                        Color(0xFF000000).copy(alpha = 0.10f)
                    ),
                    radius = 600f
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.31f),
                        Color.Black.copy(alpha = 0.19f)
                    ),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                ),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // ── HEADER ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ícono grande con fondo
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🏋️", fontSize = 26.sp)
                    }
                    Column {
                        Text(
                            "Hoy · $dayOfWeekName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            if (todayIntentions.isEmpty() && !isLoading && intentions.isNotEmpty())
                                "Sin entrenamiento planificado"
                            else if (intentions.isEmpty() && !isLoading)
                                "Planificador no configurado"
                            else
                                "${todayIntentions.size} turno${if (todayIntentions.size != 1) "s" else ""} agendado${if (todayIntentions.size != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (todayIntentions.isNotEmpty()) MaterialTheme.colorScheme.primary
                                    else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                // Botón editar premium
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { onConfigure() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Configurar",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // ── MINI CALENDARIO SEMANAL ───────────────────────────────────
            if (intentions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    calDays.forEachIndexed { idx, calDay ->
                        val hasIntention = intentions.any { it.dayOfWeek == calDay }
                        val isToday = calDay == currentWeekday
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                dayNames[idx],
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isToday) MaterialTheme.colorScheme.primary
                                        else Color.White.copy(alpha = 0.4f),
                                fontWeight = if (isToday) FontWeight.Black else FontWeight.Normal
                            )
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isToday && hasIntention -> MaterialTheme.colorScheme.primary
                                            isToday -> Color.White.copy(alpha = 0.12f)
                                            hasIntention -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                            else -> Color.Transparent
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (hasIntention) {
                                    Text(
                                        if (isToday) "•••" else "•",
                                        fontSize = if (isToday) 8.sp else 10.sp,
                                        color = if (isToday) Color.White
                                                else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp),
                color = Color.White.copy(alpha = 0.08f)
            )

            // ── CONTENIDO PRINCIPAL ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                when {
                    isLoading -> {
                        Box(Modifier.fillMaxWidth().padding(vertical = 20.dp), Alignment.Center) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    intentions.isEmpty() -> {
                        // Estado: Sin planificador configurado
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("📅", fontSize = 48.sp)
                            Text(
                                "Aún no configuraste tu rutina",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "Planifica tus días de entrenamiento y gana XP cada semana 🚀",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = onConfigure,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Configurar Planificador", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    todayIntentions.isEmpty() -> {
                        // Estado: Hoy es día de descanso o no planificado
                        val operatingHours by viewModel.gymOperatingHours.collectAsState()
                        val heatmapData by viewModel.heatmapData.collectAsState()
                        val emptyState = getEmptyStateMessage(operatingHours, heatmapData, currentWeekday)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (emptyState.isActive)
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else Color.White.copy(alpha = 0.06f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = emptyState.icon,
                                    contentDescription = null,
                                    tint = if (emptyState.isActive) MaterialTheme.colorScheme.primary
                                           else Color.White.copy(alpha = 0.3f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    emptyState.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (emptyState.isActive) Color.White else Color.White.copy(alpha = 0.6f)
                                )
                                Text(
                                    emptyState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.45f),
                                    fontStyle = if (!emptyState.isActive) FontStyle.Italic else FontStyle.Normal
                                )
                            }
                        }
                    }

                    else -> {
                        // Estado: Hay turnos agendados para hoy
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            ) {
                                todayIntentions.forEach { intention ->
                                    // Chip de turno premium
                                    Column(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                            .border(
                                                1.dp,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .padding(horizontal = 18.dp, vertical = 14.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            "🕐",
                                            fontSize = 20.sp
                                        )
                                        Text(
                                            intention.time,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        CapacityIndicatorGlassView(viewModel, currentWeekday, intention.time)
                                    }
                                }
                            }
                            // Motivación
                            val successGreen = Color(0xFF4CAF50)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(successGreen.copy(alpha = 0.08f))
                                    .border(1.dp, successGreen.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("✅", fontSize = 16.sp)
                                Text(
                                    "¡Entrenamiento planificado! Toca el planificador para ganar XP 💪",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = successGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrainingScheduleSectionGlass(viewModel: UserTrainingViewModel) {
    val intentions by viewModel.userIntentions.collectAsState()
    val operatingHours by viewModel.gymOperatingHours.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val heatmapData by viewModel.heatmapData.collectAsState()
    val scheduleXpEarned by viewModel.scheduleXpEarned.collectAsState()

    val XP_CAP = 140
    val xpProgress = (scheduleXpEarned / XP_CAP.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = xpProgress, label = "xpProgress")
    val capReached = scheduleXpEarned >= XP_CAP

    var highlightQuietHours by remember { mutableStateOf(false) }

    val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    var selectedDayTab by remember { mutableStateOf(today) }
    
    val days = listOf(2, 3, 4, 5, 6, 7, 1) // L..D
    val activeDays = days.filter { day ->
        val ranges = operatingHours[day]?.ranges
        ranges != null && ranges.isNotEmpty()
    }
    
    LaunchedEffect(activeDays) {
        if (activeDays.isNotEmpty() && !activeDays.contains(selectedDayTab)) {
            selectedDayTab = activeDays.first()
        }
    }

    var minHour = 24
    var maxHour = 0
    operatingHours[selectedDayTab]?.ranges?.forEach { range ->
        val startH = range.startTime.split(":").firstOrNull()?.toIntOrNull() ?: 24
        val endH = range.endTime.split(":").firstOrNull()?.toIntOrNull() ?: 0
        if (startH < minHour) minHour = startH
        if (endH > maxHour) maxHour = endH
    }
    val activeHours = if (minHour > maxHour) (7..22).toList() else (minHour..maxHour).toList()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Header con progreso XP
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Planificador Pro",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                // Badge de XP ganado
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (capReached) Color(0xFF34C759).copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (capReached) "140/140 XP ✓" else "$scheduleXpEarned/140 XP",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (capReached) Color(0xFF34C759) else MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Barra de progreso XP del planificador
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (capReached) Color(0xFF34C759) else MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
                Text(
                    text = if (capReached)
                        "🏆 ¡Límite alcanzado! Ya ganaste el máximo de XP con el planificador."
                    else
                        "Toca una franja para agendar tu entrenamiento y sumar XP 🚀",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (capReached) Color(0xFF34C759).copy(alpha = 0.8f) else ColorTextSecondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }


        if (activeDays.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ColorGlassSurface).padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Schedule, null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                    Text("No hay horarios configurados.", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            // Day Tabs & Equalizer Map
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Day Tabs
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    activeDays.forEach { day ->
                        val isSelected = selectedDayTab == day
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f))
                                .clickable { selectedDayTab = day }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = getDayNameShort(day),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White else Color.Gray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                            )
                        }
                    }
                }
                
                // Equalizer Timeline
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 12.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    activeHours.forEach { h ->
                        val timeStr = String.format(Locale.US, "%02d:00", h)
                        val isScheduled = intentions.any { it.dayOfWeek == selectedDayTab && it.time == timeStr }
                        val count = heatmapData[selectedDayTab]?.get(h) ?: 0
                        
                        // Height 30 to 120 dp
                        val barHeight = (30f + (count * 4f)).coerceAtMost(120f).dp
                        
                        val isQuiet = count < 5
                        val baseColor = when {
                            count == 0 -> Color.Gray.copy(alpha = 0.15f)
                            isQuiet -> Color.Green.copy(alpha = 0.9f)
                            count < 15 -> Color.Yellow.copy(alpha = 0.9f)
                            else -> Color.Red.copy(alpha = 0.9f)
                        }
                        
                        val finalColor = if (highlightQuietHours && !isQuiet && count > 0) baseColor.copy(alpha = 0.3f) else baseColor
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(36.dp)
                                    .height(barHeight)
                                    .clip(CircleShape)
                                    .background(finalColor)
                                    .run { 
                                        if (isScheduled) border(3.dp, Color.White, CircleShape) else this 
                                    }
                                    .clickable {
                                        viewModel.toggleIntention(selectedDayTab, timeStr)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isScheduled) {
                                    Icon(
                                        Icons.Default.CheckCircle, 
                                        contentDescription = null, 
                                        tint = Color.White, 
                                        modifier = Modifier.size(20.dp).offset(y = (-barHeight + 20.dp)/2)
                                    )
                                }
                            }
                            
                            Text(
                                text = "${h}h",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = if (isScheduled) FontWeight.Bold else FontWeight.Medium,
                                color = if (isScheduled) Color.White else Color.Gray
                            )
                        }
                    }
                }
            }
        }
        
        // My Week Summary
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Mi Semana", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                
                if (intentions.isEmpty()) {
                    Text("Aún no has agendado ningún turno.", color = Color.Gray, fontStyle = FontStyle.Italic, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                } else {
                    val grouped = intentions.groupBy { it.dayOfWeek }.toSortedMap()
                    grouped.forEach { (dayId, dayIntentions) ->
                        val sortedTimes = dayIntentions.map { it.time }.sorted()
                        if (sortedTimes.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(getDayNameShort(dayId), fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.width(60.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                                    sortedTimes.forEach { time ->
                                        Box(
                                            modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(time, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }

        // Preferences Toggle
        GlassCard(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔔 Notificarme", color = ColorTextPrimary, style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

@Composable
fun CapacityIndicatorGlassView(viewModel: UserTrainingViewModel, day: Int, time: String) {
    val heatmapData by viewModel.heatmapData.collectAsState()
    
    val hour = time.split(":").firstOrNull()?.toIntOrNull() ?: return
    val count = heatmapData[day]?.get(hour) ?: 0
    
    val color = when {
        count < 5 -> Color.Green
        count < 15 -> Color.Yellow
        else -> Color.Red
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (count > 0) color else Color.Green.copy(alpha = 0.5f))
        )
        Text(
            text = "~$count",
            style = MaterialTheme.typography.bodySmall,
            color = ColorTextSecondary
        )
    }
}

private fun getDayNameShort(day: Int): String {
    return when (day) {
        1 -> "Dom"
        2 -> "Lun"
        3 -> "Mar"
        4 -> "Mié"
        5 -> "Jue"
        6 -> "Vie"
        7 -> "Sáb"
        else -> ""
    }
}

data class EmptyStateMessage(val icon: androidx.compose.ui.graphics.vector.ImageVector, val title: String, val message: String, val isActive: Boolean)

fun getEmptyStateMessage(
    operatingHours: Map<Int, com.aquiles.crosschapp.presentation.viewmodel.UserTrainingViewModel.GymOperatingHours>,
    heatmapData: Map<Int, Map<Int, Int>>,
    currentWeekday: Int
): EmptyStateMessage {
    val defaultClosed = EmptyStateMessage(Icons.Default.Bedtime, "Día de descanso", "No tienes entrenamiento programado y el centro está cerrado.", false)
    val hours = operatingHours[currentWeekday] ?: return defaultClosed
    if (hours.ranges.isEmpty()) return defaultClosed

    val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val activeHours = mutableListOf<Int>()
    
    for (range in hours.ranges) {
        val startH = range.startTime.substringBefore(":").toIntOrNull()
        val endH = range.endTime.substringBefore(":").toIntOrNull()
        if (startH != null && endH != null) {
            val endCapped = maxOf(startH, endH - 1)
            activeHours.addAll(startH..endCapped)
        }
    }

    val upcomingHours = activeHours.filter { it >= currentHour }
    if (upcomingHours.isEmpty()) {
        return EmptyStateMessage(Icons.Default.Bedtime, "Casi termina el día", "El centro está por cerrar. ¡A descansar para mañana!", false)
    }

    var bestHour = upcomingHours.first()
    var minCount = Int.MAX_VALUE
    val heatmap = heatmapData[currentWeekday] ?: java.util.Collections.emptyMap()

    for (hour in upcomingHours) {
        val count = heatmap[hour] ?: 0
        if (count < minCount) {
            minCount = count
            bestHour = hour
        }
    }

    val timeStr = String.format("%02d:00", bestHour)
    val capacityStr = if (minCount <= 2) "vacío" else "menos concurrido"

    return EmptyStateMessage(Icons.Default.Bolt, "¡Aún estás a tiempo!", "El horario sugerido para hoy es a las ${timeStr}h (suele estar $capacityStr). ¡Te esperamos!", true)
}