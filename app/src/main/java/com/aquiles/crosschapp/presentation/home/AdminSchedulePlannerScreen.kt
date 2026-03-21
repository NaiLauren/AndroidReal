package com.aquiles.crosschapp.presentation.home

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
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
import com.aquiles.crosschapp.data.model.GymClass
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import com.aquiles.crosschapp.presentation.viewmodel.SchedulePlannerViewModel
import java.text.SimpleDateFormat
import java.util.*
import com.aquiles.crosschapp.presentation.components.GlassCard
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height

// --- COLORS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.45f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)

// --- COMPONENTS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSchedulePlannerScreen(
    navController: NavController,
    viewModel: SchedulePlannerViewModel = viewModel(),
    innerPadding: PaddingValues
) {
    val context = LocalContext.current
    val creationState by viewModel.creationState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // UI States matching iOS
    val classesForDate by viewModel.classesForSelectedDate.collectAsState()
    val selectedClassIds by viewModel.selectedClassIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    
    var showCreateSheet by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(Date()) }
    
    // DatePicker State (Material 3)
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.time
    )

    // Sync DatePicker with ViewModel
    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let { utcMidnight ->
            val offset = TimeZone.getDefault().getOffset(utcMidnight)
            val localDate = Date(utcMidnight - offset)
            selectedDate = localDate
            viewModel.fetchClasses(localDate)
            viewModel.fetchScheduleTemplate(localDate) // Fetch specific template for this day
        }
    }

    LaunchedEffect(Unit) {
        viewModel.fetchClasses(selectedDate)
        viewModel.fetchScheduleTemplate(selectedDate) // Fetch initial template
    }
    
    // Auto-open sheet when editing
    LaunchedEffect(isSelectionMode) {
        // En iOS el batch edit se dispara manual, aquí si entramos en selection mode esperamos.
    }

    // Toast handler
    LaunchedEffect(creationState) {
        creationState?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            if (it.startsWith("Success") || it.startsWith("Éxito")) {
                showCreateSheet = false
                viewModel.clearSelection()
            }
            viewModel.clearMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // .background(Color.Black) // Removed for glass background
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    modifier = Modifier.height(72.dp),
                    title = { Text("Planificador", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ColorTextPrimary)
                        }
                    },
                    actions = {
                        if (isSelectionMode) {
                            TextButton(onClick = { viewModel.clearSelection() }) {
                                Text("Cancelar", color = Color.Red)
                            }
                        } else {
                            TextButton(onClick = { viewModel.selectAll() }) {
                                Text("Seleccionar", color = ColorPrimaryAction)
                            }
                        }
                        
                        if (isSelectionMode) {
                            TextButton(
                                onClick = { 
                                    viewModel.prepareBatchEdit() // Not implemented in VM yet but standard flow
                                    showCreateSheet = true 
                                },
                                enabled = selectedClassIds.isNotEmpty()
                            ) {
                                Text("Editar (${selectedClassIds.size})", fontWeight = FontWeight.Bold, color = if(selectedClassIds.isNotEmpty()) ColorPrimaryAction else Color.Gray)
                            }
                        } else {
                            IconButton(onClick = { 
                                viewModel.clearSelection() // Ensure clean state
                                showCreateSheet = true 
                            }) {
                                Icon(Icons.Default.AddCircle, "Crear", tint = ColorPrimaryAction)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f))
                )
            },
            containerColor = Color.Transparent
        ) { localPadding ->
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(localPadding)
            ) {
                // 1. CALENDAR HEADER (Collapsible-ish)
                // Usamos DatePicker in a container
                GlassCard(
                    shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                   Column {
                       DatePicker(
                           state = datePickerState,
                           colors = DatePickerDefaults.colors(
                               selectedDayContainerColor = ColorPrimaryAction,
                               todayDateBorderColor = ColorPrimaryAction,
                               dayContentColor = ColorTextPrimary,
                               weekdayContentColor = ColorTextSecondary,
                               headlineContentColor = ColorTextPrimary,
                               navigationContentColor = ColorTextPrimary,
                               subheadContentColor = ColorTextSecondary,
                               yearContentColor = ColorTextPrimary,
                               currentYearContentColor = ColorTextPrimary,
                               selectedYearContentColor = Color.White,
                               disabledDayContentColor = Color.Gray,
                               disabledSelectedDayContentColor = Color.Gray,
                               containerColor = Color.Transparent
                           ),
                           showModeToggle = false,
                           title = null,
                           headline = null
                       )
                       
                       // Sección "Clases Programadas" Header
                       Row(
                           modifier = Modifier
                               .fillMaxWidth()
                               .padding(16.dp),
                           horizontalArrangement = Arrangement.SpaceBetween,
                           verticalAlignment = Alignment.CenterVertically
                       ) {
                           Text("Clases Programadas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
                           
                           Surface(color = ColorPrimaryAction, shape = CircleShape) {
                               Text(
                                   text = "${classesForDate.size}",
                                   style = MaterialTheme.typography.labelSmall,
                                   color = Color.White,
                                   modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                               )
                           }
                       }
                   }
                }

                // 2. LIST CONTENT
                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { 
                        CircularProgressIndicator(color = ColorPrimaryAction) 
                    }
                } else if (classesForDate.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        GlassCard(modifier = Modifier.padding(32.dp)) {
                            Column(modifier = Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.EventBusy, null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                                Spacer(Modifier.height(16.dp))
                                Text("No hay clases para este día", color = ColorTextSecondary)
                                Button(onClick = { showCreateSheet = true }, modifier = Modifier.padding(top = 16.dp), colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction)) {
                                    Text("Crear Clases")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(classesForDate) { gymClass ->
                            PlannerClassItem(
                                gymClass = gymClass,
                                isSelected = selectedClassIds.contains(gymClass.id),
                                onToggle = { 
                                     if(isSelectionMode) viewModel.toggleSelection(gymClass.id)
                                     else navController.navigate("create_edit_class_screen?classId=${gymClass.id}")
                                },
                                isSelectionMode = isSelectionMode
                            )
                        }
                    }
                }
            }
        }
        
        // BOTTOM SHEET FOR CREATION / EDITING
        if (showCreateSheet) {
            ModalBottomSheet(
                onDismissRequest = { 
                    showCreateSheet = false
                    if (!isSelectionMode) viewModel.clearSelection() // Limpiar si fue edición single
                },
                containerColor = Color(0xFF0D0D0D), // Fondo solido oscuro, evitando cruce visual c/ calendario
                dragHandle = { BottomSheetDefaults.DragHandle(color = Color.White.copy(alpha = 0.3f)) }
            ) {
                // Here we inject the FORM Logic
                // We pass 'selectedDate' effectively.
                CreateClassesFormContent(
                    viewModel = viewModel,
                    initialDate = selectedDate,
                    isEditMode = selectedClassIds.isNotEmpty(),
                    onCancel = { showCreateSheet = false }
                )
            }
        }
    }
}

