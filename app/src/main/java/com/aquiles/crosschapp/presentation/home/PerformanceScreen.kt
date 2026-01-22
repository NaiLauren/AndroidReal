package com.aquiles.crosschapp.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.aquiles.crosschapp.data.model.*
import com.aquiles.crosschapp.presentation.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.75f)
private val ColorDialogSurface = Color(0xFF1C1C1E)
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
    performanceViewModel: PerformanceViewModel
) {
    val benchmarkState by performanceViewModel.benchmarkRecordsState.collectAsState()
    val dailyWodState by performanceViewModel.dailyWodRecordsState.collectAsState()
    val achievementsState by performanceViewModel.achievementsState.collectAsState()
    val chartState by performanceViewModel.attendanceChartState.collectAsState()
    val currentUser by UserSession.currentUser.collectAsState()

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
                        title = { Text("Mi Rendimiento", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
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

                    // 1. GRÁFICO DE ASISTENCIA
                    AttendanceChartSectionGlass(
                        state = chartState,
                        onTimeRangeSelected = { performanceViewModel.onTimeRangeSelected(it) }
                    )

                    // 2. RÉCORDS Y MARCAS
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f)) {
                            SectionLabel("BENCHMARKS")
                            RecordCardGlass(state = benchmarkState, label = "Benchmark", viewModel = performanceViewModel, isBenchmark = true)
                        }
                        Column(Modifier.weight(1f)) {
                            SectionLabel("WODS DIARIOS")
                            RecordCardGlass(state = dailyWodState, label = "WOD", viewModel = performanceViewModel, isBenchmark = false)
                        }
                    }

                    // 3. LOGROS
                    Column {
                        SectionLabel("LOGROS DESBLOQUEADOS")
                        AchievementsSectionGlass(state = achievementsState)
                    }

                    Spacer(Modifier.height(24.dp))
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ColorBorder),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Asistencia", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
                // Icono decorativo pequeño
                Icon(Icons.Default.BarChart, null, tint = ColorPrimaryAction, modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.height(16.dp))

            GlassSegmentedControl(
                selectedRange = state.selectedRange,
                onRangeSelected = onTimeRangeSelected
            )

            Spacer(Modifier.height(24.dp))

            Box(Modifier.fillMaxWidth().height(160.dp), Alignment.Center) {
                if (state.isLoading) {
                    CircularProgressIndicator(color = ColorPrimaryAction)
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
    val barColor = ColorPrimaryAction
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
                        .background(if (day.attended) ColorPrimaryAction else Color.White.copy(alpha = 0.05f))
                        .border(1.dp, if (day.attended) Color.Transparent else ColorBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (day.attended) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = day.dayName.take(1),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.attended) ColorPrimaryAction else ColorTextSecondary
                )
            }
        }
    }
}

// --- RECORD CARDS ---

