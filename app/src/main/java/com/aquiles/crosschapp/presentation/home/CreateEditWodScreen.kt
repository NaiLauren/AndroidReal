package com.aquiles.crosschapp.presentation.home

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.presentation.viewmodel.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
private val ColorBackgroundGradientStart = Color(0xFF000000)
private val ColorBackgroundGradientEnd = Color(0xFF121212)
private val ColorDialogSurface = Color(0xFF1C1C1E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditWodScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    adminViewModel: AdminViewModel,
    wodId: String?,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val profileState by profileViewModel.userState.collectAsState()

    // --- UI STRUCTURE ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        when (val state = profileState) {
            ProfileState.Idle, is ProfileState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ColorPrimaryAction)
                }
            }
            is ProfileState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                }
            }
            is ProfileState.Success -> {
                val user = state.user
                if (user.isAdmin) {
                    CreateEditWodContent(
                        navController = navController,
                        adminViewModel = adminViewModel,
                        wodId = wodId,
                        innerPadding = innerPadding
                    )
                } else {
                    UnauthorizedAccessScreen(navController)
                }
            }
        }
    }
}

@Composable
private fun UnauthorizedAccessScreen(navController: NavController) {
    Scaffold(containerColor = Color.Transparent) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Acceso Denegado", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
            Text("No tienes permisos para acceder a esta sección.", textAlign = TextAlign.Center, color = ColorTextSecondary)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.popBackStack() },
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction)
            ) {
                Text("Volver", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateEditWodContent(
    navController: NavController,
    adminViewModel: AdminViewModel,
    wodId: String?,
    innerPadding: PaddingValues
) {
    val context = LocalContext.current

    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var scoreType by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var date by remember { mutableStateOf<Date?>(null) }

    val wodDetailsState by adminViewModel.wodDetailsState.collectAsState()
    val operationState by adminViewModel.wodOperationState.collectAsState()

    val clearForm = {
        title = ""
        type = ""
        description = ""
        scoreType = ""
        notes = ""
        date = null
    }

    LaunchedEffect(key1 = wodId) {
        if (wodId != null) {
            adminViewModel.loadWodDetails(wodId)
        } else {
            adminViewModel.clearWodDetails()
            clearForm()
        }
    }

    LaunchedEffect(key1 = wodDetailsState) {
        if (wodDetailsState is WodDetailsState.Success) {
            (wodDetailsState as WodDetailsState.Success).wod?.let {
                title = it.title
                type = it.type
                description = it.description
                scoreType = it.scoreType ?: ""
                notes = it.notes ?: ""
                date = it.date
            }
        } else if (wodDetailsState is WodDetailsState.Idle && wodId == null) {
            clearForm()
        }
    }

    LaunchedEffect(key1 = operationState) {
        when (val state = operationState) {
            is WodOperationState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                adminViewModel.resetWodOperationState()
                navController.popBackStack()
            }
            is WodOperationState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                adminViewModel.resetWodOperationState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        if (wodId == null) "Crear Nuevo WOD" else "Editar WOD",
                        color = ColorTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f))
            )
        },
        containerColor = Color.Transparent
    ) { localScaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = localScaffoldPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding()
                )
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassEditTextField(value = title, onValueChange = { title = it }, label = "Título")

            DropdownMenuWodGlass(
                label = "Tipo de WOD",
                options = listOf("For Time", "AMRAP", "EMOM", "Tabata", "Weightlifting", "Otro"),
                selectedOption = type,
                onOptionSelected = { type = it }
            )

            GlassEditTextField(
                value = description,
                onValueChange = { description = it },
                label = "Descripción",
                singleLine = false,
                modifier = Modifier.height(150.dp)
            )

            GlassEditTextField(value = notes, onValueChange = { notes = it }, label = "Notas (Opcional)")

            DropdownMenuWodGlass(
                label = "Tipo de Puntuación",
                options = listOf("Time", "Rounds + Reps", "Total Reps", "Max Weight", "Calorías", "N/A"),
                selectedOption = scoreType,
                onOptionSelected = { scoreType = it }
            )

            DateSelectorGlass(
                selectedDate = date,
                onDateSelected = { date = it }
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    date?.let {
                        // CORRECCIÓN: Nombres de parámetros actualizados
                        adminViewModel.saveWod(
                            originalId = wodId, // Antes: originalWodId
                            title = title.trim(),
                            type = type,
                            desc = description.trim(), // Antes: description
                            date = it,
                            scoreType = scoreType,
                            notes = notes.trim()
                        )
                    } ?: Toast.makeText(context, "Por favor, selecciona una fecha", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (operationState is WodOperationState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text(if (wodId == null) "Guardar WOD" else "Actualizar WOD", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- GLASS COMPONENTS ---

@Composable
fun GlassEditTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = ColorTextSecondary) },
        modifier = modifier.fillMaxWidth(),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuWodGlass(label: String, options: List<String>, selectedOption: String, onOptionSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = ColorTextSecondary) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ColorPrimaryAction,
                unfocusedBorderColor = ColorBorder,
                focusedTextColor = ColorTextPrimary,
                unfocusedTextColor = ColorTextPrimary,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedTrailingIconColor = ColorPrimaryAction,
                unfocusedTrailingIconColor = ColorTextSecondary
            ),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(ColorDialogSurface)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = ColorTextPrimary) },
                    onClick = { onOptionSelected(option); expanded = false }
                )
            }
        }
    }
}

@Composable
fun DateSelectorGlass(selectedDate: Date?, onDateSelected: (Date) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    if (selectedDate != null) { calendar.time = selectedDate }
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val datePickerDialog = DatePickerDialog(context, { _, y, m, d ->
        val c = Calendar.getInstance(); c.set(y, m, d); onDateSelected(c.time)
    }, year, month, day)

    Box(
        modifier = Modifier.clickable { datePickerDialog.show() }
    ) {
        OutlinedTextField(
            value = selectedDate?.let { dateFormatter.format(it) } ?: "Seleccionar fecha",
            onValueChange = {},
            readOnly = true,
            label = { Text("Fecha del WOD", color = ColorTextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
            trailingIcon = {
                IconButton(onClick = { datePickerDialog.show() }) {
                    Icon(Icons.Default.CalendarMonth, null, tint = ColorPrimaryAction)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = ColorTextPrimary,
                disabledBorderColor = ColorBorder,
                disabledLabelColor = ColorTextSecondary,
                disabledContainerColor = Color.Transparent,
                disabledTrailingIconColor = ColorPrimaryAction
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }
}