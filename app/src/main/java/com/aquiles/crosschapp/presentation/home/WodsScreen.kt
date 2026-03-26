package com.aquiles.crosschapp.presentation.home

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aquiles.crosschapp.data.model.*
import com.aquiles.crosschapp.presentation.viewmodel.*
import com.aquiles.crosschapp.presentation.components.FeedbackDialog
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.components.FeedbackType
import com.aquiles.crosschapp.ui.theme.LocalPrimaryColor
import java.io.File
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.*
import java.text.SimpleDateFormat
import android.widget.Toast
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.shape.RoundedCornerShape
import com.aquiles.crosschapp.presentation.home.WDesignColorTextSecondary

// Las constantes de diseño se utilizan ahora desde WodComponents.kt

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
    val userChallengeRecords by wodsViewModel.userChallengeRecords.collectAsState()

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
            wodsViewModel.loadGlobalChallenges() // Refrescar récords del usuario tras guardar
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
        val existingChallengeRecord = userChallengeRecords[selectedChallengeToLog!!.id]
        ChallengeLogDialog(
            challenge = selectedChallengeToLog!!,
            existingRecord = existingChallengeRecord,
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // HEADER
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "DASHBOARD", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = WDesignColorTextPrimary, letterSpacing = 1.sp)
                    IconButton(onClick = onNavigateToWodHistory) {
                        Icon(Icons.Default.History, contentDescription = "Historial", tint = WDesignColorTextSecondary)
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
                                    
                                    // Logica específica de visualización para competencias
                                    val isCompetition = gymClass.classType == "COMPETITION"
                                    val finalImageUrl = if (isCompetition && !gymClass.imageUrl.isNullOrBlank()) {
                                        gymClass.imageUrl
                                    } else {
                                        dynamicImage
                                    }

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
                                            imageUrl = finalImageUrl,
                                            existingResult = existingResult,
                                            onSaveResult = { result, isRx, notes, isPublic ->
                                                val wodIdToSave = gymClass.wodId ?: gymClass.id
                                                performanceViewModel.saveWodResult(
                                                    wodId = wodIdToSave,
                                                    score = result,
                                                    notes = notes,
                                                    isRx = isRx,
                                                    classSessionId = gymClass.id,
                                                    wodName = gymClass.name,
                                                    isPublic = isPublic,
                                                    competitionId = gymClass.competitionId
                                                )
                                            },
                                            isSavingResult = saveWodResultState is SaveResultState.Loading,
                                            isPremium = isCompetition
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

                // --- PRÓXIMA CLASE (PEQUEÑA, COMPACTA) ---
                when (val booking = nextBookingState) {
                    is NextBookingState.Success -> {
                        booking.nextClass?.let {
                            com.aquiles.crosschapp.presentation.components.GlassCard(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                                    .border(androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha=0.15f)), RoundedCornerShape(16.dp)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                            NextBookingInnerColumn(gymClass = it, onClick = { onNavigateToClassDetail(it.id) })
                            }
                        }
                    }
                    else -> Spacer(Modifier.height(0.dp))
                }

                // --- DESAFÍOS GLOBALES (TARJETA GRANDE - JERARQUÍA 1) ---
                if (hasValidAccess && globalChallenges.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        SectionTitle("🌎 Desafíos Globales")
                        
                        // Tarjeta Única Integrada
                        com.aquiles.crosschapp.presentation.components.GlassCard(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                .border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha=0.3f)), RoundedCornerShape(24.dp)),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp, start = 20.dp, end = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Cabecera explicativa
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "COMPETENCIA INTER-GYMS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFFD700),
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                    Text(
                                        "Compite con otros gymnasios",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Compara tu marca con otros gyms • Ve el ranking global",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = WDesignColorTextSecondary,
                                        lineHeight = 18.sp
                                    )
                                }
                                
                                // Carrusel de desafíos integrado sin fondo adicional
                                GlobalChallengePager(
                                    challenges = globalChallenges,
                                    userRecords = userChallengeRecords,
                                    onNavigateToChallengeRanking = onNavigateToChallengeRanking,
                                    onLogClick = { selectedChallengeToLog = it }
                                )
                            }
                        }
                    }
                }

                // --- BENCHMARKS (TARJETA GRANDE SEPARADA - JERARQUÍA 1) ---
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    SectionTitle("💪 Benchmarks de la Casa")
                    
                    com.aquiles.crosschapp.presentation.components.GlassCard(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToBenchmarks),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp, start = 20.dp, end = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "EJERCICIOS PROGRAMADOS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LocalPrimaryColor.current,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    "Benchmarks de tu gym",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Text(
                                "Carga tus resultados • Sube al Muro en la categoría Benchmarks • Compite con tu gym",
                                style = MaterialTheme.typography.bodySmall,
                                color = WDesignColorTextSecondary,
                                lineHeight = 18.sp
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text(
                                    "VER BENCHMARKS →",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LocalPrimaryColor.current,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

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
fun FloatingCameraButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(colors = listOf(LocalPrimaryColor.current, Color(0xFFFF8A50))))
            .clickable(onClick = onClick)
            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Compartir WOD", tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

private fun createImageUri(context: Context): Uri {
    val imageFolder = File(context.cacheDir, "images")
    imageFolder.mkdirs()
    val file = File(imageFolder, "shared_wod_${System.currentTimeMillis()}.jpg")
    val authority = "${context.packageName}.provider"
    return FileProvider.getUriForFile(context, authority, file)
}