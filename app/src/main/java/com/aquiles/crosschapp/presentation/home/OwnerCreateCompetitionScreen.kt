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
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerCreateCompetitionScreen(
    navController: NavController,
    adminViewModel: AdminViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Form State
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(CompetitionType.MONTHLY) }
    var selectedCriteria by remember { mutableStateOf(RankingCriteria.POINTS) }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
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

            Divider(color = Color.White.copy(alpha = 0.1f))

            // Tipo de Competencia
            Text(
                "Tipo de Competencia",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )
            SegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                CompetitionType.values().forEach { type ->
                    SegmentedButton(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(type.value, fontSize = 11.sp)
                    }
                }
            }

            // Criterio de Ranking
            Text(
                "Criterio de Ranking",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )
            SegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                RankingCriteria.values().forEach { criteria ->
                    SegmentedButton(
                        selected = selectedCriteria == criteria,
                        onClick = { selectedCriteria = criteria },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(criteria.value.split("(")[0].trim(), fontSize = 10.sp)
                    }
                }
            }

            Divider(color = Color.White.copy(alpha = 0.1f))

            // Fechas
            Text(
                "Duración",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )

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

                    if (startDate == null || endDate == null) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Selecciona fechas de inicio y fin")
                        }
                        return@Button
                    }

                    isLoading = true
                    scope.launch {
                        adminViewModel.createCompetition(
                            title = title,
                            description = description,
                            type = selectedType.value,
                            criteria = selectedCriteria.value,
                            startDate = startDate!!,
                            endDate = endDate!!,
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
