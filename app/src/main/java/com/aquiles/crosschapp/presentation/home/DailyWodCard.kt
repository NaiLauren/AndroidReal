package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.aquiles.crosschapp.data.model.GymClass
import com.aquiles.crosschapp.data.model.WodResult
import com.aquiles.crosschapp.ui.theme.LocalPrimaryColor
import java.text.SimpleDateFormat
import java.util.*

private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.65f)
private val ColorBorder = Color.White.copy(alpha = 0.15f)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorInputBackground = Color(0xFF2C2C2E)

@Composable
fun DailyWodCard(
    gymClass: GymClass,
    imageUrl: String?,
    existingResult: WodResult?,
    onSaveResult: (result: String, isRx: Boolean, notes: String) -> Unit,
    isSavingResult: Boolean
) {
    var userResult by remember(existingResult) { mutableStateOf(existingResult?.score ?: "") }
    var userNotes by remember(existingResult) { mutableStateOf(existingResult?.notes ?: "") }
    var isRx by remember(existingResult) { mutableStateOf(existingResult?.isRx ?: true) }
    var isExpanded by remember { mutableStateOf(false) }

    val fallbackImageUrl = "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?q=80&w=2070"
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val classColor = remember(gymClass.hexColor) {
        try {
            Color(android.graphics.Color.parseColor(gymClass.hexColor))
        } catch (e: Exception) {
            Color(0xFFFC5200)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .background(classColor.copy(alpha = 0.20f), RoundedCornerShape(24.dp))
            .clickable { isExpanded = !isExpanded }, // Expandible
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, ColorBorder),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f))
    ) {
        Column {
            // Header Image Section
            Box(modifier = Modifier.height(200.dp)) {
                SubcomposeAsyncImage(
                    model = if (!imageUrl.isNullOrBlank()) imageUrl else fallbackImageUrl,
                    contentDescription = "WOD Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = LocalPrimaryColor.current) } }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color(0xFF1C1C1E)), startY = 200f))
        )

        // Time & Coach Badges
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(text = "${timeFormatter.format(gymClass.dateTime ?: Date())} HS", color = LocalPrimaryColor.current, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
            if (gymClass.coachName.isNotBlank()) {
                Box(modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(text = "Coach: ${gymClass.coachName}", color = Color.White, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Title
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(text = "TU CLASE DE HOY", style = MaterialTheme.typography.labelSmall, color = LocalPrimaryColor.current, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(text = gymClass.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = ColorTextPrimary)
        }

                // Expand Indicator
                if (!isExpanded) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Expandir",
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).background(Color.Black.copy(0.5f), androidx.compose.foundation.shape.CircleShape)
                    )
                }
            }

            // Description & Input (Expanded Only)
            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(text = gymClass.description, color = ColorTextSecondary, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)

                    HorizontalDivider(color = ColorBorder, thickness = 1.dp)

                    WodGlassTextField(value = userResult, onValueChange = { userResult = it }, label = "Resultado (Tiempo/Reps/Lbs)")
                    WodGlassTextField(value = userNotes, onValueChange = { userNotes = it }, label = "Notas")

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { isRx = !isRx }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = isRx, onCheckedChange = { isRx = it }, colors = CheckboxDefaults.colors(checkedColor = LocalPrimaryColor.current, uncheckedColor = ColorTextSecondary, checkmarkColor = Color.White))
                            Text("RX", fontWeight = FontWeight.Bold, color = if (isRx) ColorTextPrimary else ColorTextSecondary)
                        }
                        
                        val isUpdating = existingResult != null
                        Button(
                            onClick = { onSaveResult(userResult, isRx, userNotes) },
                            enabled = userResult.isNotBlank() && !isSavingResult,
                            colors = ButtonDefaults.buttonColors(containerColor = LocalPrimaryColor.current, disabledContainerColor = LocalPrimaryColor.current.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            if (isSavingResult) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White) 
                            } else {
                                Text(if (isUpdating) "Actualizar" else "Guardar Resultado", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WodGlassTextField(
    value: String, 
    onValueChange: (String) -> Unit, 
    label: String,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = ColorTextSecondary.copy(0.5f)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = minLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = ColorTextPrimary,
            unfocusedTextColor = ColorTextPrimary,
            cursorColor = LocalPrimaryColor.current,
            focusedBorderColor = LocalPrimaryColor.current,
            unfocusedBorderColor = ColorBorder,
            focusedContainerColor = ColorInputBackground,
            unfocusedContainerColor = ColorInputBackground
        ),
        shape = RoundedCornerShape(12.dp)
    )
}



