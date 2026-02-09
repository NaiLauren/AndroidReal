package com.aquiles.crosschapp.presentation.home

import android.Manifest
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.TextStyle as JavaTextStyle
import java.util.*

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.85f)
private val ColorGlassInput = Color(0xFFFFFFFF).copy(alpha = 0.07f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.15f)

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
    onNavigateToBenchmarks: () -> Unit
) {
    val currentUser by UserSession.currentUser.collectAsState()
    val currentUserGymId by UserSession.currentUserGymId.collectAsState()

    // Estados de Datos
    val dailyClassesState by wodsViewModel.dailyClassesState.collectAsState()
    val todayWod by wodsViewModel.todayWod.collectAsState()
    val tomorrowWod by wodsViewModel.tomorrowWod.collectAsState()
    val wodsState by wodsViewModel.wodsState.collectAsState()

    val nextBookingState by scheduleViewModel.nextBookingState.collectAsState()
    val appConfigState by adminViewModel.appConfigState.collectAsState()

    // Estados de guardado
    val saveWodResultState by performanceViewModel.saveResultState.collectAsState()
    val saveBenchmarkState by performanceViewModel.saveBenchmarkState.collectAsState()

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
    val dayFormat = remember { SimpleDateFormat("EEEE", Locale("es", "ES")) }

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
            adminViewModel.loadAppConfig()
            adminViewModel.loadBenchmarkWods()
            performanceViewModel.loadInitialData()
        }
    }

    // --- TOASTS ---
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
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.fondo_principal),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.75f))
        )

        val user = currentUser
        if (user == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ColorPrimaryAction)
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
                } else if (dailyClassesState.classes.isEmpty()) {
                    RestDayWodCard(imageUrl = restDayImageUrl)
                } else {
                    val pagerState = rememberPagerState(
                        initialPage = dailyClassesState.initialScrollIndex,
                        pageCount = { dailyClassesState.classes.size }
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
                                val gymClass = dailyClassesState.classes.getOrNull(pageIndex)
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
                                            onSaveResult = { result, isRx, notes ->
                                                val wodIdToSave = gymClass.wodId ?: gymClass.id
                                                // PASAMOS EL CLASSSESSIONID (gymClass.id)
                                                performanceViewModel.saveWodResult(
                                                    wodId = wodIdToSave,
                                                    score = result,
                                                    notes = notes,
                                                    isRx = isRx,
                                                    classSessionId = gymClass.id,
                                                    wodName = gymClass.name // [Fix] Save WOD Name
                                                )
                                            },
                                            isSavingResult = saveWodResultState is SaveResultState.Loading
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
                                val color = if (pagerState.currentPage == iteration) ColorPrimaryAction else Color.Gray.copy(alpha = 0.5f)
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

                // PRÓXIMA RESERVA / MAÑANA
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle("Próxima Clase")
                        when (val booking = nextBookingState) {
                            is NextBookingState.Loading -> GlassLoadingCard(height = 140.dp)
                            is NextBookingState.Success -> {
                                booking.nextClass?.let {
                                    NextBookingCardSmall(gymClass = it, onClick = { onNavigateToClassDetail(it.id) })
                                } ?: InfoCardSmall(icon = Icons.Default.EventAvailable, title = "Sin Reserva", message = "Reserva tu plaza")
                            }
                            is NextBookingState.Error -> InfoCardSmall(icon = Icons.Default.Error, title = "Error", message = "No cargó info")
                        }
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SectionTitle("Mañana")
                        if (hasValidAccess) {
                            if (wodsState is WodsState.Loading) {
                                GlassLoadingCard(height = 140.dp)
                            } else {
                                tomorrowWod?.let {
                                    TomorrowWodCardSmall(wod = it, onClick = { onNavigateToScheduleAtDate(LocalDate.now().plusDays(1)) })
                                } ?: RestDayCardSmall()
                            }
                        } else {
                            AccessBlockedCard(title = "Bloqueado", message = "Requiere Créditos", onClick = onNavigateToRequestCredits)
                        }
                    }
                }

                // BENCHMARKS
                SectionTitle("Benchmarks")
                BenchmarkAccessCard(onClick = onNavigateToBenchmarks)

                Spacer(modifier = Modifier.height(80.dp))
            }

            if (todayWod != null) {
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
            val color = if (isSelected) ColorPrimaryAction else Color.White.copy(alpha = 0.3f)
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
    existingResult: WodResult?, // New Param
    onSaveResult: (result: String, isRx: Boolean, notes: String) -> Unit,
    isSavingResult: Boolean
) {
    // Inicializar estado con resultado existente
    var userResult by remember(existingResult) { mutableStateOf(existingResult?.score ?: "") }
    var userNotes by remember(existingResult) { mutableStateOf(existingResult?.notes ?: "") }
    var isRx by remember(existingResult) { mutableStateOf(existingResult?.isRx ?: true) }
    
    val fallbackImageUrl = "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?q=80&w=2070"
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ColorBorder),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface)
    ) {
        Column {
            Box(modifier = Modifier.height(200.dp)) {
                SubcomposeAsyncImage(
                    model = if (!imageUrl.isNullOrBlank()) imageUrl else fallbackImageUrl,
                    contentDescription = "WOD Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ColorPrimaryAction) } }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color(0xFF1C1C1E)), startY = 200f))
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(text = "${timeFormatter.format(gymClass.dateTime ?: Date())} HS", color = ColorPrimaryAction, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                    if (gymClass.coachName.isNotBlank()) {
                        Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text(text = "Coach: ${gymClass.coachName}", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                    Text(text = gymClass.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = ColorTextPrimary)
                }
            }

            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(modifier = Modifier.heightIn(min = 40.dp)) {
                    Text(text = gymClass.description, color = ColorTextSecondary, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
                }

                HorizontalDivider(color = ColorBorder, thickness = 1.dp)

                GlassTextField(value = userResult, onValueChange = { userResult = it }, label = "Resultado (Tiempo/Reps)")
                GlassTextField(value = userNotes, onValueChange = { userNotes = it }, label = "Notas")

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { isRx = !isRx }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isRx, onCheckedChange = { isRx = it }, colors = CheckboxDefaults.colors(checkedColor = ColorPrimaryAction, uncheckedColor = ColorTextSecondary, checkmarkColor = Color.White))
                        Text("RX", fontWeight = FontWeight.Bold, color = if (isRx) ColorTextPrimary else ColorTextSecondary)
                    }
                    
                    // Conditionally change Button Label if updating
                    val isUpdating = existingResult != null
                    Button(
                        onClick = { onSaveResult(userResult, isRx, userNotes) },
                        enabled = userResult.isNotBlank() && !isSavingResult,
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction, disabledContainerColor = ColorPrimaryAction.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        if (isSavingResult) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White) 
                        } else {
                            Text(if (isUpdating) "Actualizar" else "Guardar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FloatingCameraButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier = modifier.size(50.dp).clip(CircleShape).background(Brush.linearGradient(colors = listOf(ColorPrimaryAction, Color(0xFFFF8A50)))).clickable(onClick = onClick).border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape), contentAlignment = Alignment.Center) {
        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Compartir WOD", tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

@Composable
fun GlassCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, ColorBorder), colors = CardDefaults.cardColors(containerColor = ColorGlassSurface), elevation = CardDefaults.cardElevation(0.dp)) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(text = title.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = ColorTextSecondary, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
}

@Composable
fun RestDayWodCard(imageUrl: String?) {
    val fallbackImageUrl = "https://images.unsplash.com/photo-1549060279-7e168fcee0c2"
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, ColorBorder), colors = CardDefaults.cardColors(containerColor = ColorGlassSurface)) {
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
fun NextBookingCardSmall(gymClass: GymClass, onClick: () -> Unit) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dayFormatter = remember { SimpleDateFormat("EEE dd", Locale.forLanguageTag("es-ES")) }
    GlassCard(modifier = Modifier.height(140.dp), onClick = onClick) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(text = gymClass.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ColorTextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(text = gymClass.dateTime?.let { dayFormatter.format(it) }?.uppercase() ?: "", style = MaterialTheme.typography.labelSmall, color = ColorTextSecondary)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = ColorPrimaryAction, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = gymClass.dateTime?.let { timeFormatter.format(it) } ?: "", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
            }
        }
    }
}