private data class DisplayRecord(
    val name: String, val date: Date?, val score: String, val isRx: Boolean,
    val notes: String, val description: String, val uniqueId: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordCardGlass(
    state: PerformanceState,
    label: String,
    viewModel: PerformanceViewModel,
    isBenchmark: Boolean
) {
    var selectedRecord by remember { mutableStateOf<PerformanceRecord?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM", Locale("es")) }

    LaunchedEffect(state) {
        if (state is PerformanceState.Success && selectedRecord == null) {
            selectedRecord = state.records.firstOrNull()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(180.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ColorBorder),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface)
    ) {
        Column(Modifier.padding(16.dp)) {
            when (state) {
                is PerformanceState.Loading, PerformanceState.Idle -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = ColorPrimaryAction) }
                is PerformanceState.Error -> Box(Modifier.fillMaxSize(), Alignment.Center) { Icon(Icons.Default.ErrorOutline, null, tint = ColorError) }
                is PerformanceState.Success -> {
                    if (state.records.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) {
                            Text("Sin $label", color = ColorTextSecondary.copy(alpha = 0.5f), fontStyle = FontStyle.Italic)
                        }
                    } else {
                        // Header con Dropdown
                        // Header con Dropdown (CORREGIDO)
                        var menuExpanded by remember { mutableStateOf(false) }
                        val currentData = selectedRecord?.let { extractRecordData(it) }

                        // 1. Envolvemos todo en ExposedDropdownMenuBox
                        ExposedDropdownMenuBox(
                            expanded = menuExpanded,
                            onExpandedChange = { menuExpanded = !menuExpanded }
                        ) {
                            // 2. La Fila es el "Anchor" (Ancla)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor() // <--- IMPORTANTE: Esto vincula el menú a esta fila
                                    .clickable { menuExpanded = true },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = currentData?.name ?: label,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = ColorTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(Icons.Default.ArrowDropDown, null, tint = ColorTextSecondary)
                                    }
                                    currentData?.date?.let {
                                        Text(
                                            dateFormatter.format(it),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ColorTextSecondary
                                        )
                                    }
                                }

                                // Delete Action (z-index alto para que funcione el click sobre el anchor)
                                IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(24.dp)) {
                                    Icon(
                                        Icons.Default.Delete,
                                        null,
                                        tint = ColorError.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // 3. El Menú (Dentro del Box, ya no da error)
                            ExposedDropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(ColorDialogSurface)
                            ) {
                                state.records.forEach { record ->
                                    val data = extractRecordData(record)
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    data.name,
                                                    color = ColorTextPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    data.score,
                                                    color = ColorTextSecondary,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedRecord = record
                                            menuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        // Score Display (Adjusted for better fit)
                        currentData?.let { data ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = data.score,
                                    style = MaterialTheme.typography.titleLarge, // Reduced from headlineMedium
                                    fontWeight = FontWeight.Black,
                                    color = ColorPrimaryAction,
                                    modifier = Modifier.weight(1f, fill = false), // Allow text to shrink/wrap
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 28.sp
                                )
                                Spacer(Modifier.width(8.dp))
                                if (data.isRx) {
                                    Text(
                                        "RX", 
                                        style = MaterialTheme.typography.titleMedium, 
                                        fontWeight = FontWeight.Bold, 
                                        color = ColorTextSecondary, 
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                }
                            }
                            if (data.notes.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "\"${data.notes}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = ColorTextSecondary.copy(alpha = 0.7f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog && selectedRecord != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = ColorDialogSurface,
            title = { Text("Eliminar Registro", color = ColorTextPrimary) },
            text = { Text("¿Estás seguro?", color = ColorTextSecondary) },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteRecord(selectedRecord!!, isBenchmark); showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorError)
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar", color = ColorTextSecondary) } }
        )
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
            name = (record.wodDetails as? BenchmarkWod)?.name ?: res.benchmarkName,
            date = res.date, score = res.score, isRx = res.isRx, notes = res.notes,
            description = (record.wodDetails as? BenchmarkWod)?.description ?: "", uniqueId = res.benchmarkId
        )
        else -> DisplayRecord("Error", null, "", false, "", "", "")
    }
}

// --- ACHIEVEMENTS ---

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AchievementsSectionGlass(state: AchievementState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ColorBorder),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface)
    ) {
        when (state) {
            is AchievementState.Loading, AchievementState.Idle -> Box(Modifier.padding(24.dp).fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(color = ColorPrimaryAction) }
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

        else -> Icons.Default.EmojiEvents
    }

    // Estilos según estado
    val iconTint = if (isUnlocked) ColorPrimaryAction else Color.White.copy(alpha = 0.2f)
    val bgBorder = if (isUnlocked) ColorPrimaryAction.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)
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
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = ColorDialogSurface,
            icon = { Icon(icon, null, tint = iconTint) },
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
                        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale("es")).format(achievement.unlockedAt)
                        Text("Conseguido el: $dateStr", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
                    } else if (!isUnlocked) {
                        Spacer(Modifier.height(8.dp))
                        Text("Gana +${achievement.xpReward} XP", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFD700))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(if (isUnlocked) "Genial" else "Entendido", color = ColorPrimaryAction, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}