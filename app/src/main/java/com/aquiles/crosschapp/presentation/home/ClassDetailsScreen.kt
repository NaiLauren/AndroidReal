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
import com.aquiles.crosschapp.presentation.components.FeedbackDialog
import com.aquiles.crosschapp.presentation.components.FeedbackType
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.layout.height
import com.aquiles.crosschapp.presentation.common.AppBackground

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.45f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color(0xFFAAAAAA)
private val ColorSuccess = Color(0xFF4CAF50)
private val ColorError = Color(0xFFCF6679)
private val ColorBorder = Color.White.copy(alpha = 0.15f)

@Composable
fun ClassDetailsScreen(
    navController: NavController,
    classId: String,
    scheduleViewModel: ScheduleViewModel = viewModel(),
    adminViewModel: AdminViewModel = viewModel(),
    currentUser: User
) {
    LaunchedEffect(classId) {
        scheduleViewModel.loadClassDetails(classId)
    }

    AppBackground {
        ClassDetailsContent(
            navController = navController,
            scheduleViewModel = scheduleViewModel,
            adminViewModel = adminViewModel,
            currentUser = currentUser
        )
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
    val adminOperationState by adminViewModel.classOperationState.collectAsState()

    // Lista de IDs seleccionados manualmente o detectados
    var attendedUserIds by remember { mutableStateOf(setOf<String>()) }

    // --- NUEVO: Lista de IDs que REALMENTE tienen registro en history (para el Admin) ---
    var verifiedIdsFromHistory by remember { mutableStateOf(setOf<String>()) }
    // -----------------------------------------------------------------------------------

    // Verificación local para el alumno (tuerca vieja pero funcional)
    var hasLocalAttendance by remember { mutableStateOf(false) }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        if (detailsState is ClassDetailsState.Success) {
            val classId = (detailsState as ClassDetailsState.Success).gymClass.id
            checkAttendanceInFirestore(classId, currentUser.id, currentUser.gym_id) { exists ->
                hasLocalAttendance = exists
            }
            scheduleViewModel.loadClassDetails(classId)
        }
    }

    // --- FEEDBACK DIALOG STATES ---
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var feedbackType by remember { mutableStateOf(FeedbackType.INFO) }
    var feedbackTitle by remember { mutableStateOf("") }
    var feedbackMessage by remember { mutableStateOf("") }

    // --- SCORE UPLOAD DIALOG STATES ---
    var showScoreDialog by remember { mutableStateOf(false) }
    var selectedUserForScore by remember { mutableStateOf<User?>(null) }
    var scoreInput by remember { mutableStateOf("") }

    // --- HANDLERS ---
    LaunchedEffect(bookingState) {
        if (bookingState is BookingState.Success) {
            feedbackType = FeedbackType.SUCCESS
            feedbackTitle = "¡Reserva Exitosa!"
            feedbackMessage = (bookingState as BookingState.Success).message
            showFeedbackDialog = true
            scheduleViewModel.resetBookingState()
        } else if (bookingState is BookingState.Error) {
            feedbackType = FeedbackType.ERROR
            feedbackTitle = "Error al Reservar"
            feedbackMessage = (bookingState as BookingState.Error).message
            showFeedbackDialog = true
            scheduleViewModel.resetBookingState()
        }
    }

    LaunchedEffect(adminOperationState) {
        if (adminOperationState is ClassOperationState.Success) {
            feedbackType = FeedbackType.SUCCESS
            feedbackTitle = "Operación Exitosa"
            feedbackMessage = (adminOperationState as ClassOperationState.Success).message
            showFeedbackDialog = true
            adminViewModel.resetClassOperationState()
        } else if (adminOperationState is ClassOperationState.Error) {
            feedbackType = FeedbackType.ERROR
            feedbackTitle = "Error de Admin"
            feedbackMessage = (adminOperationState as ClassOperationState.Error).message
            showFeedbackDialog = true
            adminViewModel.resetClassOperationState()
        }
    }

    // --- DIALOG COMPONENT ---
    FeedbackDialog(
        show = showFeedbackDialog,
        type = feedbackType,
        title = feedbackTitle,
        message = feedbackMessage,
        onDismiss = { showFeedbackDialog = false }
    )

    LaunchedEffect(detailsState) {
        if (detailsState is ClassDetailsState.Success) {
            val gymClass = (detailsState as ClassDetailsState.Success).gymClass

            // 1. Chequeo para el Alumno (Soy yo?)
            checkAttendanceInFirestore(gymClass.id, currentUser.id, currentUser.gym_id) { exists ->
                hasLocalAttendance = exists
            }

            // 2. NUEVO: Si soy Admin, busco la verdad absoluta en el historial
            // Esto soluciona el problema de que el documento de Class no esté actualizado
            if (currentUser.isAdmin) {
                fetchRealTimeAttendance(gymClass.id, currentUser.gym_id) { realIds ->
                    verifiedIdsFromHistory = realIds
                }
            }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.height(72.dp),
                title = { Text("Detalles", fontWeight = FontWeight.Bold, color =
                    ColorTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ColorTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f))
            )
        },
        bottomBar = {
            if (detailsState is ClassDetailsState.Success) {
                val gymClass = (detailsState as ClassDetailsState.Success).gymClass
                val isLoading = bookingState is BookingState.Loading || adminOperationState is ClassOperationState.Loading

                GlassBottomBar {
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
                val isEnrolled = gymClass.enrolledUserIds.contains(currentUser.id)

                // Lógica combinada para saber si YO ya di el presente
                val alreadyCheckedIn = (gymClass.checkedInUserIds?.contains(currentUser.id) == true) || hasLocalAttendance

                val now = Date()
                val classTime = gymClass.dateTime ?: now
                val diffMinutes = (now.time - classTime.time) / (60 * 1000)
                val isCheckInTime = diffMinutes in -30..30

                LaunchedEffect(gymClass.enrolledUserIds, gymClass.waitingList) {
                    val allIds = gymClass.enrolledUserIds + gymClass.waitingList
                    adminViewModel.loadAttendeesDetails(allIds)
                }

                // Sincronización de Checkboxes para el Admin
                LaunchedEffect(gymClass.checkedInUserIds, gymClass.attendedUserIds, verifiedIdsFromHistory) {
                    val initialSet = mutableSetOf<String>()

                    // Si ya se cerró la asistencia antes
                    if (gymClass.attendanceTaken) {
                        initialSet.addAll(gymClass.attendedUserIds)
                    }

                    // Sumamos los que están en la lista oficial de la clase
                    gymClass.checkedInUserIds?.let { initialSet.addAll(it) }

                    // SUMAMOS LOS RECUPERADOS DEL HISTORIAL (El arreglo "mágico")
                    initialSet.addAll(verifiedIdsFromHistory)

                    attendedUserIds = initialSet
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
                    item { ClassHeaderSection(gymClass) }

                    // BOTÓN ALUMNO (O ADMIN SI ENTRENA)
                    if (isEnrolled && isCheckInTime) {
                        item {
                            if (alreadyCheckedIn) {
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

                    item { ClassDescriptionSection(gymClass, wod) }

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
                                        val enrolledUsers = attendeesState.attendees.filter { gymClass.enrolledUserIds.contains(it.id) }
                                        val waitingUsers = attendeesState.attendees.filter { gymClass.waitingList.contains(it.id) }

                                        if (enrolledUsers.isNotEmpty()) {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = ColorGlassSurface),
                                                border = BorderStroke(1.dp, ColorBorder),
                                                shape = RoundedCornerShape(16.dp),
                                                modifier = Modifier.fillMaxWidth().padding(bottom = if (waitingUsers.isNotEmpty()) 16.dp else 0.dp)
                                            ) {
                                                Column(Modifier.padding(16.dp)) {
                                                    enrolledUsers.forEachIndexed { index, user ->

                                                        val inClassList = gymClass.checkedInUserIds?.contains(user.id) == true
                                                        val inHistoryList = verifiedIdsFromHistory.contains(user.id)
                                                        val isMeAndLocal = (user.id == currentUser.id && hasLocalAttendance)
                                                        val isPresent = inClassList || inHistoryList || isMeAndLocal
                                                        val isCheckedForAdmin = attendedUserIds.contains(user.id) || isPresent

                                                        AttendeeItemRow(
                                                            attendee = user,
                                                            isAttendanceMode = currentUser.isAdmin,
                                                            isChecked = if (currentUser.isAdmin) isCheckedForAdmin else isPresent,
                                                            onCheckChanged = { shouldCheck ->
                                                                attendedUserIds = if (shouldCheck) attendedUserIds + user.id else attendedUserIds - user.id
                                                            },
                                                            showCheckIcon = isPresent,
                                                            showQrLabel = inClassList || inHistoryList,
                                                            isCompetitionMode = gymClass.classType == "COMPETITION",
                                                            onUploadScore = {
                                                                selectedUserForScore = user
                                                                scoreInput = ""
                                                                showScoreDialog = true
                                                            }
                                                        )
                                                        if (index < enrolledUsers.size - 1) {
                                                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (waitingUsers.isNotEmpty()) {
                                            Text("En Espera (${waitingUsers.size})", style = MaterialTheme.typography.titleMedium, color = ColorPrimaryAction, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.height(12.dp))
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = ColorGlassSurface),
                                                border = BorderStroke(1.dp, ColorPrimaryAction.copy(alpha = 0.5f)),
                                                shape = RoundedCornerShape(16.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(Modifier.padding(16.dp)) {
                                                    waitingUsers.forEachIndexed { index, user ->
                                                        AttendeeItemRow(
                                                            attendee = user,
                                                            isAttendanceMode = false,
                                                            isChecked = false,
                                                            onCheckChanged = {},
                                                            showCheckIcon = false,
                                                            showQrLabel = false,
                                                            isCompetitionMode = false,
                                                            onUploadScore = {}
                                                        )
                                                        if (index < waitingUsers.size - 1) {
                                                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 12.dp))
                                                        }
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

                    if (currentUser.isAdmin) {
                        item {
                            Spacer(Modifier.height(16.dp))
                            val isLoading = bookingState is BookingState.Loading || adminOperationState is ClassOperationState.Loading
                            AdminActionButtons(
                                gymClass = gymClass,
                                isLoading = isLoading,
                                onSaveAttendance = { adminViewModel.saveAttendance(gymClass.id, attendedUserIds.toList()) }
                            )
                        }
                    }
                }

                // DIALOGO DE CARGA DE SCORE
                if (showScoreDialog && selectedUserForScore != null) {
                    AlertDialog(
                        onDismissRequest = { showScoreDialog = false },
                        title = { Text("Cargar Resultado", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                        text = {
                            Column {
                                Text("Atleta: ${selectedUserForScore?.fullName}", color = ColorTextSecondary)
                                Spacer(Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = scoreInput,
                                    onValueChange = { scoreInput = it },
                                    label = { Text("Resultado / Marca") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (scoreInput.isNotBlank()) {
                                    adminViewModel.saveWodResult(
                                        wodId = gymClass.wodId ?: gymClass.competitionId ?: gymClass.id,
                                        userId = selectedUserForScore!!.id,
                                        score = scoreInput,
                                        scoreType = gymClass.wodScoreType ?: "Para Tiempo",
                                        isRx = true,
                                        notes = "Cargado por Admin",
                                        gymId = gymClass.gym_id ?: currentUser.gym_id
                                    )
                                    showScoreDialog = false
                                    Toast.makeText(context, "Resultado guardado", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Text("Guardar", color = ColorPrimaryAction)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showScoreDialog = false }) {
                                Text("Cancelar", color = ColorTextSecondary)
                            }
                        },
                        containerColor = ColorGlassSurface
                    )
                }
            }
        }
    }
}

// --- FUNCIÓN INDIVIDUAL (Para alumno) ---
private fun checkAttendanceInFirestore(classId: String, userId: String, gymId: String, onResult: (Boolean) -> Unit) {
    FirebaseFirestore.getInstance().collection("attendance_history")
        .whereEqualTo("gym_id", gymId)
        .whereEqualTo("userId", userId)
        .whereEqualTo("classId", classId)
        .limit(1)
        .get()
        .addOnSuccessListener { documents ->
            onResult(!documents.isEmpty)
        }
        .addOnFailureListener { onResult(false) }
}

// --- NUEVA FUNCIÓN GRUPAL (Para Admin) ---
// Busca TODAS las asistencias de esta clase para mostrarlas aunque el documento de Class esté desactualizado
private fun fetchRealTimeAttendance(classId: String, gymId: String, onResult: (Set<String>) -> Unit) {
    FirebaseFirestore.getInstance().collection("attendance_history")
        .whereEqualTo("gym_id", gymId)
        .whereEqualTo("classId", classId)
        .get()
        .addOnSuccessListener { documents ->
            val ids = documents.mapNotNull { it.getString("userId") }.toSet()
            onResult(ids)
        }
        .addOnFailureListener {
            onResult(emptySet())
        }
}

@Composable
fun ClassHeaderSection(gymClass: GymClass) {
    val dateFormatter = SimpleDateFormat("EEEE dd, MMMM", Locale.forLanguageTag("es-ES"))
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
            if (gymClass.isOpenGym != true) {
                GlassStatCard(
                    icon = Icons.Default.Person,
                    label = "Coach",
                    value = gymClass.coachName.ifBlank { "Staff" },
                    modifier = Modifier.weight(1f)
                )
            }
            GlassStatCard(
                icon = Icons.Default.Group,
                label = if (gymClass.enrolledUserIds.size >= gymClass.maxCapacity) "Llena/Espera" else "Cupos",
                value = if (gymClass.enrolledUserIds.size >= gymClass.maxCapacity) "${gymClass.maxCapacity} (+${gymClass.waitingList.size})" else "${gymClass.enrolledUserIds.size}/${gymClass.maxCapacity}",
                modifier = Modifier.weight(1f),
                isWarning = gymClass.enrolledUserIds.size >= gymClass.maxCapacity
            )
        }
    }
}

@Composable
fun GlassStatCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier = Modifier, isWarning: Boolean = false) {
    com.aquiles.crosschapp.presentation.components.GlassCard(
        modifier = modifier
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
    showCheckIcon: Boolean = false,
    showQrLabel: Boolean = false,
    isCompetitionMode: Boolean = false,
    onUploadScore: () -> Unit = {}
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(attendee.fullName, style = MaterialTheme.typography.bodyMedium, color = ColorTextPrimary)
                if (showQrLabel) {
                    Spacer(Modifier.width(8.dp))
                    Text("(QR)", style = MaterialTheme.typography.labelSmall, color = ColorSuccess, fontWeight = FontWeight.Bold)
                }
            }
            
            // Gamification Level Badge
            val level = attendee.level ?: "Novato"
            val badgeColor = getLevelColor(level)
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star, 
                    contentDescription = null, 
                    tint = badgeColor, 
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = level.uppercase(), 
                    style = MaterialTheme.typography.labelSmall, 
                    fontWeight = FontWeight.Bold, 
                    color = badgeColor
                )
            }
        }

        if (showCheckIcon && !isCompetitionMode) {
            Icon(Icons.Default.CheckCircle, null, tint = ColorSuccess, modifier = Modifier.size(20.dp))
        }

        if (isAttendanceMode) {
            if (isCompetitionMode) {
                Button(
                    onClick = onUploadScore,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Cargar", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            } else {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = onCheckChanged,
                    colors = CheckboxDefaults.colors(checkedColor = ColorPrimaryAction, uncheckedColor = ColorTextSecondary, checkmarkColor = Color.White)
                )
            }
        }
    }
}

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
    val isInWaitlist = gymClass.waitingList.contains(currentUser.id)
    val isAttached = isEnrolled || isInWaitlist

    var buttonText by remember { mutableStateOf("") }
    var isEnabled by remember { mutableStateOf(true) }

    val now = Date()
    val classTime = gymClass.dateTime ?: now
    val thirtyMinutesBefore = Date(classTime.time - (30 * 60 * 1000))

    if (isAttached) {
        buttonText = if (isInWaitlist) "SALIR DE ESPERA" else "CANCELAR RESERVA"
        isEnabled = now.before(thirtyMinutesBefore) || currentUser.isAdmin
    } else {
        val isFull = gymClass.enrolledUserIds.size >= gymClass.maxCapacity
        val actionText = if (gymClass.isOpenGym == true) "AVISAR ASISTENCIA" else "RESERVAR LUGAR"
        buttonText = if (isFull) "UNIRSE A ESPERA" else actionText
        isEnabled = classTime.after(now) && (currentUser.hasValidCredits || currentUser.isAdmin)
    }

    Button(
        onClick = { if (isAttached) onCancel() else onBook() },
        modifier = Modifier.fillMaxWidth().height(50.dp),
        enabled = isEnabled && !isLoading,
        colors = ButtonDefaults.buttonColors(containerColor = if (isAttached) ColorError else ColorPrimaryAction),
        shape = RoundedCornerShape(12.dp)
    ) {
        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
        else Text(buttonText, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun AdminActionButtons(gymClass: GymClass, isLoading: Boolean, onSaveAttendance: () -> Unit) {
    if (gymClass.enrolledUserIds.isNotEmpty()) {
        val buttonText = if (gymClass.attendanceTaken) "ACTUALIZAR ASISTENCIA" else "CONFIRMAR ASISTENCIA"
        Button(
            onClick = onSaveAttendance,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = ColorSuccess),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            else Text(buttonText, fontWeight = FontWeight.Bold)
        }
    } else {
        Text("No hay alumnos inscritos", color = ColorTextSecondary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
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

@Composable
fun getLevelColor(level: String): Color {
    return when (level) {
        "Novato" -> Color(0xFF4CAF50) // Green
        "Constante" -> Color(0xFF2196F3) // Blue
        "Atleta" -> Color(0xFFFF9800) // Orange
        "RX" -> Color(0xFFF44336) // Red
        "Elite" -> Color(0xFF9C27B0) // Purple
        else -> Color.Gray
    }
}