@Composable
fun TomorrowWodCardSmall(wod: Wod, onClick: () -> Unit) {
    GlassCard(modifier = Modifier.height(140.dp), onClick = onClick) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text(text = wod.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorTextPrimary, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Ver", tint = ColorPrimaryAction, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun RestDayCardSmall() {
    GlassCard(modifier = Modifier.height(140.dp)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Spa, contentDescription = null, tint = ColorTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Descanso", color = ColorTextSecondary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InfoCardSmall(icon: ImageVector, title: String, message: String) {
    GlassCard(modifier = Modifier.height(140.dp)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(24.dp), tint = ColorTextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = ColorTextSecondary)
            Text(text = message, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = ColorTextSecondary.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun AccessBlockedCard(title: String, message: String, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().height(140.dp).clickable(onClick = onClick), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, ColorPrimaryAction.copy(alpha = 0.6f)), colors = CardDefaults.cardColors(containerColor = ColorGlassSurface)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
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

@Composable
fun GlassLoadingCard(height: androidx.compose.ui.unit.Dp = 200.dp) {
    GlassCard(modifier = Modifier.height(height)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ColorPrimaryAction)
        }
    }
}

@Composable
fun BenchmarkAccessCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface),
        border = BorderStroke(1.dp, ColorBorder)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "REGISTRAR TU PROGRESO", style = MaterialTheme.typography.labelSmall, color = ColorPrimaryAction, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Ir a Benchmarks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                Text(text = "Registra tus PRs: Fran, Murph, Max Lifts...", style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)
            }
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(ColorPrimaryAction),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowForward, null, tint = Color.White)
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
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = ColorGlassInput, unfocusedContainerColor = ColorGlassInput, focusedBorderColor = Color.White.copy(alpha = 0.5f), unfocusedBorderColor = ColorBorder, focusedTextColor = ColorTextPrimary, unfocusedTextColor = ColorTextPrimary, cursorColor = ColorPrimaryAction),
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