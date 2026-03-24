package com.aquiles.crosschapp.presentation.home

import android.Manifest
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager // [Fix] Changed to Horizontal
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.graphicsLayer // [Fix] For scale animation
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.filled.ArrowForward // [New Import]
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.aquiles.crosschapp.R
import com.aquiles.crosschapp.data.model.*
import com.aquiles.crosschapp.presentation.viewmodel.*
import com.aquiles.crosschapp.presentation.components.FeedbackDialog
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.components.FeedbackType
import com.aquiles.crosschapp.ui.theme.LocalPrimaryColor // [Fix] Import LocalPrimaryColor
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.*
import androidx.compose.ui.draw.scale // [Fix] Import scale
import androidx.compose.animation.core.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardType

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.65f)
private val ColorGlassInput = Color(0xFFFFFFFF).copy(alpha = 0.10f)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color(0xFFAAAAAA)
private val ColorBorder = Color.White.copy(alpha = 0.15f)
private val ColorError = Color(0xFFEF5350)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WodsScreen(
    innerPadding: PaddingValues,
    wodsViewModel: WodsViewModel = viewModel(),
    adminViewModel: AdminViewModel = viewModel(),
    scheduleViewModel: ScheduleViewModel = viewModel(),
    performanceViewModel: PerformanceViewModel,
    onNavigateToClassDetail: (String) -> Unit,
    onNavigateToScheduleAtDate: (LocalDate) -> Unit,
    onNavigateToWodHistory: () -> Unit,
    onNavigateToRequestCredits: () -> Unit,
    onNavigateToBenchmarks: () -> Unit,
    onNavigateToChallengeRanking: (String) -> Unit = {}
) {
    val currentUser by UserSession.currentUser.collectAsState()
    val currentUserGymId by UserSession.currentUserGymId.collectAsState()

    // --- DYNAMIC THEMING ---
    val gym by UserSession.currentGym.collectAsState()
    val primaryColor = remember(gym) {
        try {
            if (gym?.primaryColor != null) Color(android.graphics.Color.parseColor(gym!!.primaryColor)) else Color(0xFFFC5200)
        } catch (e: Exception) {
            Color(0xFFFC5200)
        }
    }

    // Estados de Datos
    val dailyClassesState by wodsViewModel.dailyClassesState.collectAsState()
    val todayWod by wodsViewModel.todayWod.collectAsState()
    val wodsState by wodsViewModel.wodsState.collectAsState()
    val benchmarkWodsState by adminViewModel.benchmarksState.collectAsState()
    val hasActiveCompetition by wodsViewModel.hasActiveCompetition.collectAsState()

    val nextBookingState by scheduleViewModel.nextBookingState.collectAsState()
    val appConfigState by adminViewModel.appConfigState.collectAsState()
    val globalChallenges by wodsViewModel.globalChallenges.collectAsState()

    // Estados de guardado
    val saveWodResultState by performanceViewModel.saveResultState.collectAsState()
    val saveChallengeResultState by wodsViewModel.saveChallengeResultState.collectAsState()
    val saveBenchmarkState by performanceViewModel.saveBenchmarkState.collectAsState()
    val globalBenchmarkRecords by performanceViewModel.globalBenchmarkRecordsState.collectAsState()

    // --- DIALOGS FOR GLOBAL CHALLENGES ---
    var selectedChallengeToLog by remember { mutableStateOf<GlobalChallenge?>(null) }

    // Imágenes de Actividad (iOS Parity)
    val activityImages by adminViewModel.activityImagesState.collectAsState()
    LaunchedEffect(Unit) {
        if (activityImages.isEmpty()) {
            adminViewModel.loadActivityImages()
        }
    }

    // Resultados Multi-Sesión (Lista Completa)
    val wodResultState by performanceViewModel.dailyWodRecordsState.collectAsState()
    val dailyRecords = (wodResultState as? PerformanceState.Success)?.records ?: emptyList()

    // Resultado principal para compartir (Fallback al primero o lógica específica si se desea)
    val todayWodResultObject = dailyRecords.firstOrNull()?.result
    val todayWodResult = todayWodResultObject as? WodResult

    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    
    // Helper fecha
    val dayFormat = remember { SimpleDateFormat("EEEE", Locale.forLanguageTag("es-ES")) }

    // --- LAUNCHERS ---
    val cameraLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.TakePicture()) { success ->
        if (success && imageUri != null) {
            if (todayWod != null) {
                adminViewModel.generateAndShareImage(context, imageUri!!, todayWod!!, todayWodResult)
            } else {
                Toast.makeText(context, "No hay WOD principal para compartir.", Toast.LENGTH_SHORT).show()
            }
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

    // --- INIT ---
    LaunchedEffect(currentUserGymId) {
        val user = currentUser
        if (user != null && !currentUserGymId.isNullOrBlank()) {
            wodsViewModel.listenForDashboardWods()
            wodsViewModel.loadGlobalChallenges() // Cargar todos los desafíos globales
            adminViewModel.loadAppConfig()
            adminViewModel.loadBenchmarkWods()
            performanceViewModel.loadInitialData()
        }
    }

    // --- FEEDBACK DIALOG STATES ---
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    // --- HANDLERS ---
    LaunchedEffect(saveWodResultState) {
        if (saveWodResultState is SaveResultState.Success) {
            successMessage = (saveWodResultState as SaveResultState.Success).message
            showSuccessDialog = true
            performanceViewModel.resetSaveResultState()
        }
    }
    LaunchedEffect(saveBenchmarkState) {
        if (saveBenchmarkState is BenchmarkSaveState.Success) {
            successMessage = (saveBenchmarkState as BenchmarkSaveState.Success).message
            showSuccessDialog = true
            performanceViewModel.resetSaveState()
        }
    }

    LaunchedEffect(saveChallengeResultState) {
        if (saveChallengeResultState is ChallengeSaveState.Success) {
            successMessage = (saveChallengeResultState as ChallengeSaveState.Success).message
            showSuccessDialog = true
            performanceViewModel.loadInitialData() // Refresh records for Performance Screen
            wodsViewModel.resetChallengeSaveState()
        } else if (saveChallengeResultState is ChallengeSaveState.Error) {
            successMessage = (saveChallengeResultState as ChallengeSaveState.Error).message
            // Usamos el mismo diálogo pero podríamos cambiar el icono a ERROR si FeedbackDialog lo soporta
            showSuccessDialog = true 
            wodsViewModel.resetChallengeSaveState()
        }
    }

    // --- DIALOG COMPONENT ---
    FeedbackDialog(
        show = showSuccessDialog,
        type = FeedbackType.SUCCESS,
        title = "¡Registro Guardado!",
        message = successMessage,
        onDismiss = { showSuccessDialog = false }
    )

    // --- CHALLENGE LOG DIALOG ---
    if (selectedChallengeToLog != null) {
        ChallengeLogDialog(
            challenge = selectedChallengeToLog!!,
            onDismiss = { selectedChallengeToLog = null },
            onSave = { score, isRx, notes, videoUrl ->
                wodsViewModel.logChallengeResult(
                    challengeId = selectedChallengeToLog!!.id,
                    challengeName = selectedChallengeToLog!!.name,
                    score = score,
                    isRx = isRx,
                    notes = notes,
                    videoUrl = videoUrl.ifBlank { null }
                )
                selectedChallengeToLog = null
            },
            isSaving = saveChallengeResultState is ChallengeSaveState.Loading
        )
    }

    // --- UI STRUCTURE ---
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        CompositionLocalProvider(LocalPrimaryColor provides primaryColor) {
        // Fondo eliminado. Se delega a AppBackground que debe rodear el NavHost en MainActivity.

        val user = currentUser
        if (user == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = LocalPrimaryColor.current)
            }
        } else {
            val hasValidAccess = user.canAccessSchedule

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "DASHBOARD", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = ColorTextPrimary, letterSpacing = 1.sp)
                    IconButton(onClick = onNavigateToWodHistory) {
                        Icon(Icons.Default.History, contentDescription = "Historial", tint = ColorTextSecondary)
                    }
                }

                // IMAGEN DEL DIA (Legacy/Fallback)
                val imagesByDay = (appConfigState as? AppConfigState.Success)?.imagesByDay
                val restDayImageUrl = imagesByDay?.get("REST_DAY")

                // SECCIÓN WOD (PAGER)
                SectionTitle("Workout del Día")

                if (dailyClassesState.isLoading) {
                    GlassLoadingCard(height = 400.dp)
                } else {
                    // [Fix] Filtrar clases para mostrar SOLO las reservas del usuario
                    // Asumimos que user != null porque está verificado arriba
                    val enrolledClasses = remember(dailyClassesState.classes, user.id) {
                        dailyClassesState.classes.filter { 
                            it.enrolledUserIds.contains(user.id) || it.checkedInUserIds.contains(user.id)
                        }
                    }

                    if (enrolledClasses.isEmpty()) {
                        InfoCardSmall(
                            icon = Icons.Default.EventBusy, 
                            title = "Sin Reservas para Hoy", 
                            message = "Ve a Horarios para inscribirte en una clase."
                        )
                    } else {
                        val pagerState = rememberPagerState(
                            initialPage = 0, // Siempre empezar en el primero si son pocos
                            pageCount = { enrolledClasses.size }
                        )

                    LaunchedEffect(dailyClassesState.initialScrollIndex) {
                        if (dailyClassesState.classes.isNotEmpty()) {
                            pagerState.scrollToPage(dailyClassesState.initialScrollIndex)
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.fillMaxWidth().height(550.dp)) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                pageSpacing = 16.dp,
                                contentPadding = PaddingValues(horizontal = 32.dp) // Add horizontal padding for peek effect
                            ) { pageIndex ->
                                val gymClass = enrolledClasses.getOrNull(pageIndex)
                                if (gymClass != null) {
                                    val isCurrentPage = pagerState.currentPage == pageIndex
                                    
                                    // Scale animation for focus effect
                                    val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
                                    val scale = 1f - (0.1f * kotlin.math.abs(pageOffset)).coerceIn(0f, 0.2f)
                                    val alpha = if (isCurrentPage) 1f else 0.5f

                                    // Calcular imagen basada en el día de la clase
                                    val dayName = remember(gymClass.dateTime) {
                                        gymClass.dateTime?.let { dayFormat.format(it).uppercase() } ?: ""
                                    }
                                    val dynamicImage = activityImages[dayName]

                                    // Buscar Resultado Existente (Multi-Sesión)
                                    val existingResult = remember(gymClass.id, dailyRecords, saveWodResultState) {
                                        dailyRecords.find { record ->
                                            val res = record.result as? WodResult
                                            val matchSession = res?.classSessionId == gymClass.id
                                            val matchWodLegacy = (res?.classSessionId == null && res?.wodId == gymClass.wodId)
                                            matchSession || matchWodLegacy
                                        }?.result as? WodResult
                                    }

                                    Box(
                                        modifier = Modifier
                                            .graphicsLayer {
                                                scaleX = scale
                                                scaleY = scale
                                                this.alpha = alpha
                                            }
                                            .fillMaxHeight()
                                    ) {
                                        WodPagerCard(
                                            gymClass = gymClass,
                                            imageUrl = dynamicImage,
                                            existingResult = existingResult,
                                            onSaveResult = { result, isRx, notes, isPublic ->
                                                val wodIdToSave = gymClass.wodId ?: gymClass.id
                                                // PASAMOS EL CLASSSESSIONID (gymClass.id)
                                                performanceViewModel.saveWodResult(
                                                    wodId = wodIdToSave,
                                                    score = result,
                                                    notes = notes,
                                                    isRx = isRx,
                                                    classSessionId = gymClass.id,
                                                    wodName = gymClass.name, // [Fix] Save WOD Name
                                                    isPublic = isPublic
                                                )
                                            },
                                            isSavingResult = saveWodResultState is SaveResultState.Loading,
                                            isPremium = hasActiveCompetition
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Simple Dots Indicator
                        Row(
                            Modifier
                                .wrapContentHeight()
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(pagerState.pageCount) { iteration ->
                                val color = if (pagerState.currentPage == iteration) LocalPrimaryColor.current else Color.Gray.copy(alpha = 0.5f)
                                Box(
                                    modifier = Modifier
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .size(8.dp)
                                )
                            }
                        }
                    }
                }
                }

                // --- NUEVO: CARRUSEL DE DESAFÍOS GLOBALES (DEBAJO DEL WOD) ---
                if (hasValidAccess && globalChallenges.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SectionTitle("Desafíos Globales")
                    Spacer(modifier = Modifier.height(12.dp))
                    if (globalChallenges.isNotEmpty()) {
                        GlobalChallengePager(
                            challenges = globalChallenges, 
                            onNavigateToChallengeRanking = onNavigateToChallengeRanking,
                            onLogClick = { selectedChallengeToLog = it }
                        )
                    }
                }

                // --- PRÓXIMA CLASE ---
                when (val booking = nextBookingState) {
                    is NextBookingState.Success -> {
                        booking.nextClass?.let {
                            com.aquiles.crosschapp.presentation.components.GlassCard(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                    NextBookingInnerColumn(gymClass = it, onClick = { onNavigateToClassDetail(it.id) })
                                }
                            }
                        }
                    }
                    else -> Spacer(Modifier.height(0.dp))
                }

                // --- ACCESO A BENCHMARKS ---
                SectionTitle("Benchmarks")
                BenchmarkAccessCard(onClick = onNavigateToBenchmarks)

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // --- BOTÓN FLOTANTE DE CÁMARA (Si hay WOD) ---
        if (todayWod != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                FloatingCameraButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 50.dp, end = 16.dp),
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }
        }
    }
    }
}


