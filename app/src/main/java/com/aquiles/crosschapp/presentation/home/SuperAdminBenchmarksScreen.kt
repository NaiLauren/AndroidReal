package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.data.model.BenchmarkWod
import com.aquiles.crosschapp.data.model.Gym
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.viewmodel.SuperAdminBenchmarksViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminBenchmarksScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: SuperAdminBenchmarksViewModel = viewModel()
) {
    val globalBenchmarks by viewModel.globalBenchmarks.collectAsState()
    val allGyms by viewModel.allGyms.collectAsState()
    val loadingState by viewModel.loadingState.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var benchmarkToEdit: BenchmarkWod? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        viewModel.loadGlobalBenchmarks()
        viewModel.loadAllGyms()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(72.dp),
                title = {
                    Column {
                        Text(
                            "Benchmarks Globales",
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                        Text(
                            "Gestión Multi-Gym",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        benchmarkToEdit = null
                        showCreateDialog = true
                    }) {
                        Icon(Icons.Default.Add, "Crear", tint = Color(0xFFFC5200))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f)
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        when {
            loadingState is SuperAdminBenchmarksViewModel.LoadingState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFFC5200))
                }
            }
            loadingState is SuperAdminBenchmarksViewModel.LoadingState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        (loadingState as SuperAdminBenchmarksViewModel.LoadingState.Error).message,
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            globalBenchmarks.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("📊", fontSize = 50.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No hay benchmarks globales",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Toca + para crear uno",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(globalBenchmarks) { benchmark ->
                        GlobalBenchmarkCard(
                            benchmark = benchmark,
                            allGyms = allGyms,
                            onEdit = {
                                benchmarkToEdit = benchmark
                                showCreateDialog = true
                            },
                            onDelete = {
                                viewModel.deleteBenchmark(benchmark)
                            }
                        )
                    }
                }
            }
        }
    }

    // Create/Edit Dialog
    if (showCreateDialog) {
        BenchmarkFormDialog(
            viewModel = viewModel,
            benchmark = benchmarkToEdit,
            allGyms = allGyms,
            onDismiss = {
                showCreateDialog = false
                benchmarkToEdit = null
            }
        )
    }
}

