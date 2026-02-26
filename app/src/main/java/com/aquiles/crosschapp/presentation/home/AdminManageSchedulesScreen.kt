package com.aquiles.crosschapp.presentation.home

import com.aquiles.crosschapp.presentation.components.GlassCard
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import com.aquiles.crosschapp.presentation.viewmodel.UserSession
import java.util.Calendar
import java.util.Locale

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.45f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.15f)
private val ColorError = Color(0xFFEF5350)

// Info Row Component for Dialog
@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = ColorTextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = ColorTextPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageSchedulesScreen(
    navController: NavController,
    adminViewModel: AdminViewModel,
    innerPadding: PaddingValues,
    setupStepKey: String? = null
) {
    val context = LocalContext.current
    
    var showSetupPopup by remember { mutableStateOf(setupStepKey != null) }
    
    if (showSetupPopup) {
        SetupStep.values().find { it.key == setupStepKey }?.let { step ->
            AlertDialog(
                onDismissRequest = { showSetupPopup = false },
                title = { Text(step.title, color = ColorTextPrimary) },
                text = { Text(step.description, color = ColorTextSecondary) },
                confirmButton = {
                    Button(onClick = { showSetupPopup = false }, colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction)) { 
                        Text("Entendido", color = Color.White) 
                    }
                },
                containerColor = ColorGlassSurface
            )
        }
    }
    
    // Observe Weekly Schedule
    val weeklySchedule by adminViewModel.weeklyScheduleState.collectAsState()
    val gymOperatingHours by adminViewModel.gymOperatingHoursState.collectAsState()
    val operationMessage by adminViewModel.scheduleOperationState.collectAsState()
    
    // Get gym and its primary color
    val currentGym by UserSession.currentGym.collectAsState()
    val gymPrimaryColor = remember(currentGym?.primaryColor) {
        try {
            Color(android.graphics.Color.parseColor(currentGym?.primaryColor ?: "#FC5200"))
        } catch (e: Exception) {
            Color(0xFFFC5200)
        }
    }

    // 1 = Domingo, 2 = Lunes, ... 7 = Sábado (Calendar.SUNDAY is 1)
    // Vamos a iniciar en Lunes (2) por usabilidad
    var selectedDay by remember { mutableStateOf(2) } 
    var timeToDelete by remember { mutableStateOf<AdminViewModel.TemplateSlot?>(null) }
    
    // --- DATE SELECTOR STATE ---
    var showDateRangeSelector by remember { mutableStateOf(false) }
    val selectedStartDate = remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }) }
    val selectedEndDate = remember { mutableStateOf(Calendar.getInstance().apply { add(Calendar.WEEK_OF_YEAR, 4) }) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        adminViewModel.loadWeeklyScheduleTemplate()
        adminViewModel.loadGymOperatingHours()
    }

    LaunchedEffect(operationMessage) {
        operationMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            adminViewModel.clearScheduleOperationMessage()
        }
    }

    // Get times for selected day
    val timesForDay = weeklySchedule[selectedDay] ?: emptyList()
    val rangesForDay = gymOperatingHours[selectedDay]?.ranges ?: emptyList()
    
    // Rango Dialog State
    var showRangeDialog by remember { mutableStateOf(false) }
    var rangeToDelete by remember { mutableStateOf<AdminViewModel.TimeRange?>(null) }
    
    // Persistent Range Dialog State
    var localStart by remember { mutableStateOf("08:00") }
    var localEnd by remember { mutableStateOf("22:00") }
    
    // Helper para mostrar picker de hora regular (Clases)
    var lastClassHour by remember { mutableStateOf(18) }
    var lastClassMinute by remember { mutableStateOf(0) }
    val showTimePicker = {
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                lastClassHour = hourOfDay
                lastClassMinute = minute
                val formattedTime = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                adminViewModel.saveTimeForDay(selectedDay, formattedTime, false)
            },
            lastClassHour,
            lastClassMinute,
            true
        ).show()
    }

    // --- UI STRUCTURE ---
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Plantilla Semanal", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ColorTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f))
                )
            },
            floatingActionButton = {}, // Removed FAB, we'll use bottom bar
            containerColor = Color.Transparent
        ) { localPadding ->
            
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = localPadding.calculateTopPadding())
                ) {
                    // DAY SELECTOR
                    DaySelector(
                        selectedDay = selectedDay,
                        onDaySelected = { selectedDay = it },
                        primaryColor = gymPrimaryColor
                    )
                    
                    Spacer(Modifier.height(16.dp))

                    if (timesForDay.isEmpty() && rangesForDay.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.AccessTime, null, tint = ColorTextSecondary, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "No hay horarios para el ${getDayNameFull(selectedDay)}.\nPulsa abajo para añadir.",
                                    color = ColorTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(
                                bottom = 180.dp,
                                start = 16.dp,
                                end = 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (rangesForDay.isNotEmpty()) {
                                item {
                                    Text("Horario de Apertura Libre", style = MaterialTheme.typography.labelMedium, color = Color(0xFF42A5F5), modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                }
                                items(rangesForDay) { range ->
                                    GlassCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.AccessTime, null, tint = Color(0xFF42A5F5), modifier = Modifier.size(24.dp))
                                                Spacer(Modifier.width(16.dp))
                                                Text("De ${range.startTime} a ${range.endTime}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
                                            }
                                            IconButton(onClick = { rangeToDelete = range }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = ColorError)
                                            }
                                        }
                                    }
                                }
                            }
                            
                            if (timesForDay.isNotEmpty()) {
                                item {
                                    Text("Clases Guiadas", style = MaterialTheme.typography.labelMedium, color = gymPrimaryColor, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                                }
                                items(timesForDay) { slot ->
                                    ScheduleTemplateItemGlass(
                                        slot = slot,
                                        onDeleteClick = { timeToDelete = slot },
                                        primaryColor = gymPrimaryColor
                                    )
                                }
                            }
                        }
                    }
                }
                
                // BOTTOM ACTION BAR with Glass Effect
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = innerPadding.calculateBottomPadding()), // Respect system navigation
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Generate Month Button (Left)
                        Button(
                            onClick = { showDateRangeSelector = true }, 
                            colors = ButtonDefaults.buttonColors(containerColor = gymPrimaryColor),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Text("Generar Mes", fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        
                        Spacer(Modifier.width(8.dp))
                        
                        // Add Class Button
                        Button(
                            onClick = { showTimePicker() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text("+ Clase", fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                        
                        Spacer(Modifier.width(8.dp))
                        
                        // Add Open Gym Range Button
                        Button(
                            onClick = { showRangeDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A5F5).copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text("+ Libre", fontWeight = FontWeight.Bold, color = Color(0xFF42A5F5), maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    // --- DATE RANGE SELECTOR DIALOG ---
    if (showDateRangeSelector) {
        val dateFormatter = remember { java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
        
        AlertDialog(
            onDismissRequest = { showDateRangeSelector = false },
            containerColor = Color(0xFF1C1C1E).copy(alpha = 0.70f).copy(alpha = 0.70f),
            title = { Text("Seleccionar Período", color = ColorTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Elige el rango de fechas para generar clases:", color = ColorTextSecondary)
                    Spacer(Modifier.height(16.dp))
                    
                    // Start Date Button
                    OutlinedButton(
                        onClick = { showStartDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorTextPrimary),
                        border = BorderStroke(1.dp, gymPrimaryColor)
                    ) {
                        Icon(Icons.Default.DateRange, null, tint = gymPrimaryColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Desde: ${dateFormatter.format(selectedStartDate.value.time)}")
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    // End Date Button
                    OutlinedButton(
                        onClick = { showEndDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorTextPrimary),
                        border = BorderStroke(1.dp, gymPrimaryColor)
                    ) {
                        Icon(Icons.Default.DateRange, null, tint = gymPrimaryColor, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Hasta: ${dateFormatter.format(selectedEndDate.value.time)}")
                    }
                    
                    // Validation Warning
                    if (selectedEndDate.value.time.before(selectedStartDate.value.time) || 
                        selectedEndDate.value.time == selectedStartDate.value.time) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "⚠️ La fecha fin debe ser posterior",
                            color = ColorError,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.previewMonthGeneration(
                            selectedStartDate.value.time,
                            selectedEndDate.value.time
                        )
                        showDateRangeSelector = false
                    },
                    enabled = selectedEndDate.value.time.after(selectedStartDate.value.time),
                    colors = ButtonDefaults.buttonColors(containerColor = gymPrimaryColor)
                ) {
                    Text("Continuar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangeSelector = false }) {
                    Text("Cancelar", color = ColorTextSecondary)
                }
            }
        )
    }
    
    // DatePicker for Start Date
    if (showStartDatePicker) {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedStartDate.value.set(year, month, dayOfMonth)
                showStartDatePicker = false
            },
            selectedStartDate.value.get(Calendar.YEAR),
            selectedStartDate.value.get(Calendar.MONTH),
            selectedStartDate.value.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnCancelListener { showStartDatePicker = false }
            show()
        }
    }
    
    // DatePicker for End Date
    if (showEndDatePicker) {
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedEndDate.value.set(year, month, dayOfMonth)
                showEndDatePicker = false
            },
            selectedEndDate.value.get(Calendar.YEAR),
            selectedEndDate.value.get(Calendar.MONTH),
            selectedEndDate.value.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnCancelListener { showEndDatePicker = false }
            show()
        }
    }

    // Dialogos de confirmación / ingreso
    timeToDelete?.let { slot ->
        AlertDialog(
            onDismissRequest = { timeToDelete = null },
            containerColor = Color(0xFF1C1C1E).copy(alpha = 0.70f).copy(alpha = 0.70f),
            title = { Text("Eliminar Horario", color = ColorTextPrimary) },
            text = { Text("¿Eliminar ${slot.time} del ${getDayNameFull(selectedDay)}?", color = ColorTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.removeTimeFromDay(selectedDay, slot)
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

    rangeToDelete?.let { range ->
        AlertDialog(
            onDismissRequest = { rangeToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ColorError) },
            title = { Text("Eliminar bloque libre") },
            text = { Text("¿Seguro que deseas eliminar el horario de ${range.startTime} a ${range.endTime}?", color = ColorTextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newRanges = rangesForDay.filter { it != range }
                        adminViewModel.saveGymOperatingHours(selectedDay, newRanges)
                        rangeToDelete = null
                    }
                ) {
                    Text("Eliminar", color = ColorError)
                }
            },
            dismissButton = {
                TextButton(onClick = { rangeToDelete = null }) { Text("Cancelar", color = ColorTextPrimary) }
            },
            containerColor = Color(0xFF1C1C1E).copy(alpha = 0.70f).copy(alpha = 0.70f),
            titleContentColor = ColorTextPrimary,
            textContentColor = ColorTextSecondary
        )
    }
    
    // --- ADD OPEN GYM RANGE DIALOG ---
    if (showRangeDialog) {
        
        AlertDialog(
            onDismissRequest = { showRangeDialog = false },
            containerColor = Color(0xFF1C1C1E).copy(alpha = 0.70f).copy(alpha = 0.70f),
            title = { Text("Nuevo Bloque Libre", color = ColorTextPrimary) },
            text = {
                Column {
                    Text("Define a qué hora abre y cierra el gimnasio para entrenamientos libres.", color = ColorTextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Apertura:", color = ColorTextPrimary)
                        OutlinedButton(
                            onClick = {
                                val parts = localStart.split(":")
                                val initialH = parts.getOrNull(0)?.toIntOrNull() ?: 8
                                val initialM = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                TimePickerDialog(context, { _, h, m -> localStart = String.format(Locale.US, "%02d:%02d", h, m) }, initialH, initialM, true).show()
                            },
                        ) {
                            Text(localStart, color = ColorTextPrimary)
                        }
                    }
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Cierre:", color = ColorTextPrimary)
                        OutlinedButton(
                            onClick = {
                                val parts = localEnd.split(":")
                                val initialH = parts.getOrNull(0)?.toIntOrNull() ?: 22
                                val initialM = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                TimePickerDialog(context, { _, h, m -> localEnd = String.format(Locale.US, "%02d:%02d", h, m) }, initialH, initialM, true).show()
                            },
                        ) {
                            Text(localEnd, color = ColorTextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedRanges = rangesForDay + AdminViewModel.TimeRange(localStart, localEnd)
                        adminViewModel.saveGymOperatingHours(selectedDay, updatedRanges)
                        showRangeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A5F5))
                ) {
                    Text("Añadir", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRangeDialog = false }) {
                    Text("Cancelar", color = ColorTextPrimary)
                }
            }
        )
    }

    // --- GENERATION DIALOGS ---
    val generationPreview by adminViewModel.generationPreviewState.collectAsState()
    val generationStatus by adminViewModel.generationStatusState.collectAsState()

    generationPreview?.let { preview ->
        // Calculate secondary color (darker shade of primary)
        val secondaryColor = remember(gymPrimaryColor) {
            Color(
                red = gymPrimaryColor.red * 0.7f,
                green = gymPrimaryColor.green * 0.7f,
                blue = gymPrimaryColor.blue * 0.7f,
                alpha = 1f
            )
        }
        
        val dateFormatter = remember { java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
        
        Dialog(onDismissRequest = { adminViewModel.clearGenerationState() }) {
            GlassCard(
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // HEADER
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = gymPrimaryColor,
                            modifier = Modifier.size(32.dp)
                        )
                        Text(
                            "Vista Previa de Generación",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ColorTextPrimary
                        )
                    }
                    
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    
                    // PREVIEW INFO
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Period
                        InfoRow(
                            icon = Icons.Default.DateRange,
                            label = "Período",
                            value = "${dateFormatter.format(preview.startDate)} - ${dateFormatter.format(preview.endDate)}",
                            color = gymPrimaryColor
                        )
                        
                        // Total classes
                        InfoRow(
                            icon = Icons.Default.Add,
                            label = "Total de clases a crear",
                            value = "${preview.totalNewClasses}",
                            color = gymPrimaryColor
                        )
                        
                        // Warning if exists
                        if (preview.existingClassesCount > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = Color(0xFFEF5350).copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("⚠️", fontSize = 20.sp)
                                    Text(
                                        "Ya hay ${preview.existingClassesCount} clases en este período",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ColorTextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // BUTTONS
                    if (preview.existingClassesCount > 0) {
                        // Two button layout
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    adminViewModel.executeGeneration(AdminViewModel.GenerationMode.APPEND)
                                    adminViewModel.clearGenerationState()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = gymPrimaryColor),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Agregar (Mantener Existentes)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            
                            Button(
                                onClick = {
                                    adminViewModel.executeGeneration(AdminViewModel.GenerationMode.OVERWRITE)
                                    adminViewModel.clearGenerationState()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = secondaryColor),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Sobrescribir (Borrar Existentes)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }
                            
                            TextButton(
                                onClick = { adminViewModel.clearGenerationState() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancelar", color = ColorTextSecondary, fontWeight = FontWeight.Medium)
                            }
                        }
                    } else {
                        // Single button layout
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = {
                                    adminViewModel.executeGeneration(AdminViewModel.GenerationMode.APPEND)
                                    adminViewModel.clearGenerationState()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = gymPrimaryColor),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Confirmar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            
                            TextButton(
                                onClick = { adminViewModel.clearGenerationState() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancelar", color = ColorTextSecondary, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }

    // Status Toast
    LaunchedEffect(generationStatus) {
        generationStatus?.let { status ->
            if (status != "Calculando..." && status != "Generando...") {
                Toast.makeText(context, status, Toast.LENGTH_LONG).show()
                adminViewModel.clearGenerationState()
            }
        }
    }
}

@Composable
fun DaySelector(selectedDay: Int, onDaySelected: (Int) -> Unit, primaryColor: Color) {
    // 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri, 7=Sat, 1=Sun
    val days = listOf(2, 3, 4, 5, 6, 7, 1)
    
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEach { day ->
                val isSelected = selectedDay == day
                val label = getDayInitial(day)
                
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) primaryColor else Color.Transparent)
                        .clickable { onDaySelected(day) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else ColorTextSecondary
                    )
                }
            }
        }
    }
}

fun getDayName(day: Int): String {
    return when(day) {
        1 -> "Dom"
        2 -> "Lun"
        3 -> "Mar"
        4 -> "Mié"
        5 -> "Jue"
        6 -> "Vie"
        7 -> "Sáb"
        else -> ""
    }
}

fun getDayInitial(day: Int): String {
    return when(day) {
        1 -> "D"
        2 -> "L"
        3 -> "M"
        4 -> "M"
        5 -> "J"
        6 -> "V"
        7 -> "S"
        else -> ""
    }
}

fun getDayNameFull(day: Int): String {
    return when(day) {
        1 -> "Domingo"
        2 -> "Lunes"
        3 -> "Martes"
        4 -> "Miércoles"
        5 -> "Jueves"
        6 -> "Viernes"
        7 -> "Sábado"
        else -> ""
    }
}

@Composable
fun ScheduleTemplateItemGlass(
    slot: AdminViewModel.TemplateSlot,
    onDeleteClick: () -> Unit,
    primaryColor: Color
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccessTime, 
                    null, 
                    tint = if (slot.isOpenGym) Color(0xFF42A5F5) else primaryColor, 
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = slot.time,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
                
                if (slot.isOpenGym) {
                    Spacer(Modifier.width(12.dp))
                    Surface(
                        color = Color(0xFF42A5F5).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Libre",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF42A5F5),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, "Eliminar", tint = ColorError.copy(alpha = 0.8f))
            }
        }
    }
}