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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import java.time.format.TextStyle
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
    onNavigateToRequestCredits: () -> Unit
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

    // Resultado previo
    val wodResultState by performanceViewModel.dailyWodRecordsState.collectAsState()
    val todayWodResultObject = (wodResultState as? PerformanceState.Success)?.records?.firstOrNull()?.result
    val todayWodResult = todayWodResultObject as? WodResult

    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }

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

                // IMAGEN DEL DIA
                val spanishLocale = remember { Locale.forLanguageTag("es-ES") }
                val todayNameRaw = remember { LocalDate.now().dayOfWeek.getDisplayName(TextStyle.FULL, spanishLocale) }
                val todayKey = remember(todayNameRaw) { todayNameRaw.uppercase(spanishLocale) }

                val imagesByDay = (appConfigState as? AppConfigState.Success)?.imagesByDay
                val todayImageUrl = imagesByDay?.get(todayKey)
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

                    Box(modifier = Modifier.fillMaxWidth().height(550.dp)) {
                        VerticalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            pageSpacing = 16.dp,
                            contentPadding = PaddingValues(vertical = 0.dp)
                        ) { pageIndex ->
                            val gymClass = dailyClassesState.classes[pageIndex]
                            val isCurrentPage = pagerState.currentPage == pageIndex
                            val alpha = if (isCurrentPage) 1f else 0.5f

                            Box(modifier = Modifier.alpha(alpha)) {
                                WodPagerCard(
                                    gymClass = gymClass,
                                    imageUrl = todayImageUrl,
                                    onSaveResult = { result, isRx, notes ->
                                        val wodIdToSave = gymClass.wodId ?: gymClass.id
                                        performanceViewModel.saveWodResult(wodId = wodIdToSave, score = result, notes = notes, isRx = isRx)
                                    },
                                    isSavingResult = saveWodResultState is SaveResultState.Loading
                                )
                            }
                        }

                        VerticalPagerIndicator(
                            pagerState = pagerState,
                            itemCount = dailyClassesState.classes.size,
                            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)
                        )
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
                BenchmarkLogger(
                    adminViewModel = adminViewModel,
                    performanceViewModel = performanceViewModel,
                    isSaving = saveBenchmarkState is BenchmarkSaveState.Loading
                )

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
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(itemCount) { iteration ->
            val isSelected = pagerState.currentPage == iteration
            val color = if (isSelected) ColorPrimaryAction else Color.White.copy(alpha = 0.3f)
            val size = if (isSelected) 10.dp else 6.dp

            Box(
                modifier = Modifier
                    .padding(vertical = 4.dp)
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
    onSaveResult: (result: String, isRx: Boolean, notes: String) -> Unit,
    isSavingResult: Boolean
) {
    var userResult by remember { mutableStateOf("") }
    var userNotes by remember { mutableStateOf("") }
    var isRx by remember { mutableStateOf(false) }
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
                    Button(
                        onClick = { onSaveResult(userResult, isRx, userNotes); userResult = ""; userNotes = ""; isRx = false },
                        enabled = userResult.isNotBlank() && !isSavingResult,
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction, disabledContainerColor = ColorPrimaryAction.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        if (isSavingResult) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White) else Text("Guardar", fontWeight = FontWeight.Bold)
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
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    value = selectedWod?.name ?: "Selecciona Benchmark...",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = ColorGlassInput, unfocusedContainerColor = ColorGlassInput, focusedBorderColor = ColorBorder, unfocusedBorderColor = ColorBorder, focusedTextColor = ColorTextPrimary, unfocusedTextColor = ColorTextPrimary, cursorColor = ColorPrimaryAction),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color(0xFF2C2C2E))) {
                    if (benchmarkWodsState is BenchmarkWodsState.Success) {
                        (benchmarkWodsState as BenchmarkWodsState.Success).wods.forEach { wod ->
                            DropdownMenuItem(text = { Text(wod.name, color = Color.White) }, onClick = { selectedWod = wod; expanded = false })
                        }
                    }
                }
            }

            AnimatedVisibility(visible = selectedWod != null) {
                selectedWod?.let { wod ->
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FitnessCenter, null, tint = ColorTextSecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = "ESQUEMA DEL WOD", style = MaterialTheme.typography.labelSmall, color = ColorTextSecondary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = wod.description, style = MaterialTheme.typography.bodyMedium, color = Color.White, lineHeight = 24.sp, fontWeight = FontWeight.Medium)
                        }

                        if (wod.strategy.isNotBlank()) {
                            Box(modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp)).border(1.dp, ColorBorder.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(12.dp)) {
                                Column {
                                    Text(text = "ESTRATEGIA / SCALING", style = MaterialTheme.typography.labelSmall, color = ColorPrimaryAction, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = wod.strategy, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f), lineHeight = 20.sp)
                                }
                            }
                        }

                        GlassTextField(value = score, onValueChange = { score = it }, label = "Tu Marca (Tiempo/Reps)")
                        GlassTextField(value = notes, onValueChange = { notes = it }, label = "Notas personales")

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { isRx = !isRx }.padding(end = 8.dp)) {
                                Checkbox(checked = isRx, onCheckedChange = { isRx = it }, colors = CheckboxDefaults.colors(checkedColor = ColorPrimaryAction, uncheckedColor = ColorTextSecondary, checkmarkColor = Color.White))
                                Text(text = "RX", color = if (isRx) ColorTextPrimary else ColorTextSecondary, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    currentUser?.let { user ->
                                        performanceViewModel.saveBenchmarkResult(BenchmarkResult(userId = user.id, gym_id = user.gym_id, benchmarkId = wod.id, benchmarkName = wod.name, score = score.trim(), isRx = isRx, notes = notes.trim()))
                                    }
                                },
                                enabled = score.isNotBlank() && !isSaving,
                                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction, disabledContainerColor = ColorPrimaryAction.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                if (isSaving) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text("Guardar Marca", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassTextField(value: String, onValueChange: (String) -> Unit, label: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = ColorTextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = ColorGlassInput, unfocusedContainerColor = ColorGlassInput, focusedBorderColor = Color.White.copy(alpha = 0.5f), unfocusedBorderColor = ColorBorder, focusedTextColor = ColorTextPrimary, unfocusedTextColor = ColorTextPrimary, cursorColor = ColorPrimaryAction),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

private fun createImageUri(context: Context): Uri {
    val imageFolder = File(context.cacheDir, "images")
    imageFolder.mkdirs()
    val file = File(imageFolder, "shared_wod_${System.currentTimeMillis()}.jpg")
    val authority = "${context.packageName}.provider"
    return FileProvider.getUriForFile(context, authority, file)
}