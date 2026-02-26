package com.aquiles.crosschapp.presentation.home

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.data.model.Competition
import com.aquiles.crosschapp.data.model.CompetitionStatus
import com.aquiles.crosschapp.data.model.GymClass
import com.aquiles.crosschapp.presentation.viewmodel.CompetitionDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.aquiles.crosschapp.presentation.components.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCompetitionDetailScreen(
    navController: NavController,
    competitionId: String,
    viewModel: CompetitionDetailViewModel = viewModel()
) {
    val competition by viewModel.competition.collectAsState()
    val linkedClasses by viewModel.linkedClasses.collectAsState()
    val isLoadingClasses by viewModel.isLoadingClasses.collectAsState()
    
    var showClassSelector by remember { mutableStateOf(false) }

    LaunchedEffect(competitionId) {
        viewModel.loadCompetition(competitionId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Detalle de Competencia", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.70f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        if (competition == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFC5200))
            }
        } else {
            val comp = competition!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header Section
                CompetitionHeader(comp)

                // Linked Classes Section
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Clases / Eventos", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        
                        Button(
                            onClick = { showClassSelector = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC5200)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Agregar", style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    if (isLoadingClasses) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (linkedClasses.isEmpty()) {
                        GlassCard {
                            Box(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "No hay eventos vinculados aún. Agrega clases para que sumen puntos.",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            linkedClasses.forEach { gymClass ->
                                LinkedClassItem(gymClass, onUnlink = { viewModel.unlinkClass(gymClass.documentId) })
                            }
                        }
                    }
                }

                // Prize & Actions Section
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Premios y Acciones", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    
                    GlassCard(
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Premio", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(comp.prizeDescription ?: "Sin premio definido", style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Recompensa", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("${comp.xpReward ?: 0} XP", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (comp.isActive) {
                        Button(
                            onClick = { viewModel.finishCompetition() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC0000)), // Red for destructive/finish
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Icon(Icons.Default.Flag, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Finalizar y Premiar", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        GlassCard(
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("Competencia Finalizada", color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClassSelector && competition != null) {
        ClassSelectorDialog(
            gymId = competition!!.gymId,
            startDate = competition!!.startDate ?: Date(),
            endDate = competition!!.endDate ?: Date(),
            existingIds = linkedClasses.map { it.documentId }.toSet(),
            onDismiss = { showClassSelector = false },
            onConfirm = { selectedClasses ->
                viewModel.linkClasses(selectedClasses)
                showClassSelector = false
            },
            viewModel = viewModel
        )
    }
}

@Composable
fun CompetitionHeader(comp: Competition) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = comp.getTypeEnum().value.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            
            val status = comp.getStatus()
            val statusColor = when (status) {
                CompetitionStatus.ONGOING -> Color.Green
                CompetitionStatus.UPCOMING -> Color.Blue
                CompetitionStatus.FINISHED -> Color.Red
                else -> Color.Gray
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(statusColor, CircleShape))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = when(status) {
                        CompetitionStatus.ONGOING -> "En Curso"
                        CompetitionStatus.UPCOMING -> "Próxima"
                        CompetitionStatus.FINISHED -> "Finalizada"
                        else -> "Inactiva"
                    },
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Text(
            text = comp.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            val dateFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
            val start = comp.startDate ?: Date()
            val end = comp.endDate ?: Date()
            Text(
                "${dateFormat.format(start)} - ${dateFormat.format(end)}",
                color = Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
    }
}

@Composable
fun LinkedClassItem(gymClass: GymClass, onUnlink: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        // Time Box
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(50.dp)
        ) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dayFormat = SimpleDateFormat("d MMM", Locale.getDefault())
            val date = gymClass.dateTime ?: Date()
            
            Text(timeFormat.format(date), color = Color(0xFFFC5200), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(dayFormat.format(date), color = Color.Gray, style = MaterialTheme.typography.labelSmall)
        }
        
        Box(
            Modifier
                .width(1.dp)
                .height(40.dp)
                .background(Color.White.copy(alpha = 0.2f))
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(gymClass.name, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(gymClass.coachName, color = Color.Gray, style = MaterialTheme.typography.labelSmall)
            }
            
            Spacer(Modifier.height(4.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Participants
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFFFC5200), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(2.dp))
                    // Assuming EnrolledUserIds count + attendedUserIds count or similiar logic from iOS
                    // iOS uses attendees.count. GymClass has enrolledUserIds (registered).
                    Text("${gymClass.enrolledUserIds.size} part.", color = Color(0xFFFC5200), style = MaterialTheme.typography.labelSmall)
                }
                
                // Score Type
                if (!gymClass.wodScoreType.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFC5200), modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(2.dp))
                        Text(gymClass.wodScoreType, color = Color(0xFFFC5200), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        
        IconButton(onClick = onUnlink) {
            Icon(Icons.Default.Delete, contentDescription = "Desvincular", tint = Color.Red.copy(alpha = 0.7f))
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassSelectorDialog(
    gymId: String,
    startDate: Date,
    endDate: Date,
    existingIds: Set<String>,
    onDismiss: () -> Unit,
    onConfirm: (List<GymClass>) -> Unit,
    viewModel: CompetitionDetailViewModel
) {
    var selectedDate by remember { mutableStateOf(startDate) } // Start with competition start date
    val classesForDate by viewModel.classesForDate.collectAsState()
    val isLoading by viewModel.isLoadingSelector.collectAsState()
    
    // Selection state
    val selectedClasses = remember { mutableStateMapOf<String, GymClass>() }

    LaunchedEffect(selectedDate) {
        viewModel.loadClassesForDate(selectedDate, gymId)
    }

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate.time)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = Date(it) }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar Clases") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Date Selector Button
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(selectedDate))
                }

                if (isLoading) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (classesForDate.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("No hay clases para este día", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false), // Allows shrinking if content is small
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(classesForDate) { gymClass ->
                            val isAlreadyLinked = existingIds.contains(gymClass.documentId)
                            val isSelected = selectedClasses.containsKey(gymClass.documentId)
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isAlreadyLinked) {
                                        if (isSelected) {
                                            selectedClasses.remove(gymClass.documentId)
                                        } else {
                                            selectedClasses[gymClass.documentId] = gymClass
                                        }
                                    }
                                    .background(if (isSelected) Color(0xFFFC5200).copy(alpha = 0.1f) else Color.Transparent)
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    tint = if (isAlreadyLinked) Color.Gray else if (isSelected) Color(0xFFFC5200) else Color.Gray
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(gymClass.name, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${SimpleDateFormat("HH:mm", Locale.getDefault()).format(gymClass.dateTime ?: Date())} - ${gymClass.coachName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                                if (isAlreadyLinked) {
                                    Spacer(Modifier.weight(1f))
                                    Text("Vinculado", color = Color.Green, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedClasses.values.toList()) },
                enabled = selectedClasses.isNotEmpty()
            ) {
                Text("Guardar (${selectedClasses.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
