package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aquiles.crosschapp.presentation.components.GlassCard

@Composable
fun SetupProgressCard(
    progress: Map<String, Boolean>,
    skipped: Map<String, Boolean> = emptyMap(),
    onStepTapped: (SetupStep) -> Unit,
    onStepSkipped: (SetupStep) -> Unit
) {
    val completedCount = SetupStep.entries.count { progress[it.toKey()] == true }
    val skippedCount = SetupStep.entries.count { skipped[it.toKey()] == true }
    val totalCount = SetupStep.entries.size
    val progressPercentage = if (totalCount > 0) (completedCount + skippedCount).toFloat() / totalCount.toFloat() else 0f

    val pendingSteps = SetupStep.entries.filter { progress[it.toKey()] != true && skipped[it.toKey()] != true }

    var selectedStepForConfig by remember { mutableStateOf<SetupStep?>(null) }
    var showingEducationalAlert by remember { mutableStateOf(false) }

    if (pendingSteps.isNotEmpty()) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Configura tu Gimnasio",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Completado al ${(progressPercentage * 100).toInt()}%",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    CircularProgressIndicator(
                        progress = { progressPercentage },
                        modifier = Modifier.size(40.dp),
                        color = Color.Green,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        strokeWidth = 4.dp
                    )
                }

                // Carousel
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pendingSteps) { step ->
                        SetupStepCardItem(
                            step = step,
                            onConfigurar = {
                                selectedStepForConfig = step
                                showingEducationalAlert = true
                            },
                            onOmitir = { onStepSkipped(step) }
                        )
                    }
                }
            }
        }

        if (showingEducationalAlert && selectedStepForConfig != null) {
            AlertDialog(
                onDismissRequest = { showingEducationalAlert = false },
                title = { Text("💡 Dónde encontrar esto") },
                text = { Text("Para volver aquí en el futuro:\n\n${selectedStepForConfig!!.educationalLocation}") },
                confirmButton = {
                    TextButton(onClick = {
                        showingEducationalAlert = false
                        onStepTapped(selectedStepForConfig!!)
                    }) {
                        Text("Entendido, ir a configurar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showingEducationalAlert = false }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                },
                containerColor = Color(0xFF1C1C1E),
                titleContentColor = Color.White,
                textContentColor = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun SetupStepCardItem(
    step: SetupStep,
    onConfigurar: () -> Unit,
    onOmitir: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(260.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(16.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = Color(0xFFFC5200),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = step.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Text(
                text = step.description,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.height(36.dp) // Alineación de altura fija
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = onOmitir,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Omitir", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                }
                Button(
                    onClick = onConfigurar,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFC5200)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Configurar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
