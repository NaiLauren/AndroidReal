package com.aquiles.crosschapp.presentation.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import coil.compose.SubcomposeAsyncImage
import com.aquiles.crosschapp.data.model.GymClass
import com.aquiles.crosschapp.data.model.WodResult
import com.aquiles.crosschapp.presentation.components.GlassTextField
import com.aquiles.crosschapp.ui.theme.LocalPrimaryColor
import java.text.SimpleDateFormat
import java.util.*

// Las constantes de diseño se utilizan ahora desde WodComponents.kt con el prefijo WDesign_

@Composable
fun WodPagerCard(
    gymClass: GymClass,
    imageUrl: String?,
    existingResult: WodResult?,
    onSaveResult: (result: String, isRx: Boolean, notes: String, isPublic: Boolean) -> Unit,
    isSavingResult: Boolean,
    isPremium: Boolean = false
) {
    var userResult by remember(existingResult) { mutableStateOf(existingResult?.score ?: "") }
    var userNotes by remember(existingResult) { mutableStateOf(existingResult?.notes ?: "") }
    var isRx by remember(existingResult) { mutableStateOf(existingResult?.isRx ?: true) }
    var isPublic by remember(existingResult) { mutableStateOf(existingResult?.isPublic ?: true) }

    val fallbackImageUrl = "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?q=80&w=2070"
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val hasResult = existingResult != null
    val validationStatus = existingResult?.validationStatus
    
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scaleCard by animateFloatAsState(if (isPremium && isPressed) 0.97f else 1f)
    
    // Premium Border Animation
    val infiniteTransition = rememberInfiniteTransition()
    val rotationAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart)
    )

    val classColor = remember(gymClass.hexColor) {
        try {
            Color(android.graphics.Color.parseColor(gymClass.hexColor))
        } catch (e: Exception) {
            Color(0xFFFC5200) // Fallback al naranja original
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scaleCard)
            .padding(top = if (isPremium) 8.dp else 0.dp)
            .clickable(
                 interactionSource = interactionSource,
                 indication = null,
                 onClick = { if (isPremium) haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
            )
    ) {
        // Glowing Border for Premium (Desert Luxury)
        if (isPremium) {
             Box(
                 modifier = Modifier
                     .matchParentSize()
                     .padding(1.dp)
                     .background(
                         Brush.sweepGradient(
                             colors = listOf(
                                 Color(0xFFD4AF37), 
                                 Color(0xFFF7E7CE), // Retiro Champagne
                                 Color(0xFFD4AF37),
                                 Color(0xFFBF9B30),
                                 Color(0xFFD4AF37)
                             )
                         ),
                         shape = RoundedCornerShape(24.dp)
                     )
                     .graphicsLayer { rotationZ = rotationAnimation }
                     .border(2.5.dp, Color(0xFFD4AF37).copy(alpha = 0.8f), RoundedCornerShape(24.dp))
             )
        }

        com.aquiles.crosschapp.presentation.components.GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (isPremium) Color(0xFF1C1C1E).copy(alpha = 0.95f) else classColor.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(24.dp)
                )
                .then(if (isPremium) Modifier.border(1.dp, Color(0xFFD4AF37).copy(alpha = 0.3f), RoundedCornerShape(24.dp)) else Modifier),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column {
            // ── IMAGEN CABECERA ──────────────────────────────────
            Box(modifier = Modifier.height(200.dp)) {
                SubcomposeAsyncImage(
                    model = if (!imageUrl.isNullOrBlank()) imageUrl else fallbackImageUrl,
                    contentDescription = "WOD Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = LocalPrimaryColor.current) } }
                )
                // Gradiente inferior
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
                )

                // ── Top: hora + coach ───────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Badge HORA (iOS style)
                    Row(
                        modifier = Modifier
                            .background(LocalPrimaryColor.current, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccessTime, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = timeFormatter.format(gymClass.dateTime ?: Date()),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    // Badge HOY (naranja, estilo iOS)
                    Row(
                        modifier = Modifier
                            .background(LocalPrimaryColor.current, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text("HOY", color = Color.White, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium, letterSpacing = 1.sp)
                    }
                }

                // ── Bottom: nombre clase + badge RX resultado ───
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = gymClass.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    if (gymClass.coachName.isNotBlank()) {
                        Text(
                            text = "Coach: ${gymClass.coachName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // ── CONTENIDO ────────────────────────────────────────
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Descripción en fuente monospaced (iOS parity)
                if (gymClass.description.isNotBlank()) {
                    Text(
                        text = gymClass.description,
                        color = WDesignColorTextSecondary,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        lineHeight = 22.sp,
                        modifier = Modifier.heightIn(max = 120.dp)
                    )
                }

                HorizontalDivider(color = WDesignColorBorder, thickness = 1.dp)

                // ── ZONA RESULTADO (si ya existe) ─────────────────
                if (existingResult != null) {
                    // Mostrar resultado guardado + indicador validación
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "RESULTADO REGISTRADO",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF30D158),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Score grande
                                Text(
                                    text = existingResult.score,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    // Badge RX
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (existingResult.isRx) LocalPrimaryColor.current.copy(0.15f) else Color.Gray.copy(0.1f),
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (existingResult.isRx) "NIVEL A" else "NIVEL B",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (existingResult.isRx) LocalPrimaryColor.current else WDesignColorTextSecondary,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    // Badge validación
                                    when (validationStatus) {
                                        "pending" -> BadgeValidation(text = "Pendiente", icon = Icons.Default.AccessTime, bgColor = Color(0xFFFF9500).copy(0.15f), textColor = Color(0xFFFF9500))
                                        "approved" -> BadgeValidation(text = "Validado ✓", icon = null, bgColor = Color(0xFF30D158).copy(0.15f), textColor = Color(0xFF30D158))
                                        "rejected" -> BadgeValidation(text = "Rechazado ✗", icon = null, bgColor = Color(0xFFFF3B30).copy(0.15f), textColor = Color(0xFFFF3B30))
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }
                }

                // ── CAMPOS PARA INGRESAR/ACTUALIZAR RESULTADO ────
                GlassTextField(value = userResult, onValueChange = { userResult = it }, label = if (hasResult) "Nuevo resultado" else "Resultado (Tiempo/Reps)")
                GlassTextField(value = userNotes, onValueChange = { userNotes = it }, label = "Notas")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { isRx = !isRx }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isRx, onCheckedChange = { isRx = it },
                            colors = CheckboxDefaults.colors(checkedColor = LocalPrimaryColor.current, uncheckedColor = WDesignColorTextSecondary, checkmarkColor = Color.White)
                        )
                        Text("RX", fontWeight = FontWeight.Bold, color = if (isRx) WDesignColorTextPrimary else WDesignColorTextSecondary)
                    }
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { isPublic = !isPublic }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = isPublic, onCheckedChange = { isPublic = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = LocalPrimaryColor.current, uncheckedThumbColor = WDesignColorTextSecondary, uncheckedTrackColor = WDesignColorGlassSurface),
                            modifier = Modifier.scale(0.8f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (isPublic) "Público" else "Privado", fontWeight = FontWeight.Bold, color = if (isPublic) WDesignColorTextPrimary else WDesignColorTextSecondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Button(
                    onClick = { onSaveResult(userResult, isRx, userNotes, isPublic) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = userResult.isNotBlank() && !isSavingResult,
                    colors = ButtonDefaults.buttonColors(containerColor = LocalPrimaryColor.current, disabledContainerColor = LocalPrimaryColor.current.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 14.dp)
                ) {
                    if (isSavingResult) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    } else {
                        Text(if (hasResult) "ACTUALIZAR RESULTADO" else "GUARDAR RESULTADO", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun BadgeValidation(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector?, bgColor: Color, textColor: Color) {
    Row(
        modifier = Modifier.background(bgColor, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (icon != null) Icon(icon, null, tint = textColor, modifier = Modifier.size(10.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = textColor, fontWeight = FontWeight.Bold)
    }
}}