@Composable
private fun GlobalBenchmarkCard(
    benchmark: BenchmarkWod,
    allGyms: List<Gym>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            benchmark.name,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )

                        if (benchmark.isDesafio == true) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when {
                                    benchmark.startDate != null && benchmark.startDate!!.toDate() > Date() -> "PRÓXIMAMENTE"
                                    benchmark.endDate != null && benchmark.endDate!!.toDate() < Date() -> "FINALIZADA"
                                    else -> "ACTIVA"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    benchmark.startDate != null && benchmark.startDate!!.toDate() > Date() -> Color.Yellow
                                    benchmark.endDate != null && benchmark.endDate!!.toDate() < Date() -> Color.Gray
                                    else -> Color.Green
                                },
                                modifier = Modifier
                                    .background(
                                        when {
                                            benchmark.startDate != null && benchmark.startDate!!.toDate() > Date() -> Color.Yellow.copy(alpha = 0.2f)
                                            benchmark.endDate != null && benchmark.endDate!!.toDate() < Date() -> Color.Gray.copy(alpha = 0.2f)
                                            else -> Color.Green.copy(alpha = 0.2f)
                                        },
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        benchmark.scoreType,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFC5200),
                        modifier = Modifier
                            .background(
                                Color(0xFFFC5200).copy(alpha = 0.2f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Menu
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Opciones",
                            tint = Color.Gray
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            onClick = {
                                onEdit()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar", color = Color.Red) },
                            onClick = {
                                showDeleteConfirm = true
                                showMenu = false
                            }
                        )
                    }
                }
            }

            // Description
            if (benchmark.description.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    benchmark.description,
                    fontSize = 14.sp,
                    color = Color.Gray,
                    maxLines = 2
                )
            }

            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color.Gray.copy(alpha = 0.3f)
            )

            // Gyms
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🏢", fontSize = 16.sp)

                Column {
                    if (benchmark.allowedGymIds.isNullOrEmpty()) {
                        Text(
                            "Disponible para TODOS los gyms",
                            fontSize = 12.sp,
                            color = Color.Green
                        )
                    } else {
                        Text(
                            "${benchmark.allowedGymIds.size} gyms seleccionados",
                            fontSize = 12.sp,
                            color = Color.Blue
                        )

                        val gymNames = benchmark.allowedGymIds.take(3)
                            .mapNotNull { id -> allGyms.find { it.id == id }?.name }
                            .joinToString(", ")

                        if (gymNames.isNotEmpty()) {
                            Text(
                                gymNames + if (benchmark.allowedGymIds.size > 3) "..." else "",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            // Dates (if challenge)
            if (benchmark.isDesafio == true && benchmark.startDate != null && benchmark.endDate != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color.Gray.copy(alpha = 0.3f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text(
                            "Inicio",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(
                            dateFormatter.format(benchmark.startDate!!),
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }

                    Column {
                        Text(
                            "Fin",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(
                            dateFormatter.format(benchmark.endDate!!),
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar Benchmark") },
            text = {
                Text("¿Estás seguro de que deseas eliminar '${benchmark.name}'? Esta acción no se puede deshacer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BenchmarkFormDialog(
    viewModel: SuperAdminBenchmarksViewModel,
    benchmark: BenchmarkWod?,
    allGyms: List<Gym>,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(benchmark?.name ?: "") }
    var description by remember { mutableStateOf(benchmark?.description ?: "") }
    var scoreType by remember { mutableStateOf(benchmark?.scoreType ?: "TIME") }
    var strategy by remember { mutableStateOf(benchmark?.strategy ?: "") }
    var isDesafio by remember { mutableStateOf(benchmark?.isDesafio ?: false) }
    var useDates by remember { mutableStateOf(benchmark?.startDate != null || benchmark?.endDate != null) }
    var startDate by remember { mutableStateOf(benchmark?.startDate?.toDate() ?: Date()) }
    var endDate by remember { mutableStateOf(benchmark?.endDate?.toDate() ?: Date()) }
    var allGymsSelected by remember { mutableStateOf(benchmark?.allowedGymIds.isNullOrEmpty()) }
    var selectedGymIds by remember { mutableStateOf(benchmark?.allowedGymIds?.toSet() ?: emptySet()) }

    val scoreTypes = listOf("TIME", "WEIGHT", "REPS", "ROUNDS", "POINTS", "DISTANCE")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (benchmark == null) "Nuevo Benchmark" else "Editar Benchmark") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    placeholder = { Text("ej: Fran") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                // Score Type Dropdown
                var expandedScoreType by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedScoreType,
                    onExpandedChange = { expandedScoreType = !expandedScoreType }
                ) {
                    OutlinedTextField(
                        value = scoreType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de Puntaje") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedScoreType) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    ExposedDropdownMenu(
                        expanded = expandedScoreType,
                        onDismissRequest = { expandedScoreType = false }
                    ) {
                        scoreTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    scoreType = type
                                    expandedScoreType = false
                                }
                            )
                        }
                    }
                }

                // Desafio toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Desafío Global Temporal", fontSize = 14.sp)
                    Switch(
                        checked = isDesafio,
                        onCheckedChange = { isDesafio = it }
                    )
                }

                // All Gyms toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Todos los Gyms", fontSize = 14.sp, color = Color.Green)
                    Switch(
                        checked = allGymsSelected,
                        onCheckedChange = {
                            allGymsSelected = it
                            if (it) selectedGymIds = emptySet()
                        }
                    )
                }

                Text(
                    "Formulario simplificado. Más opciones disponibles en versión completa.",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.saveBenchmark(
                        id = benchmark?.id,
                        name = name,
                        description = description,
                        scoreType = scoreType,
                        strategy = strategy,
                        isDesafio = isDesafio,
                        startDate = if (isDesafio && useDates) com.google.firebase.Timestamp(startDate) else null,
                        endDate = if (isDesafio && useDates) com.google.firebase.Timestamp(endDate) else null,
                        selectedGymIds = if (allGymsSelected) emptyList() else selectedGymIds.toList()
                    )
                    onDismiss()
                },
                enabled = name.isNotEmpty()
            ) {
                Text("Guardar", color = Color(0xFFFC5200))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}
