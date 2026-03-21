package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.data.model.GlobalChallenge
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import com.aquiles.crosschapp.presentation.viewmodel.BenchmarkOperationState
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.viewmodel.GlobalChallengesState
import com.aquiles.crosschapp.presentation.viewmodel.UserSession
import com.google.firebase.Timestamp
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import android.app.DatePickerDialog

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
private val ColorDialogSurface = Color(0xFF1C1C1E)
private val ColorError = Color(0xFFEF5350)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageChallengesScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    adminViewModel: AdminViewModel = viewModel(),
    onlyGlobal: Boolean = true // Por defecto para Super Admin
) {
    val challengesState by adminViewModel.challengesState.collectAsState()
    val operationState by adminViewModel.benchmarkOperationState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDialog by remember { mutableStateOf(false) }
    var selectedChallenge by remember { mutableStateOf<GlobalChallenge?>(null) }

    LaunchedEffect(Unit) { adminViewModel.loadChallenges() }

    LaunchedEffect(operationState) {
        when (val current = operationState) {
            is BenchmarkOperationState.Success -> {
                snackbarHostState.showSnackbar(current.message)
                adminViewModel.resetBenchmarkOperationState()
            }
            is BenchmarkOperationState.Error -> {
                snackbarHostState.showSnackbar("Error: ${current.message}")
                adminViewModel.resetBenchmarkOperationState()
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.height(72.dp),
                title = { 
                    Text(
                        if (onlyGlobal) "Desafíos Globales" else "Desafíos del Gimnasio", 
                        fontWeight = FontWeight.Bold, 
                        color = ColorTextPrimary
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ColorTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedChallenge = null
                    showDialog = true
                },
                containerColor = ColorPrimaryAction,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "Añadir")
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Transparent
    ) { localScaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = localScaffoldPadding.calculateTopPadding())
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (val state = challengesState) {
                is GlobalChallengesState.Loading -> {
                    CircularProgressIndicator(color = ColorPrimaryAction, modifier = Modifier.align(Alignment.Center))
                }
                is GlobalChallengesState.Success -> {
                    // FILTRAR POR GLOBALIDAD
                    val filteredChallenges = state.challenges.filter { 
                        it.isGlobal == onlyGlobal
                    }
                    
                    if (filteredChallenges.isEmpty()) {
                        GlassCard(modifier = Modifier.align(Alignment.Center).padding(32.dp)) {
                            Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    if (onlyGlobal) "No hay Desafíos Globales creados." else "No hay Desafíos registrados.",
                                    color = ColorTextSecondary
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(filteredChallenges) { challenge ->
                                ChallengeItemCard(
                                    challenge = challenge,
                                    onEdit = { 
                                        selectedChallenge = challenge
                                        showDialog = true 
                                    },
                                    onDelete = { adminViewModel.deleteGlobalChallenge(challenge.id) }
                                )
                            }
                        }
                    }
                }
                is GlobalChallengesState.Error -> {
                    Text("Error: ${state.message}", color = ColorError, modifier = Modifier.align(Alignment.Center))
                }
                else -> Unit
            }
        }
    }

    if (showDialog) {
        ChallengeEditDialog(
            challenge = selectedChallenge,
            onlyGlobal = onlyGlobal,
            onDismiss = { showDialog = false },
            onSave = { updated ->
                adminViewModel.saveGlobalChallenge(updated)
                showDialog = false
            }
        )
    }
}

