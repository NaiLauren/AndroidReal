package com.aquiles.crosschapp.presentation.home

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import com.aquiles.crosschapp.data.model.GymClass
import com.aquiles.crosschapp.data.model.User
import com.aquiles.crosschapp.presentation.viewmodel.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*
import androidx.compose.foundation.layout.height
import androidx.compose.ui.draw.drawBehind
import com.aquiles.crosschapp.presentation.components.GlassCardSurface

// --- HELPER EXTENSION ---
fun String.toColorSafe(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color(0xFFFC5200) // Fallback Naranja
    }
}

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.45f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)

// Status Colors
private val ColorStatusOngoing = Color(0xFFFFD600)
private val ColorStatusPast = Color.Gray
private val ColorStatusCancelled = Color(0xFFEF5350)
private val ColorSuccess = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    innerPadding: PaddingValues,
    scheduleViewModel: ScheduleViewModel,
    initialDateStr: String?,
    onClassClick: (String) -> Unit,
    onNavigateToRequestCredits: () -> Unit
) {
    val context = LocalContext.current

    val initialDate = remember(initialDateStr) {
        if (initialDateStr != null) {
            try { LocalDate.parse(initialDateStr) } catch (_: Exception) { LocalDate.now() }
        } else { LocalDate.now() }
    }
    var selectedDate by remember { mutableStateOf(initialDate) }

    val classesState by scheduleViewModel.classesState.collectAsState()
    val bookingState by scheduleViewModel.bookingState.collectAsState()
    val gymOperatingHoursState by scheduleViewModel.gymOperatingHours.collectAsState()
    val currentUser by UserSession.currentUser.collectAsState()

    LaunchedEffect(selectedDate) {
        scheduleViewModel.listenForClassesOnDate(selectedDate)
    }

    LaunchedEffect(bookingState) {
        if (bookingState is BookingState.Success) {
            Toast.makeText(context, (bookingState as BookingState.Success).message, Toast.LENGTH_SHORT).show()
            scheduleViewModel.resetBookingState()
        } else if (bookingState is BookingState.Error) {
            Toast.makeText(context, (bookingState as BookingState.Error).message, Toast.LENGTH_LONG).show()
            scheduleViewModel.resetBookingState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.1f))
    ) {
        if (currentUser == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ColorPrimaryAction)
            }
        } else {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        modifier = Modifier.height(72.dp),
                        title = { Text("Horarios", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f))
                    )
                },
                containerColor = Color.Transparent
            ) { localPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = localPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
                ) {
                    DateSelector(
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it },
                        user = currentUser!!
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!currentUser!!.canAccessSchedule) {
                        AccessBlockedView(onNavigateToRequestCredits)
                    } else {
                        ClassesListContent(
                            classesState = classesState,
                            gymOperatingHoursState = gymOperatingHoursState,
                            selectedDate = selectedDate,
                            onClassClick = onClassClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClassesListContent(
    classesState: ClassesState,
    gymOperatingHoursState: Map<Int, ScheduleViewModel.GymOperatingHours>,
    selectedDate: LocalDate,
    onClassClick: (String) -> Unit
) {
    val dateFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale.forLanguageTag("es-ES")) }

    when (classesState) {
        is ClassesState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = ColorPrimaryAction)
            }
        }
        is ClassesState.Error -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ColorStatusCancelled)
                    Text(classesState.message, color = ColorTextSecondary, modifier = Modifier.padding(16.dp))
                }
            }
        }
        is ClassesState.Idle -> Box(Modifier.fillMaxSize())
        is ClassesState.Success -> {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 4.dp)
                            .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(percent = 50))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(percent = 50))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = selectedDate.format(dateFormatter).replaceFirstChar { it.uppercase() },
                            color = ColorTextPrimary, // Color de texto primario brillando en fondo oscuro
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // 1. Mostrar Horarios de "Centro Abierto" (Si existen para este día)
                val adjustedDayOfWeek = if (selectedDate.dayOfWeek.value == 7) 1 else selectedDate.dayOfWeek.value + 1
                val openHours = gymOperatingHoursState[adjustedDayOfWeek]?.ranges?.sortedBy { it.startTime } ?: emptyList()
                
                if (openHours.isNotEmpty()) {
                    openHours.forEach { range ->
                        item {
                            TimelineSection(
                                range = range,
                                classes = classesState.classes,
                                selectedDate = selectedDate,
                                onClassClick = onClassClick
                            )
                        }
                    }
                    
                    // Standalone classes (out of range)
                    val inRangeIds = classesState.classes.filter { gymClass ->
                        val classTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val classTime = gymClass.dateTime?.let { classTimeFormat.format(it) } ?: ""
                        openHours.any { range -> classTime >= range.startTime && classTime <= range.endTime }
                    }.map { it.id }
                    
                    val standaloneClasses = classesState.classes.filter { it.id !in inRangeIds }
                    if (standaloneClasses.isNotEmpty()) {
                        item {
                            StandaloneClassesSection(classes = standaloneClasses, onClassClick = onClassClick)
                        }
                    }
                } else {
                    if (classesState.classes.isEmpty()) {
                        item { EmptyScheduleCard() }
                    } else {
                        item {
                            StandaloneClassesSection(classes = classesState.classes, onClassClick = onClassClick)
                        }
                    }
                }
                
                item { Spacer(Modifier.height(40.dp)) }
            }
        }
    }
}

