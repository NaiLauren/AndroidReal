package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.BorderStroke
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
    
    // Contenedor con borde dorado degradado (Desert Luxury)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(listOf(Color(0xFFFFD700).copy(alpha = 0.3f), Color(0xFFFC5200).copy(alpha = 0.1f), Color(0xFFFFD700).copy(alpha = 0.3f))),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(1.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 0.dp),
            pageSpacing = 0.dp
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
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(challenges.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) Color(0xFFFFD700) else Color.White.copy(alpha = 0.2f)
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
    val scoreType = challenge.scoreType.uppercase()
    
    // Paleta de Colores Contextual (iOS Parity)
    val themeColor = when {
        scoreType.contains("TIME") -> Color(0xFF00FBFF)    // Cyan
        scoreType.contains("WEIGHT") -> Color(0xFFFF8000)  // Orange
        scoreType.contains("REPS") -> Color(0xFF30D158)    // Green
        scoreType.contains("ROUND") -> Color(0xFFAF52DE)   // Purple
        scoreType.contains("DISTANCE") -> Color(0xFFFFD600) // Yellow
        else -> Color(0xFFFFD700)                          // Gold/Fallback
    }

    val scoreIcon = when {
        scoreType.contains("TIME") -> Icons.Default.Timer
        scoreType.contains("WEIGHT") -> Icons.Default.FitnessCenter
        scoreType.contains("REPS") -> Icons.Default.Repeat
        scoreType.contains("ROUND") -> Icons.Default.Refresh
        scoreType.contains("DISTANCE") -> Icons.Default.Straighten
        else -> Icons.Default.EmojiEvents
    }

    // Calculamos el estado del desafío
    val status = challenge.getChallengeStatus()
    val statusColor = when(status) {
        GlobalChallenge.ChallengeStatus.ACTIVE -> Color(0xFF30D158)
        GlobalChallenge.ChallengeStatus.UPCOMING -> Color(0xFFFFD600)
        GlobalChallenge.ChallengeStatus.FINISHED -> Color.Red.copy(alpha = 0.7f)
    }
    val statusText = when(status) {
        GlobalChallenge.ChallengeStatus.ACTIVE -> "ACTIVO"
        GlobalChallenge.ChallengeStatus.UPCOMING -> "PRÓXIMAMENTE"
        GlobalChallenge.ChallengeStatus.FINISHED -> "FINALIZADO"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color.Transparent)
    ) {
        // --- HEADER CON GRADIENTE ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(themeColor.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.6f))
                    )
                )
        ) {
            // Partículas decorativas (Blur effect simulation with semi-transparent circles)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = -30.dp)
                    .background(themeColor.copy(alpha = 0.15f), CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 20.dp, y = 20.dp)
                    .background(themeColor.copy(alpha = 0.1f), CircleShape)
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icono Circular
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(themeColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = scoreIcon,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                VStack(spacing = 4, modifier = Modifier.weight(1f)) {
                    Text(
                        text = challenge.name.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(statusColor, CircleShape))
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = statusColor,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // Badges
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "GLOBAL",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontWeight = FontWeight.Black
                        )
                    }
                    Surface(
                        color = themeColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = challenge.scoreType.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = themeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // --- CUERPO ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (challenge.description.isNotBlank()) {
                Text(
                    text = challenge.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 2,
                    lineHeight = 18.sp
                )
            }

            // Fechas e Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (challenge.startDate != null && challenge.endDate != null) {
                    val sdf = remember { java.text.SimpleDateFormat("dd/MM", java.util.Locale.getDefault()) }
                    val startStr = sdf.format(challenge.startDate.toDate())
                    val endStr = sdf.format(challenge.endDate.toDate())
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.CalendarToday, null, tint = themeColor.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                        Text(
                            text = "$startStr → $endStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (userRecord != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("TU RÉCORD:", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
                        Text(userRecord.score, style = MaterialTheme.typography.labelSmall, color = themeColor, fontWeight = FontWeight.Black)
                    }
                }
            }

            // --- ACCIONES ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // REGISTRAR
                Button(
                    onClick = onLogClick,
                    modifier = Modifier.weight(1.2f).height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.AddCircle, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Text(
                            text = if (userRecord != null) "MEJORAR" else "REGISTRAR",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Black,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // PODIO
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier.weight(0.8f).height(44.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "PODIO",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Auxiliar para diseño vertical similar a SwiftUI VStack
@Composable
fun VStack(
    modifier: Modifier = Modifier,
    spacing: Int = 0,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.dp)) {
        content()
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