@Composable
fun ChallengeItemCard(
    challenge: GlobalChallenge,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(challenge.name, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
                Text(challenge.scoreType, style = MaterialTheme.typography.labelSmall, color = ColorPrimaryAction)
                if (challenge.description.isNotBlank()) {
                    Text(
                        challenge.description, 
                        style = MaterialTheme.typography.bodySmall, 
                        color = ColorTextSecondary,
                        maxLines = 2
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Editar", tint = ColorTextSecondary)
                }
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, "Eliminar", tint = ColorError.copy(alpha = 0.7f))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = ColorDialogSurface,
            title = { Text("Eliminar Desafío", color = ColorTextPrimary) },
            text = { Text("¿Estás seguro de eliminar este desafío? Esta acción no se puede deshacer.", color = ColorTextSecondary) },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Eliminar", color = ColorError)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = ColorTextSecondary)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeEditDialog(
    challenge: GlobalChallenge?,
    onlyGlobal: Boolean,
    onDismiss: () -> Unit,
    onSave: (GlobalChallenge) -> Unit
) {
    var name by remember { mutableStateOf(challenge?.name ?: "") }
    var description by remember { mutableStateOf(challenge?.description ?: "") }
    var scoreType by remember { mutableStateOf(challenge?.scoreType ?: "TIME") }
    var strategy by remember { mutableStateOf(challenge?.strategy ?: "") }
    var sortOrder by remember { 
        mutableStateOf(challenge?.sortOrder ?: if (scoreType == "TIME") "ASC" else "DESC") 
    }
    
    var useDates by remember { mutableStateOf(challenge?.startDate != null || challenge?.endDate != null) }
    var startDate by remember { mutableStateOf(challenge?.startDate?.toDate() ?: Date()) }
    var endDate by remember { 
        mutableStateOf(challenge?.endDate?.toDate() ?: Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)) 
    }

    val context = LocalContext.current
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    var isNameError by remember { mutableStateOf(false) }
    var scoreTypeExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorDialogSurface,
        title = { 
            Text(if (challenge == null) "Nuevo Desafío" else "Editar Desafío", fontWeight = FontWeight.Bold, color = ColorTextPrimary) 
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; isNameError = it.isBlank() },
                    label = { Text("Nombre del Desafío *", color = ColorTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = isNameError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorPrimaryAction,
                        unfocusedBorderColor = ColorBorder,
                        focusedTextColor = ColorTextPrimary,
                        unfocusedTextColor = ColorTextPrimary
                    )
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción / WOD", color = ColorTextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorPrimaryAction,
                        unfocusedBorderColor = ColorBorder,
                        focusedTextColor = ColorTextPrimary,
                        unfocusedTextColor = ColorTextPrimary
                    )
                )

                ExposedDropdownMenuBox(
                    expanded = scoreTypeExpanded,
                    onExpandedChange = { scoreTypeExpanded = !scoreTypeExpanded }
                ) {
                    OutlinedTextField(
                        value = scoreType,
                        onValueChange = {},
                        label = { Text("Tipo de Resultado", color = ColorTextSecondary) },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scoreTypeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorPrimaryAction,
                            unfocusedBorderColor = ColorBorder,
                            focusedTextColor = ColorTextPrimary,
                            unfocusedTextColor = ColorTextPrimary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = scoreTypeExpanded,
                        onDismissRequest = { scoreTypeExpanded = false },
                        modifier = Modifier.background(ColorDialogSurface)
                    ) {
                        listOf("TIME", "WEIGHT", "REPS", "ROUNDS", "DISTANCE").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type, color = ColorTextPrimary) },
                                onClick = { 
                                    scoreType = type
                                    sortOrder = if (type == "TIME") "ASC" else "DESC"
                                    scoreTypeExpanded = false 
                                }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = useDates,
                        onCheckedChange = { useDates = it },
                        colors = CheckboxDefaults.colors(checkedColor = ColorPrimaryAction)
                    )
                    Text("Limitar por fechas (Temporal)", color = ColorTextPrimary)
                }

                if (useDates) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val cal = Calendar.getInstance()
                                cal.time = startDate
                                DatePickerDialog(context, { _, y, m, d ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(y, m, d)
                                    startDate = newCal.time
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, ColorBorder)
                        ) {
                            Text("Desde: ${sdf.format(startDate)}", color = ColorTextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                        Button(
                            onClick = {
                                val cal = Calendar.getInstance()
                                cal.time = endDate
                                DatePickerDialog(context, { _, y, m, d ->
                                    val newCal = Calendar.getInstance()
                                    newCal.set(y, m, d)
                                    endDate = newCal.time
                                }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, ColorBorder)
                        ) {
                            Text("Hasta: ${sdf.format(endDate)}", color = ColorTextSecondary, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val updated = (challenge ?: GlobalChallenge()).copy(
                            name = name,
                            description = description,
                            scoreType = scoreType,
                            measurementUnit = scoreType,
                            sortOrder = sortOrder,
                            strategy = strategy,
                            isGlobal = onlyGlobal,
                            startDate = if (useDates) Timestamp(startDate) else null,
                            endDate = if (useDates) Timestamp(endDate) else null
                        )
                        onSave(updated)
                    } else { isNameError = true }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction)
            ) { Text("Guardar Desafío") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = ColorTextSecondary) }
        }
    )
}
