package com.aquiles.crosschapp.presentation.home

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.data.model.Competition
import com.aquiles.crosschapp.data.model.CompetitionStatus
import com.aquiles.crosschapp.data.model.CompetitionType
import com.aquiles.crosschapp.data.model.RankingCriteria
import com.aquiles.crosschapp.data.model.ScoreStrategy
import com.aquiles.crosschapp.data.model.ValidationRule
import com.aquiles.crosschapp.presentation.viewmodel.CompetitionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.aquiles.crosschapp.presentation.components.GlassCard
import androidx.compose.foundation.layout.height

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCompetitionManagerScreen(
    navController: NavController,
    viewModel: CompetitionViewModel = viewModel(),
    setupStepKey: String? = null
) {
    var showSetupPopup by remember { mutableStateOf(setupStepKey != null) }
    
    if (showSetupPopup) {
        SetupStep.entries.find { it.toKey() == setupStepKey }?.let { step ->
            AlertDialog(
                onDismissRequest = { showSetupPopup = false },
                title = { Text(step.title, color = Color.White) },
                text = { Text(step.description, color = Color.White.copy(alpha = 0.7f)) },
                confirmButton = {
                    Button(onClick = { showSetupPopup = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC5200))) { 
                        Text("Entendido", color = Color.White) 
                    }
                },
                containerColor = Color(0xFF1C1C1E).copy(alpha = 0.70f).copy(alpha = 0.70f)
            )
        }
    }
    
    val competitions by viewModel.competitions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val context = LocalContext.current

    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadCompetitions()
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.height(72.dp),
                title = { Text("Competencias", fontWeight = FontWeight.Bold) },
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
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Color(0xFFFC5200), // Brand Orange
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Competencia")
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading && competitions.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFFFC5200)
                )
            } else if (competitions.isEmpty()) {
                GlassCard(
                    modifier = Modifier.align(Alignment.Center).padding(32.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.White.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No hay competencias activas",
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(competitions) { comp ->
                        CompetitionCard(
                            competition = comp,
                            onClick = { navController.navigate("admin_competition_detail/${comp.id}") },
                            onDelete = { viewModel.deleteCompetition(comp.id) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCompetitionDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, desc, type, crit, start, end, prize, xp, score, validation, eventTime, capacity ->
                viewModel.createCompetitionWithEvent(
                    title, desc, type, crit, start, end, prize, xp, score, validation, eventTime, capacity
                )
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun CompetitionCard(competition: Competition, onClick: () -> Unit, onDelete: () -> Unit) {
    GlassCard(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = competition.resolveTypeEnum().value.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = Color.Red.copy(alpha = 0.7f)
                    )
                }
            }

            Text(
                text = competition.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = competition.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                maxLines = 2
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Status Dot
                val status = competition.resolveStatus()
                val statusColor = when (status) {
                    CompetitionStatus.ONGOING -> Color.Green
                    CompetitionStatus.UPCOMING -> Color.Blue
                    CompetitionStatus.FINISHED -> Color.Red
                    else -> Color.Gray
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when(status) {
                            CompetitionStatus.ONGOING -> "En Curso"
                            CompetitionStatus.UPCOMING -> "Próxima"
                            CompetitionStatus.FINISHED -> "Finalizada"
                            else -> "Inactiva"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }

                // Date
                val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                val dateStr = "${dateFormat.format(competition.startDate ?: Date())} - ${dateFormat.format(competition.endDate ?: Date())}"
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCompetitionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, CompetitionType, RankingCriteria, Date, Date, String?, Int?, ScoreStrategy, ValidationRule, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(CompetitionType.MONTHLY) }
    var criteria by remember { mutableStateOf(RankingCriteria.POINTS) }
    var startDate by remember { mutableStateOf(Date()) }
    var endDate by remember { mutableStateOf(Date()) } 
    var prizeDescription by remember { mutableStateOf("") }
    var xpReward by remember { mutableStateOf("500") }
    // Event specific
    var eventTime by remember { mutableStateOf("10:00") }
    var maxCapacity by remember { mutableStateOf("50") }
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(initialHour = 10, initialMinute = 0)

    var scoreStrategy by remember { mutableStateOf(ScoreStrategy.RELATIVE) }
    var validationRule by remember { mutableStateOf(ValidationRule.MANUAL) }

    // Date Picker Logic
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    val datePickerStateStart = rememberDatePickerState(initialSelectedDateMillis = startDate.time)
    val datePickerStateEnd = rememberDatePickerState(initialSelectedDateMillis = endDate.time)

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerStateStart.selectedDateMillis?.let { startDate = Date(it) }
                    showStartDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerStateStart)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    eventTime = String.format(Locale.getDefault(), "%02d:%02d", timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva Competencia") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp) 
                    .padding(vertical = 8.dp),
                 verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier.verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción") },
                        maxLines = 3
                    )
                    
                    HorizontalDivider()
                    Text("Configuración General", style = MaterialTheme.typography.labelMedium)

                    // Type Selector
                    var expandedType by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(
                            value = type.value,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo") },
                            trailingIcon = { IconButton(onClick = { expandedType = true }) { Icon(Icons.Default.ArrowDropDown, "Select") } }
                        )
                        DropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                            CompetitionType.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.value) },
                                    onClick = { type = option; expandedType = false }
                                )
                            }
                        }
                    }

                    // Criteria Selector
                    var expandedCrit by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(
                            value = criteria.value,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Criterio") },
                            trailingIcon = { IconButton(onClick = { expandedCrit = true }) { Icon(Icons.Default.ArrowDropDown, "Select") } }
                        )
                        DropdownMenu(expanded = expandedCrit, onDismissRequest = { expandedCrit = false }) {
                            RankingCriteria.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.value) },
                                    onClick = { criteria = option; expandedCrit = false }
                                )
                            }
                        }
                    }

                    // Score Strategy Selector
                    var expandedScore by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(
                            value = scoreStrategy.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Estrategia de Puntaje") },
                            trailingIcon = { IconButton(onClick = { expandedScore = true }) { Icon(Icons.Default.ArrowDropDown, "Select") } },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(expanded = expandedScore, onDismissRequest = { expandedScore = false }) {
                            ScoreStrategy.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.name) },
                                    onClick = { scoreStrategy = option; expandedScore = false }
                                )
                            }
                        }
                    }

                    // Validation Rule Selector
                    var expandedValidation by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(
                            value = validationRule.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Regla de Validación") },
                            trailingIcon = { IconButton(onClick = { expandedValidation = true }) { Icon(Icons.Default.ArrowDropDown, "Select") } },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(expanded = expandedValidation, onDismissRequest = { expandedValidation = false }) {
                            ValidationRule.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.name) },
                                    onClick = { validationRule = option; expandedValidation = false }
                                )
                            }
                        }
                    }

                    HorizontalDivider()
                    Text("Fechas", style = MaterialTheme.typography.labelMedium)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { showStartDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(startDate))
                        }
                        OutlinedButton(
                            onClick = { showEndDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(endDate))
                        }
                    }

                    HorizontalDivider()
                    Text("Primer Prueba base (Heat)", style = MaterialTheme.typography.labelMedium)
                    Text("Toda competencia inicia con una prueba que puede ser en un horario físico (presencial) o una franja límite (Open).", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(eventTime)
                        }
                        
                        OutlinedTextField(
                            value = maxCapacity,
                            onValueChange = { if (it.all { c -> c.isDigit() }) maxCapacity = it },
                            label = { Text("Atletas (Max)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    HorizontalDivider()
                    Text("Premios", style = MaterialTheme.typography.labelMedium)
                    
                    OutlinedTextField(
                        value = prizeDescription,
                        onValueChange = { prizeDescription = it },
                        label = { Text("Premio (Opcional)") }
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = xpReward,
                            onValueChange = { if (it.all { c -> c.isDigit() }) xpReward = it },
                            label = { Text("XP (Opcional)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        title, 
                        description, 
                        type, 
                        criteria, 
                        startDate, 
                        endDate, 
                        prizeDescription, 
                        xpReward.toIntOrNull(), 
                        scoreStrategy,
                        validationRule,
                        eventTime,
                        maxCapacity.toIntOrNull() ?: 50
                    )
                },
                enabled = title.isNotEmpty() && maxCapacity.isNotEmpty()
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
