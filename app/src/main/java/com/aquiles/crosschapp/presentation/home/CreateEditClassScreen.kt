package com.aquiles.crosschapp.presentation.home

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import com.aquiles.crosschapp.presentation.viewmodel.ClassForEditState
import com.aquiles.crosschapp.presentation.viewmodel.ClassOperationState
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.layout.height

// --- CONSTANTS ---
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.15f)
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.45f)
private val ColorError = Color(0xFFEF5350)

private const val TYPE_WOD = "WOD"
private const val TYPE_OTHER = "Otra Clase"

enum class AppClassColor(val label: String, val hex: String, val color: Color) {
    Orange("Naranja", "#FF7A00", Color(0xFFFF7A00)),
    Green("Verde", "#7DCD45", Color(0xFF7DCD45)),
    Blue("Azul", "#2993F5", Color(0xFF2993F5)),
    Purple("Morado", "#C56FFF", Color(0xFFC56FFF));

    companion object {
        fun fromHex(hex: String): AppClassColor {
            return entries.find { it.hex.equals(hex, ignoreCase = true) } ?: Orange
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditClassScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    adminViewModel: AdminViewModel,
    classId: String?
) {
    val context = LocalContext.current
    val isEditMode = classId != null

    // Estados
    var selectedClassType by remember { mutableStateOf(TYPE_WOD) }
    var selectedDate by remember { mutableStateOf(Date()) }
    var wodTitle by remember { mutableStateOf("") }
    var wodDescription by remember { mutableStateOf("") }
    var wodScoreType by remember { mutableStateOf("") }
    var otherClassName by remember { mutableStateOf("") }
    var otherClassDesc by remember { mutableStateOf("") }
    var coachName by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("60") }
    var capacity by remember { mutableStateOf("10") }
    var selectedTimes by remember { mutableStateOf(setOf<String>()) }
    var selectedColorOption by remember { mutableStateOf(AppClassColor.Orange) }

    var isLoadingData by remember { mutableStateOf(isEditMode) }

    val operationState by adminViewModel.classOperationState.collectAsState()
    val scheduleTemplate by adminViewModel.scheduleTemplateState.collectAsState()
    val classForEditState by adminViewModel.classForEditState.collectAsState()

    LaunchedEffect(Unit) { adminViewModel.loadScheduleTemplate() }

    LaunchedEffect(key1 = classId) {
        if (classId != null) adminViewModel.loadClassForEditing(classId)
    }

    LaunchedEffect(key1 = classForEditState) {
        if (classForEditState is ClassForEditState.Success) {
            val successState = classForEditState as ClassForEditState.Success
            val gymClass = successState.gymClass
            val wod = successState.wod

            selectedDate = gymClass.dateTime ?: Date()
            coachName = gymClass.coachName
            duration = gymClass.durationMinutes.toString()
            capacity = gymClass.maxCapacity.toString()
            selectedColorOption = AppClassColor.fromHex(gymClass.hexColor)
            selectedClassType = if (gymClass.classType == TYPE_WOD) TYPE_WOD else TYPE_OTHER

            if (gymClass.classType == TYPE_WOD && wod != null) {
                wodTitle = wod.title
                wodDescription = wod.description
                wodScoreType = wod.scoreType ?: ""
            } else {
                otherClassName = gymClass.name
                otherClassDesc = gymClass.description
            }
            val calendar = Calendar.getInstance().apply { time = gymClass.dateTime ?: Date() }
            val timeString = String.format(Locale.US, "%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
            selectedTimes = setOf(timeString)
            isLoadingData = false
        } else if (classForEditState is ClassForEditState.Error) {
            Toast.makeText(context, (classForEditState as ClassForEditState.Error).message, Toast.LENGTH_LONG).show()
            isLoadingData = false
        }
    }

    LaunchedEffect(operationState) {
        if (operationState is ClassOperationState.Success) {
            Toast.makeText(context, (operationState as ClassOperationState.Success).message, Toast.LENGTH_LONG).show()
            adminViewModel.resetClassOperationState()
            navController.popBackStack()
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
                    modifier = Modifier.height(72.dp),
                    title = { Text(if (isEditMode) "Editar Clase" else "Planificar", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ColorTextPrimary) } },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f))
                )
            },
            containerColor = Color.Transparent
        ) { localScaffoldPadding ->
            if (isLoadingData) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ColorPrimaryAction) }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = localScaffoldPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {

                    ClassTypeSelector(selectedType = selectedClassType, onTypeSelected = { selectedClassType = it }, isEnabled = !isEditMode)

                    GlassCardSection {
                        if (selectedClassType == TYPE_WOD) {
                            GlassEditTextField(value = wodTitle, onValueChange = { wodTitle = it }, label = "Título del WOD")
                            Spacer(Modifier.height(12.dp))
                            GlassEditTextField(value = wodDescription, onValueChange = { wodDescription = it }, label = "Descripción", singleLine = false, modifier = Modifier.height(100.dp))
                            Spacer(Modifier.height(12.dp))
                            GlassEditTextField(value = wodScoreType, onValueChange = { wodScoreType = it }, label = "Tipo Puntuación")
                        } else {
                            GlassEditTextField(value = otherClassName, onValueChange = { otherClassName = it }, label = "Nombre Clase")
                            Spacer(Modifier.height(12.dp))
                            GlassEditTextField(value = otherClassDesc, onValueChange = { otherClassDesc = it }, label = "Descripción", singleLine = false, modifier = Modifier.height(100.dp))
                        }
                    }

                    GlassCardSection {
                        DateSelectorFieldGlass(selectedDate = selectedDate, onDateSelected = { selectedDate = it })
                        Spacer(Modifier.height(12.dp))
                        GlassEditTextField(value = coachName, onValueChange = { coachName = it }, label = "Nombre del Coach")
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            GlassEditTextField(value = duration, onValueChange = { duration = it }, label = "Minutos", modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number)
                            GlassEditTextField(value = capacity, onValueChange = { capacity = it }, label = "Capacidad", modifier = Modifier.weight(1f), keyboardType = KeyboardType.Number)
                        }
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = ColorBorder)
                        Spacer(Modifier.height(16.dp))
                        Text("Color de la Clase", style = MaterialTheme.typography.labelLarge, color = ColorTextSecondary)
                        Spacer(Modifier.height(12.dp))
                        ColorPickerRow(selectedOption = selectedColorOption, onOptionSelected = { selectedColorOption = it })
                    }

                    GlassCardSection {
                        Text("Horarios:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
                        Spacer(Modifier.height(8.dp))
                        if (isEditMode) {
                            Text("Para cambiar horario, elimina y recrea.", style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)
                            ScheduleCheckboxItem(time = selectedTimes.firstOrNull() ?: "", isChecked = true, onCheckedChange = {}, isEnabled = false)
                        } else {
                            if (scheduleTemplate.isEmpty()) {
                                Text("Plantilla vacía.", color = ColorError)
                            } else {
                                scheduleTemplate.forEach { time ->
                                    ScheduleCheckboxItem(
                                        time = time,
                                        isChecked = selectedTimes.contains(time),
                                        onCheckedChange = {
                                            selectedTimes = if (selectedTimes.contains(time)) selectedTimes - time else selectedTimes + time
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (isEditMode) {
                                val originalClass = (classForEditState as? ClassForEditState.Success)?.gymClass
                                // CORRECCIÓN AQUÍ: Usar nombres nuevos del ViewModel
                                adminViewModel.updateClass(
                                    classId = originalClass?.id ?: "",
                                    wodId = originalClass?.wodId,
                                    isWodType = (selectedClassType == TYPE_WOD),
                                    wodTitle = wodTitle,
                                    wodDesc = wodDescription, // Nombre corregido
                                    scoreType = wodScoreType, // Nombre corregido
                                    otherName = otherClassName, // Nombre corregido
                                    otherDesc = otherClassDesc, // Nombre corregido
                                    date = selectedDate,
                                    coach = coachName, // Nombre corregido
                                    duration = duration.toIntOrNull() ?: 0, // Nombre corregido
                                    capacity = capacity.toIntOrNull() ?: 0, // Nombre corregido
                                    hexColor = selectedColorOption.hex // Nombre corregido
                                )
                            } else {
                                adminViewModel.createWodAndClassesForDay(
                                    isWodType = (selectedClassType == TYPE_WOD),
                                    wodTitle = wodTitle,
                                    wodDescription = wodDescription,
                                    wodScoreType = wodScoreType,
                                    otherClassName = otherClassName,
                                    otherClassDescription = otherClassDesc,
                                    date = selectedDate,
                                    coachName = coachName,
                                    durationMinutes = duration.toIntOrNull() ?: 0,
                                    maxCapacity = capacity.toIntOrNull() ?: 0,
                                    selectedTimes = selectedTimes.toList(),
                                    wodColor = selectedColorOption.hex,
                                    otherColor = selectedColorOption.hex
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = operationState !is ClassOperationState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (operationState is ClassOperationState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text(if (isEditMode) "Guardar Cambios" else "Crear Clases", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// ========================================================
// COMPONENTES AUXILIARES
// ========================================================

@Composable
fun ColorPickerRow(selectedOption: AppClassColor, onOptionSelected: (AppClassColor) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        AppClassColor.entries.forEach { option ->
            val isSelected = option == selectedOption
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(option.color)
                        .border(if (isSelected) 3.dp else 0.dp, if (isSelected) Color.White else Color.Transparent, CircleShape)
                        .clickable { onOptionSelected(option) },
                    contentAlignment = Alignment.Center
                ) { if (isSelected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
                Spacer(Modifier.height(4.dp))
                Text(option.label, style = MaterialTheme.typography.labelSmall, color = if (isSelected) ColorTextPrimary else ColorTextSecondary)
            }
        }
    }
}

@Composable
fun ClassTypeSelector(selectedType: String, onTypeSelected: (String) -> Unit, isEnabled: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SegmentedButton(modifier = Modifier.weight(1f), label = "WOD", isSelected = selectedType == TYPE_WOD, onClick = { onTypeSelected(TYPE_WOD) }, isEnabled = isEnabled)
        SegmentedButton(modifier = Modifier.weight(1f), label = "Otra", isSelected = selectedType == TYPE_OTHER, onClick = { onTypeSelected(TYPE_OTHER) }, isEnabled = isEnabled)
    }
}

@Composable
fun SegmentedButton(modifier: Modifier = Modifier, label: String, isSelected: Boolean, onClick: () -> Unit, isEnabled: Boolean = true) {
    Button(
        onClick = onClick, modifier = modifier.height(40.dp), enabled = isEnabled,
        colors = ButtonDefaults.buttonColors(containerColor = if (isSelected) ColorPrimaryAction else Color.White.copy(alpha = 0.1f), contentColor = Color.White, disabledContainerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(0.dp)
    ) { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
}

@Composable
fun GlassCardSection(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = ColorGlassSurface), border = BorderStroke(1.dp, ColorBorder)) { Column(modifier = Modifier.padding(16.dp), content = content) }
}

@Composable
fun GlassEditTextField(value: String, onValueChange: (String) -> Unit, label: String, singleLine: Boolean = true, keyboardType: KeyboardType = KeyboardType.Text, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(label, color = ColorTextSecondary) },
        modifier = modifier.fillMaxWidth(), singleLine = singleLine, keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ColorPrimaryAction, unfocusedBorderColor = ColorBorder, focusedTextColor = ColorTextPrimary, unfocusedTextColor = ColorTextPrimary, cursorColor = ColorPrimaryAction, focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun DateSelectorFieldGlass(selectedDate: Date, onDateSelected: (Date) -> Unit) {
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("EEEE, dd 'de' MMMM", Locale.forLanguageTag("es-ES")) }

    OutlinedTextField(
        value = dateFormatter.format(selectedDate).replaceFirstChar { it.uppercase() },
        onValueChange = {}, readOnly = true,
        label = { Text("Fecha", color = ColorTextSecondary) },
        trailingIcon = {
            Icon(Icons.Default.Event, "Fecha", tint = ColorPrimaryAction, modifier = Modifier.clickable {
                val cal = Calendar.getInstance().apply { time = selectedDate }
                DatePickerDialog(context, { _, y, m, d -> val c = Calendar.getInstance(); c.set(y, m, d); onDateSelected(c.time) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
            })
        },
        modifier = Modifier.fillMaxWidth().clickable {
            val cal = Calendar.getInstance().apply { time = selectedDate }
            DatePickerDialog(context, { _, y, m, d -> val c = Calendar.getInstance(); c.set(y, m, d); onDateSelected(c.time) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        },
        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = ColorTextPrimary, disabledBorderColor = ColorBorder, disabledLabelColor = ColorTextSecondary, disabledContainerColor = Color.Transparent, disabledTrailingIconColor = ColorPrimaryAction),
        enabled = false, shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun ScheduleCheckboxItem(time: String, isChecked: Boolean, onCheckedChange: () -> Unit, isEnabled: Boolean = true) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onCheckedChange, enabled = isEnabled).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = isChecked, onCheckedChange = { onCheckedChange() }, enabled = isEnabled, colors = CheckboxDefaults.colors(checkedColor = ColorPrimaryAction, uncheckedColor = ColorTextSecondary, checkmarkColor = Color.White))
        Text(text = time, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp), color = ColorTextPrimary)
    }
}