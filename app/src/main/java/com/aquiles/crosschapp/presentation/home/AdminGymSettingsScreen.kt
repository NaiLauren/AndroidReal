package com.aquiles.crosschapp.presentation.home

import com.aquiles.crosschapp.presentation.components.GlassCard
import android.widget.Toast
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.aquiles.crosschapp.presentation.viewmodel.UserSession
import com.aquiles.crosschapp.data.model.Gym
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import com.aquiles.crosschapp.presentation.components.animatedGlowGradient

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.45f)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.15f)
private val ColorPrimaryAction = Color(0xFFFC5200)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminGymSettingsScreen(
    navController: NavController,
    adminViewModel: AdminViewModel,
    innerPadding: PaddingValues,
    setupStepKey: String? = null
) {
    val context = LocalContext.current
    var showSetupPopup by remember { mutableStateOf(setupStepKey != null) }

    if (showSetupPopup) {
        SetupStep.entries.find { it.toKey() == setupStepKey }?.let { step ->
            AlertDialog(
                onDismissRequest = { showSetupPopup = false },
                title = { Text(step.title, color = ColorTextPrimary) },
                text = { Text(step.description, color = ColorTextSecondary) },
                confirmButton = {
                    Button(onClick = { showSetupPopup = false }, colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction)) {
                        Text("Entendido", color = Color.White)
                    }
                },
                containerColor = ColorGlassSurface
            )
        }
    }

    val currentGymState = UserSession.currentGym.collectAsState()
    var isLoading by remember { mutableStateOf(false) }

    val initialColor: String = remember(currentGymState.value) {
        val color = currentGymState.value?.primaryColor
        if (color != null && color.isNotBlank()) color else "#FC5200"
    }

    var selectedColorHex: String by remember(currentGymState.value) {
        mutableStateOf(initialColor)
    }

    // Paleta ampliada tipo iOS con muchos tonos
    val presetColors = listOf(
        // Rojos y naranjas
        "#FF3B30", "#FF6B35", "#FC5200", "#FF9500", "#FFCC00",
        // Verdes
        "#34C759", "#30D158", "#00C7BE", "#32ADE6", "#007AFF",
        // Azules y morados
        "#5856D6", "#AF52DE", "#FF2D55", "#FF375F", "#E91E63",
        // Oscuros y especiales
        "#1C1C1E", "#636366", "#8E8E93", "#FFFFFF", "#FFD700"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Ajustes de Marca", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ColorTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f)
                )
            )
        },
        containerColor = Color.Transparent
    ) { localPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(localPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Header icon animado
            GlassCard(
                modifier = Modifier
                    .size(100.dp)
                    .animatedGlowGradient(
                        color1 = try { selectedColorHex.toColor() } catch (e: Exception) { ColorPrimaryAction },
                        color2 = Color(0xFF8B00FF),
                        durationMs = 4000
                    ),
                shape = CircleShape
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = try { selectedColorHex.toColor() } catch (e: Exception) { ColorPrimaryAction },
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Text(
                "Personalización del Gimnasio",
                style = MaterialTheme.typography.titleLarge,
                color = ColorTextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "Elige el color principal de tu marca. Se usa en botones y destacados.",
                style = MaterialTheme.typography.bodyMedium,
                color = ColorTextSecondary,
                textAlign = TextAlign.Center
            )

            // Acceso a imágenes de actividad
            GlassCard(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate("admin_manage_activity_images") }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).background(ColorPrimaryAction.copy(0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Image, null, tint = ColorPrimaryAction, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Imágenes de Actividad Diaria", color = ColorTextPrimary, fontWeight = FontWeight.Bold)
                        Text("Personaliza las fotos de Lunes a Domingo.", style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)
                    }
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = ColorTextSecondary, modifier = Modifier.size(16.dp).then(Modifier))
                }
            }

            // Vista previa GRANDE del color
            GlassCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
                    .animatedGlowGradient(
                        color1 = try { selectedColorHex.toColor() } catch (e: Exception) { ColorPrimaryAction },
                        color2 = Color(0xFF8B00FF),
                        durationMs = 5000
                    )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Vista Previa", color = ColorTextSecondary, style = MaterialTheme.typography.labelLarge)

                    // Muestra del color grande
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        try { selectedColorHex.toColor() } catch (e: Exception) { ColorPrimaryAction },
                                        try { selectedColorHex.toColor().copy(alpha = 0.4f) } catch (e: Exception) { ColorPrimaryAction.copy(0.4f) }
                                    )
                                ),
                                CircleShape
                            )
                            .border(3.dp, Color.White.copy(0.3f), CircleShape)
                    )

                    Button(
                        onClick = {},
                        colors = ButtonDefaults.buttonColors(
                            containerColor = try { selectedColorHex.toColor() } catch (e: Exception) { ColorPrimaryAction }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Botón Principal", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        "Texto Destacado",
                        color = try { selectedColorHex.toColor() } catch (e: Exception) { ColorPrimaryAction },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            // Selector de color HEX personalizado
            GlassCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Color Personalizado (HEX)", color = ColorTextPrimary, fontWeight = FontWeight.SemiBold)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Preview del color actual
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    try { selectedColorHex.toColor() } catch (e: Exception) { ColorPrimaryAction },
                                    RoundedCornerShape(12.dp)
                                )
                                .border(2.dp, ColorBorder, RoundedCornerShape(12.dp))
                        )
                        OutlinedTextField(
                            value = selectedColorHex,
                            onValueChange = {
                                if (it.length <= 9) selectedColorHex = it
                            },
                            label = { Text("Ej: #FC5200", color = ColorTextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = try { selectedColorHex.toColor() } catch (e: Exception) { ColorPrimaryAction },
                                unfocusedBorderColor = ColorBorder,
                                focusedTextColor = ColorTextPrimary,
                                unfocusedTextColor = ColorTextPrimary,
                                cursorColor = ColorPrimaryAction
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Paleta de colores premium
            GlassCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Colores Sugeridos", color = ColorTextPrimary, fontWeight = FontWeight.SemiBold)

                    // 5 columnas de colores
                    val rows = presetColors.chunked(5)
                    rows.forEach { rowColors ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowColors.forEach { hex ->
                                val color = try { hex.toColor() } catch (e: Exception) { Color.Gray }
                                val isSelected = hex.equals(selectedColorHex, ignoreCase = true)

                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color.White.copy(0.2f),
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColorHex = hex },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            // Relleno si la fila tiene menos de 5
                            repeat(5 - rowColors.size) {
                                Spacer(Modifier.size(52.dp))
                            }
                        }
                    }
                }
            }

            // Botón Guardar — siempre al final del scroll, bien ubicado
            Button(
                onClick = {
                    isLoading = true
                    adminViewModel.updateGymPrimaryColor(selectedColorHex) { success ->
                        isLoading = false
                        if (success) {
                            Toast.makeText(context, "✅ Color actualizado", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Error al actualizar", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = try { selectedColorHex.toColor() } catch (e: Exception) { ColorPrimaryAction }
                ),
                shape = RoundedCornerShape(14.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                } else {
                    Text("Guardar Cambios", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun String.toColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor(this))
    } catch (e: Exception) {
        Color(0xFFFC5200)
    }
}
