package com.aquiles.crosschapp.presentation.home

import android.Manifest
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.aquiles.crosschapp.data.model.*
import com.aquiles.crosschapp.presentation.viewmodel.*
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.*

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.75f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
private val ColorBackgroundGradientStart = Color(0xFF000000)
private val ColorBackgroundGradientEnd = Color(0xFF121212)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WodsScreen(
    innerPadding: PaddingValues,
    wodsViewModel: WodsViewModel = viewModel(),
    adminViewModel: AdminViewModel = viewModel(),
    scheduleViewModel: ScheduleViewModel = viewModel(),
    performanceViewModel: PerformanceViewModel,
    onNavigateToCreateWod: () -> Unit,
    onNavigateToEditWod: (String) -> Unit,
    onNavigateToClassDetail: (String) -> Unit,
    onNavigateToScheduleAtDate: (LocalDate) -> Unit,
    onNavigateToWodHistory: () -> Unit,
    onNavigateToRequestCredits: () -> Unit
) {
    val currentUser by UserSession.currentUser.collectAsState()
    val currentUserGymId by UserSession.currentUserGymId.collectAsState()

    // Estados
    val todayWod by wodsViewModel.todayWod.collectAsState()
    val tomorrowWod by wodsViewModel.tomorrowWod.collectAsState()
    val wodsState by wodsViewModel.wodsState.collectAsState()
    val nextBookingState by scheduleViewModel.nextBookingState.collectAsState()
    val appConfigState by adminViewModel.appConfigState.collectAsState()

    // Estados de guardado
    val saveWodResultState by performanceViewModel.saveResultState.collectAsState()
    val saveBenchmarkState by performanceViewModel.saveBenchmarkState.collectAsState()

    val wodResultState by performanceViewModel.dailyWodRecordsState.collectAsState()
    val todayWodResultObject = (wodResultState as? PerformanceState.Success)?.records?.firstOrNull()?.result
    val todayWodResult = todayWodResultObject as? WodResult

    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Launchers
    val cameraLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { success ->
        if (success && imageUri != null) {
            adminViewModel.generateAndShareImage(context, imageUri!!, todayWodResult)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            val newImageUri = createImageUri(context)
            imageUri = newImageUri
            cameraLauncher.launch(newImageUri)
        } else {
            Toast.makeText(context, "Permiso de cámara denegado.", Toast.LENGTH_SHORT).show()
        }
    }

    // INIT
    LaunchedEffect(currentUserGymId) {
        val user = currentUser
        if (user != null && !currentUserGymId.isNullOrBlank()) {
            wodsViewModel.listenForDashboardWods()
            adminViewModel.loadAppConfig()
            adminViewModel.loadBenchmarkWods()
            performanceViewModel.loadInitialData()
        }
    }

    // Toasts
    LaunchedEffect(saveWodResultState) {
        if (saveWodResultState is SaveResultState.Success) {
            Toast.makeText(context, (saveWodResultState as SaveResultState.Success).message, Toast.LENGTH_SHORT).show()
            performanceViewModel.resetSaveResultState()
        }
    }
    LaunchedEffect(saveBenchmarkState) {
        if (saveBenchmarkState is BenchmarkSaveState.Success) {
            Toast.makeText(context, (saveBenchmarkState as BenchmarkSaveState.Success).message, Toast.LENGTH_SHORT).show()
            performanceViewModel.resetSaveState()
        }
    }

    // --- UI STRUCTURE ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            // CORRECCIÓN: Usamos negro semitransparente para que se vea tu fondo
            .background(Color.Black.copy(alpha = 0.1f))
    ) {
        val user = currentUser
        if (user == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ColorPrimaryAction)
            }
        } else {
            val hasValidAccess = user.canAccessSchedule

            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                "Dashboard",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = ColorTextPrimary
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    if (todayWod != null) permissionLauncher.launch(Manifest.permission.CAMERA)
                                    else Toast.makeText(context, "No hay WOD para compartir.", Toast.LENGTH_SHORT).show()
                                },
                                enabled = todayWod != null
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Compartir WOD",
                                    tint = if (todayWod != null) ColorPrimaryAction else ColorTextSecondary
                                )
                            }
                        },
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
                    Spacer(modifier = Modifier.height(0.dp))

                    // =========================================
                    // SECCIÓN: WOD DE HOY
                    // =========================================
                    SectionTitle("Workout del Día")

                    val spanishLocale = remember { Locale("es", "ES") }
                    val todayName = remember { LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, spanishLocale).uppercase(spanishLocale) }
                    val imagesByDay = (appConfigState as? AppConfigState.Success)?.imagesByDay
                    val todayImageUrl = imagesByDay?.get(todayName)
                    val restDayImageUrl = imagesByDay?.get("REST_DAY")

                    if (wodsState is WodsState.Loading) {
                        Box(Modifier.fillMaxWidth().height(250.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ColorPrimaryAction)
                        }
                    } else if (wodsState is WodsState.Error) {
                        InfoCard(icon = Icons.Default.Error, title = "Error", message = (wodsState as WodsState.Error).message)
                    } else {
                        if (todayWod != null) {
                            TodayWodCard(
                                wod = todayWod!!,
                                imageUrl = todayImageUrl,
                                onSaveResult = { result, isRx, notes ->
                                    performanceViewModel.saveWodResult(wodId = todayWod!!.id, score = result, notes = notes, isRx = isRx)
                                },
                                isSavingResult = saveWodResultState is SaveResultState.Loading
                            )
                        } else {
                            RestDayWodCard(imageUrl = restDayImageUrl)
                        }
                    }

                    // =========================================
                    // FILA: PRÓXIMA RESERVA Y MAÑANA
                    // =========================================
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {

                        // Izquierda: Próxima Reserva
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle("Próxima Clase")
                            when (val booking = nextBookingState) {
                                is NextBookingState.Loading -> GlassCard(modifier = Modifier.height(150.dp)) {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ColorPrimaryAction) }
                                }
                                is NextBookingState.Success -> {
                                    booking.nextClass?.let {
                                        NextBookingCardSmall(gymClass = it, onClick = { onNavigateToClassDetail(it.id) })
                                    } ?: InfoCardSmall(icon = Icons.Default.EventAvailable, title = "Sin Reserva", message = "Reserva tu plaza")
                                }
                                is NextBookingState.Error -> InfoCardSmall(icon = Icons.Default.Error, title = "Error", message = "No cargó info")
                            }
                        }

                        // Derecha: WOD Mañana
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SectionTitle("Mañana")
                            if (hasValidAccess) {
                                if (wodsState is WodsState.Loading) {
                                    GlassCard(modifier = Modifier.height(150.dp)) {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ColorPrimaryAction) }
                                    }
                                } else {
                                    tomorrowWod?.let {
                                        TomorrowWodCardSmall(wod = it, onClick = { onNavigateToScheduleAtDate(LocalDate.now().plusDays(1)) })
                                    } ?: RestDayCardSmall()
                                }
                            } else {
                                AccessBlockedCard(title = "Bloqueado", message = "Créditos", onClick = onNavigateToRequestCredits)
                            }
                        }
                    }

                    // =========================================
                    // SECCIÓN: BENCHMARKS
                    // =========================================
                    SectionTitle("Benchmarks")
                    BenchmarkLogger(
                        adminViewModel = adminViewModel,
                        performanceViewModel = performanceViewModel,
                        isSaving = saveBenchmarkState is BenchmarkSaveState.Loading
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// =====================================================
// COMPONENTES DE UI (DESIGN SYSTEM IMPLEMENTATION)
// =====================================================

/**
 * Componente base para el efecto Glassmorphism en Android
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        border = BorderStroke(1.dp, ColorBorder),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = ColorTextSecondary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

// --- TODAYS WOD CARD ---

@Composable
fun TodayWodCard(
    wod: Wod,
    imageUrl: String?,
    onSaveResult: (result: String, isRx: Boolean, notes: String) -> Unit,
    isSavingResult: Boolean
) {
    var userResult by remember { mutableStateOf("") }
    var userNotes by remember { mutableStateOf("") }
    var isRx by remember { mutableStateOf(false) } // Default Rx false (Scaled)
    val fallbackImageUrl = "https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?q=80&w=2070"

    // Tarjeta unificada: Imagen arriba, contenido abajo, todo sobre el "vidrio"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ColorBorder),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column {
            // Imagen Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                SubcomposeAsyncImage(
                    model = if (imageUrl.isNullOrBlank()) fallbackImageUrl else imageUrl,
                    contentDescription = "WOD Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ColorPrimaryAction) } }
                )
                // Gradiente para que el título resalte
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f)),
                                startY = 300f
                            )
                        )
                )
                Text(
                    text = wod.title,
                    color = ColorTextPrimary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                )
            }

            // Contenido (Descripción + Inputs)
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = wod.description,
                    color = ColorTextSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp
                )

                HorizontalDivider(color = ColorBorder)

                // Inputs
                GlassTextField(value = userResult, onValueChange = { userResult = it }, label = "Tu Resultado (Tiempo/Reps)")
                GlassTextField(value = userNotes, onValueChange = { userNotes = it }, label = "Notas (Opcional)")

                // Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { isRx = !isRx }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Checkbox(
                            checked = isRx,
                            onCheckedChange = { isRx = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = ColorPrimaryAction,
                                uncheckedColor = ColorTextSecondary,
                                checkmarkColor = Color.White
                            )
                        )
                        Text("RX", color = if (isRx) ColorTextPrimary else ColorTextSecondary, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onSaveResult(userResult, isRx, userNotes); userResult = ""; userNotes = ""; isRx = false },
                        enabled = userResult.isNotBlank() && !isSavingResult,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorPrimaryAction,
                            disabledContainerColor = ColorPrimaryAction.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSavingResult) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RestDayWodCard(imageUrl: String?) {
    val fallbackImageUrl = "https://images.unsplash.com/photo-1549060279-7e168fcee0c2"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ColorBorder),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface)
    ) {
        Box(modifier = Modifier.height(300.dp)) {
            SubcomposeAsyncImage(
                model = if (imageUrl.isNullOrBlank()) fallbackImageUrl else imageUrl,
                contentDescription = "Rest Day",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)) // Oscurecer toda la imagen un poco
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "REST DAY",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Recupera. Descansa. Repite.",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}

// --- SMALL CARDS ---

@Composable
fun NextBookingCardSmall(gymClass: GymClass, onClick: () -> Unit) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dayFormatter = remember { SimpleDateFormat("EEE dd", Locale("es", "ES")) }

    GlassCard(
        modifier = Modifier.height(150.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Column {
                Text(
                    text = gymClass.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = gymClass.dateTime?.let { dayFormatter.format(it) }?.uppercase() ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = ColorPrimaryAction,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = gymClass.dateTime?.let { timeFormatter.format(it) } ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ColorPrimaryAction
                )
            }
        }
    }
}

@Composable
fun TomorrowWodCardSmall(wod: Wod, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.height(150.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = wod.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    Icons.Default.ArrowForwardIos,
                    contentDescription = "Ver",
                    tint = ColorPrimaryAction,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun RestDayCardSmall() {
    GlassCard(modifier = Modifier.height(150.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Spa, contentDescription = null, tint = ColorTextSecondary, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Descanso", color = ColorTextSecondary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun InfoCardSmall(icon: ImageVector, title: String, message: String) {
    GlassCard(modifier = Modifier.height(150.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(28.dp), tint = ColorTextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = ColorTextSecondary)
            Text(text = message, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = ColorTextSecondary.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun AccessBlockedCard(title: String, message: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ColorPrimaryAction.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface) // Mantener consistencia visual pero con borde rojo
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = ColorPrimaryAction)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
            Text(text = message, style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)
        }
    }
}

@Composable
fun InfoCard(icon: ImageVector, title: String, message: String) {
    GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(imageVector = icon, contentDescription = title, tint = ColorTextSecondary)
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
                Text(text = message, style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)
            }
        }
    }
}

// --- BENCHMARK & INPUTS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkLogger(
    adminViewModel: AdminViewModel,
    performanceViewModel: PerformanceViewModel,
    isSaving: Boolean
) {
    val benchmarkWodsState by adminViewModel.benchmarkWodsState.collectAsState()
    val currentUser by UserSession.currentUser.collectAsState()

    var expanded by remember { mutableStateOf(false) }
    var selectedWod by remember { mutableStateOf<BenchmarkWod?>(null) }
    var score by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isRx by remember { mutableStateOf(true) }

    val saveState by performanceViewModel.saveBenchmarkState.collectAsState()
    LaunchedEffect(saveState) {
        if (saveState is BenchmarkSaveState.Success) {
            selectedWod = null; score = ""; notes = ""; isRx = true
        }
    }

    GlassCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Dropdown personalizado
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    value = selectedWod?.name ?: "Selecciona Benchmark...",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorBorder,
                        unfocusedBorderColor = ColorBorder,
                        focusedTextColor = ColorTextPrimary,
                        unfocusedTextColor = ColorTextPrimary,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = ColorPrimaryAction
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                // Menú desplegable con estilo oscuro
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color(0xFF2C2C2E)) // Gris oscuro solido para legibilidad del menu
                ) {
                    if (benchmarkWodsState is BenchmarkWodsState.Success) {
                        (benchmarkWodsState as BenchmarkWodsState.Success).wods.forEach { wod ->
                            DropdownMenuItem(
                                text = { Text(wod.name, color = ColorTextPrimary) },
                                onClick = { selectedWod = wod; expanded = false }
                            )
                        }
                    } else {
                        DropdownMenuItem(text = { Text("Cargando...", color = ColorTextSecondary) }, onClick = {})
                    }
                }
            }

            AnimatedVisibility(visible = selectedWod != null) {
                selectedWod?.let { wod ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = wod.description, style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)

                        if (wod.strategy.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = ColorPrimaryAction,
                                    modifier = Modifier.size(16.dp)
                                )
                                // CORRECCIÓN: Usamos bodySmall en lugar de caption
                                Text(
                                    text = wod.strategy,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = ColorTextSecondary
                                )
                            }
                        }

                        GlassTextField(value = score, onValueChange = { score = it }, label = "Tu Marca")
                        GlassTextField(value = notes, onValueChange = { notes = it }, label = "Notas")

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isRx = !isRx }) {
                                Checkbox(
                                    checked = isRx,
                                    onCheckedChange = { isRx = it },
                                    colors = CheckboxDefaults.colors(checkedColor = ColorPrimaryAction, uncheckedColor = ColorTextSecondary, checkmarkColor = Color.White)
                                )
                                Text("RX", color = ColorTextPrimary)
                            }
                            Button(
                                onClick = {
                                    currentUser?.let { user ->
                                        performanceViewModel.saveBenchmarkResult(
                                            BenchmarkResult(
                                                userId = user.id,
                                                gym_id = user.gym_id,
                                                benchmarkId = wod.id,
                                                benchmarkName = wod.name,
                                                score = score.trim(),
                                                isRx = isRx,
                                                notes = notes.trim()
                                            )
                                        )
                                    }
                                },
                                enabled = score.isNotBlank() && !isSaving,
                                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                if (isSaving) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White) else Text("Guardar")
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * TextField estandarizado para el estilo Glass
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = ColorTextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.White.copy(alpha = 0.5f),
            unfocusedBorderColor = ColorBorder,
            focusedTextColor = ColorTextPrimary,
            unfocusedTextColor = ColorTextPrimary,
            cursorColor = ColorPrimaryAction,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

// Helpers
private fun createImageUri(context: Context): Uri {
    val imageFolder = File(context.cacheDir, "images")
    imageFolder.mkdirs()
    val file = File(imageFolder, "shared_image_${System.currentTimeMillis()}.jpg")
    val authority = "${context.packageName}.provider"
    return FileProvider.getUriForFile(context, authority, file)
}