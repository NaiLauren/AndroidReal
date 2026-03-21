package com.aquiles.crosschapp.presentation.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.data.model.Notification
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.viewmodel.AllNotificationsState
import com.aquiles.crosschapp.presentation.viewmodel.NotificationsViewModel
import com.aquiles.crosschapp.ui.theme.LocalPrimaryColor
import java.text.SimpleDateFormat
import java.util.*

private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.65f)

// Ícono por categoría de notificación
private fun notificationIcon(type: String?): ImageVector = when (type?.lowercase()) {
    "credit", "credito", "credits"    -> Icons.Default.CreditCard
    "booking", "reserva"              -> Icons.Default.CalendarMonth
    "expiry", "vencimiento"           -> Icons.Default.Schedule
    "competition", "competencia"      -> Icons.Default.EmojiEvents
    "achievement", "logro"            -> Icons.Default.Star
    "warning", "alerta"               -> Icons.Default.Warning
    else                              -> Icons.Default.Notifications
}

// Color de acento por categoría (independiente del color del gym)
private fun notificationAccent(type: String?): Color? = when (type?.lowercase()) {
    "credit", "credito", "credits"    -> Color(0xFF34C759)
    "booking", "reserva"              -> Color(0xFF007AFF)
    "expiry", "vencimiento"           -> Color(0xFFFF9500)
    "competition", "competencia"      -> Color(0xFFFFD700)
    "achievement", "logro"            -> Color(0xFFAF52DE)
    "warning", "alerta"               -> Color(0xFFFF3B30)
    else                              -> null // null = usa el color del gym
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavController,
    notificationsViewModel: NotificationsViewModel = viewModel()
) {
    val notificationsState by notificationsViewModel.notificationsState.collectAsState()
    val gymColor = LocalPrimaryColor.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Notificaciones", fontWeight = FontWeight.Bold, color = ColorTextPrimary, fontSize = 18.sp)
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
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = notificationsState) {
                is AllNotificationsState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = gymColor, strokeWidth = 3.dp)
                    }
                }
                is AllNotificationsState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = Color(0xFFFF3B30), textAlign = TextAlign.Center, modifier = Modifier.padding(24.dp))
                    }
                }
                is AllNotificationsState.Empty -> {
                    NotificationsEmptyState(gymColor)
                }
                is AllNotificationsState.Success -> {
                    val unread = state.notifications.filter { !it.isRead }
                    val read = state.notifications.filter { it.isRead }

                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (unread.isNotEmpty()) {
                            item {
                                NotificationSectionHeader("Nuevas", unread.size, gymColor)
                            }
                            items(unread, key = { it.id }) { notification ->
                                NotificationCard(
                                    notification = notification,
                                    gymColor = gymColor,
                                    onMarkAsRead = {
                                        notificationsViewModel.markNotificationAsRead(notification.id)
                                    }
                                )
                            }
                        }
                        if (read.isNotEmpty()) {
                            item {
                                Spacer(Modifier.height(4.dp))
                                NotificationSectionHeader("Anteriores", read.size, Color.White.copy(0.35f))
                            }
                            items(read, key = { it.id }) { notification ->
                                NotificationCard(notification = notification, gymColor = gymColor, onMarkAsRead = {})
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationSectionHeader(label: String, count: Int, accentColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Box(modifier = Modifier.size(7.dp).background(accentColor, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .background(accentColor.copy(alpha = 0.15f), RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text("$count", style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NotificationCard(
    notification: Notification,
    gymColor: Color,
    onMarkAsRead: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM · HH:mm", Locale.forLanguageTag("es-ES")) }
    val dateString = notification.timestamp?.let { dateFormat.format(it) } ?: ""
    val isUnread = !notification.isRead

    // El color de acento usa el de categoría si existe, si no el color del gym
    val accentColor = notificationAccent(notification.type) ?: gymColor
    val icon = notificationIcon(notification.type)

    val cardAlpha by animateFloatAsState(
        targetValue = if (isUnread) 1f else 0.65f,
        animationSpec = tween(300),
        label = "cardAlpha"
    )

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
            .then(
                if (isUnread) Modifier.border(
                    1.dp,
                    Brush.linearGradient(listOf(accentColor.copy(0.5f), accentColor.copy(0.1f))),
                    RoundedCornerShape(20.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            // Ícono de tipo con glow
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Brush.radialGradient(listOf(accentColor.copy(0.25f), Color.Transparent)),
                        CircleShape
                    )
                    .border(1.dp, accentColor.copy(0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accentColor, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Título + punto no leído
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        notification.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium,
                        color = if (isUnread) ColorTextPrimary else ColorTextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    if (isUnread) {
                        Box(modifier = Modifier.size(8.dp).background(gymColor, CircleShape))
                    }
                }

                Spacer(Modifier.height(5.dp))

                // Mensaje
                Text(
                    notification.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(8.dp))

                // Fecha
                Text(dateString, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.3f))

                // Botón "Marcar como leída" — solo cuando es no leída
                if (isUnread) {
                    Spacer(Modifier.height(10.dp))
                    // Usar Button en lugar de Box clickable para garantizar tap correcto
                    OutlinedButton(
                        onClick = onMarkAsRead,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, gymColor.copy(0.4f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = gymColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(Icons.Default.MarkEmailRead, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Marcar como leída", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationsEmptyState(gymColor: Color) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Brush.radialGradient(listOf(gymColor.copy(0.15f), Color.Transparent)), CircleShape)
                    .border(1.dp, gymColor.copy(0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.NotificationsOff, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(48.dp))
            }
            Text("Todo al día", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
            Text(
                "No tenés notificaciones pendientes. Te avisaremos cuando haya novedades.",
                style = MaterialTheme.typography.bodyMedium,
                color = ColorTextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}