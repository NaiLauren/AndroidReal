package com.aquiles.crosschapp.presentation.home

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import com.aquiles.crosschapp.R
import com.aquiles.crosschapp.data.model.GymClass
import com.aquiles.crosschapp.data.model.User
import com.aquiles.crosschapp.data.model.Wod
import com.aquiles.crosschapp.presentation.viewmodel.*
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.75f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
private val ColorError = Color(0xFFEF5350)
private val ColorSuccess = Color(0xFF4CAF50)

@Composable
fun ClassDetailsScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    classId: String,
    scheduleViewModel: ScheduleViewModel = viewModel(),
    adminViewModel: AdminViewModel = viewModel()
) {
    val currentUser by UserSession.currentUser.collectAsState()

    LaunchedEffect(key1 = classId) {
        scheduleViewModel.loadClassDetails(classId)
    }

    // --- UI STRUCTURE ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        currentUser?.let { user ->
            ClassDetailsContent(
                navController = navController,
                scheduleViewModel = scheduleViewModel,
                adminViewModel = adminViewModel,
                currentUser = user
            )
        } ?: Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ColorPrimaryAction)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClassDetailsContent(
    navController: NavController,
    scheduleViewModel: ScheduleViewModel,
    adminViewModel: AdminViewModel,
    currentUser: User
) {
    val context = LocalContext.current
    val detailsState by scheduleViewModel.classDetailsState.collectAsState()
    val bookingState by scheduleViewModel.bookingState.collectAsState()
    val attendeeListState by adminViewModel.attendeeListState.collectAsState()
    var attendedUserIds by remember { mutableStateOf(setOf<String>()) }
    val adminOperationState by adminViewModel.classOperationState.collectAsState()

    // --- NUEVO ESTADO: Verificación independiente de asistencia ---
    var hasLocalAttendance by remember { mutableStateOf(false) }
    // -----------------------------------------------------------

    // Estado del CheckIn local
    val checkInState by scheduleViewModel.checkInResult.collectAsState()

    // Cuando volvemos del escáner, forzamos una re-verificación local
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (detailsState is ClassDetailsState.Success) {
            val classId = (detailsState as ClassDetailsState.Success).gymClass.id
            // CONSULTA CON GYM_ID
            checkAttendanceInFirestore(classId, currentUser.id, currentUser.gym_id) { exists ->
                hasLocalAttendance = exists
            }
        }
    }

    // Feedback
    LaunchedEffect(bookingState) {
        if (bookingState is BookingState.Success) {
            Toast.makeText(context, (bookingState as BookingState.Success).message, Toast.LENGTH_SHORT).show()
            scheduleViewModel.resetBookingState()
        } else if (bookingState is BookingState.Error) {
            Toast.makeText(context, (bookingState as BookingState.Error).message, Toast.LENGTH_LONG).show()
            scheduleViewModel.resetBookingState()
        }
    }

    // Verificación inicial cuando carga la clase
    LaunchedEffect(detailsState) {
        if (detailsState is ClassDetailsState.Success) {
            val classId = (detailsState as ClassDetailsState.Success).gymClass.id
            // CONSULTA CON GYM_ID
            checkAttendanceInFirestore(classId, currentUser.id, currentUser.gym_id) { exists ->
                hasLocalAttendance = exists
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Detalles", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ColorTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            if (detailsState is ClassDetailsState.Success) {
                val gymClass = (detailsState as ClassDetailsState.Success).gymClass
                val isLoading = bookingState is BookingState.Loading || adminOperationState is ClassOperationState.Loading

                GlassBottomBar {
                    if (currentUser.isAdmin) {
                        AdminActionButtons(
                            gymClass = gymClass,
                            isLoading = isLoading,
                            onSaveAttendance = { adminViewModel.saveAttendance(gymClass.id, attendedUserIds.toList()) }
                        )
                    } else {
                        StudentActionButtons(
                            gymClass = gymClass,
                            currentUser = currentUser,
                            isLoading = isLoading,
                            onBook = { scheduleViewModel.bookClass(gymClass.id, currentUser) },
                            onCancel = { scheduleViewModel.cancelBooking(gymClass.id, currentUser) }
                        )
                    }
                }
            }
        }
    ) { localPaddingValues ->
        when (val state = detailsState) {
            ClassDetailsState.Idle, ClassDetailsState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ColorPrimaryAction) }
            }
            is ClassDetailsState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.message, color = ColorError) }
            }
            is ClassDetailsState.Success -> {
                val gymClass = state.gymClass
                val wod = state.wod
                val hasAccessToList = currentUser.hasValidCredits || currentUser.isAdmin

                // LÓGICA DE VISUALIZACIÓN
                val isEnrolled = gymClass.enrolledUserIds.contains(currentUser.id)

                // COMBINAMOS: El dato del documento de clase O nuestra verificación local en attendance_history
                val alreadyCheckedIn = (gymClass.checkedInUserIds?.contains(currentUser.id) == true) || hasLocalAttendance

                val now = Date()
                val classTime = gymClass.dateTime ?: now
                val diffMinutes = (now.time - classTime.time) / (60 * 1000)
                val isCheckInTime = diffMinutes in -30..30

                LaunchedEffect(gymClass.enrolledUserIds) {
                    adminViewModel.loadAttendeesDetails(gymClass.enrolledUserIds)
                    attendedUserIds = if (gymClass.attendanceTaken) gymClass.attendedUserIds.toSet() else gymClass.enrolledUserIds.toSet()
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = localPaddingValues.calculateTopPadding() + 16.dp,
                        bottom = localPaddingValues.calculateBottomPadding() + 100.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. HEADER & STATS
                    item { ClassHeaderSection(gymClass) }

                    // --- BOTÓN DAR PRESENTE ---
                    if (isEnrolled && isCheckInTime && !currentUser.isAdmin) {
                        item {
                            if (alreadyCheckedIn) {
                                // YA DIO PRESENTE (Verde)
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = ColorSuccess.copy(alpha = 0.2f)),
                                    border = BorderStroke(1.dp, ColorSuccess),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, null, tint = ColorSuccess)
                                        Spacer(Modifier.width(8.dp))
                                        Text("¡Presente Registrado!", color = ColorSuccess, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                // BOTÓN PARA ESCANEAR (Naranja)
                                Button(
                                    onClick = {
                                        val intent = Intent(context, QrScannerActivity::class.java)
                                        intent.putExtra("CLASS_ID_PARAM", gymClass.id)
                                        scannerLauncher.launch(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = ButtonDefaults.buttonElevation(8.dp)
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, null, tint = Color.White)
                                    Spacer(Modifier.width(12.dp))
                                    Text("DAR PRESENTE AHORA", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    // ----------------------------------

                    // 2. WOD / DESCRIPCIÓN
                    item { ClassDescriptionSection(gymClass, wod) }

                    // 3. ASISTENTES
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = ColorBorder)
                        Text("Asistentes (${gymClass.enrolledUserIds.size})", style = MaterialTheme.typography.titleMedium, color = ColorTextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                    }

                    if (hasAccessToList) {
                        when (val attendeesState = attendeeListState) {
                            is AttendeeListState.Loading -> { item { Box(Modifier.fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(color = ColorPrimaryAction) } } }
                            is AttendeeListState.Error -> { item { Text("Error: ${attendeesState.message}", color = ColorError) } }
                            is AttendeeListState.Success -> {
                                if (attendeesState.attendees.isEmpty()) {
                                    item {
                                        Text("Nadie se ha inscrito aún.", style = MaterialTheme.typography.bodyMedium, color = ColorTextSecondary, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                    }
                                } else {
                                    item {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = ColorGlassSurface),
                                            border = BorderStroke(1.dp, ColorBorder),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(Modifier.padding(16.dp)) {
                                                attendeesState.attendees.forEachIndexed { index, user ->

                                                    // Chequeo visual en la lista también
                                                    val hasCheckedIn = (gymClass.checkedInUserIds?.contains(user.id) == true) ||
                                                            (user.id == currentUser.id && hasLocalAttendance)

                                                    val isClassInThePast = gymClass.dateTime?.before(Date()) == true
                                                    val isAttendanceMode = currentUser.isAdmin && isClassInThePast && !gymClass.attendanceTaken

                                                    AttendeeItemRow(
                                                        attendee = user,
                                                        isAttendanceMode = isAttendanceMode,
                                                        isChecked = if (currentUser.isAdmin) attendedUserIds.contains(user.id) else hasCheckedIn,
                                                        onCheckChanged = { isChecked ->
                                                            attendedUserIds = if (isChecked) attendedUserIds + user.id else attendedUserIds - user.id
                                                        },
                                                        showCheckIcon = hasCheckedIn
                                                    )
                                                    if (index < attendeesState.attendees.size - 1) {
                                                        HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        item { AccessDeniedCard { navController.navigate("request_credits_screen") } }
                    }
                }
            }
        }
    }
}

// --- FUNCIÓN AUXILIAR CORREGIDA CON GYM_ID ---
private fun checkAttendanceInFirestore(classId: String, userId: String, gymId: String, onResult: (Boolean) -> Unit) {
    FirebaseFirestore.getInstance().collection("attendance_history")
        .whereEqualTo("gym_id", gymId) // <--- ESTO ES LO IMPORTANTE
        .whereEqualTo("userId", userId)
        .whereEqualTo("classId", classId)
        .limit(1)
        .get()
        .addOnSuccessListener { documents ->
            onResult(!documents.isEmpty)
        }
        .addOnFailureListener {
            onResult(false)
        }
}


// --- COMPONENTES VISUALES ---

@Composable
fun ClassHeaderSection(gymClass: GymClass) {
    val dateFormatter = SimpleDateFormat("EEEE dd, MMMM", Locale("es", "ES"))
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = gymClass.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = ColorTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = gymClass.dateTime?.let { "${dateFormatter.format(it).uppercase()} • ${timeFormatter.format(it)}" } ?: "",
            style = MaterialTheme.typography.titleMedium,
            color = ColorPrimaryAction,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GlassStatCard(
                icon = Icons.Default.Person,
                label = "Coach",
                value = gymClass.coachName.ifBlank { "Staff" },
                modifier = Modifier.weight(1f)
            )
            GlassStatCard(
                icon = Icons.Default.Group,
                label = "Cupos",
                value = "${gymClass.enrolledUserIds.size}/${gymClass.maxCapacity}",
                modifier = Modifier.weight(1f),
                isWarning = gymClass.enrolledUserIds.size >= gymClass.maxCapacity
            )
        }
    }
}

@Composable
fun GlassStatCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier = Modifier, isWarning: Boolean = false) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isWarning) ColorError else ColorBorder),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = if(isWarning) ColorError else ColorTextSecondary)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = ColorTextSecondary)
        }
    }
}