// Extracted Form Content
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateClassesFormContent(
    viewModel: SchedulePlannerViewModel,
    initialDate: Date,
    isEditMode: Boolean,
    onCancel: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val templateTimes by viewModel.scheduleTemplate.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    // States
    var startDate by remember { mutableStateOf(initialDate) }
    var repeatMonths by remember { mutableStateOf(0) }
    var selectedWeekdays by remember { mutableStateOf(setOf<Int>()) }
    var selectedTimes by remember { mutableStateOf(setOf<AdminViewModel.TemplateSlot>()) }

    var className by remember { mutableStateOf("WOD") }
    var coachName by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("15") }
    var duration by remember { mutableStateOf("60") }
    
    var createWod by remember { mutableStateOf(true) }
    var wodDescription by remember { mutableStateOf("") }
    
    // Color predeterminado
    var selectedColor by remember { mutableStateOf("#FC5200") } // Naranja por defecto

    val colorOptions = listOf(
        "#FC5200", // Naranja (Primario)
        "#42A5F5", // Azul
        "#66BB6A", // Verde
        "#AB47BC", // Morado
        "#EF5350", // Rojo
        "#FFA726"  // Ámbar
    )
    
    // Auto-fill if editing
    LaunchedEffect(Unit) {
        if (isEditMode) {
            val sample = viewModel.getFirstSelectedClass()
            if (sample != null) {
                // Pre-populate
                className = sample.name
                coachName = sample.coachName
                capacity = sample.maxCapacity.toString()
                duration = sample.durationMinutes.toString()
                wodDescription = sample.description ?: ""
                createWod = (sample.wodId != null)
                selectedColor = sample.hexColor ?: "#FC5200"
                // Note: Times/Days usually cleared or set to match sample? 
                // In iOS batch edit, we edit ATTRIBUTES, not dates/times usually.
                // Re-creating dates logic for editing existing ones implies MOVING them, which is complex.
                // For simplified parity: Edit only properties.
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            if (isEditMode) "Editar Clases" else "Programar Clases",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = ColorTextPrimary
        )
        
        // --- FORM SECTIONS ---
        
        // DATE & TIME (Hide in Edit Mode typically, unless moving)
        if (!isEditMode) {
            PlannerSectionCard("Fecha y Repetición") {
                Text("Inicio: ${dateFormatter.format(startDate)}", color = ColorTextPrimary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Solo Hoy", 1 to "1 Mes").forEach { (valMonths, label) ->
                        FilterChip(
                            selected = repeatMonths == valMonths,
                            onClick = { repeatMonths = valMonths },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = ColorPrimaryAction, labelColor = ColorTextPrimary)
                        )
                    }
                }
            }
            
            PlannerSectionCard("Horarios") {
                 if (templateTimes.isEmpty()) Text("Sin plantilla de horarios", color = ColorTextSecondary)
                 else {
                     FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                         templateTimes.forEach { slot ->
                             val isSelected = selectedTimes.contains(slot)
                             FilterChip(
                                 selected = isSelected,
                                 onClick = { if (isSelected) selectedTimes = selectedTimes - slot else selectedTimes = selectedTimes + slot },
                                 label = { 
                                     Row(verticalAlignment = Alignment.CenterVertically) {
                                         if (slot.isOpenGym) {
                                            Icon(Icons.Default.Schedule, null, tint = Color(0xFF42A5F5), modifier = Modifier.size(14.dp))
                                            Spacer(Modifier.width(4.dp))
                                         }
                                         Text(slot.time)
                                     } 
                                 },
                                 colors = FilterChipDefaults.filterChipColors(selectedContainerColor = if (slot.isOpenGym) Color(0xFF42A5F5).copy(alpha=0.5f) else ColorPrimaryAction, labelColor = ColorTextPrimary)
                             )
                         }
                     }
                 }
            }
        }
        

        
        PlannerSectionCard("Detalles") {
            OutlinedTextField(value = className, onValueChange = { className = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = coachName, onValueChange = { coachName = it }, label = { Text("Coach") }, modifier = Modifier.fillMaxWidth())
            Row {
                OutlinedTextField(value = capacity, onValueChange = { capacity = it }, label = { Text("Cupo") }, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Min") }, modifier = Modifier.weight(1f))
            }
            
            Spacer(Modifier.height(8.dp))
            Text("Color de la Clase", style = MaterialTheme.typography.labelMedium, color = ColorTextSecondary)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                colorOptions.forEach { hex ->
                    val isSelected = selectedColor == hex
                    val color = try { Color(android.graphics.Color.parseColor(hex)) } catch(e: Exception) { Color.Gray }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                2.dp,
                                if (isSelected) Color.White else Color.Transparent,
                                CircleShape
                            )
                            .clickable { selectedColor = hex },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, "Seleccionado", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
        
        PlannerSectionCard("Contenido (Rutina)") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = createWod, onCheckedChange = { createWod = it })
                Text("Incluir Rutina", color = ColorTextPrimary)
            }
            if (createWod) {
                OutlinedTextField(
                    value = wodDescription, 
                    onValueChange = { wodDescription = it }, 
                    label = { Text("Descripción") }, 
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 5
                )
            }
        }
        
        Button(
            onClick = {
                 viewModel.createOrUpdateBatch(
                    startDate = startDate,
                    selectedTimes = selectedTimes, 
                    selectedWeekdays = selectedWeekdays,
                    repeatMonths = repeatMonths,
                    className = className,
                    coachName = coachName,
                    description = wodDescription,
                    capacity = capacity.toIntOrNull() ?: 15,
                    durationMinutes = duration.toIntOrNull() ?: 60,
                    createWod = createWod,
                    wodScoreType = "Time",
                    hexColor = selectedColor, // Pasando el color recuperado
                    isUpdateMode = isEditMode
                )
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (isEditMode) Color.Green else ColorPrimaryAction),
            enabled = (isEditMode || selectedTimes.isNotEmpty()) && !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            else Text(if (isEditMode) "Guardar Cambios" else "Generar Clases", fontWeight = FontWeight.Bold, color = if(isEditMode) Color.Black else Color.White)
        }
        
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun PlannerClassItem(
    gymClass: GymClass,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggle: () -> Unit
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = gymClass.dateTime?.let { timeFormat.format(it) } ?: "--:--"
    
    // Style matching iOS "Neon" card
    val stripColor = try { Color(android.graphics.Color.parseColor(gymClass.hexColor)) } catch(e:Exception) { ColorPrimaryAction }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Time Column
            Text(
                text = timeStr,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = ColorPrimaryAction,
                modifier = Modifier.width(50.dp)
            )
            
            // Vertical Color Strip
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(stripColor)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gymClass.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = ColorTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(12.dp), tint = ColorTextSecondary)
                    Spacer(Modifier.width(4.dp))
                    Text(gymClass.coachName, style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)
                }
            }
            
            // Badges
            Column(horizontalAlignment = Alignment.End) {
                if (gymClass.wodId != null || gymClass.description?.isNotEmpty() == true) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color.Green, modifier = Modifier.size(16.dp))
                }
                
                Surface(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Text(
                        "${gymClass.enrolledUserIds.size}/${gymClass.maxCapacity}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorTextPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            // Checkmark for selection
            if (isSelectionMode) {
                Spacer(Modifier.width(16.dp))
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) ColorPrimaryAction else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun PlannerSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    GlassCard(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = ColorPrimaryAction, fontWeight = FontWeight.Bold)
            content()
        }
    }
}
