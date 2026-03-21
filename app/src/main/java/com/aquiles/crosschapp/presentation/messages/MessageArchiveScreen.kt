package com.aquiles.crosschapp.presentation.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.data.model.PersonalMessage
import com.aquiles.crosschapp.presentation.components.FullScreenMediaDialog
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.viewmodel.MessageArchiveViewModel
import com.aquiles.crosschapp.presentation.viewmodel.MessageHistoryState
import java.text.SimpleDateFormat
import java.util.*

private val ColorPrimary = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.65f)
private val ColorBubbleBg = Color(0xFF1C1C2E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageArchiveScreen(
    navController: NavController,
    viewModel: MessageArchiveViewModel = viewModel()
) {
    val messagesState by viewModel.messagesState.collectAsState()
    var selectedMediaUrl by remember { mutableStateOf<String?>(null) }
    var messageToDelete by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Mensajes",
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ColorTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0D0D0D).copy(alpha = 0.9f)
                )
            )
        },
        containerColor = Color.Transparent
    ) { localPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(localPadding)) {
            when (val state = messagesState) {
                is MessageHistoryState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ColorPrimary, strokeWidth = 3.dp)
                    }
                }
                is MessageHistoryState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = Color(0xFFFF3B30), modifier = Modifier.padding(24.dp), textAlign = TextAlign.Center)
                    }
                }
                is MessageHistoryState.Empty -> {
                    MessagesEmptyState()
                }
                is MessageHistoryState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.messages, key = { it.id }) { message ->
                            MessageCard(
                                message = message,
                                onDeleteClick = { messageToDelete = message.id },
                                onAttachmentClick = { url -> selectedMediaUrl = url }
                            )
                        }
                    }
                }
            }

            // Dialog confirmación de borrar
            messageToDelete?.let { id ->
                AlertDialog(
                    onDismissRequest = { messageToDelete = null },
                    title = { Text("Eliminar mensaje", color = ColorTextPrimary, fontWeight = FontWeight.Bold) },
                    text = { Text("¿Querés eliminar este mensaje? Esta acción no se puede deshacer.", color = ColorTextSecondary) },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.deleteMessage(id); messageToDelete = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30))
                        ) { Text("Eliminar", color = Color.White) }
                    },
                    dismissButton = {
                        TextButton(onClick = { messageToDelete = null }) { Text("Cancelar", color = ColorTextSecondary) }
                    },
                    containerColor = Color(0xFF1C1C2E),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        selectedMediaUrl?.let { url ->
            FullScreenMediaDialog(mediaUrl = url, onDismiss = { selectedMediaUrl = null })
        }
    }
}

@Composable
private fun MessageCard(
    message: PersonalMessage,
    onDeleteClick: () -> Unit,
    onAttachmentClick: (String) -> Unit
) {
    val initials = message.sender_name
        .split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifBlank { "?" }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(Color.White.copy(0.12f), ColorPrimary.copy(0.15f), Color.White.copy(0.05f))
                ),
                RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar con iniciales y gradiente
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(ColorPrimary, Color(0xFFFF6B35))
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message.sender_name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary
                    )
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(0.35f)
                    )
                }

                // Botón borrar
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color.White.copy(0.05f), CircleShape)
                        .border(1.dp, Color.White.copy(0.1f), CircleShape)
                        .clickable { onDeleteClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Borrar",
                        tint = Color.White.copy(0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Separador sutil
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White.copy(0.08f), Color.Transparent)
                        )
                    )
            )
            Spacer(Modifier.height(12.dp))

            // Cuerpo del mensaje con fondo sutil
            if (message.content.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(0.04f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorTextSecondary,
                        lineHeight = 22.sp
                    )
                }
            }

            // Adjunto
            if (!message.attachmentUrl.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorPrimary.copy(0.08f), RoundedCornerShape(12.dp))
                        .border(1.dp, ColorPrimary.copy(0.25f), RoundedCornerShape(12.dp))
                        .clickable { onAttachmentClick(message.attachmentUrl!!) }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(ColorPrimary.copy(0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Attachment, null, tint = ColorPrimary, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Archivo adjunto", style = MaterialTheme.typography.labelMedium, color = ColorTextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Toca para ver", style = MaterialTheme.typography.labelSmall, color = ColorTextSecondary)
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = ColorPrimary.copy(0.7f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MessagesEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.radialGradient(listOf(ColorPrimary.copy(0.15f), Color.Transparent)),
                        CircleShape
                    )
                    .border(1.dp, ColorPrimary.copy(0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MarkEmailUnread, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(48.dp))
            }
            Text("Bandeja vacía", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
            Text(
                "Cuando el coach o el gym te envíen un mensaje, va a aparecer acá.",
                style = MaterialTheme.typography.bodyMedium,
                color = ColorTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

fun formatTimestamp(date: Date?): String {
    if (date == null) return ""
    val now = System.currentTimeMillis()
    val diff = now - date.time
    return when {
        diff < 60_000L -> "Ahora"
        diff < 3600_000L -> "${diff / 60_000} min"
        diff < 86400_000L -> "${diff / 3600_000} h"
        diff < 172800_000L -> "Ayer"
        else -> SimpleDateFormat("dd MMM · HH:mm", Locale.forLanguageTag("es-ES")).format(date)
    }
}