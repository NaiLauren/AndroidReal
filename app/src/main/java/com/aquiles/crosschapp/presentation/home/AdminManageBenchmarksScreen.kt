package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.data.model.BenchmarkWod
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import com.aquiles.crosschapp.presentation.viewmodel.BenchmarkOperationState
import com.aquiles.crosschapp.presentation.viewmodel.BenchmarkWodsState

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.75f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
private val ColorBackgroundGradientStart = Color(0xFF000000)
private val ColorBackgroundGradientEnd = Color(0xFF121212)
private val ColorDialogSurface = Color(0xFF1C1C1E)
private val ColorError = Color(0xFFEF5350)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageBenchmarksScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    adminViewModel: AdminViewModel = viewModel()
) {
    val benchmarkWodsState by adminViewModel.benchmarkWodsState.collectAsState()
    val operationState by adminViewModel.benchmarkOperationState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDialog by remember { mutableStateOf(false) }
    var selectedBenchmark by remember { mutableStateOf<BenchmarkWod?>(null) }

    LaunchedEffect(Unit) { adminViewModel.loadBenchmarkWods() }

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

    // --- UI STRUCTURE ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Benchmarks", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ColorTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        selectedBenchmark = null
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
                when (val state = benchmarkWodsState) {
                    is BenchmarkWodsState.Loading -> {
                        CircularProgressIndicator(color = ColorPrimaryAction, modifier = Modifier.align(Alignment.Center))
                    }
                    is BenchmarkWodsState.Success -> {
                        if (state.wods.isEmpty()) {
                            Text(
                                "No hay Benchmarks creados.",
                                modifier = Modifier.align(Alignment.Center),
                                color = ColorTextSecondary
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 16.dp,
                                    bottom = 80.dp // Espacio para FAB
                                ),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(state.wods) { benchmark ->
                                    GlassBenchmarkCard(
                                        benchmark = benchmark,
                                        onEdit = {
                                            selectedBenchmark = it
                                            showDialog = true
                                        },
                                        onDelete = { adminViewModel.deleteBenchmark(it.id) }
                                    )
                                }
                            }
                        }
                    }
                    is BenchmarkWodsState.Error -> {
                        Text("Error: ${state.message}", color = ColorError, modifier = Modifier.align(Alignment.Center))
                    }
                }
            }
        }
    }

    if (showDialog) {
        GlassBenchmarkDialog(
            benchmark = selectedBenchmark,
            onDismiss = { showDialog = false },
            onSave = { benchmarkToSave ->
                adminViewModel.saveBenchmark(benchmarkToSave)
                showDialog = false
            }
        )
    }
}

