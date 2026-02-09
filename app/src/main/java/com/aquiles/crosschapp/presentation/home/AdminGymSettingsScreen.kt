package com.aquiles.crosschapp.presentation.home

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aquiles.crosschapp.presentation.viewmodel.UserSession
import com.aquiles.crosschapp.data.model.Gym
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.85f)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.15f)
private val ColorPrimaryAction = Color(0xFFFC5200)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminGymSettingsScreen(
    navController: NavController,
    adminViewModel: AdminViewModel,
    innerPadding: PaddingValues
) {
    val context = LocalContext.current
    val currentGymState = UserSession.currentGym.collectAsState()
    var isLoading by remember { mutableStateOf(false) }
    
    val initialColor: String = remember(currentGymState.value) {
        val gym = currentGymState.value
        val color = gym?.primaryColor
        if (color != null && color.isNotBlank()) color else "#FFA500"
    }
    
    var selectedColorHex: String by remember(currentGymState.value) { 
        mutableStateOf(initialColor) 
    }

    val presetColors = listOf(
        "#FFA500", // Orange (Brand)
        "#2196F3", // Blue
        "#F44336", // Red
        "#4CAF50", // Green
        "#9C27B0", // Purple
        "#E91E63", // Pink
        "#FFEB3B", // Yellow
        "#00BCD4", // Cyan
        "#009688"  // Teal
    )


    // Helpers moved to top level


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Ajustes de Marca", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ColorTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { localPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(localPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = selectedColorHex.toColor(),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Personalización del Gimnasio",
                    style = MaterialTheme.typography.titleLarge,
                    color = ColorTextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Elige el color principal de tu marca. Este color se usará en botones y destacados.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // --- NEW: Activity Images Management ---
                Card(
                    colors = CardDefaults.cardColors(containerColor = ColorGlassSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().clickable { navController.navigate("admin_manage_activity_images") }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Image, null, tint = ColorPrimaryAction, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Imágenes de Actividad Diaria", color = ColorTextPrimary, fontWeight = FontWeight.Bold)
                            Text("Personaliza las fotos de Lunes a Domingo.", style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                // Color Picker Grid

                // Color Picker Grid
                Text(
                    "Colores Sugeridos",
                    style = MaterialTheme.typography.titleMedium,
                    color = ColorTextPrimary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(16.dp))

                OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    presetColors.forEach { hex ->
                        val color = hex.toColor()
                        val isSelected = hex.equals(selectedColorHex, ignoreCase = true)

                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Preview Section
                Card(
                    colors = CardDefaults.cardColors(containerColor = ColorGlassSurface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Vista Previa", color = ColorTextSecondary, fontSize = MaterialTheme.typography.labelMedium.fontSize)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(containerColor = selectedColorHex.toColor())
                        ) {
                            Text("Botón Principal")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Texto Destacado", color = selectedColorHex.toColor(), fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Save Button
                Button(
                    onClick = {
                        isLoading = true
                        adminViewModel.updateGymPrimaryColor(selectedColorHex) { success ->
                            isLoading = false
                            if (success) {
                                Toast.makeText(context, "Color actualizado", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Error al actualizar", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = selectedColorHex.toColor()),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Guardar Cambios", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun Color.toHex(): String {
    return String.format("#%06X", (0xFFFFFF and this.hashCode()))
}

private fun String.toColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color(0xFFFFA500)
    }
}
