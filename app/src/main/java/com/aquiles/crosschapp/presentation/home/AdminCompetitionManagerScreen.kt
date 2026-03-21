package com.aquiles.crosschapp.presentation.home

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.data.model.Competition
import com.aquiles.crosschapp.data.model.CompetitionStatus
import com.aquiles.crosschapp.data.model.CompetitionType
import com.aquiles.crosschapp.data.model.RankingCriteria
import com.aquiles.crosschapp.data.model.ScoreStrategy
import com.aquiles.crosschapp.data.model.ValidationRule
import com.aquiles.crosschapp.domain.competition.CompetitionFormValidator
import com.aquiles.crosschapp.presentation.viewmodel.CompetitionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.ui.theme.*
import com.aquiles.crosschapp.presentation.components.SetupStepBottomSheet
import com.aquiles.crosschapp.presentation.components.ValidationErrorBanner
import com.aquiles.crosschapp.presentation.components.BrandOrange
import com.aquiles.crosschapp.presentation.components.ErrorRed
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Brush

import com.aquiles.crosschapp.presentation.common.AppBackground

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
            SetupStepBottomSheet(
                step = step,
                onDismiss = { showSetupPopup = false }
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
                    CompetitionStatus.ONGOING -> SuccessGreen
                    CompetitionStatus.UPCOMING -> StatusUpcoming
                    CompetitionStatus.FINISHED -> ErrorRed
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
    var endDate by remember { mutableStateOf(Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)) }
    var prizeDescription by remember { mutableStateOf("") }
    var xpReward by remember { mutableStateOf("500") }
    var criteriaFieldValue by remember { mutableStateOf("") } // campo dinámico por criterio
    // Event specific
    var eventTime by remember { mutableStateOf("10:00") }
    var maxCapacity by remember { mutableStateOf("50") }
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(initialHour = 10, initialMinute = 0)

    var scoreStrategy by remember { mutableStateOf(ScoreStrategy.RELATIVE) }
    var validationRule by remember { mutableStateOf(ValidationRule.MANUAL) }

    // Estados de validación del formulario
    var formErrors by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showValidationAlert by remember { mutableStateOf(false) }

    // Campos dinámicos según criterio seleccionado
    val criteriaFieldConfig = remember(criteria) {
        CompetitionFormValidator.getCriteriaFieldConfig(criteria)
    }

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

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerStateEnd.selectedDateMillis?.let { endDate = Date(it) }
                    showEndDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerStateEnd)
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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Nueva Competencia", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.9f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        AppBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Un Box para proporcionar BoxScope y permitir .align()
            Box(modifier = Modifier.fillMaxSize()) {
                val scrollState = rememberScrollState()
                
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    if (formErrors.isNotEmpty()) {
                        ValidationErrorBanner(errors = formErrors)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                                      Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            "Configura los detalles de tu próximo evento para motivar a la comunidad.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f)
                        )

                        BasicInfoSection(
                            title = title,
                            onTitleChange = { 
                                title = it
                                if (it.isNotBlank()) formErrors = formErrors.toMutableMap().also { m -> m.remove("title") }
                            },
                            description = description,
                            onDescriptionChange = { 
                                description = it
                                if (it.isNotBlank()) formErrors = formErrors.toMutableMap().also { m -> m.remove("description") }
                            },
                            formErrors = formErrors
                        )

                        TechnicalConfigSection(
                            type = type,
                            onTypeChange = { type = it },
                            criteria = criteria,
                            onCriteriaChange = { 
                                criteria = it
                                criteriaFieldValue = ""
                                formErrors = formErrors.toMutableMap().also { m -> m.remove("criteriaField") }
                            },
                            criteriaFieldValue = criteriaFieldValue,
                            onCriteriaFieldValueChange = { criteriaFieldValue = it },
                            criteriaFieldConfig = criteriaFieldConfig,
                            formErrors = formErrors
                        )

                        ScheduleAndCapacitySection(
                            startDate = startDate,
                            onShowStartDatePicker = { showStartDatePicker = true },
                            endDate = endDate,
                            onShowEndDatePicker = { showEndDatePicker = true },
                            eventTime = eventTime,
                            onShowTimePicker = { showTimePicker = true },
                            isEvent = type == CompetitionType.RANGE,
                            maxCapacity = maxCapacity,
                            onMaxCapacityChange = { if (it.all { c -> c.isDigit() }) maxCapacity = it }
                        )

                        RewardsSection(
                            prizeDescription = prizeDescription,
                            onPrizeDescriptionChange = { prizeDescription = it },
                            xpReward = xpReward,
                            onXpRewardChange = { if (it.all { c -> c.isDigit() }) xpReward = it }
                        )
                        
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }

                // Botón de Acción Fijo
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
                                startY = 0f
                            )
                        )
                        .padding(24.dp)
                ) {
                    Button(
                        onClick = {
                            val capacity = maxCapacity.toIntOrNull() ?: 0
                            val validation = CompetitionFormValidator.validate(
                                title = title,
                                description = description,
                                startDate = startDate,
                                endDate = endDate,
                                maxCapacity = capacity,
                                criteriaSpecificValue = criteriaFieldValue.ifBlank { null },
                                criteria = criteria
                            )
                            if (!validation.isValid) {
                                formErrors = validation.errors
                            } else {
                                onConfirm(
                                    title, description, type, criteria, startDate, endDate,
                                    prizeDescription.ifBlank { null }, xpReward.toIntOrNull(),
                                    scoreStrategy, validationRule, eventTime, capacity
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                        shape = RoundedCornerShape(20.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                    ) {
                        Text("PUBLICAR COMPETENCIA", fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun BasicInfoSection(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    formErrors: Map<String, String>
) {
    GlassCard(shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                label = { Text("Título de la Competencia *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = formErrors.containsKey("title"),
                supportingText = formErrors["title"]?.let { error ->
                    { Text(error, color = ErrorRed, style = MaterialTheme.typography.labelSmall) }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    errorBorderColor = ErrorRed
                )
            )
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("Descripción *") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                isError = formErrors.containsKey("description"),
                supportingText = formErrors["description"]?.let { error ->
                    { Text(error, color = ErrorRed, style = MaterialTheme.typography.labelSmall) }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    errorBorderColor = ErrorRed
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TechnicalConfigSection(
    type: CompetitionType,
    onTypeChange: (CompetitionType) -> Unit,
    criteria: RankingCriteria,
    onCriteriaChange: (RankingCriteria) -> Unit,
    criteriaFieldValue: String,
    onCriteriaFieldValueChange: (String) -> Unit,
    criteriaFieldConfig: CompetitionFormValidator.CriteriaFieldConfig?,
    formErrors: Map<String, String>
) {
    Text("CONFIGURACIÓN TÉCNICA", style = MaterialTheme.typography.labelLarge, color = BrandOrange, fontWeight = FontWeight.Black)
    GlassCard(shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Type Selector
            var expandedType by remember { mutableStateOf(false) }
            Box {
                OutlinedTextField(
                    value = type.value,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo de Evento") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { IconButton(onClick = { expandedType = true }) { Icon(Icons.Default.ArrowDropDown, "Select", tint = Color.White) } },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                DropdownMenu(expanded = expandedType, onDismissRequest = { expandedType = false }) {
                    CompetitionType.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.value) },
                            onClick = { onTypeChange(option); expandedType = false }
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
                    label = { Text("Ranking por...") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = { IconButton(onClick = { expandedCrit = true }) { Icon(Icons.Default.ArrowDropDown, "Select", tint = Color.White) } },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
                DropdownMenu(expanded = expandedCrit, onDismissRequest = { expandedCrit = false }) {
                    RankingCriteria.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.value) },
                            onClick = {
                                onCriteriaChange(option)
                                expandedCrit = false
                            }
                        )
                    }
                }
            }

            // Dynamic Field
            AnimatedVisibility(visible = criteriaFieldConfig != null) {
                criteriaFieldConfig?.let { config ->
                    OutlinedTextField(
                        value = criteriaFieldValue,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                onCriteriaFieldValueChange(it)
                            }
                        },
                        label = { Text(config.label) },
                        placeholder = { Text(config.placeholder, color = Color.White.copy(0.4f)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = formErrors.containsKey("criteriaField"),
                        supportingText = { Text(config.hint, style = MaterialTheme.typography.labelSmall) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (config.inputType == CompetitionFormValidator.InputType.INTEGER)
                                KeyboardType.Number
                            else
                                KeyboardType.Decimal
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            errorBorderColor = ErrorRed
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ScheduleAndCapacitySection(
    startDate: Date,
    onShowStartDatePicker: () -> Unit,
    endDate: Date,
    onShowEndDatePicker: () -> Unit,
    eventTime: String,
    onShowTimePicker: () -> Unit,
    isEvent: Boolean,
    maxCapacity: String,
    onMaxCapacityChange: (String) -> Unit
) {
    Text("TIEMPO Y CAPACIDAD", style = MaterialTheme.typography.labelLarge, color = BrandOrange, fontWeight = FontWeight.Black)
    GlassCard(shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Inicia", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                    OutlinedButton(
                        onClick = onShowStartDatePicker,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text(SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(startDate), color = Color.White)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Finaliza", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                    OutlinedButton(
                        onClick = onShowEndDatePicker,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text(SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(endDate), color = Color.White)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (isEvent) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Hora de Inicio", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
                        OutlinedButton(
                            onClick = onShowTimePicker,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Text(eventTime, color = Color.White)
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                OutlinedTextField(
                    value = maxCapacity,
                    onValueChange = onMaxCapacityChange,
                    label = { Text("Cupos Max") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                )
            }
        }
    }
}

@Composable
private fun RewardsSection(
    prizeDescription: String,
    onPrizeDescriptionChange: (String) -> Unit,
    xpReward: String,
    onXpRewardChange: (String) -> Unit
) {
    Text("RECOMPENSAS Y GAMIFICACIÓN", style = MaterialTheme.typography.labelLarge, color = BrandOrange, fontWeight = FontWeight.Black)
    GlassCard(shape = RoundedCornerShape(24.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = prizeDescription,
                onValueChange = onPrizeDescriptionChange,
                label = { Text("Premio (Ej: Suplemento, 1 Mes Gratis)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
            
            OutlinedTextField(
                value = xpReward,
                onValueChange = onXpRewardChange,
                label = { Text("XP Reward") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.EmojiEvents, null, tint = BrandOrange) },
                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
            )
        }
    }
}