@Composable
fun ClassDescriptionSection(gymClass: GymClass, wod: Wod?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ColorBorder),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface)
    ) {
        Column(Modifier.padding(20.dp)) {
            if (wod != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FitnessCenter, null, tint = ColorPrimaryAction, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("WORKOUT OF THE DAY", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ColorTextSecondary)
                }
                Spacer(Modifier.height(12.dp))
                Text(wod.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
                if (!wod.scoreType.isNullOrBlank()) {
                    Text("Modalidad: ${wod.scoreType}", style = MaterialTheme.typography.bodyMedium, color = ColorPrimaryAction)
                }
                Spacer(Modifier.height(8.dp))
                Text(wod.description, style = MaterialTheme.typography.bodyLarge, color = ColorTextSecondary, lineHeight = 24.sp)
            } else {
                Text("DESCRIPCIÓN", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = ColorTextSecondary)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = gymClass.description.ifBlank { "Sin descripción adicional." },
                    style = MaterialTheme.typography.bodyLarge,
                    color = ColorTextPrimary
                )
            }
        }
    }
}

@Composable
fun AttendeeItemRow(
    attendee: User,
    isAttendanceMode: Boolean,
    isChecked: Boolean,
    onCheckChanged: (Boolean) -> Unit,
    showCheckIcon: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SubcomposeAsyncImage(
            model = attendee.profileImageUrl,
            contentDescription = null,
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Color.Gray),
            contentScale = ContentScale.Crop,
            loading = { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp)) } },
            error = { Icon(Icons.Default.Person, null, tint = Color.White) }
        )
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(attendee.fullName, style = MaterialTheme.typography.bodyMedium, color = ColorTextPrimary)
            // Aquí podríamos mostrar los iconos de logros en el futuro
        }

        // Si dio presente con QR, mostramos el check verde
        if (showCheckIcon) {
            Icon(Icons.Default.CheckCircle, null, tint = ColorSuccess, modifier = Modifier.size(20.dp))
        }

        // Modo Admin: Checkbox manual
        if (isAttendanceMode) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckChanged,
                colors = CheckboxDefaults.colors(checkedColor = ColorPrimaryAction, uncheckedColor = ColorTextSecondary, checkmarkColor = Color.White)
            )
        }
    }
}

