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
import java.text.SimpleDateFormat
import java.util.*

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.45f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
private val ColorBackgroundGradientStart = Color(0xFF000000)
private val ColorBackgroundGradientEnd = Color(0xFF121212)
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
    val uriHandler = LocalUriHandler.current
    val isFromCurrentUser = false // Read-only archive, mostly received.

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = if (isFromCurrentUser) Alignment.End else Alignment.Start
    ) {
        // Sender Name (only if not from me)
        if (!isFromCurrentUser) {
            Text(
                text = message.sender_name,
                style = MaterialTheme.typography.labelSmall,
                color = ColorTextSecondary,
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomEnd = 16.dp,
                        bottomStart = 4.dp
                    )
                )
                .background(ColorGlassSurface)
                .border(1.dp, ColorBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp))
                .padding(12.dp)
        ) {
            Column {
                if (message.content.isNotBlank()) {
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorTextPrimary
                    )
                }

                if (!message.attachmentUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onAttachmentClick(message.attachmentUrl!!) },
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, ColorPrimaryAction.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorPrimaryAction),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Attachment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ver Adjunto", style = MaterialTheme.typography.labelSmall)
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTimestamp(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorTextSecondary.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Borrar",
                        tint = ColorError.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onDeleteClick() }
                    )
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