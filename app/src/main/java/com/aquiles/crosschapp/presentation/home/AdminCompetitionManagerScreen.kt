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
import androidx.compose.material.icons.filled.ArrowBack
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
import com.aquiles.crosschapp.presentation.viewmodel.CompetitionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.aquiles.crosschapp.presentation.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCompetitionManagerScreen(
    navController: NavController,
    viewModel: CompetitionViewModel = viewModel()
) {
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
                title = { Text("Competencias", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black,
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
            onConfirm = { title, desc, type, crit, start, end, prize, xp, intergym ->
                viewModel.createCompetition(title, desc, type, crit, start, end, prize, xp, intergym)
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
                    text = competition.getTypeEnum().value.uppercase(),
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
                val status = competition.getStatus()
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
    onConfirm: (String, String, CompetitionType, RankingCriteria, Date, Date, String?, Int?, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(CompetitionType.MONTHLY) }
    var criteria by remember { mutableStateOf(RankingCriteria.POINTS) }
    var startDate by remember { mutableStateOf(Date()) }
    var endDate by remember { mutableStateOf(Date()) } 
    var prizeDescription by remember { mutableStateOf("") }
    var xpReward by remember { mutableStateOf("500") }
    var isIntergym by remember { mutableStateOf(false) }

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
                    Text("Configuración", style = MaterialTheme.typography.labelMedium)

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
                            label = { Text("XP") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Inter-Gym")
                        Switch(checked = isIntergym, onCheckedChange = { isIntergym = it })
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
                        isIntergym
                    )
                },
                enabled = title.isNotEmpty()
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