@Composable
private fun GlassBenchmarkCard(
    benchmark: BenchmarkWod,
    onEdit: (BenchmarkWod) -> Unit,
    onDelete: (BenchmarkWod) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ColorBorder),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = benchmark.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Score: ${benchmark.scoreType}",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorPrimaryAction,
                    fontWeight = FontWeight.SemiBold
                )
                if (benchmark.description.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = benchmark.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorTextSecondary,
                        maxLines = 2
                    )
                }
            }
            Column {
                IconButton(onClick = { onEdit(benchmark) }) {
                    Icon(Icons.Default.Edit, "Editar", tint = ColorPrimaryAction)
                }
                IconButton(onClick = { onDelete(benchmark) }) {
                    Icon(Icons.Default.Delete, "Eliminar", tint = ColorError.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassBenchmarkDialog(
    benchmark: BenchmarkWod?,
    onDismiss: () -> Unit,
    onSave: (BenchmarkWod) -> Unit
) {
    var name by remember { mutableStateOf(benchmark?.name ?: "") }
    var description by remember { mutableStateOf(benchmark?.description ?: "") }
    var scoreType by remember { mutableStateOf(benchmark?.scoreType?.takeIf { it.isNotBlank() } ?: "TIME") }
    var strategy by remember { mutableStateOf(benchmark?.strategy ?: "") }
    
    // Moved up for scope visibility
    var sortOrderExpanded by remember { mutableStateOf(false) }
    // Default logic: Time -> ASC (Menor es mejor), Weight/Reps -> DESC (Mayor es mejor)
    // Initialize based on existing benchmark or default by scoreType
    var sortOrder by remember { 
        mutableStateOf(benchmark?.sortOrder?.takeIf { it.isNotBlank() } ?: if (scoreType == "TIME") "ASC" else "DESC") 
    }
    
    // Auto-update sortOrder default when scoreType changes, ONLY if it's a fresh creation or user is changing types
    // We can use a side effect for this, or just let user change it manually. 
    // Let's force update default if user changes scoreType to help them.
    LaunchedEffect(scoreType) {
        if (benchmark == null) { // Only auto-switch for new benchmarks to avoid overriding edit
             sortOrder = if (scoreType == "TIME") "ASC" else "DESC"
        }
    }

    var isNameError by remember { mutableStateOf(false) }
    var scoreTypeExpanded by remember { mutableStateOf(false) }
    val scoreTypes = remember { listOf("TIME", "REPS", "WEIGHT", "ROUNDS", "AMRAP") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorDialogSurface,
        titleContentColor = ColorTextPrimary,
        textContentColor = ColorTextSecondary,
        title = { Text(if (benchmark == null) "Nuevo Benchmark" else "Editar Benchmark", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // 1. CAMPO NOMBRE
                // USAMOS EL NUEVO NOMBRE DE LA FUNCIÓN 'BenchmarkTextField'
                BenchmarkTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isNameError = it.isBlank()
                    },
                    label = "Nombre *"
                )
                if (isNameError) {
                    Text("El nombre es obligatorio", color = ColorError, style = MaterialTheme.typography.labelSmall)
                }

                // 2. CAMPO DESCRIPCIÓN
                BenchmarkTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Descripción (21-15-9...)",
                    singleLine = false
                )

                // 3. CAMPO ESTRATEGIA
                BenchmarkTextField(
                    value = strategy,
                    onValueChange = { strategy = it },
                    label = "Estrategia (Opcional)"
                )

                // 4. DROPDOWN UNIDAD DE MEDIDA (Antes scoreType)
                ExposedDropdownMenuBox(
                    expanded = scoreTypeExpanded,
                    onExpandedChange = { scoreTypeExpanded = !scoreTypeExpanded }
                ) {
                    OutlinedTextField(
                        value = scoreType, // Mantenemos variable scoreType para visualización
                        onValueChange = {},
                        label = { Text("¿Qué se mide? (Unidad)", color = ColorTextSecondary) },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = scoreTypeExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorPrimaryAction,
                            unfocusedBorderColor = ColorBorder,
                            focusedTextColor = ColorTextPrimary,
                            unfocusedTextColor = ColorTextPrimary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = scoreTypeExpanded,
                        onDismissRequest = { scoreTypeExpanded = false },
                        modifier = Modifier.background(ColorDialogSurface)
                    ) {
                        // TIME, WEIGHT, REPS, DISTANCE, PERCENTAGE
                        listOf("TIME", "WEIGHT", "REPS", "DISTANCE", "PERCENTAGE").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type, color = ColorTextPrimary) },
                                onClick = { scoreType = type; scoreTypeExpanded = false }
                            )
                        }
                    }
                }
                
                // 5. DROPDOWN ORDENAMIENTO (Smart Ranking)
                // sortOrder variables moved to top scope

                ExposedDropdownMenuBox(
                    expanded = sortOrderExpanded,
                    onExpandedChange = { sortOrderExpanded = !sortOrderExpanded }
                ) {
                    OutlinedTextField(
                        value = if (sortOrder == "ASC") "Menor es mejor (Tiempo, Carreras)" else "Mayor es mejor (Peso, Reps)",
                        onValueChange = {},
                        label = { Text("Criterio de Ranking", color = ColorTextSecondary) },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortOrderExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorPrimaryAction,
                            unfocusedBorderColor = ColorBorder,
                            focusedTextColor = ColorTextPrimary,
                            unfocusedTextColor = ColorTextPrimary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = sortOrderExpanded,
                        onDismissRequest = { sortOrderExpanded = false },
                        modifier = Modifier.background(ColorDialogSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Mayor es mejor (Peso, Reps, Distancia)", color = ColorTextPrimary) },
                            onClick = { sortOrder = "DESC"; sortOrderExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Menor es mejor (Tiempo rápido)", color = ColorTextPrimary) },
                            onClick = { sortOrder = "ASC"; sortOrderExpanded = false }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val newOrUpdatedBenchmark = benchmark?.copy(
                            name = name,
                            description = description,
                            scoreType = scoreType, // Lo mantenemos por legacy
                            measurementUnit = scoreType, // Nuevo campo
                            sortOrder = sortOrder,
                            strategy = strategy.trim()
                        ) ?: BenchmarkWod(
                            name = name,
                            description = description,
                            scoreType = scoreType,
                            measurementUnit = scoreType,
                            sortOrder = sortOrder,
                            strategy = strategy.trim()
                        )
                        onSave(newOrUpdatedBenchmark)
                    } else { isNameError = true }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction)
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = ColorTextSecondary) }
        }
    )
}

// --- RENOMBRADA PARA EVITAR CONFLICTOS ---
@Composable
private fun BenchmarkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = ColorTextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = singleLine,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ColorPrimaryAction,
            unfocusedBorderColor = ColorBorder,
            focusedTextColor = ColorTextPrimary,
            unfocusedTextColor = ColorTextPrimary,
            cursorColor = ColorPrimaryAction,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp)
    )
}