// --- BARRAS DE ACCIÓN ---

@Composable
fun GlassBottomBar(content: @Composable RowScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF121212).copy(alpha = 0.9f))
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
fun StudentActionButtons(
    gymClass: GymClass,
    currentUser: User,
    isLoading: Boolean,
    onBook: () -> Unit,
    onCancel: () -> Unit
) {
    val isEnrolled = gymClass.enrolledUserIds.contains(currentUser.id)
    var buttonText by remember { mutableStateOf("") }
    var isEnabled by remember { mutableStateOf(true) }

    // Logic check
    val now = Date()
    val classTime = gymClass.dateTime ?: now
    val thirtyMinutesBefore = Date(classTime.time - (30 * 60 * 1000))

    if (isEnrolled) {
        buttonText = "CANCELAR RESERVA"
        isEnabled = now.before(thirtyMinutesBefore) || currentUser.isAdmin
    } else {
        val isFull = gymClass.enrolledUserIds.size >= gymClass.maxCapacity
        buttonText = if (isFull) "CLASE LLENA" else "RESERVAR LUGAR"
        isEnabled = !isFull && classTime.after(now) && (currentUser.hasValidCredits || currentUser.isAdmin)
    }

    Button(
        onClick = { if (isEnrolled) onCancel() else onBook() },
        modifier = Modifier.fillMaxWidth().height(50.dp),
        enabled = isEnabled && !isLoading,
        colors = ButtonDefaults.buttonColors(containerColor = if (isEnrolled) ColorError else ColorPrimaryAction),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
        else Text(buttonText, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AdminActionButtons(gymClass: GymClass, isLoading: Boolean, onSaveAttendance: () -> Unit) {
    val isPast = gymClass.dateTime?.before(Date()) == true

    if (isPast && !gymClass.attendanceTaken && gymClass.enrolledUserIds.isNotEmpty()) {
        Button(
            onClick = onSaveAttendance,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = ColorSuccess),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text("CONFIRMAR ASISTENCIA", fontWeight = FontWeight.Bold)
        }
    } else if (gymClass.attendanceTaken) {
        Text(
            "Asistencia Registrada",
            color = ColorSuccess,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        Text(
            "Gestión habilitada al finalizar la clase",
            color = ColorTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AccessDeniedCard(onRequestCredits: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface),
        border = BorderStroke(1.dp, ColorError.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(40.dp), tint = ColorError)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Lista Oculta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
            Text("Necesitas créditos activos para ver quién va.", textAlign = TextAlign.Center, color = ColorTextSecondary, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRequestCredits,
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Recargar Créditos", color = Color.White)
            }
        }
    }
}