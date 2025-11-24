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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import java.util.Calendar
import java.util.Locale

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.75f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
private val ColorBackgroundGradientStart = Color(0xFF000000)
private val ColorBackgroundGradientEnd = Color(0xFF121212)
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

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            val formattedTime = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
            adminViewModel.addTimeToTemplate(formattedTime)
        },
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        Calendar.getInstance().get(Calendar.MINUTE),
        true
    )

    // --- UI STRUCTURE ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
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
                    onClick = { timePickerDialog.show() },
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
                    Text(
                        "No hay horarios configurados.\nPulsa + para añadir uno.",
                        color = ColorTextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        top = localPadding.calculateTopPadding() + 16.dp,
                        bottom = innerPadding.calculateBottomPadding() + 80.dp, // Espacio para FAB
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

    timeToDelete?.let { time ->
        AlertDialog(
            onDismissRequest = { timeToDelete = null },
            containerColor = ColorGlassSurface, // Usando superficie glass para el diálogo también
            title = { Text("Eliminar Horario", color = ColorTextPrimary) },
            text = { Text("¿Eliminar $time de la plantilla?", color = ColorTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.removeTimeFromTemplate(time)
                        timeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorError)
                ) { Text("Eliminar") }
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
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = time,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = ColorTextPrimary
            )
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, "Eliminar", tint = ColorError.copy(alpha = 0.8f))
            }
        }
    }
}