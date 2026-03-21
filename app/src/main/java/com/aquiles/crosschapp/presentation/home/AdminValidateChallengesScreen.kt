package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aquiles.crosschapp.data.model.ChallengeResult
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminValidateChallengesScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    adminViewModel: AdminViewModel
) {
    val pendingResults by adminViewModel.pendingResults.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        adminViewModel.loadPendingChallengeResults()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Validar Desafíos", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (pendingResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.5f))
                        Spacer(Modifier.height(16.dp))
                        Text("No hay resultados pendientes", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(pendingResults) { result ->
                        ValidationResultCard(
                            result = result,
                            dateFormat = dateFormat,
                            onApprove = { adminViewModel.validateChallengeResult(result, true) },
                            onReject = { adminViewModel.validateChallengeResult(result, false) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ValidationResultCard(
    result: ChallengeResult,
    dateFormat: SimpleDateFormat,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar Placeholder o inicial
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        result.userName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("${result.userName} ${result.userLastName}", fontWeight = FontWeight.Bold, color = Color.White)
                    Text(result.date?.let { dateFormat.format(it) } ?: "", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Info Desafío
            Column {
                Text("DESAFÍO", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF9500), fontWeight = FontWeight.Bold)
                Text(result.challengeName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(Modifier.height(12.dp))

            // Resultado
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column {
                    Text("RESULTADO", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text(
                        result.score,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                if (result.isRx) {
                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("RX", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            if (result.notes.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("“${result.notes}”", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }

            if (!result.videoUrl.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { /* TODO: Open Video Player */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ver Video de Prueba", fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Acciones
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                ) {
                    Text("RECHAZAR", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC5200)) // Strava Orange
                ) {
                    Text("APROBAR", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