// =====================================================
// COMPONENTES UI
// =====================================================

@Composable
fun VerticalPagerIndicator(
    pagerState: androidx.compose.foundation.pager.PagerState,
    itemCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(itemCount) { iteration ->
            val isSelected = pagerState.currentPage == iteration
            val color = if (isSelected) LocalPrimaryColor.current else Color.White.copy(alpha = 0.3f)
            val size = if (isSelected) 10.dp else 6.dp

            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clip(CircleShape)
                    .background(color)
                    .size(size)
            )
        }
    }
}

@Composable
fun WodPagerCard(
    gymClass: GymClass,
    imageUrl: String?,
    existingResult: WodResult?,
    onSaveResult: (result: String, isRx: Boolean, notes: String, isPublic: Boolean) -> Unit,
    isSavingResult: Boolean,
    isPremium: Boolean = false
) {
    var userResult by remember(existingResult) { mutableStateOf(existingResult?.score ?: "") }
    var userNotes by remember(existingResult) { mutableStateOf(existingResult?.notes ?: "") }
    var isRx by remember(existingResult) { mutableStateOf(existingResult?.isRx ?: true) }
    var isPublic by remember(existingResult) { mutableStateOf(existingResult?.isPublic ?: true) }

    val fallbackImageUrl = "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?q=80&w=2070"
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val hasResult = existingResult != null
    val validationStatus = existingResult?.validationStatus
    
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scaleCard by animateFloatAsState(if (isPremium && isPressed) 0.97f else 1f)
    
    // Premium Border Animation
    val infiniteTransition = rememberInfiniteTransition()
    val rotationAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart)
    )

    val classColor = remember(gymClass.hexColor) {
        try {
            Color(android.graphics.Color.parseColor(gymClass.hexColor))
        } catch (e: Exception) {
            Color(0xFFFC5200) // Fallback al naranja original
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scaleCard)
            .padding(top = if (isPremium) 24.dp else 0.dp)
            .clickable(
                 interactionSource = interactionSource,
                 indication = null,
                 onClick = { if (isPremium) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
            )
    ) {
        // Glowing Border for Premium
        if (isPremium) {
             Box(
                 modifier = Modifier
                     .matchParentSize()
                     .background(
                         Brush.sweepGradient(
                             colors = listOf(Color(0xFFD4AF37), Color(0xFFCC4800), Color(0xFFD4AF37)),
                             center = androidx.compose.ui.geometry.Offset(Float.NaN, Float.NaN) // Just to bypass errors, graphicsLayer takes care of rotation natively below
                         ),
                         shape = RoundedCornerShape(24.dp)
                     )
                     .graphicsLayer { rotationZ = rotationAnimation }
                     .border(2.dp, Brush.sweepGradient(listOf(Color(0xFFD4AF37), Color(0xFFCC4800), Color(0xFFD4AF37))), RoundedCornerShape(24.dp))
                     // Applying blur is tricky in standard modifier, we just use a border to simulate glow.
             )
        }

        com.aquiles.crosschapp.presentation.components.GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (isPremium) Color(0xFFD4AF37).copy(alpha = 0.15f) else classColor.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column {
            // ── IMAGEN CABECERA ──────────────────────────────────
            Box(modifier = Modifier.height(200.dp)) {
                SubcomposeAsyncImage(
                    model = if (!imageUrl.isNullOrBlank()) imageUrl else fallbackImageUrl,
                    contentDescription = "WOD Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = LocalPrimaryColor.current) } }
                )
                // Gradiente inferior
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
                )

                // ── Top: hora + coach ───────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Badge HORA (iOS style)
                    Row(
                        modifier = Modifier
                            .background(LocalPrimaryColor.current, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccessTime, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = timeFormatter.format(gymClass.dateTime ?: Date()),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    // Badge HOY (naranja, estilo iOS)
                    Row(
                        modifier = Modifier
                            .background(LocalPrimaryColor.current, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("HOY", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.sp)
                    }
                }

                // ── Bottom: nombre clase + badge RX resultado ───
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = gymClass.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    if (gymClass.coachName.isNotBlank()) {
                        Text(
                            text = "Coach: ${gymClass.coachName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // ── CONTENIDO ────────────────────────────────────────
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Descripción en fuente monospaced (iOS parity)
                if (gymClass.description.isNotBlank()) {
                    Text(
                        text = gymClass.description,
                        color = ColorTextSecondary,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        lineHeight = 22.sp,
                        modifier = Modifier.heightIn(max = 120.dp)
                    )
                }

                HorizontalDivider(color = ColorBorder, thickness = 1.dp)

                // ── ZONA RESULTADO (si ya existe) ─────────────────
                if (existingResult != null) {
                    // Mostrar resultado guardado + indicador validación
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "RESULTADO REGISTRADO",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF30D158),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Score grande
                                Text(
                                    text = existingResult.score,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Badge RX
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (existingResult.isRx) LocalPrimaryColor.current.copy(0.15f) else Color.Gray.copy(0.1f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (existingResult.isRx) "NIVEL A" else "NIVEL B",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (existingResult.isRx) LocalPrimaryColor.current else ColorTextSecondary,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    // Badge validación
                                    when (validationStatus) {
                                        "pending" -> BadgeValidation(text = "Pendiente", icon = Icons.Default.AccessTime, bgColor = Color(0xFFFF9500).copy(0.15f), textColor = Color(0xFFFF9500))
                                        "approved" -> BadgeValidation(text = "Validado ✓", icon = null, bgColor = Color(0xFF30D158).copy(0.15f), textColor = Color(0xFF30D158))
                                        "rejected" -> BadgeValidation(text = "Rechazado ✗", icon = null, bgColor = Color(0xFFFF3B30).copy(0.15f), textColor = Color(0xFFFF3B30))
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }
                }

                // ── CAMPOS PARA INGRESAR/ACTUALIZAR RESULTADO ────
                GlassTextField(value = userResult, onValueChange = { userResult = it }, label = if (hasResult) "Nuevo resultado" else "Resultado (Tiempo/Reps)")
                GlassTextField(value = userNotes, onValueChange = { userNotes = it }, label = "Notas")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { isRx = !isRx }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isRx, onCheckedChange = { isRx = it },
                            colors = CheckboxDefaults.colors(checkedColor = LocalPrimaryColor.current, uncheckedColor = ColorTextSecondary, checkmarkColor = Color.White)
                        )
                        Text("RX", fontWeight = FontWeight.Bold, color = if (isRx) ColorTextPrimary else ColorTextSecondary)
                    }
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { isPublic = !isPublic }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = isPublic, onCheckedChange = { isPublic = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = LocalPrimaryColor.current, uncheckedThumbColor = ColorTextSecondary, uncheckedTrackColor = ColorGlassSurface),
                            modifier = Modifier.scale(0.8f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (isPublic) "Público" else "Privado", fontWeight = FontWeight.Bold, color = if (isPublic) ColorTextPrimary else ColorTextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Button(
                    onClick = { onSaveResult(userResult, isRx, userNotes, isPublic) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = userResult.isNotBlank() && !isSavingResult,
                    colors = ButtonDefaults.buttonColors(containerColor = LocalPrimaryColor.current, disabledContainerColor = LocalPrimaryColor.current.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    if (isSavingResult) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text(if (hasResult) "ACTUALIZAR RESULTADO" else "GUARDAR RESULTADO", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
}

// Badge helper para validación
@Composable
private fun BadgeValidation(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, bgColor: Color, textColor: Color) {
    Row(
        modifier = Modifier.background(bgColor, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (icon != null) Icon(icon, null, tint = textColor, modifier = Modifier.size(10.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = textColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FloatingCameraButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier = modifier.size(50.dp).clip(CircleShape).background(Brush.linearGradient(colors = listOf(LocalPrimaryColor.current, Color(0xFFFF8A50)))).clickable(onClick = onClick).border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Compartir WOD", tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

// Composable GlassCard is now imported globally from components

@Composable
fun SectionTitle(title: String) {
    Text(text = title.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = ColorTextSecondary, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
}

@Composable
fun RestDayWodCard(imageUrl: String?) {
    val fallbackImageUrl = "https://images.unsplash.com/photo-1549060279-7e168fcee0c2"
    com.aquiles.crosschapp.presentation.components.GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(modifier = Modifier.height(350.dp)) {
            SubcomposeAsyncImage(model = if (!imageUrl.isNullOrBlank()) imageUrl else fallbackImageUrl, contentDescription = "Rest Day", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "REST DAY", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 4.sp)
                Text(text = "Recupera. Descansa. Repite.", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.9f))
            }
        }
    }
}

@Composable
fun NextBookingInnerColumn(gymClass: GymClass, onClick: () -> Unit) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dayFormatter = remember { SimpleDateFormat("EEE dd", Locale.forLanguageTag("es-ES")) }
    val dateStr = gymClass.dateTime?.let { dayFormatter.format(it) }?.uppercase() ?: ""
    val timeStr = gymClass.dateTime?.let { timeFormatter.format(it) } ?: ""

    Column(
        modifier = Modifier.fillMaxSize().clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "PRÓXIMA CLASE",
                style = MaterialTheme.typography.labelSmall,
                color = LocalPrimaryColor.current,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Text(
                text = gymClass.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (gymClass.coachName.isNotBlank()) {
                Text(
                    text = gymClass.coachName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = dateStr, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
            Row(
                modifier = Modifier
                    .background(LocalPrimaryColor.current, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AccessTime, null, tint = Color.White, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(4.dp))
                Text(timeStr, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlobalChallengePager(
    challenges: List<GlobalChallenge>, 
    onNavigateToChallengeRanking: (String) -> Unit,
    onLogClick: (GlobalChallenge) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { challenges.size })
    
    Column(modifier = Modifier.fillMaxWidth().height(200.dp)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 0.dp),
            pageSpacing = 12.dp
        ) { page ->
            val challenge = challenges[page]

            GlobalChallengeCard(
                challenge = challenge, 
                userRecord = null, // TODO: Cargar el récord real del usuario de WodsViewModel
                onClick = { onNavigateToChallengeRanking(challenge.id) },
                onLogClick = { onLogClick(challenge) }
            )
        }
        
        if (challenges.size > 1) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(challenges.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) Color(0xFFFFD700) else Color.White.copy(alpha = 0.3f)
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GlobalChallengeCard(
    challenge: GlobalChallenge, 
    userRecord: ChallengeResult?,
    onClick: () -> Unit,
    onLogClick: () -> Unit
) {
    val (typeText, icon) = remember(challenge.scoreType, challenge.measurementUnit) {
        val unit = (challenge.measurementUnit ?: challenge.scoreType).uppercase()
        when {
            unit.contains("TIME") || unit.contains("TIEMPO") -> "MEJOR TIEMPO" to Icons.Default.Timer
            unit.contains("WEIGHT") || unit.contains("PESO") || unit.contains("KG") || unit.contains("LB") -> "MÁXIMO PESO" to Icons.Default.FitnessCenter
            unit.contains("REPS") || unit.contains("REPETICIONES") || unit.contains("AMRAP") -> "MÁX REPETICIONES" to Icons.Default.Repeat
            unit.contains("ROUND") || unit.contains("RONDAS") -> "MÁX RONDAS" to Icons.Default.Refresh
            unit.contains("DISTANCE") || unit.contains("DISTANCIA") || unit.contains("METROS") -> "MAYOR DISTANCIA" to Icons.Default.Straighten
            else -> "PUNTUACIÓN" to Icons.Default.EmojiEvents
        }
    }

    com.aquiles.crosschapp.presentation.components.GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Fondo decorativo sutil
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 30.dp, y = 30.dp)
                    .background(Color(0xFFFFD700).copy(alpha = 0.05f), CircleShape)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .background(Color(0xFFFFD700).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(icon, null, tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                        Text(
                            text = typeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    }
                    
                    Text(
                        text = challenge.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (userRecord != null) {
                        Column {
                            Text(
                                text = "TU RÉCORD",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = userRecord.score,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700)
                            )
                        }
                    } else {
                        Text(
                            text = "Sin marcas aún",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.4f),
                            fontStyle = FontStyle.Italic
                        )
                    }

                    Text(
                        text = "Desafío de Comunidad 🌎",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                        color = Color(0xFFFFD700).copy(alpha = 0.8f)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (userRecord != null) "✓ REGISTRADO" else "PENDIENTE",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = if (userRecord != null) Color(0xFF30D158) else Color.White.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold
                    )

                    if (userRecord == null) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFC5200))), 
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable(onClick = onLogClick)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                "CARGAR MARCA", 
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), 
                                color = Color.Black.copy(alpha = 0.9f), 
                                fontWeight = FontWeight.Black
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .clickable(onClick = onClick)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                "VER PODIO", 
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), 
                                color = Color.White.copy(alpha = 0.7f), 
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChallengeLogDialog(
    challenge: GlobalChallenge,
    onDismiss: () -> Unit,
    onSave: (String, Boolean, String, String) -> Unit,
    isSaving: Boolean
) {
    var score by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }
    var isRx by remember { mutableStateOf(true) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.padding(16.dp).fillMaxWidth().wrapContentHeight()) {
            com.aquiles.crosschapp.presentation.components.GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "REGISTRAR DESAFÍO",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = challenge.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    GlassTextField(
                        value = score,
                        onValueChange = { score = it },
                        label = "Resultado (${challenge.measurementUnit ?: "Puntos"})",
                        keyboardType = if (challenge.measurementUnit.contains("TIME")) 
                            KeyboardType.Text 
                        else 
                            KeyboardType.Number
                    )

                    GlassTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "Notas (Opcional)"
                    )

                    GlassTextField(
                        value = videoUrl,
                        onValueChange = { videoUrl = it },
                        label = "Enlace de video (YouTube, Drive, etc.)",
                        keyboardType = KeyboardType.Uri
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { isRx = !isRx }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isRx,
                            onCheckedChange = { isRx = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFFD700))
                        )
                        Text("RX (Nivel competitivo)", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("CANCELAR", color = Color.White.copy(alpha = 0.6f))
                        }
                        Button(
                            onClick = { onSave(score, isRx, notes, videoUrl) },
                            enabled = score.isNotBlank() && !isSaving,
                            modifier = Modifier.weight(1f).height(45.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                            } else {
                                Text("GUARDAR", color = Color.Black, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

// TomorrowWodInnerColumn ELIMINADA (Se utiliza espacio para Desafíos Globales)

@Composable
fun InfoInnerColumn(icon: ImageVector, title: String, message: String) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(24.dp), tint = ColorTextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = ColorTextSecondary)
        Text(text = message, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = ColorTextSecondary.copy(alpha = 0.5f))
    }
}

@Composable
fun RestDayInnerColumn() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Spa, contentDescription = null, tint = ColorTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Descanso", color = ColorTextSecondary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AccessBlockedInnerColumn(title: String, message: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().clickable(onClick = onClick).padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(imageVector = Icons.Default.Lock, contentDescription = title, modifier = Modifier.size(24.dp), tint = ColorError)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = ColorError)
        Text(text = message, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = ColorError.copy(alpha = 0.8f))
    }
}

@Composable
fun RestDayCardSmall() {
    GlassCard(modifier = Modifier.height(140.dp)) {
        RestDayInnerColumn()
    }
}

@Composable
fun InfoCardSmall(icon: ImageVector, title: String, message: String) {
    GlassCard(modifier = Modifier.height(140.dp)) {
        InfoInnerColumn(icon, title, message)
    }
}

@Composable
fun AccessBlockedCard(title: String, message: String, onClick: () -> Unit = {}) {
    com.aquiles.crosschapp.presentation.components.GlassCard(
        modifier = Modifier.height(140.dp)
    ) {
        AccessBlockedInnerColumn(title, message, onClick)
    }
}

@Composable
fun InfoCard(icon: ImageVector, title: String, message: String) {
    com.aquiles.crosschapp.presentation.components.GlassCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(imageVector = icon, contentDescription = title, tint = ColorTextSecondary)
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
                Text(text = message, style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)
            }
        }
    }
}

@Composable
fun GlassLoadingCard(height: androidx.compose.ui.unit.Dp = 200.dp) {
    com.aquiles.crosschapp.presentation.components.GlassCard(modifier = Modifier.height(height)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = LocalPrimaryColor.current)
        }
    }
}

@Composable
fun BenchmarkAccessCard(onClick: () -> Unit) {
    com.aquiles.crosschapp.presentation.components.GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "REGISTRAR TU PROGRESO", style = MaterialTheme.typography.labelSmall, color = LocalPrimaryColor.current, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Ir a Benchmarks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                Text(text = "Registra tus PRs: Fran, Murph, Max Lifts...", style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)
            }
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(LocalPrimaryColor.current),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White)
            }
        }
    }
}
// Old BenchmarkLogger removed as it is now in BenchmarksScreen

@Composable
fun GlassTextField(
    value: String, 
    onValueChange: (String) -> Unit, 
    label: String,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = ColorTextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = ColorGlassInput, unfocusedContainerColor = ColorGlassInput, focusedBorderColor = Color.White.copy(alpha = 0.5f), unfocusedBorderColor = ColorBorder, focusedTextColor = ColorTextPrimary, unfocusedTextColor = ColorTextPrimary, cursorColor = LocalPrimaryColor.current),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType)
    )
}


private fun createImageUri(context: Context): Uri {
    val imageFolder = File(context.cacheDir, "images")
    imageFolder.mkdirs()
    val file = File(imageFolder, "shared_wod_${System.currentTimeMillis()}.jpg")
    val authority = "${context.packageName}.provider"
    return FileProvider.getUriForFile(context, authority, file)
}