package com.aquiles.crosschapp.presentation.home

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.aquiles.crosschapp.data.model.CompetitionType
import com.aquiles.crosschapp.data.model.RankingCriteria
import com.aquiles.crosschapp.data.model.ScoreStrategy
import com.aquiles.crosschapp.data.model.ValidationRule
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import com.aquiles.crosschapp.presentation.viewmodel.CompetitionViewModel
import com.aquiles.crosschapp.domain.competition.CompetitionFormValidator
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerCreateCompetitionScreen(
    navController: NavController,
    adminViewModel: AdminViewModel = viewModel(),
    competitionViewModel: CompetitionViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Form State
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isSingleDay by remember { mutableStateOf(true) }
    var selectedCriteria by remember { mutableStateOf(RankingCriteria.POINTS) }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var eventTime by remember { mutableStateOf("10:00") }
    var eventCapacity by remember { mutableStateOf("30") }
    var registrationCredits by remember { mutableStateOf("1") }
    var isLoading by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(72.dp),
                title = {
                    Column {
                        Text(
                            "Crear Competencia",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                        Text(
                            "Cargá una competencia para tu gym",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Image Picker
            if (selectedImageUri == null) {
                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White.copy(alpha = 0.6f)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.foundation.BorderStroke(
                            2.dp,
                            Color.White.copy(alpha = 0.2f)
                        ).brush
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            Icons.Default.Image,
                            "Portada",
                            modifier = Modifier.size(48.dp),
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Subir portada de competencia", fontSize = 14.sp)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            color = Color(0xFF1C1C1E),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "Portada",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { selectedImageUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(50)
                            )
                    ) {
                        Icon(Icons.Default.Close, "Remover", tint = Color.White)
                    }
                }
            }

            // Título
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Nombre de la Competencia") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedBorderColor = Color(0xFFFC5200)
                ),
                textStyle = androidx.compose.material3.LocalTextStyle.current.copy(color = Color.White)
            )

            // Descripción
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedBorderColor = Color(0xFFFC5200)
                ),
                textStyle = androidx.compose.material3.LocalTextStyle.current.copy(color = Color.White)
            )

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Duración de la Competencia
            Text(
                "Duración *",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(true to "1 Día", false to "Múltiples Días").forEach { (isSingle, label) ->
                    Button(
                        onClick = { isSingleDay = isSingle },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSingleDay == isSingle) Color(0xFFFC5200) else Color.Transparent,
                            contentColor = if (isSingleDay == isSingle) Color.White else Color.White.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Criterio de Ranking
            Spacer(Modifier.height(8.dp))
            Text(
                "Criterio de Ranking",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RankingCriteria.values().forEach { criteria ->
                    FilterChip(
                        selected = selectedCriteria == criteria,
                        onClick = { selectedCriteria = criteria },
                        label = { Text(criteria.value.split("(")[0].trim(), fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFC5200)
                        )
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Fechas
            Text(
                if (isSingleDay) "Fecha de la Competencia *" else "Duración *",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )

            if (isSingleDay) {
                DatePickerButton(
                    label = "Fecha",
                    selectedDate = startDate,
                    onDateSelected = { startDate = it },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DatePickerButton(
                        label = "Inicio",
                        selectedDate = startDate,
                        onDateSelected = { startDate = it },
                        modifier = Modifier.weight(1f)
                    )

                    DatePickerButton(
                        label = "Fin",
                        selectedDate = endDate,
                        onDateSelected = { endDate = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
            
            // Horario, Cupo y Valor (Créditos)
            Text(
                "Configuración del Evento",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = eventTime,
                    onValueChange = { eventTime = it },
                    label = { Text("Horario (HH:mm)") },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedBorderColor = Color(0xFFFC5200)
                    ),
                    textStyle = androidx.compose.material3.LocalTextStyle.current.copy(color = Color.White)
                )

                OutlinedTextField(
                    value = eventCapacity,
                    onValueChange = { eventCapacity = it },
                    label = { Text("Cupo Máx") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedBorderColor = Color(0xFFFC5200)
                    ),
                    textStyle = androidx.compose.material3.LocalTextStyle.current.copy(color = Color.White)
                )
            }
            
            OutlinedTextField(
                value = registrationCredits,
                onValueChange = { registrationCredits = it },
                label = { Text("Costo en Créditos") },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedBorderColor = Color(0xFFFC5200)
                ),
                textStyle = androidx.compose.material3.LocalTextStyle.current.copy(color = Color.White)
            )

            Spacer(Modifier.height(8.dp))

            // Submit Button
            Button(
                onClick = {
                    if (title.isEmpty()) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Ingresa un nombre para la competencia")
                        }
                        return@Button
                    }

                    if (startDate == null) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Selecciona una fecha")
                        }
                        return@Button
                    }

                    if (!isSingleDay && endDate == null) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Selecciona fecha de fin")
                        }
                        return@Button
                    }

                    val capacityInt = eventCapacity.toIntOrNull() ?: 0
                    val creditsInt = registrationCredits.toIntOrNull() ?: 0
                    val finalEndDate = if (isSingleDay) startDate else endDate

                    val validationResult = CompetitionFormValidator.validate(
                        title = title,
                        description = description,
                        startDate = startDate!!,
                        endDate = finalEndDate!!,
                        maxCapacity = capacityInt,
                        registrationCredits = creditsInt,
                        criteriaSpecificValue = null,
                        criteria = selectedCriteria
                    )

                    if (!validationResult.isValid) {
                        scope.launch {
                            snackbarHostState.showSnackbar(validationResult.errorMessages.first())
                        }
                        return@Button
                    }

                    isLoading = true
                    scope.launch {
                        val competitionType = if (isSingleDay) CompetitionType.DAILY else CompetitionType.RANGE
                        competitionViewModel.createCompetitionWithEvent(
                            title = title,
                            description = description,
                            type = competitionType,
                            criteria = selectedCriteria,
                            startDate = startDate!!,
                            endDate = finalEndDate!!,
                            prizeDescription = null,
                            xpReward = null,
                            scoreStrategy = ScoreStrategy.ABSOLUTE,
                            validationRule = ValidationRule.AUTOMATIC,
                            eventTime = eventTime,
                            eventCapacity = capacityInt,
                            registrationCredits = creditsInt,
                            imageUri = selectedImageUri
                        )
                        isLoading = false
                        snackbarHostState.showSnackbar("Competencia creada exitosamente")
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFC5200)
                ),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Crear Competencia", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun DatePickerButton(
    label: String,
    selectedDate: Date?,
    onDateSelected: (Date) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showDatePicker = true },
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color.White
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
            Text(
                selectedDate?.let {
                    java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it)
                } ?: "Seleccionar",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }

    if (showDatePicker) {
        val calendar = Calendar.getInstance()
        selectedDate?.let { calendar.time = it }

        val datePickerDialog = android.app.DatePickerDialog(
            LocalContext.current,
            { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance()
                newCalendar.set(year, month, dayOfMonth)
                onDateSelected(newCalendar.time)
                showDatePicker = false
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        LaunchedEffect(showDatePicker) {
            if (showDatePicker) {
                datePickerDialog.show()
            }
        }
    }
}
