package com.aquiles.crosschapp.presentation.home

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Schedule
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
import com.aquiles.crosschapp.presentation.viewmodel.SchedulePlannerViewModel
import java.text.SimpleDateFormat
import java.util.*

// --- COLORS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.9f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSchedulePlannerScreen(
    navController: NavController,
    viewModel: SchedulePlannerViewModel = viewModel(), // Allow injection or default
    innerPadding: PaddingValues
) {
    val context = LocalContext.current
    val creationState by viewModel.creationState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val templateTimes by viewModel.scheduleTemplate.collectAsState()

    // Form State
    var startDate by remember { mutableStateOf(Date()) }
    var repeatMonths by remember { mutableStateOf(0) } // 0=None, 1=1 Month
    
    // Weekdays (1=Sun, 7=Sat)
    var selectedWeekdays by remember { mutableStateOf(setOf<Int>()) }
    
    // Times
    var selectedTimes by remember { mutableStateOf(setOf<String>()) }

    var className by remember { mutableStateOf("WOD") }
    var coachName by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("15") }
    var duration by remember { mutableStateOf("60") }
    
    var createWod by remember { mutableStateOf(true) }
    var wodDescription by remember { mutableStateOf("") }
    // wodScoreType could be added

    LaunchedEffect(Unit) {
        viewModel.fetchScheduleTemplate()
    }

    LaunchedEffect(creationState) {
        creationState?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            if (it.startsWith("Success")) {
                 // Optional: Navigate back or clear form
            }
            viewModel.clearMessage()
        }
    }

    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Planificador Masivo", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ColorTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { localPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(localPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                // DATA CARD
                PlannerSectionCard(title = "Fecha y Repetición") {
                    // Date Picker Trigger
                    OutlinedTextField(
                        value = dateFormatter.format(startDate),
                        onValueChange = {},
                        label = { Text("Fecha de Inicio") },
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.CalendarMonth, null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val cal = Calendar.getInstance()
                                cal.time = startDate
                                DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        val c = Calendar.getInstance()
                                        c.set(y, m, d)
                                        startDate = c.time
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ColorTextPrimary,
                            unfocusedTextColor = ColorTextPrimary,
                            focusedBorderColor = ColorPrimaryAction,
                            unfocusedBorderColor = ColorTextSecondary
                        ),
                        enabled = false // Disable direct editing, but clickable logic above handles it? Compose nuances.
                        // Actually better to use a Row or Box with clickable.
                    )
                    
                    // Simple hack for clickability over disabled textfield:
                    // Just put a transparent box over it if needed, or rely on readOnly=true (which works fine with clickable modifier usually)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Repetir durante:", color = ColorTextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(0 to "Solo Hoy", 1 to "1 Mes", 2 to "2 Meses").forEach { (valMonths, label) ->
                            FilterChip(
                                selected = repeatMonths == valMonths,
                                onClick = { repeatMonths = valMonths },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ColorPrimaryAction,
                                    labelColor = ColorTextPrimary
                                )
                            )
                        }
                    }
                }

                // WEEKDAYS
                PlannerSectionCard(title = "Días de la Semana") {
                    Text("Selecciona los días a repetir:", color = ColorTextSecondary, style = MaterialTheme.typography.bodySmall)
                    val days = listOf(
                        Calendar.MONDAY to "Lun", Calendar.TUESDAY to "Mar", Calendar.WEDNESDAY to "Mié",
                        Calendar.THURSDAY to "Jue", Calendar.FRIDAY to "Vie", Calendar.SATURDAY to "Sáb", Calendar.SUNDAY to "Dom"
                    )
                    
                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        days.forEach { (calDay, label) ->
                            val isSelected = selectedWeekdays.contains(calDay)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) selectedWeekdays = selectedWeekdays - calDay
                                    else selectedWeekdays = selectedWeekdays + calDay
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ColorPrimaryAction,
                                    selectedLabelColor = Color.White,
                                    labelColor = ColorTextSecondary
                                )
                            )
                        }
                    }
                }

                // TIMES
                PlannerSectionCard(title = "Horarios (Plantilla)") {
                    if (templateTimes.isEmpty()) {
                        Text("No hay horarios definidos en configuración.", color = ColorTextSecondary)
                    } else {
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            templateTimes.forEach { time ->
                                val isSelected = selectedTimes.contains(time)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) selectedTimes = selectedTimes - time
                                        else selectedTimes = selectedTimes + time
                                    },
                                    label = { Text(time) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ColorPrimaryAction,
                                        selectedLabelColor = Color.White,
                                        labelColor = ColorTextSecondary
                                    )
                                )
                            }
                        }
                    }
                    
                    Text(
                        "${selectedTimes.size} horarios seleccionados",
                        color = ColorTextSecondary,
                        style = MaterialTheme.typography.caption
                    )
                }
                
                // DETAILS
                PlannerSectionCard(title = "Detalles de la Clase") {
                    OutlinedTextField(
                        value = className, onValueChange = { className = it }, label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = ColorTextPrimary, unfocusedTextColor = ColorTextPrimary)
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = coachName, onValueChange = { coachName = it }, label = { Text("Coach") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = ColorTextPrimary, unfocusedTextColor = ColorTextPrimary)
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        OutlinedTextField(
                            value = capacity, onValueChange = { capacity = it }, label = { Text("Cupo") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = ColorTextPrimary, unfocusedTextColor = ColorTextPrimary)
                        )
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = duration, onValueChange = { duration = it }, label = { Text("Minutos") },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = ColorTextPrimary, unfocusedTextColor = ColorTextPrimary)
                        )
                    }
                }
                
                // WOD
                PlannerSectionCard(title = "WOD del Día") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = createWod, onCheckedChange = { createWod = it })
                        Text("Crear WOD automáticamente", color = ColorTextPrimary)
                    }
                    
                    if (createWod) {
                        OutlinedTextField(
                            value = wodDescription,
                            onValueChange = { wodDescription = it },
                            label = { Text("Descripción del WOD") },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            maxLines = 5,
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = ColorTextPrimary, unfocusedTextColor = ColorTextPrimary)
                        )
                        Text(
                             "Nota: Si repites por mes, este mismo WOD se asignará a todos los días (útil para ciclos o benchmarks, o editar después).",
                             style = MaterialTheme.typography.bodySmall,
                             color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.createClassesBatch(
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
                            wodScoreType = "Time" // Default
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction),
                    enabled = !isLoading && selectedTimes.isNotEmpty()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Generar Clases", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun PlannerSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface),
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
