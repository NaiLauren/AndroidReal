package com.aquiles.crosschapp.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

// --- CONSTANTS COPY ---
private val ColorGlassInput = Color(0xFFFFFFFF).copy(alpha = 0.07f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.15f)

@Composable
fun SmartScoreInput(
    measurementUnit: String,
    score: String,
    onScoreChange: (String) -> Unit
) {
    if (measurementUnit == "TIME") {
        StopwatchInput(
            currentScore = score,
            onScoreChange = onScoreChange
        )
    } else {
        // Peso, Reps, etc.
        val label = when(measurementUnit) {
            "WEIGHT" -> "Peso (kg)"
            "REPS" -> "Repeticiones"
            "DISTANCE" -> "Distancia (mts)"
            "PERCENTAGE" -> "Porcentaje %"
            else -> "Tu Marca"
        }
        val keyboardType = if (measurementUnit == "WEIGHT" || measurementUnit == "REPS") 
             KeyboardType.Number else KeyboardType.Text

        GlassTextField(
            value = score, 
            onValueChange = onScoreChange, 
            label = label,
            keyboardType = keyboardType
        )
    }
}

@Composable
fun StopwatchInput(
    currentScore: String,
    onScoreChange: (String) -> Unit
) {
    var isRunning by remember { mutableStateOf(false) }
    var elapsedTimeSeconds by remember { mutableStateOf(0L) }
    
    // Timer Loop
    LaunchedEffect(isRunning) {
        if (isRunning) {
            val startTime = System.currentTimeMillis() - (elapsedTimeSeconds * 1000)
            while (isRunning) {
                delay(100L) // Update every 100ms for smoothness
                val now = System.currentTimeMillis()
                elapsedTimeSeconds = (now - startTime) / 1000
            }
        }
    }

    // Formatter HH:MM:SS or MM:SS
    val formattedTime = remember(elapsedTimeSeconds) {
        val h = elapsedTimeSeconds / 3600
        val m = (elapsedTimeSeconds % 3600) / 60
        val s = elapsedTimeSeconds % 60
        if (h > 0) String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s) 
        else String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("TIEMPO", style = MaterialTheme.typography.labelSmall, color = ColorTextSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom=4.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp) // Más alto para énfasis
                .background(ColorGlassInput, RoundedCornerShape(16.dp))
                .border(1.dp, ColorBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            
            Box(Modifier.weight(1f)) {
               if (isRunning) {
                   Text(
                       text = formattedTime,
                       style = MaterialTheme.typography.displayMedium, // Más grande
                       color = ColorPrimaryAction,
                       fontWeight = FontWeight.Black,
                       letterSpacing = 2.sp
                   )
               } else {
                   androidx.compose.foundation.text.BasicTextField(
                       value = currentScore,
                       onValueChange = onScoreChange,
                       textStyle = TextStyle(
                           color = ColorTextPrimary, 
                           fontSize = 32.sp, // Más grande para input manual
                           fontWeight = FontWeight.Bold
                       ),
                       cursorBrush = androidx.compose.ui.graphics.SolidColor(ColorPrimaryAction),
                       decorationBox = { innerTextField ->
                           if (currentScore.isEmpty()) {
                               Text("00:00", color = ColorTextSecondary.copy(alpha = 0.5f), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                           }
                           innerTextField()
                       }
                   )
               }
            }
            
            // Controles
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isRunning) {
                    FilledIconButton(
                        onClick = { 
                            isRunning = false
                            onScoreChange(formattedTime) 
                        },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFFFCC00))
                    ) {
                        Icon(Icons.Default.Pause, null, tint = Color.Black)
                    }
                } else {
                    FilledIconButton(
                        onClick = { isRunning = true },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                    }
                }
                
                if (!isRunning && elapsedTimeSeconds > 0) {
                     Spacer(modifier = Modifier.width(16.dp))
                     IconButton(onClick = { 
                         isRunning = false
                         elapsedTimeSeconds = 0
                         onScoreChange("")
                     }) {
                        Icon(Icons.Default.Refresh, null, tint = ColorTextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun GlassTextField(
    value: String, 
    onValueChange: (String) -> Unit, 
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = ColorTextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = ColorGlassInput, unfocusedContainerColor = ColorGlassInput, focusedBorderColor = Color.White.copy(alpha = 0.5f), unfocusedBorderColor = ColorBorder, focusedTextColor = ColorTextPrimary, unfocusedTextColor = ColorTextPrimary, cursorColor = ColorPrimaryAction),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}
