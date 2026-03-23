package com.aquiles.crosschapp.presentation.home

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.data.model.BenchmarkWod
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkEditScreen(
    navController: NavController,
    benchmarkToEdit: BenchmarkWod?,
    adminViewModel: AdminViewModel = viewModel()
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf(benchmarkToEdit?.name ?: "") }
    var description by remember { mutableStateOf(benchmarkToEdit?.description ?: "") }
    var scoreType by remember { mutableStateOf(benchmarkToEdit?.scoreType ?: "TIME") }
    var strategy by remember { mutableStateOf(benchmarkToEdit?.strategy ?: "") }
    var isGlobal by remember { mutableStateOf((benchmarkToEdit?.gym_id ?: "LOCAL") == "" || (benchmarkToEdit?.isGlobal ?: false)) }
    var isDesafio by remember { mutableStateOf(benchmarkToEdit?.isDesafio ?: false) }
    var useDates by remember { mutableStateOf(benchmarkToEdit?.startDate != null || benchmarkToEdit?.endDate != null) }
    var startDate by remember { mutableStateOf(benchmarkToEdit?.startDate?.toDate() ?: Date()) }
    var endDate by remember {
        mutableStateOf(
            benchmarkToEdit?.endDate?.toDate() ?: Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 7)
            }.time
        )
    }

    val scoreTypes = listOf("TIME", "REPS", "WEIGHT", "ROUNDS", "AMRAP")
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.height(72.dp),
                title = {
                    Text(
                        if (benchmarkToEdit == null) "Nuevo Benchmark" else "Editar Benchmark",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f)
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del Benchmark") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFC5200),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFFC5200)
                )
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción (ej: 21-15-9...)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFFC5200),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFFFC5200)
                )
            )

            Divider(color = Color.White.copy(alpha = 0.1f))

            // Desafío Section
            Column {
                Text(
                    "Desafío y Visibilidad",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )

                Spacer(Modifier.height(12.dp))

                // Is Desafio Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "¿Es un Desafío Temporal? 🏆",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Tendrá fecha de inicio y fin",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isDesafio,
                        onCheckedChange = { isDesafio = it }
                    )
                }

                // Use Dates Toggle (if Desafio)
                if (isDesafio) {
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "¿Definir fechas de vigencia?",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Switch(
                            checked = useDates,
                            onCheckedChange = { useDates = it }
                        )
                    }

                    // Date Pickers
                    if (useDates) {
                        Spacer(Modifier.height(12.dp))

                        // Start Date
                        OutlinedButton(
                            onClick = {
                                val calendar = Calendar.getInstance()
                                calendar.time = startDate

                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val newCal = Calendar.getInstance()
                                        newCal.set(year, month, day)
                                        startDate = newCal.time
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("Fecha Inicio", fontSize = 12.sp, color = Color.Gray)
                                Text(dateFormatter.format(startDate), fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // End Date
                        OutlinedButton(
                            onClick = {
                                val calendar = Calendar.getInstance()
                                calendar.time = endDate

                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val newCal = Calendar.getInstance()
                                        newCal.set(year, month, day)
                                        endDate = newCal.time
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("Fecha Fin", fontSize = 12.sp, color = Color.Gray)
                                Text(dateFormatter.format(endDate), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Is Global Toggle (only for super admin)
                // TODO: Add permission check here
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.White.copy(alpha = 0.05f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Benchmark Global 🌎",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Visible para todos los gimnasios",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = isGlobal,
                        onCheckedChange = { isGlobal = it }
                    )
                }
            }

            Divider(color = Color.White.copy(alpha = 0.1f))

            // Save Button
            Button(
                onClick = {
                    val benchmarkData = BenchmarkWod(
                        id = benchmarkToEdit?.id ?: "",
                        name = name,
                        description = description,
                        strategy = strategy,
                        scoreType = scoreType,
                        gym_id = "", // Handled by AdminViewModel
                        isDesafio = isDesafio,
                        startDate = if (isDesafio && useDates) com.google.firebase.Timestamp(startDate) else null,
                        endDate = if (isDesafio && useDates) com.google.firebase.Timestamp(endDate) else null,
                        isGlobal = isGlobal
                    )

                    adminViewModel.saveBenchmark(benchmarkData)
                    navController.popBackStack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFC5200)
                ),
                shape = RoundedCornerShape(12.dp),
                enabled = name.trim().isNotEmpty()
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        }
    }
}
