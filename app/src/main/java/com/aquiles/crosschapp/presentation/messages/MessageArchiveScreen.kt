package com.aquiles.crosschapp.presentation.messages

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.data.model.PersonalMessage
import com.aquiles.crosschapp.presentation.viewmodel.MessageHistoryState
import com.aquiles.crosschapp.presentation.viewmodel.MessageArchiveViewModel
import com.aquiles.crosschapp.presentation.components.FullScreenMediaDialog
import com.aquiles.crosschapp.presentation.components.GlassCard
import java.text.SimpleDateFormat
import java.util.*

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.15f)
private val ColorError = Color(0xFFEF5350)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageArchiveScreen(
    navController: NavController,
    viewModel: MessageArchiveViewModel = viewModel()
) {
    val messagesState by viewModel.messagesState.collectAsState()
    var selectedMediaUrl by remember { mutableStateOf<String?>(null) }

    // --- UI STRUCTURE ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    modifier = Modifier.height(72.dp),
                    title = { Text("Mensajes", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ColorTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f))
                )
            },
            containerColor = Color.Transparent
        ) { localPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(localPadding)) {
                when (val state = messagesState) {
                    is MessageHistoryState.Loading -> {
                        CircularProgressIndicator(color = ColorPrimaryAction, modifier = Modifier.align(Alignment.Center))
                    }
                    is MessageHistoryState.Error -> {
                        Text(state.message, color = ColorError, modifier = Modifier.align(Alignment.Center))
                    }
                    is MessageHistoryState.Empty -> {
                        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MarkEmailUnread, null, tint = ColorTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Bandeja vacía", color = ColorTextSecondary)
                        }
                    }
                    is MessageHistoryState.Success -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.messages, key = { it.id }) { message ->
                                MessageBubble(
                                    message = message,
                                    onDeleteClick = { viewModel.deleteMessage(message.id) },
                                    onAttachmentClick = { url -> selectedMediaUrl = url }
                                )
                            }
                        }
                    }
                }
            }
        }
        
        selectedMediaUrl?.let { url ->
            FullScreenMediaDialog(mediaUrl = url, onDismiss = { selectedMediaUrl = null })
        }
    }
}

@Composable
fun MessageBubble(
    message: PersonalMessage,
    onDeleteClick: () -> Unit,
    onAttachmentClick: (String) -> Unit
) {
    // Usamos GlassCard para cada mensaje en la bandeja en lugar de una burbuja de chat genérica
    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable {
            // Si no hay botón adjunto, podríamos expandir o hacer algo al click. Por ahora no hace nada.
        },
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Icono + Remitente + Fecha
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar / Icono Sender
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(ColorPrimaryAction.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MarkEmailUnread,
                        contentDescription = null,
                        tint = ColorPrimaryAction,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = message.sender_name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary
                    )
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorTextSecondary
                    )
                }

                // Delete Button
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Borrar",
                        tint = ColorTextSecondary.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body
            if (message.content.isNotBlank()) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorTextSecondary,
                    lineHeight = 22.sp
                )
            }

            // Attachment
            if (!message.attachmentUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { onAttachmentClick(message.attachmentUrl!!) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ColorBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorTextPrimary)
                ) {
                    Icon(Icons.Default.Attachment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ver Adjunto", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

fun formatTimestamp(date: Date?): String {
    if (date == null) return ""
    val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.forLanguageTag("es-ES")) // Formato más corto
    return sdf.format(date)
}