// MARK: - TIMELINE COMPONENTS (Android)
@Composable
fun TimelineSection(
    range: ScheduleViewModel.TimeRange,
    classes: List<GymClass>,
    selectedDate: LocalDate,
    onClassClick: (String) -> Unit
) {
    val classesInRange = classes.filter { gymClass ->
        val classTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val classTime = gymClass.dateTime?.let { classTimeFormat.format(it) } ?: ""
        classTime >= range.startTime && classTime <= range.endTime
    }
    
    val progress = remember(range, selectedDate) {
        if (selectedDate.isBefore(LocalDate.now())) return@remember 1f
        if (selectedDate.isAfter(LocalDate.now())) return@remember 0f
        
        val nowFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val nowStr = nowFormat.format(Date())
        
        if (nowStr < range.startTime) return@remember 0f
        if (nowStr > range.endTime) return@remember 1f
        
        fun toMinutes(time: String): Int {
            val parts = time.split(":")
            if (parts.size != 2) return 0
            return (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)
        }
        
        val startM = toMinutes(range.startTime)
        val endM = toMinutes(range.endTime)
        val curM = toMinutes(nowStr)
        
        if (endM <= startM) 1f else (curM - startM).toFloat() / (endM - startM).toFloat()
    }
    
    val primaryColor = MaterialTheme.colorScheme.primary
    val dimColor = Color.Gray.copy(alpha = 0.3f)
    val circleRadiusDp = 8.dp
    
    GlassCardSurface(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp)
                .drawBehind {
                    val circleRadiusDp = 8.dp
                    val circleRadius = circleRadiusDp.toPx()
                    val xOffset = 24.dp.toPx() // Same margin as iOS
                    val yStart = 28.dp.toPx() // Adjusted to center with pill
                    val yEnd = size.height - 28.dp.toPx()
                    
                    val distance = yEnd - yStart
                    val splitY = yStart + (distance * progress)
                    val strokeW = 4.dp.toPx()
                    
                    if (progress > 0f) {
                        drawLine(color = dimColor, start = androidx.compose.ui.geometry.Offset(xOffset, yStart), end = androidx.compose.ui.geometry.Offset(xOffset, splitY), strokeWidth = strokeW)
                    }
                    if (progress < 1f) {
                        drawLine(color = primaryColor, start = androidx.compose.ui.geometry.Offset(xOffset, splitY), end = androidx.compose.ui.geometry.Offset(xOffset, yEnd), strokeWidth = strokeW)
                    }
                    
                    val startColor = if (progress > 0f) Color.Gray.copy(alpha=0.5f) else primaryColor
                    drawCircle(color = Color.Black, radius = circleRadius, center = androidx.compose.ui.geometry.Offset(xOffset, yStart))
                    drawCircle(color = startColor, radius = circleRadius - 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(xOffset, yStart))
                    
                    val endColor = if (progress >= 1f) Color.Gray.copy(alpha=0.5f) else primaryColor
                    drawCircle(color = Color.Black, radius = circleRadius, center = androidx.compose.ui.geometry.Offset(xOffset, yEnd))
                    drawCircle(color = endColor, radius = circleRadius - 2.dp.toPx(), center = androidx.compose.ui.geometry.Offset(xOffset, yEnd))
                    
                     // Current time indicator
                    if (progress > 0f && progress < 1f) {
                        drawCircle(color = primaryColor, radius = 10.dp.toPx(), center = androidx.compose.ui.geometry.Offset(xOffset, splitY))
                        drawCircle(color = Color.White, radius = 10.dp.toPx(), center = androidx.compose.ui.geometry.Offset(xOffset, splitY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                    }
                }
        ) {
            Spacer(modifier = Modifier.width(48.dp))
            
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                // HEADER (Start)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color.White.copy(0.1f), shape = RoundedCornerShape(16.dp)) {
                        Text(range.startTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (progress > 0f) Color.White.copy(0.6f) else Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(color = primaryColor.copy(0.15f), shape = RoundedCornerShape(16.dp)) {
                         Text("APERTURA", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = primaryColor, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // CONTENT
                if (classesInRange.isEmpty()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Acceso Libre", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Espacio disponible para entrenar por tu cuenta. No hay clases guiadas en este horario.", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(0.6f))
                    }
                } else {
                    classesInRange.forEach { gymClass ->
                        ClassItemCardGlass(gymClass = gymClass, onClick = { onClassClick(gymClass.id) })
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // FOOTER (End)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color.White.copy(0.1f), shape = RoundedCornerShape(16.dp)) {
                        Text(range.endTime, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (progress >= 1f) Color.White.copy(0.6f) else Color.White, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(color = Color.White.copy(0.1f), shape = RoundedCornerShape(16.dp)) {
                         Text("CIERRE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White.copy(0.5f), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun StandaloneClassesSection(classes: List<GymClass>, onClassClick: (String) -> Unit) {
    Column {
        Box(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 12.dp)
                .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(percent = 50))
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(percent = 50))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Otras Clases",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ColorTextPrimary
            )
        }
        classes.forEach { gymClass ->
             ClassItemCardGlass(gymClass = gymClass, onClick = { onClassClick(gymClass.id) })
             Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ClassItemCardGlass(
    gymClass: GymClass,
    onClick: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val classDate = gymClass.dateTime ?: Date()
    val timeString = timeFormat.format(classDate)

    val now = Date()
    val endTime = Date(classDate.time + (gymClass.durationMinutes * 60 * 1000).toLong())
    val isPast = now.after(endTime)
    val isOngoing = now.after(classDate) && now.before(endTime)
    val isCancelled = try { gymClass.isCancelled } catch (e: Exception) { false }

    // Logic for Color
    val classSpecificColor = gymClass.hexColor.toColorSafe()

    val isCompetition = gymClass.classType == "COMPETITION"
    val compColor = Color(0xFFFFD700)
    val compBrush = Brush.linearGradient(listOf(compColor.copy(alpha=0.15f), Color.Transparent))
    
    val statusColor = when {
        isCancelled -> ColorStatusCancelled
        isPast -> ColorStatusPast
        isOngoing -> ColorStatusOngoing
        isCompetition -> compColor
        else -> classSpecificColor // Use custom color from DB
    }

    val alphaAnim = remember { Animatable(1f) }
    LaunchedEffect(isOngoing) {
        if (isOngoing) {
            alphaAnim.animateTo(0.5f, infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse))
        }
    }

    GlassCardSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .alpha(if (isPast && !isOngoing) 0.6f else 1f)
            .let { if (isCompetition) it.border(1.dp, compColor.copy(alpha=0.5f), RoundedCornerShape(16.dp)) else it }
            .background(if (isCompetition) compBrush else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Sidebar Color
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(statusColor.copy(alpha = if (isOngoing) alphaAnim.value else 1f))
            )

            // Content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeString,
                        color = if (isPast || isCancelled) ColorTextPrimary else statusColor,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (isOngoing) {
                        Text("EN VIVO", style = MaterialTheme.typography.labelSmall, color = ColorStatusOngoing, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(ColorBorder)
                )

                Spacer(modifier = Modifier.width(20.dp))

                // Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = gymClass.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isCompetition) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = compColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = compColor, modifier = Modifier.size(10.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text(
                                        text = "EVENTO",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = compColor,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        } else if (gymClass.isOpenGym == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "LIBRE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    if (gymClass.isOpenGym != true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = ColorTextSecondary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = gymClass.coachName.ifBlank { "Staff" },
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorTextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = ColorTextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        val isFull = gymClass.enrolledUserIds.size >= gymClass.maxCapacity
                        val currentUserSession by UserSession.currentUser.collectAsState()
                        val myWaitlistPos = if (currentUserSession != null) gymClass.waitingList.indexOf(currentUserSession!!.id) else -1

                        if (myWaitlistPos >= 0) {
                            Text(
                                text = "EN ESPERA (#${myWaitlistPos + 1})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFA000), // Naranja
                                fontWeight = FontWeight.Bold
                            )
                        } else if (isFull) {
                            Text(
                                text = "LLENA (${gymClass.enrolledUserIds.size}/${gymClass.maxCapacity})",
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorStatusCancelled,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "${gymClass.enrolledUserIds.size}/${gymClass.maxCapacity}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorTextSecondary
                            )
                        }
                    }
                }

                // Status Icons
                if (gymClass.attendanceTaken) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Asistió",
                        tint = ColorSuccess,
                        modifier = Modifier.size(24.dp)
                    )
                } else if (isCancelled) {
                    Text("CANCELADA", style = MaterialTheme.typography.labelSmall, color = ColorStatusCancelled, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DateSelector(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    user: User
) {
    val dates = remember { generateDates() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val index = dates.indexOf(selectedDate)
        if (index > 0) listState.scrollToItem(index)
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(dates) { date ->
            val isSelected = date == selectedDate
            val isEnabled = isDateEnabled(date, user)

                val containerColor = when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isEnabled -> ColorGlassSurface
                    else -> ColorGlassSurface.copy(alpha = 0.3f)
                }

            val textColor = if (isSelected) Color.White else ColorTextSecondary.copy(alpha = if(isEnabled) 1f else 0.5f)
            val border = if (isSelected) null else BorderStroke(1.dp, ColorBorder)

            Card(
                modifier = Modifier
                    .width(64.dp)
                    .height(80.dp)
                    .clickable(enabled = isEnabled) {
                        onDateSelected(date)
                        coroutineScope.launch { listState.animateScrollToItem(dates.indexOf(date)) }
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                border = border
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es-ES")).uppercase().take(3),
                        color = textColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${date.dayOfMonth}",
                        color = textColor,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyScheduleCard() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, ColorBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.EventBusy, contentDescription = null, tint = ColorTextSecondary, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Sin clases", style = MaterialTheme.typography.titleMedium, color = ColorTextPrimary, fontWeight = FontWeight.Bold)
            Text("No hay programación para este día.", style = MaterialTheme.typography.bodyMedium, color = ColorTextSecondary)
        }
    }
}

@Composable
fun AccessBlockedView(onNavigate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LockClock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Acceso Restringido",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = ColorTextPrimary
        )
        Text(
            "Tus créditos han vencido o se han agotado.\nRenueva tu plan para reservar.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = ColorTextSecondary,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNavigate,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp)
        ) {
            Text("Solicitar Créditos", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

// --- LOGIC HELPERS ---

private fun generateDates(): List<LocalDate> {
    val today = LocalDate.now()
    return (0..30).map { today.plusDays(it.toLong()) }
}

private fun isDateEnabled(date: LocalDate, user: User): Boolean {
    if (user.isAdmin) return true
    if (user.credits <= 0) return false
    val validUntil = user.creditValidUntil
    if (validUntil != null) {
        val expiration = validUntil.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        return !date.isAfter(expiration)
    }
    return true
}