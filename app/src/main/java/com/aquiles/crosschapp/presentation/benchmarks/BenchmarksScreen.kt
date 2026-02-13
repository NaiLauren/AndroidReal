package com.aquiles.crosschapp.presentation.benchmarks

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aquiles.crosschapp.data.model.BenchmarkResult
import com.aquiles.crosschapp.data.model.BenchmarkWod
import com.aquiles.crosschapp.presentation.components.SmartScoreInput
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import com.aquiles.crosschapp.presentation.viewmodel.BenchmarkWodsState
import com.aquiles.crosschapp.presentation.viewmodel.PerformanceViewModel
import com.aquiles.crosschapp.presentation.viewmodel.UserSession
import com.aquiles.crosschapp.presentation.viewmodel.BenchmarkSaveState
import java.util.Date

private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.85f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.15f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarksScreen(
    adminViewModel: AdminViewModel,
    performanceViewModel: PerformanceViewModel,
    onBack: () -> Unit
) {
    val benchmarkWodsState by adminViewModel.benchmarkWodsState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedWod by remember { mutableStateOf<BenchmarkWod?>(null) }
    
    // Effects
    LaunchedEffect(Unit) {
        adminViewModel.loadBenchmarkWods()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BENCHMARKS", fontWeight = FontWeight.Bold, letterSpacing = 1.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Background
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black, Color(0xFF121212)))))

            Column(modifier = Modifier.fillMaxSize()) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar Benchmark...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ColorGlassSurface,
                        unfocusedContainerColor = ColorGlassSurface,
                        focusedBorderColor = ColorPrimaryAction,
                        unfocusedBorderColor = ColorBorder,
                        focusedTextColor = Color.White,
                        cursorColor = ColorPrimaryAction
                    )
                )

                // List
                if (benchmarkWodsState is BenchmarkWodsState.Loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ColorPrimaryAction)
                    }
                } else if (benchmarkWodsState is BenchmarkWodsState.Success) {
                    val wods = (benchmarkWodsState as BenchmarkWodsState.Success).wods.filter {
                        it.name.contains(searchQuery, ignoreCase = true)
                    }
                    
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(wods) { wod ->
                             BenchmarkCard(wod = wod, onClick = { selectedWod = wod })
                        }
                    }
                }
            }
            
            // Register Sheet
            if (selectedWod != null) {
                ModalBottomSheet(
                    onDismissRequest = { selectedWod = null },
                    containerColor = Color(0xFF1C1C1E),
                    contentColor = Color.White,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = Color.Gray) }
                ) {
                    BenchmarkRegisterContent(
                        wod = selectedWod!!, 
                        performanceViewModel = performanceViewModel,
                        onSuccess = { selectedWod = null }
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun BenchmarkCard(wod: BenchmarkWod, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface),
        border = BorderStroke(1.dp, ColorBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Nombre del benchmark + Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = wod.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // 🆕 BADGE: GLOBAL o LOCAL
                    if (wod.gym_id.isEmpty()) {
                        // Benchmark Global
                        Text(
                            text = "GLOBAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700), // Oro
                            modifier = Modifier
                                .background(
                                    Color(0xFFFFD700).copy(alpha = 0.2f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    } else {
                        // Benchmark Local
                        Text(
                            text = "LOCAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2196F3), // Azul
                            modifier = Modifier
                                .background(
                                    Color(0xFF2196F3).copy(alpha = 0.2f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = wod.measurementUnit,
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorPrimaryAction,
                    fontWeight = FontWeight.Bold
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = ColorTextSecondary)
        }
    }
}

@Composable
fun BenchmarkRegisterContent(
    wod: BenchmarkWod,
    performanceViewModel: PerformanceViewModel,
    onSuccess: () -> Unit
) {
    var score by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isRx by remember { mutableStateOf(true) }
    var isPublic by remember { mutableStateOf(true) }
    val currentUser by UserSession.currentUser.collectAsState()
    val saveState by performanceViewModel.saveBenchmarkState.collectAsState()
    
    // Reset state on success
    LaunchedEffect(saveState) {
        if (saveState is BenchmarkSaveState.Success) {
            onSuccess()
            performanceViewModel.resetSaveState()
        }
    }

    Column(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FitnessCenter, null, tint = ColorPrimaryAction, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = wod.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
        }
        
        Text(text = wod.description, style = MaterialTheme.typography.bodyMedium, color = ColorTextSecondary)

        HorizontalDivider(color = ColorBorder, thickness = 1.dp)

        // Inputs
        SmartScoreInput(measurementUnit = wod.measurementUnit, score = score, onScoreChange = { score = it })
        
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notas", color = ColorTextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ColorGlassSurface,
                unfocusedContainerColor = ColorGlassSurface,
                focusedBorderColor = Color.White.copy(alpha = 0.5f),
                unfocusedBorderColor = ColorBorder,
                focusedTextColor = ColorTextPrimary,
                unfocusedTextColor = ColorTextPrimary,
                cursorColor = ColorPrimaryAction
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Toggles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isRx = !isRx }) {
                Checkbox(checked = isRx, onCheckedChange = { isRx = it }, colors = CheckboxDefaults.colors(checkedColor = ColorPrimaryAction))
                Text("RX", color = Color.White, fontWeight = FontWeight.Bold)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { isPublic = !isPublic }) {
                Checkbox(checked = isPublic, onCheckedChange = { isPublic = it }, colors = CheckboxDefaults.colors(checkedColor = ColorPrimaryAction))
                Text("Publicar", color = Color.White)
            }
        }

        // Action Button
        Button(
            onClick = {
                  currentUser?.let { user ->
                      performanceViewModel.saveBenchmarkResult(
                          benchmark = wod,
                          score = score,
                          isRx = isRx,
                          notes = notes,
                          date = Date(),
                          isPublic = isPublic
                      )
                  }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction),
            enabled = score.isNotBlank() && saveState !is BenchmarkSaveState.Loading
        ) {
            if (saveState is BenchmarkSaveState.Loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("GUARDAR RESULTADO", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
