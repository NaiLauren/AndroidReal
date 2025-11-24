package com.aquiles.crosschapp.presentation.home

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aquiles.crosschapp.data.model.GymClass
import com.aquiles.crosschapp.data.model.User
import com.aquiles.crosschapp.presentation.viewmodel.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*

// --- HELPER EXTENSION ---
fun String.toColorSafe(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color(0xFFFC5200) // Fallback Naranja
    }
}

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.75f)
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
                        title = { Text("Horarios", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
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
    selectedDate: LocalDate,
    onClassClick: (String) -> Unit
) {
    val dateFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es", "ES")) }

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
                    Text(
                        text = selectedDate.format(dateFormatter).replaceFirstChar { it.uppercase() },
                        color = ColorTextSecondary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )
                }

                if (classesState.classes.isEmpty()) {
                    item { EmptyScheduleCard() }
                } else {
                    items(classesState.classes, key = { it.id }) { gymClass ->
                        ClassItemCardGlass(gymClass = gymClass, onClick = { onClassClick(gymClass.id) })
                    }
                }
                item { Spacer(Modifier.height(40.dp)) }
            }
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

    val statusColor = when {
        isCancelled -> ColorStatusCancelled
        isPast -> ColorStatusPast
        isOngoing -> ColorStatusOngoing
        else -> classSpecificColor // Use custom color from DB
    }

    val alphaAnim = remember { Animatable(1f) }
    LaunchedEffect(isOngoing) {
        if (isOngoing) {
            alphaAnim.animateTo(0.5f, infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .alpha(if (isPast && !isOngoing) 0.6f else 1f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ColorBorder),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface),
        elevation = CardDefaults.cardElevation(0.dp)
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
                    Text(
                        text = gymClass.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))

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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = ColorTextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${gymClass.enrolledUserIds.size}/${gymClass.maxCapacity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (gymClass.enrolledUserIds.size >= gymClass.maxCapacity) ColorStatusCancelled else ColorTextSecondary
                        )
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
                isSelected -> ColorPrimaryAction
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
                        text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es", "ES")).uppercase().take(3),
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
            tint = ColorPrimaryAction,
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
            colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction),
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