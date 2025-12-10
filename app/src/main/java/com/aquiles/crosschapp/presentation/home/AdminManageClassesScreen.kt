package com.aquiles.crosschapp.presentation.home

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.data.model.GymClass
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import com.aquiles.crosschapp.presentation.viewmodel.ClassListState
import com.aquiles.crosschapp.presentation.viewmodel.ClassOperationState
import java.text.SimpleDateFormat
import java.util.*

// --- CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.85f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.15f)
private val ColorError = Color(0xFFEF5350)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageClassesScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    adminViewModel: AdminViewModel = viewModel(),
    onNavigateToCreateClass: () -> Unit,
    onNavigateToEditClass: (classId: String) -> Unit,
    onNavigateToClassDetails: (classId: String) -> Unit
) {
    val classListState by adminViewModel.classListState.collectAsState()
    val operationState by adminViewModel.classOperationState.collectAsState()
    val context = LocalContext.current
    var classToDelete by remember { mutableStateOf<GymClass?>(null) }

    LaunchedEffect(key1 = Unit) {
        adminViewModel.loadFutureClasses()
    }

    LaunchedEffect(operationState) {
        when (val state = operationState) {
            is ClassOperationState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                adminViewModel.resetClassOperationState()
            }
            is ClassOperationState.Error -> {
                // Solo mostramos el Toast, NO cerramos la pantalla
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                adminViewModel.resetClassOperationState()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Gestionar Clases", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = ColorTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNavigateToCreateClass,
                    containerColor = ColorPrimaryAction,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Añadir Clase")
                }
            },
            containerColor = Color.Transparent
        ) { localScaffoldPadding ->
            when (val state = classListState) {
                is ClassListState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ColorPrimaryAction) }

                is ClassListState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ErrorOutline, null, tint = ColorError)
                            Text("Error: ${state.message}", color = ColorTextSecondary, modifier = Modifier.padding(16.dp))
                            Button(onClick = { adminViewModel.loadFutureClasses() }) { Text("Reintentar") }
                        }
                    }
                }

                is ClassListState.Success -> {
                    if (state.classes.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No hay clases futuras programadas.", color = ColorTextSecondary)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = localScaffoldPadding.calculateTopPadding() + 16.dp,
                                bottom = innerPadding.calculateBottomPadding() + 80.dp,
                                start = 16.dp,
                                end = 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.classes, key = { it.id }) { gymClass ->
                                ClassManagementItemGlass(
                                    gymClass = gymClass,
                                    onDeleteClick = { classToDelete = it },
                                    onEditClick = { onNavigateToEditClass(gymClass.id) },
                                    onDetailsClick = { onNavigateToClassDetails(gymClass.id) }
                                )
                            }
                        }
                    }
                }
                is ClassListState.Idle -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Cargando...", color = ColorTextSecondary) }
            }
        }
    }

    classToDelete?.let { gymClass ->
        AlertDialog(
            onDismissRequest = { classToDelete = null },
            containerColor = Color(0xFF1C1C1E),
            title = { Text("Eliminar Clase", color = ColorTextPrimary) },
            text = {
                Text(
                    "¿Eliminar '${gymClass.name}'?\nEsta acción es irreversible y cancelará las reservas.",
                    color = ColorTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = { adminViewModel.deleteClass(gymClass.id); classToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorError)
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { classToDelete = null }) {
                    Text("Cancelar", color = ColorTextSecondary)
                }
            }
        )
    }
}

@Composable
fun ClassManagementItemGlass(
    gymClass: GymClass,
    onDeleteClick: (GymClass) -> Unit,
    onEditClick: (GymClass) -> Unit,
    onDetailsClick: (GymClass) -> Unit
) {
    val dateFormat = SimpleDateFormat("EEE dd, HH:mm", Locale("es", "ES"))
    val classDate = gymClass.dateTime?.let { dateFormat.format(it) }?.uppercase() ?: "N/A"
    val occupancy = "${gymClass.enrolledUserIds.size}/${gymClass.maxCapacity}"

    // Determinar color del borde según el tipo o color de la clase
    val customColor = try { Color(android.graphics.Color.parseColor(gymClass.hexColor)) } catch(e: Exception) { ColorPrimaryAction }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, customColor.copy(alpha = 0.3f)),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        gymClass.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(classDate, style = MaterialTheme.typography.bodySmall, color = customColor, fontWeight = FontWeight.Bold)
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Group, null, tint = ColorTextSecondary, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(occupancy, style = MaterialTheme.typography.labelSmall, color = ColorTextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, null, tint = ColorTextSecondary, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Coach: ${gymClass.coachName}", style = MaterialTheme.typography.bodyMedium, color = ColorTextSecondary)
            }
        }

        HorizontalDivider(color = ColorBorder)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = { onDetailsClick(gymClass) }) {
                Text("Detalles", color = ColorTextPrimary)
            }
            IconButton(onClick = { onEditClick(gymClass) }) {
                Icon(Icons.Default.Edit, "Editar", tint = ColorPrimaryAction)
            }
            IconButton(onClick = { onDeleteClick(gymClass) }) {
                Icon(Icons.Default.Delete, "Eliminar", tint = ColorError.copy(alpha = 0.8f))
            }
        }
    }
}