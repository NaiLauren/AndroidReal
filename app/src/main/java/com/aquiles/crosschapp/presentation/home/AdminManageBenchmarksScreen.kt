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
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.viewmodel.BenchmarkWodsState
import androidx.compose.foundation.layout.height
import com.aquiles.crosschapp.presentation.viewmodel.UserSession
import android.app.DatePickerDialog
import com.google.firebase.Timestamp
import androidx.compose.ui.platform.LocalContext
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.65f)
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
    adminViewModel: AdminViewModel = viewModel(),
    onlyGlobal: Boolean = false
) {
    val benchmarksState by adminViewModel.benchmarksState.collectAsState()
    val operationState by adminViewModel.benchmarkOperationState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDialog by remember { mutableStateOf(false) }
    var selectedBenchmark by remember { mutableStateOf<BenchmarkWod?>(null) }

    LaunchedEffect(Unit) { adminViewModel.loadBenchmarks() }

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
            // .background(Color.Black.copy(alpha = 0.4f)) // Removed for glass background
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    modifier = Modifier.height(72.dp),
                    title = { 
                        Text(
                            if (onlyGlobal) "Desafíos Globales" else "Benchmarks Locales", 
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
                when (val state = benchmarksState) {
                    is BenchmarkWodsState.Loading -> {
                        CircularProgressIndicator(color = ColorPrimaryAction, modifier = Modifier.align(Alignment.Center))
                    }
                    is BenchmarkWodsState.Success -> {
                        if (state.wods.isEmpty()) {
                            GlassCard(modifier = Modifier.align(Alignment.Center).padding(32.dp)) {
                                Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text(
                                        "No hay Benchmarks creados.",
                                        color = ColorTextSecondary
                                    )
                                }
                            }
                        } else {
                            val filteredWods = state.wods.filter { it.isGlobal == onlyGlobal }
                            
                            if (filteredWods.isEmpty()) {
                                GlassCard(modifier = Modifier.align(Alignment.Center).padding(32.dp)) {
                                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text(
                                            if (onlyGlobal) "No hay Desafíos Globales." else "No hay Benchmarks locales.",
                                            color = ColorTextSecondary
                                        )
                                    }
                                }
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
                                    items(filteredWods) { benchmark ->
                                        GlassBenchmarkCard(
                                            benchmark = benchmark,
                                            onEdit = { benchmarkToEdit ->
                                                selectedBenchmark = benchmarkToEdit
                                                showDialog = true
                                            },
                                            onDelete = { benchmarkToDelete -> adminViewModel.deleteBenchmark(benchmarkToDelete.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is BenchmarkWodsState.Error -> {
                        Text("Error: ${state.message}", color = ColorError, modifier = Modifier.align(Alignment.Center))
                    }
                    is BenchmarkWodsState.Idle -> {
                        // Estado inicial
                    }
                }
            }
        }
    }

    if (showDialog) {
        GlassBenchmarkDialog(
            benchmark = selectedBenchmark,
            onlyGlobal = onlyGlobal,
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
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Score: ${benchmark.scoreType}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorPrimaryAction,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    if (benchmark.isDesafio == true) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = ColorPrimaryAction.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "DESAFÍO",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorPrimaryAction,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (benchmark.isGlobal) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFFFFD700).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "GLOBAL",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFD700),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                if (benchmark.isDesafio == true && (benchmark.startDate != null || benchmark.endDate != null)) {
                    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    val start = benchmark.startDate?.toDate()?.let { sdf.format(it) } ?: "?"
                    val end = benchmark.endDate?.toDate()?.let { sdf.format(it) } ?: "?"
                    
                    Text(
                        text = "$start - $end",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorTextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
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
    onlyGlobal: Boolean = false,
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

    val isSuperAdmin by UserSession.isSuperAdmin.collectAsState()
    var isGlobal by remember { mutableStateOf(benchmark?.isGlobal ?: onlyGlobal) }
    var isDesafio by remember { mutableStateOf(benchmark?.isDesafio ?: onlyGlobal) }
    
    var useDates by remember { mutableStateOf(benchmark?.startDate != null || benchmark?.endDate != null) }
    var startDate by remember { mutableStateOf(benchmark?.startDate?.toDate() ?: Date()) }
    var endDate by remember { 
        mutableStateOf(benchmark?.endDate?.toDate() ?: Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000)) 
    }
    
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    var isNameError by remember { mutableStateOf(false) }
    var scoreTypeExpanded by remember { mutableStateOf(false) }
    val scoreTypes = remember { listOf("TIME", "REPS", "WEIGHT", "ROUNDS", "AMRAP") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorDialogSurface,
        titleContentColor = ColorTextPrimary,
        textContentColor = ColorTextSecondary,
        title = { 
            Text(
                if (benchmark == null) {
                    if (onlyGlobal) "Nuevo Desafío" else "Nuevo Benchmark"
                } else {
                    if (onlyGlobal) "Editar Desafío" else "Editar Benchmark"
                }, 
                fontWeight = FontWeight.Bold
            ) 
        },
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

                // 6. SELECTOR GLOBAL (SOLO EN MODO LOCAL Y SI ES SUPER ADMIN)
                
                if (isSuperAdmin && !onlyGlobal) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Convertir a Global",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ColorTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Hacer este benchmark visible para todos",
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorTextSecondary
                            )
                        }
                        Switch(
                            checked = isGlobal,
                            onCheckedChange = { isGlobal = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ColorPrimaryAction,
                                checkedTrackColor = ColorPrimaryAction.copy(alpha = 0.5f)
                            )
                        )
                    }
                } else if (onlyGlobal) {
                    // En modo global, forzamos que sea global siempre
                    LaunchedEffect(Unit) { isGlobal = true }
                    
                    Text(
                        "Tipo: Desafío de Comunidad (Global)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFD700), // Dorado
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // --- 7. CONFIGURACIÓN DEL DESAFÍO (isDesafio + FECHAS) ---
                Divider(color = ColorBorder, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Activar como Desafío",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorTextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Tendrá un periodo de validez y aparecerá en el carrusel de Hoy",
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorTextSecondary
                        )
                    }
                    Switch(
                        checked = isDesafio,
                        onCheckedChange = { isDesafio = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ColorPrimaryAction,
                            checkedTrackColor = ColorPrimaryAction.copy(alpha = 0.5f)
                        )
                    )
                }

                if (isDesafio) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Definir fechas de vigencia",
                            style = MaterialTheme.typography.bodySmall,
                            color = ColorTextSecondary
                        )
                        Checkbox(
                            checked = useDates,
                            onCheckedChange = { useDates = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = ColorPrimaryAction,
                                uncheckedColor = ColorBorder
                            )
                        )
                    }
                }

                if (isDesafio && useDates) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Fecha Inicio
                        OutlinedCard(
                            onClick = {
                                calendar.time = startDate
                                DatePickerDialog(context, { _, y, m, d ->
                                    calendar.set(y, m, d)
                                    startDate = calendar.time
                                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, ColorBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Inicia", style = MaterialTheme.typography.labelSmall, color = ColorTextSecondary)
                                Text(sdf.format(startDate), style = MaterialTheme.typography.bodyMedium, color = ColorTextPrimary)
                            }
                        }

                        // Fecha Fin
                        OutlinedCard(
                            onClick = {
                                calendar.time = endDate
                                DatePickerDialog(context, { _, y, m, d ->
                                    calendar.set(y, m, d)
                                    endDate = calendar.time
                                }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.outlinedCardColors(containerColor = Color.Transparent),
                            border = BorderStroke(1.dp, ColorBorder),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Finaliza", style = MaterialTheme.typography.labelSmall, color = ColorTextSecondary)
                                Text(sdf.format(endDate), style = MaterialTheme.typography.bodyMedium, color = ColorTextPrimary)
                            }
                        }
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
                            strategy = strategy.trim(),
                            isGlobal = isGlobal,
                            isDesafio = isDesafio,
                            startDate = if (isDesafio && useDates) Timestamp(startDate) else null,
                            endDate = if (isDesafio && useDates) Timestamp(endDate) else null
                        ) ?: BenchmarkWod(
                            name = name,
                            description = description,
                            scoreType = scoreType,
                            measurementUnit = scoreType,
                            sortOrder = sortOrder,
                            strategy = strategy.trim(),
                            isGlobal = isGlobal,
                            isDesafio = isDesafio,
                            startDate = if (isDesafio && useDates) Timestamp(startDate) else null,
                            endDate = if (isDesafio && useDates) Timestamp(endDate) else null
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