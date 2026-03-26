package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aquiles.crosschapp.data.model.GlobalChallenge
import com.aquiles.crosschapp.data.model.ChallengeResult
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.components.glassmorphicInteractive
import com.aquiles.crosschapp.ui.theme.LocalPrimaryColor

@Composable
fun GlobalChallengePager(
    challenges: List<GlobalChallenge>, 
    userRecords: Map<String, ChallengeResult> = emptyMap(),
    onNavigateToChallengeRanking: (String) -> Unit,
    onLogClick: (GlobalChallenge) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { challenges.size })
    
    Column(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 0.dp),
            pageSpacing = 12.dp
        ) { page ->
            val challenge = challenges[page]

            GlobalChallengeCard(
                challenge = challenge, 
                userRecord = userRecords[challenge.id],
                onClick = { onNavigateToChallengeRanking(challenge.id) },
                onLogClick = { onLogClick(challenge) }
            )
        }
        
        if (challenges.size > 1) {
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(challenges.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) Color(0xFFFFD700) else Color.White.copy(alpha = 0.3f)
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .clip(CircleShape)
                            .background(color)
                            .size(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GlobalChallengeCard(
    challenge: GlobalChallenge, 
    userRecord: ChallengeResult?,
    onClick: () -> Unit,
    onLogClick: () -> Unit
) {
    val (typeText, icon) = remember(challenge.scoreType, challenge.measurementUnit) {
        val unit = (challenge.measurementUnit ?: challenge.scoreType).uppercase()
        when {
            unit.contains("TIME") || unit.contains("TIEMPO") -> "MEJOR TIEMPO" to Icons.Default.Timer
            unit.contains("WEIGHT") || unit.contains("PESO") || unit.contains("KG") || unit.contains("LB") -> "MÁXIMO PESO" to Icons.Default.FitnessCenter
            unit.contains("REPS") || unit.contains("REPETICIONES") || unit.contains("AMRAP") -> "MÁX REPETICIONES" to Icons.Default.Repeat
            unit.contains("ROUND") || unit.contains("RONDAS") -> "MÁX RONDAS" to Icons.Default.Refresh
            unit.contains("DISTANCE") || unit.contains("DISTANCIA") || unit.contains("METROS") -> "MAYOR DISTANCIA" to Icons.Default.Straighten
            else -> "PUNTUACIÓN" to Icons.Default.EmojiEvents
        }
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .glassmorphicInteractive(onClick = onClick), // Usamos el modificador interactivo standard
        shape = RoundedCornerShape(20.dp)
    ) {
        // Mantenemos el brillo dorado interno característico de los desafíos
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 30.dp, y = 30.dp)
                .background(Color(0xFFFFD700).copy(alpha = 0.08f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .background(Color(0xFFFFD700).copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(icon, null, tint = Color(0xFFFFD700), modifier = Modifier.size(12.dp))
                        Text(
                            text = typeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFFFFD700),
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        )
                    }
                    
                    Text(
                        text = challenge.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    if (userRecord != null) {
                        Column {
                            Text(
                                text = "TU RÉCORD",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                            Text(
                                text = userRecord.score,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700)
                            )
                        }
                    } else {
                        Text(
                            text = "Sin marcas aún",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.4f),
                            fontStyle = FontStyle.Italic
                        )
                    }

                    Text(
                        text = "Desafío de Comunidad 🌎",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Medium),
                        color = Color(0xFFFFD700).copy(alpha = 0.8f)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (userRecord != null) "✓ REGISTRADO" else "PENDIENTE",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = if (userRecord != null) Color(0xFF30D158) else Color.White.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Botón CARGAR / MEJORAR MARCA siempre visible
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(listOf(Color(0xFFFFD700), Color(0xFFFC5200))), 
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable(onClick = onLogClick)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                if (userRecord != null) "MEJORAR MARCA" else "CARGAR MARCA", 
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), 
                                color = Color.Black.copy(alpha = 0.9f), 
                                fontWeight = FontWeight.Black
                            )
                        }

                        // Botón VER PODIO solo si hay récord
                        if (userRecord != null) {
                            Box(
                                modifier = Modifier
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                    .clickable(onClick = onClick)
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    "VER PODIO", 
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), 
                                    color = Color.White.copy(alpha = 0.7f), 
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
fun ChallengeLogDialog(
    challenge: GlobalChallenge,
    existingRecord: ChallengeResult? = null,
    onDismiss: () -> Unit,
    onSave: (String, Boolean, String, String) -> Unit,
    isSaving: Boolean
) {
    var score by remember { mutableStateOf(existingRecord?.score ?: "") }
    var notes by remember { mutableStateOf(existingRecord?.notes ?: "") }
    var videoUrl by remember { mutableStateOf(existingRecord?.videoUrl ?: "") }
    var isRx by remember { mutableStateOf(existingRecord?.isRx ?: true) }
    val isUpdate = existingRecord != null

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            // Fondo opaco en vez de GlassCard (más visible)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Color(0xFF1C1C1E).copy(alpha = 0.97f),
                        RoundedCornerShape(24.dp)
                    )
                    .border(
                        1.dp,
                        Color(0xFFFFD700).copy(alpha = 0.3f),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isUpdate) "MEJORAR DESAFÍO" else "REGISTRAR DESAFÍO",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = challenge.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    if (isUpdate) {
                        Text(
                            text = "Tu marca actual: ${existingRecord!!.score}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFFD700).copy(alpha = 0.7f)
                        )
                    }

                    GlassTextField(
                        value = score,
                        onValueChange = { score = it },
                        label = "Resultado (${challenge.measurementUnit ?: "Puntos"})",
                        keyboardType = if (challenge.measurementUnit?.contains("TIME") == true) 
                            KeyboardType.Text 
                        else 
                            KeyboardType.Number
                    )

                    GlassTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "Notas (Opcional)"
                    )

                    GlassTextField(
                        value = videoUrl,
                        onValueChange = { videoUrl = it },
                        label = "Enlace de video (YouTube, Drive, etc.)",
                        keyboardType = KeyboardType.Uri
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { isRx = !isRx }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isRx,
                            onCheckedChange = { isRx = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFFD700))
                        )
                        Text("RX (Nivel competitivo)", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("CANCELAR", color = Color.White.copy(alpha = 0.6f))
                        }
                        Button(
                            onClick = { onSave(score, isRx, notes, videoUrl) },
                            enabled = score.isNotBlank() && !isSaving,
                            modifier = Modifier.weight(1f).height(45.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                            } else {
                                Text(
                                    if (isUpdate) "MEJORAR" else "GUARDAR", 
                                    color = Color.Black, 
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
