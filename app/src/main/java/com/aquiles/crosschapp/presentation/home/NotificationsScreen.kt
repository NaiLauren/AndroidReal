package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.data.model.Notification
import com.aquiles.crosschapp.presentation.viewmodel.AllNotificationsState
import com.aquiles.crosschapp.presentation.viewmodel.NotificationsViewModel
import java.text.SimpleDateFormat
import java.util.*

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.45f)
private val ColorGlassSurfaceRead = Color(0xFF1C1C1E).copy(alpha = 0.4f) // Más transparente para leídas
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
private val ColorBackgroundGradientStart = Color(0xFF000000)
private val ColorBackgroundGradientEnd = Color(0xFF121212)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavController,
    notificationsViewModel: NotificationsViewModel = viewModel()
) {
    val notificationsState by notificationsViewModel.notificationsState.collectAsState()

    // --- UI STRUCTURE ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Notificaciones", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = ColorTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f))
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                when (val state = notificationsState) {
                    is AllNotificationsState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ColorPrimaryAction)
                        }
                    }
                    is AllNotificationsState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = state.message,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    is AllNotificationsState.Empty -> {
                        EmptyNotificationsViewGlass()
                    }
                    is AllNotificationsState.Success -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.notifications, key = { it.id }) { notification ->
                                NotificationCardGlass(
                                    notification = notification,
                                    onMarkAsRead = {
                                        if (!notification.isRead) {
                                            notificationsViewModel.markNotificationAsRead(notification.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCardGlass(
    notification: Notification,
    onMarkAsRead: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd 'de' MMMM, HH:mm", Locale.forLanguageTag("es-ES")) }
    val dateString = notification.timestamp?.let { dateFormat.format(it) } ?: ""

    // Diferenciación visual Leída vs No Leída
    val containerColor = if (notification.isRead) ColorGlassSurfaceRead else ColorGlassSurface
    val borderColor = if (notification.isRead) Color.Transparent else ColorBorder
    val textColor = if (notification.isRead) ColorTextSecondary else ColorTextPrimary
    val messageColor = if (notification.isRead) ColorTextSecondary.copy(alpha = 0.7f) else ColorTextSecondary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Indicador de "No leído"
                if (!notification.isRead) {
                    Box(
                        Modifier
                            .padding(top = 6.dp, end = 12.dp)
                            .size(10.dp)
                            .background(ColorPrimaryAction, CircleShape)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (!notification.isRead) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorTextSecondary.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium,
                color = messageColor,
                lineHeight = 22.sp
            )

            if (!notification.isRead) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onMarkAsRead,
                        colors = ButtonDefaults.textButtonColors(contentColor = ColorPrimaryAction)
                    ) {
                        Icon(Icons.Default.MarkEmailRead, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Marcar como leída")
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyNotificationsViewGlass() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(ColorGlassSurface, CircleShape)
                .border(1.dp, ColorBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.NotificationsOff,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = ColorTextSecondary.copy(alpha = 0.5f)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Sin notificaciones",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = ColorTextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Te avisaremos cuando tengas novedades importantes o cambios en tus reservas.",
            style = MaterialTheme.typography.bodyMedium,
            color = ColorTextSecondary,
            textAlign = TextAlign.Center
        )
    }
}