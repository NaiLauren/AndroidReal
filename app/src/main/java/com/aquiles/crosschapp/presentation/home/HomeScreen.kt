package com.aquiles.crosschapp.presentation.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.aquiles.crosschapp.data.model.PersonalMessage
import com.aquiles.crosschapp.data.model.User
import com.aquiles.crosschapp.presentation.viewmodel.*

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.75f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
private val ColorBackgroundGradientStart = Color(0xFF000000)
private val ColorBackgroundGradientEnd = Color(0xFF121212)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    innerPadding: PaddingValues,
    homeViewModel: HomeViewModel = viewModel(),
    adminViewModel: AdminViewModel = viewModel(),
    onNavigateToAdminCreditRequests: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToMessageArchive: () -> Unit
) {
    val currentUser by UserSession.currentUser.collectAsState()
    val notificationsState by homeViewModel.notificationsState.collectAsState()
    val pendingRequestsCount by adminViewModel.pendingRequestsCount.collectAsState()
    val personalMessageState by homeViewModel.personalMessageState.collectAsState()

    val hasUnreadNotifications = notificationsState is NotificationsState.Success &&
            (notificationsState as NotificationsState.Success).notifications.isNotEmpty()

    // --- UI STRUCTURE ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Inicio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    ),
                    actions = {
                        // Archivo de Mensajes
                        IconButton(onClick = onNavigateToMessageArchive) {
                            Icon(
                                imageVector = Icons.Outlined.MailOutline,
                                contentDescription = "Archivo",
                                tint = ColorTextPrimary
                            )
                        }

                        // Notificaciones
                        IconButton(onClick = onNavigateToNotifications) {
                            Box {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Notificaciones",
                                    tint = if (hasUnreadNotifications) ColorPrimaryAction else ColorTextPrimary
                                )
                                if (hasUnreadNotifications) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(10.dp)
                                            .background(ColorPrimaryAction, CircleShape)
                                            .border(1.dp, Color.Black, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                )
            },
            containerColor = Color.Transparent
        ) { localScaffoldPadding ->
            val user = currentUser
            if (user == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ColorPrimaryAction)
                }
            } else {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
                ) {
                    HomeScreenContent(
                        user = user,
                        pendingRequestsCount = pendingRequestsCount,
                        personalMessageState = personalMessageState,
                        onMarkMessageAsRead = { messageId ->
                            homeViewModel.markPersonalMessageAsRead(messageId)
                        },
                        onNavigateToAdminCreditRequests = onNavigateToAdminCreditRequests,
                        localScaffoldPadding = localScaffoldPadding
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreenContent(
    user: User,
    pendingRequestsCount: Int,
    personalMessageState: PersonalMessageState,
    onMarkMessageAsRead: (String) -> Unit,
    onNavigateToAdminCreditRequests: () -> Unit,
    localScaffoldPadding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = localScaffoldPadding.calculateTopPadding() + 16.dp,
            bottom = 24.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column {
                Text(
                    text = "¡Hola, ${user.name.split(" ").first()}!",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
                Text(
                    text = "¿Listo para entrenar hoy?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = ColorTextSecondary
                )
            }
        }

        when (user.role) {
            "owner", "coach" -> {
                item {
                    AdminSummaryCardGlass(
                        pendingRequests = pendingRequestsCount,
                        onClick = onNavigateToAdminCreditRequests
                    )
                }
            }
            "member" -> {
                item {
                    when(personalMessageState) {
                        is PersonalMessageState.Success -> {
                            PersonalMessageCardGlass(
                                message = personalMessageState.message,
                                onAcknowledge = { onMarkMessageAsRead(personalMessageState.message.id) }
                            )
                        }
                        is PersonalMessageState.Loading -> {
                            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = ColorPrimaryAction)
                            }
                        }
                        is PersonalMessageState.Empty -> { /* Nada */ }
                        is PersonalMessageState.Error -> { /* Nada */ }
                    }
                }
                item {
                    WelcomeGuideCardGlass()
                }
            }
        }
    }
}

// =====================================================
// COMPONENTES UI (GLASS STYLE)
// =====================================================

@Composable
fun PersonalMessageCardGlass(message: PersonalMessage, onAcknowledge: () -> Unit) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ColorPrimaryAction.copy(alpha = 0.5f)), // Borde naranja suave para destacar
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(ColorPrimaryAction.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = ColorPrimaryAction)
                }
                Column {
                    Text(
                        text = "Mensaje del Coach",
                        style = MaterialTheme.typography.titleSmall,
                        color = ColorTextSecondary
                    )
                    Text(
                        text = message.sender_name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary
                    )
                }
            }

            HorizontalDivider(color = ColorBorder)

            if (message.content.isNotBlank()) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = ColorTextPrimary
                )
            }

            when (message.attachmentType) {
                "image" -> {
                    Image(
                        painter = rememberAsyncImagePainter(message.attachmentUrl),
                        contentDescription = "Imagen adjunta",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentScale = ContentScale.Crop
                    )
                }
                "pdf" -> {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(message.attachmentUrl))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        border = BorderStroke(1.dp, ColorBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = ColorPrimaryAction)
                        Spacer(Modifier.size(8.dp))
                        Text("Ver Rutina Adjunta (PDF)", color = ColorTextPrimary)
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onAcknowledge) {
                    Text("Marcar como Leído", color = ColorPrimaryAction, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


@Composable
fun WelcomeGuideCardGlass() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ColorBorder),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.RocketLaunch, contentDescription = null, tint = ColorPrimaryAction, modifier = Modifier.size(28.dp))
                Text(
                    text = "¡Bienvenido a Real Fitness!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
            }

            Text(
                "Aquí tienes una guía rápida para empezar:",
                style = MaterialTheme.typography.bodyMedium,
                color = ColorTextSecondary
            )

            HorizontalDivider(color = ColorBorder)

            GuideStep(Icons.Default.AccountBalanceWallet, "Perfil", "Solicita créditos y gestiona tu cuenta.")
            GuideStep(Icons.Default.EventAvailable, "Horarios", "Reserva clases (necesitas créditos activos).")
            GuideStep(Icons.Default.FitnessCenter, "WODs", "Mira el entreno del día y registra marcas.")
            GuideStep(Icons.Default.Timeline, "Rendimiento", "Sigue tu progreso y desbloquea logros.")

            HorizontalDivider(color = ColorBorder)

            Text(
                "¡A superar tus límites!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ColorPrimaryAction,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun GuideStep(icon: ImageVector, title: String, text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp).padding(top = 2.dp),
            tint = ColorTextSecondary
        )
        Column {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
            Text(text = text, style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)
        }
    }
}

@Composable
fun AdminSummaryCardGlass(pendingRequests: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if(pendingRequests > 0) ColorPrimaryAction else ColorBorder),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AdminPanelSettings, null, tint = ColorTextPrimary)
                }

                Column {
                    Text(
                        "Panel de Admin",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextPrimary
                    )
                    if (pendingRequests > 0) {
                        Text(
                            "$pendingRequests solicitudes pendientes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorPrimaryAction,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            "Todo al día",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorTextSecondary
                        )
                    }
                }
            }

            Icon(Icons.Default.ChevronRight, null, tint = ColorTextSecondary)
        }
    }
}