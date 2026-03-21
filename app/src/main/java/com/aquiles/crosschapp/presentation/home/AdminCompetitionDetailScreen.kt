package com.aquiles.crosschapp.presentation.home

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.data.model.Competition
import com.aquiles.crosschapp.data.model.CompetitionStatus
import com.aquiles.crosschapp.data.model.GymClass
import com.aquiles.crosschapp.presentation.viewmodel.CompetitionDetailViewModel
import com.aquiles.crosschapp.presentation.viewmodel.CompetitionResultEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.components.CompetitionSegmentedControl
import com.aquiles.crosschapp.ui.theme.*
import androidx.compose.foundation.layout.height

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCompetitionDetailScreen(
    navController: NavController,
    competitionId: String,
    viewModel: CompetitionDetailViewModel = viewModel()
) {
    val competition by viewModel.competition.collectAsState()
    val linkedClasses by viewModel.linkedClasses.collectAsState()
    val isLoadingClasses by viewModel.isLoadingClasses.collectAsState()
    val competitionResults by viewModel.competitionResults.collectAsState()
    val enrolledUsers by viewModel.enrolledUsers.collectAsState()
    val isLoadingRanking by viewModel.isLoadingRanking.collectAsState()
    val isSubmittingScore by viewModel.isSubmittingScore.collectAsState()
    
    var showClassSelector by remember { mutableStateOf(false) }
    var showCreateEventDialog by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showScoreSheet by remember { mutableStateOf(false) }
    var scoreTargetUserId by remember { mutableStateOf("") }
    var scoreTargetUserName by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Eventos, 1=Ranking

    LaunchedEffect(competitionId) {
        viewModel.loadCompetition(competitionId)
    }
    LaunchedEffect(competition) {
        if (competition != null) viewModel.loadCompetitionRanking()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.height(72.dp),
                title = { Text("Detalle de Competencia", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.70f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        if (competition == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFC5200))
            }
        } else {
            val comp = competition!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header Section
                CompetitionHeader(comp, onEditClick = { showEditSheet = true })

                // Tabs
                CompetitionSegmentedControl(
                    selectedIndex = selectedTab,
                    items = listOf("Eventos", "Ranking"),
                    onIndexChanged = { selectedTab = it }
                )

                if (selectedTab == 0) {
                // Linked Classes Section
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Eventos y Heats", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        
                        Button(
                            onClick = { showCreateEventDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC5200)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Nuevo Evento", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    if (isLoadingClasses) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (linkedClasses.isEmpty()) {
                        GlassCard {
                            Box(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "No hay eventos vinculados aún. Agrega clases para que sumen puntos.",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            linkedClasses.forEach { gymClass ->
                                LinkedClassItem(gymClass, onUnlink = { viewModel.unlinkClass(gymClass.documentId) })
                            }
                        }
                    }
                }
                } else {
                    // --- RANKING TAB ---
                    CompetitionRankingSection(
                        results = competitionResults,
                        enrolledUsers = enrolledUsers,
                        isLoading = isLoadingRanking,
                        onRefresh = { viewModel.loadCompetitionRanking() },
                        onAddScore = { userId, userName ->
                            scoreTargetUserId = userId
                            scoreTargetUserName = userName
                            showScoreSheet = true
                        },
                        onApprove = { resultId ->
                            // TODO: approve result
                        },
                        onReject = { resultId ->
                            // TODO: reject result
                        }
                    )
                }


                // Prize & Actions Section
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Premios y Acciones", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    
                    GlassCard(
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Premio", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(comp.prizeDescription ?: "Sin premio definido", style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Recompensa", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("${comp.xpReward ?: 0} XP", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (comp.isActive) {
                        Button(
                            onClick = { viewModel.finishCompetition() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC0000)), // Red for destructive/finish
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Finalizar y Premiar", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        GlassCard(
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("Competencia Finalizada", color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateEventDialog && competition != null) {
        CreateCompetitionEventDialog(
            competition = competition!!,
            onDismiss = { showCreateEventDialog = false },
            onConfirm = { date, maxCapacity ->
                viewModel.createAndLinkCompetitionClass(
                    gymId = competition!!.gymId,
                    dateTime = date,
                    durationMinutes = 60,
                    maxCapacity = maxCapacity
                )
                showCreateEventDialog = false
            }
        )
    }
    
    if (showEditSheet && competition != null) {
        EditCompetitionSheet(
            competition = competition!!,
            onDismiss = { showEditSheet = false },
            onSave = { title, desc, prize ->
                viewModel.editCompetition(title, desc, prize)
                showEditSheet = false
            }
        )
    }

    if (showScoreSheet && scoreTargetUserId.isNotBlank()) {
        AdminScoreInjectionSheet(
            userName = scoreTargetUserName,
            isSubmitting = isSubmittingScore,
            onDismiss = { showScoreSheet = false },
            onSubmit = { score, notes, isRx ->
                viewModel.submitScoreForUser(
                    userId = scoreTargetUserId,
                    userName = scoreTargetUserName,
                    score = score,
                    notes = notes,
                    isRx = isRx
                )
                showScoreSheet = false
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCompetitionEventDialog(
    competition: Competition,
    onDismiss: () -> Unit,
    onConfirm: (Date, Int) -> Unit
) {
    var selectedDate by remember { mutableStateOf<Long?>(competition.startDate?.time) }
    var capacityStr by remember { mutableStateOf("20") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedHour by remember { mutableStateOf(10) }
    var selectedMinute by remember { mutableStateOf(0) }

    val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    val dateToShow = remember(selectedDate, selectedHour, selectedMinute) {
        val cal = java.util.Calendar.getInstance()
        if (selectedDate != null) cal.timeInMillis = selectedDate!!
        cal.set(java.util.Calendar.HOUR_OF_DAY, selectedHour)
        cal.set(java.util.Calendar.MINUTE, selectedMinute)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.time
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDate = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = selectedHour, initialMinute = selectedMinute)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancelar") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear Nuevo Evento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Este evento se creará automáticamente como parte de '${competition.title}'.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                
                OutlinedTextField(
                    value = dateTimeFormat.format(dateToShow),
                    onValueChange = {},
                    label = { Text("Fecha y Hora") },
                    readOnly = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f)) { Text("Cambiar Día") }
                    Button(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f)) { Text("Cambiar Hora") }
                }

                OutlinedTextField(
                    value = capacityStr,
                    onValueChange = { capacityStr = it },
                    label = { Text("Cupo Máximo (Atletas)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cap = capacityStr.toIntOrNull() ?: 20
                    onConfirm(dateToShow, cap)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC5200))
            ) {
                Text("Cargar Evento")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        containerColor = Color(0xFF1E1E1E),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}


// =========================================================
// COMPETITION RANKING SECTION (Admin)
// =========================================================

@Composable
fun CompetitionRankingSection(
    results: List<CompetitionResultEntry>,
    enrolledUsers: List<com.aquiles.crosschapp.data.model.User>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onAddScore: (userId: String, userName: String) -> Unit,
    onApprove: (resultId: String) -> Unit,
    onReject: (resultId: String) -> Unit
) {
    val resultsByUser = results.associateBy { it.userId }
    val usersWithoutResult = enrolledUsers.filter { user -> !resultsByUser.containsKey(user.id) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ranking", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
            TextButton(onClick = onRefresh) {
                Text("Actualizar", color = Color(0xFFFC5200))
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFC5200))
            }
        } else if (results.isEmpty() && usersWithoutResult.isEmpty()) {
            GlassCard {
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("Aún no hay participantes ni resultados.", color = Color.Gray)
                }
            }
        } else {
            // Resultados existentes
            results.forEachIndexed { index, entry ->
                CompetitionParticipantRow(
                    rank = index + 1,
                    entry = entry,
                    onApprove = { onApprove(entry.resultId) },
                    onReject = { onReject(entry.resultId) }
                )
            }

            // Participantes sin resultado
            if (usersWithoutResult.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Sin resultado enviado:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                usersWithoutResult.forEach { user ->
                    GlassCard(shape = RoundedCornerShape(12.dp)) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(36.dp).background(Color.White.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Text("${user.name} ${user.lastName}", color = Color.White, fontWeight = FontWeight.Medium)
                            }
                            OutlinedButton(
                                onClick = { onAddScore(user.id ?: "", "${user.name} ${user.lastName}") },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFC5200)),
                                border = BorderStroke(1.dp, Color(0xFFFC5200)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Score", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CompetitionParticipantRow(
    rank: Int,
    entry: CompetitionResultEntry,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val rankColor = when (rank) {
        1 -> ColorGold
        2 -> ColorSilver
        3 -> ColorBronze
        else -> Color.White.copy(alpha = 0.3f)
    }

    GlassCard(shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Box(
                Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (rank <= 3) {
                    Icon(Icons.Default.EmojiEvents, null, tint = rankColor, modifier = Modifier.size(28.dp))
                }
                Text(
                    "$rank",
                    color = if (rank <= 3) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (rank <= 3) 10.sp else 14.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(entry.userName, color = Color.White, fontWeight = FontWeight.Bold)
                if (entry.isPending) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(StatusPending, CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text("Pendiente de validación", style = MaterialTheme.typography.labelSmall, color = StatusPending)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(SuccessGreen, CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text("Aprobado", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    entry.score,
                    color = if (entry.isPending) StatusPending else Color.White,
                    fontWeight = FontWeight.Bold
                )
                if (entry.isRx) {
                    Text("RX", style = MaterialTheme.typography.labelSmall, color = StatusPending, fontWeight = FontWeight.Black)
                }
            }

            if (entry.isPending) {
                Spacer(Modifier.width(8.dp))
                Column {
                    IconButton(onClick = onApprove, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Check, "Aprobar", tint = SuccessGreen, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onReject, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, "Rechazar", tint = ErrorRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScoreInjectionSheet(
    userName: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (score: String, notes: String, isRx: Boolean) -> Unit
) {
    var score by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isRx by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth().padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Cargar Score para $userName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)

            OutlinedTextField(
                value = score,
                onValueChange = { score = it },
                label = { Text("Score (ej: 10:45, 85 reps)") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas (opcional)") },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth().clickable { isRx = !isRx }.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (isRx) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    null,
                    tint = if (isRx) Color(0xFFFC5200) else Color.Gray
                )
                Spacer(Modifier.width(8.dp))
                Text("RX (peso reglamentario)", color = Color.White)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    modifier = Modifier.weight(1f)
                ) { Text("Cancelar", color = Color.White) }

                Button(
                    onClick = { if (score.isNotBlank()) onSubmit(score, notes, isRx) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC5200)),
                    modifier = Modifier.weight(1f),
                    enabled = score.isNotBlank() && !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Guardar Score", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CompetitionHeader(comp: Competition, onEditClick: () -> Unit) {

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = comp.resolveTypeEnum().value.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            
            val status = comp.resolveStatus()
            val statusColor = when (status) {
                CompetitionStatus.ONGOING -> SuccessGreen
                CompetitionStatus.UPCOMING -> StatusUpcoming
                CompetitionStatus.FINISHED -> ErrorRed
                else -> Color.Gray
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(statusColor, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when(status) {
                        CompetitionStatus.ONGOING -> "En Curso"
                        CompetitionStatus.UPCOMING -> "Próxima"
                        CompetitionStatus.FINISHED -> "Finalizada"
                        else -> "Inactiva"
                    },
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = comp.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            IconButton(onClick = onEditClick) {
                Icon(
                    androidx.compose.material.icons.Icons.Default.Edit,
                    contentDescription = "Editar",
                    tint = Color(0xFFFC5200)
                )
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
            val start = comp.startDate ?: Date()
            val end = comp.endDate ?: Date()
            Text(
                "${dateFormat.format(start)} - ${dateFormat.format(end)}",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
    }
}

@Composable
fun LinkedClassItem(gymClass: GymClass, onUnlink: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        // Time Box
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(50.dp)
        ) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dayFormat = SimpleDateFormat("d MMM", Locale.getDefault())
            val date = gymClass.dateTime ?: Date()
            
            Text(timeFormat.format(date), color = Color(0xFFFC5200), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(dayFormat.format(date), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
        
        Box(
            Modifier
                .width(1.dp)
                .height(40.dp)
                .background(Color.White.copy(alpha = 0.2f))
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(gymClass.name, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(gymClass.coachName, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
            
            Spacer(Modifier.height(4.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Participants
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFFC5200), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    // Assuming EnrolledUserIds count + attendedUserIds count or similiar logic from iOS
                    // iOS uses attendees.count. GymClass has enrolledUserIds (registered).
                    Text("${gymClass.enrolledUserIds.size} part.", color = Color(0xFFFC5200), style = MaterialTheme.typography.labelSmall)
                }
                
                // Score Type
                if (!gymClass.wodScoreType.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFC5200), modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(gymClass.wodScoreType, color = Color(0xFFFC5200), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        
        IconButton(onClick = onUnlink) {
            Icon(Icons.Default.Delete, contentDescription = "Desvincular", tint = Color.Red.copy(alpha = 0.7f))
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassSelectorDialog(
    gymId: String,
    startDate: Date,
    endDate: Date,
    existingIds: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<GymClass>) -> Unit,
    viewModel: CompetitionDetailViewModel
) {
    var selectedDate by remember { mutableStateOf(startDate) } // Start with competition start date
    val classesForDate by viewModel.classesForDate.collectAsState()
    val isLoading by viewModel.isLoadingSelector.collectAsState()
    
    // Selection state
    val selectedClasses = remember { mutableStateMapOf<String, GymClass>() }

    LaunchedEffect(selectedDate) {
        viewModel.loadClassesForDate(selectedDate, gymId)
    }

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.time)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = Date(it) }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Clases") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Date Selector Button
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(selectedDate))
                }

                if (isLoading) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (classesForDate.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("No hay clases para este día", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false), // Allows shrinking if content is small
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(classesForDate) { gymClass ->
                            val isAlreadyLinked = existingIds.contains(gymClass.documentId)
                            val isSelected = selectedClasses.containsKey(gymClass.documentId)
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isAlreadyLinked) {
                                        if (isSelected) {
                                            selectedClasses.remove(gymClass.documentId)
                                        } else {
                                            selectedClasses[gymClass.documentId] = gymClass
                                        }
                                    }
                                    .background(if (isSelected) Color(0xFFFC5200).copy(alpha = 0.1f) else Color.Transparent)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    tint = if (isAlreadyLinked) Color.Gray else if (isSelected) Color(0xFFFC5200) else Color.Gray
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(gymClass.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${SimpleDateFormat("HH:mm", Locale.getDefault()).format(gymClass.dateTime ?: Date())} - ${gymClass.coachName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                if (isAlreadyLinked) {
                                    Spacer(Modifier.weight(1f))
                                    Text("Vinculado", color = Color.Green, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedClasses.values.toList()) },
                enabled = selectedClasses.isNotEmpty()
            ) {
                Text("Guardar (${selectedClasses.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCompetitionSheet(
    competition: Competition,
    onDismiss: () -> Unit,
    onSave: (String, String, String?) -> Unit
) {
    var editTitle by remember { mutableStateOf(competition.title) }
    var editDescription by remember { mutableStateOf(competition.description) }
    var editPrizeDescription by remember { mutableStateOf(competition.prizeDescription ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1E1E1E), // Dark theme
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Editar Evento", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            
            OutlinedTextField(
                value = editTitle,
                onValueChange = { editTitle = it },
                label = { Text("Título del Evento") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = editDescription,
                onValueChange = { editDescription = it },
                label = { Text("Descripción / Detalles WOD") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth().height(120.dp)
            )

            OutlinedTextField(
                value = editPrizeDescription,
                onValueChange = { editPrizeDescription = it },
                label = { Text("Premio (Opcional)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancelar", color = Color.White)
                }
                
                Button(
                    onClick = {
                        onSave(editTitle, editDescription, editPrizeDescription.ifBlank { null })
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC5200)),
                    modifier = Modifier.weight(1f),
                    enabled = editTitle.isNotBlank()
                ) {
                    Text("Guardar Cambios", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
