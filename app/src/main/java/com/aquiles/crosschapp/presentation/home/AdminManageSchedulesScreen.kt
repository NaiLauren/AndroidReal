package com.aquiles.crosschapp.presentation.home

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import java.util.Calendar
import java.util.Locale

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.85f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.15f)
private val ColorError = Color(0xFFEF5350)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageSchedulesScreen(
    navController: NavController,
    adminViewModel: AdminViewModel,
    innerPadding: PaddingValues
) {
    val context = LocalContext.current
    val scheduleTemplate by adminViewModel.scheduleTemplateState.collectAsState()
    val operationMessage by adminViewModel.scheduleOperationState.collectAsState()

    var timeToDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        adminViewModel.loadScheduleTemplate()
    }

    LaunchedEffect(operationMessage) {
        operationMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            adminViewModel.clearScheduleOperationMessage()
        }
    }

    // --- UI STRUCTURE ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Fondo sólido negro para evitar problemas visuales
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Plantilla Horaria", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
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
                        // Crear el dialogo AQUÍ dentro del onClick para evitar crash de contexto
                        val cal = Calendar.getInstance()
                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                val formattedTime = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                                adminViewModel.addTimeToTemplate(formattedTime)
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE),
                            true
                        ).show()
                    },
                    containerColor = ColorPrimaryAction,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, "Añadir Horario")
                }
            },
            containerColor = Color.Transparent
        ) { localPadding ->
            if (scheduleTemplate.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(localPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AccessTime, null, tint = ColorTextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No hay horarios base.\nPulsa + para añadir horas fijas.",
                            color = ColorTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = localPadding.calculateTopPadding() + 16.dp,
                        bottom = innerPadding.calculateBottomPadding() + 80.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(scheduleTemplate) { time ->
                        ScheduleTemplateItemGlass(
                            time = time,
                            onDeleteClick = { timeToDelete = time }
                        )
                    }
                }
            }
        }
    }

    // Dialogo de confirmación Glass
    timeToDelete?.let { time ->
        AlertDialog(
            onDismissRequest = { timeToDelete = null },
            containerColor = Color(0xFF1C1C1E),
            title = { Text("Eliminar Horario", color = ColorTextPrimary) },
            text = { Text("¿Eliminar $time de la plantilla?", color = ColorTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.removeTimeFromTemplate(time)
                        timeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorError)
                ) { Text("Eliminar", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { timeToDelete = null }) {
                    Text("Cancelar", color = ColorTextSecondary)
                }
            }
        )
    }
}

@Composable
fun ScheduleTemplateItemGlass(
    time: String,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ColorBorder),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, null, tint = ColorPrimaryAction, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    text = time,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, "Eliminar", tint = ColorError.copy(alpha = 0.8f))
            }
        }
    }
}