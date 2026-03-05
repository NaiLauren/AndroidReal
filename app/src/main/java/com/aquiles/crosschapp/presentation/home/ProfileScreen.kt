package com.aquiles.crosschapp.presentation.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.aquiles.crosschapp.data.model.*
import com.aquiles.crosschapp.presentation.viewmodel.*
import com.aquiles.crosschapp.presentation.components.AnimatedXpRing
import com.aquiles.crosschapp.presentation.components.animatedCounter
import com.aquiles.crosschapp.presentation.components.animatedGlowGradient
import com.aquiles.crosschapp.presentation.components.pulsingGlow
import com.aquiles.crosschapp.presentation.components.FloatingParticlesBackground
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.layout.height

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.45f)
private val ColorPrimaryAction = Color(0xFFFC5200) // Tu naranja
private val ColorAdminAction = Color(0xFF673AB7)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f) // Borde fino
private val ColorSuccess = Color(0xFF4CAF50)
private val ColorError = Color(0xFFEF5350)

@Composable
fun ProfileScreen(
    innerPadding: PaddingValues,
    profileViewModel: ProfileViewModel = viewModel(),
    onEditProfileClicked: () -> Unit,
    onNavigateToRequestCredits: () -> Unit,
    onNavigateToAdminDashboard: () -> Unit,
    onNavigateToRules: () -> Unit,
    onNavigateToHistory: () -> Unit, // Nuevo parámetro
    onLogout: () -> Unit
) {
    val profileState by profileViewModel.userState.collectAsState()
    val profileUpdateState by profileViewModel.profileUpdateState.collectAsState()

    // --- CORRECCIÓN: Leemos el estado del historial para contar las asistencias reales ---
    val attendanceState by profileViewModel.attendanceHistoryState.collectAsState()
    val realAttendanceCount = if (attendanceState is AttendanceHistoryState.Success) {
        (attendanceState as AttendanceHistoryState.Success).records.size
    } else {
        0
    }
    // -----------------------------------------------------------------------------------

    val context = LocalContext.current

    LaunchedEffect(profileUpdateState) {
        if (profileUpdateState is ProfileUpdateState.Success) {
            Toast.makeText(context, "Foto actualizada correctamente", Toast.LENGTH_SHORT).show()
            profileViewModel.resetProfileUpdateState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // --- SCAFFOLD SIN TOP BAR DE CRISTAL ---
        Scaffold(
            containerColor = Color.Transparent
        ) { localPadding ->

            when (val state = profileState) {
                is ProfileState.Loading, ProfileState.Idle -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = ColorPrimaryAction) }
                }
                is ProfileState.Error -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.ErrorOutline, null, tint = ColorError)
                            Text(state.message, color = ColorTextSecondary)
                        }
                    }
                }
                is ProfileState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = localPadding.calculateTopPadding() + 8.dp, bottom = innerPadding.calculateBottomPadding() + 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // HEADER NUEVO (Estilo iOS)
                        item {
                            ProfileHeaderIOSStyle(
                                user = state.user,
                                activeBookingsCount = state.activeBookings.size,
                                attendanceCount = realAttendanceCount,
                                onEditClick = onEditProfileClicked,
                                isLoadingImage = profileUpdateState is ProfileUpdateState.Loading,
                                onNavigateToRules = onNavigateToRules,
                                onNavigateToHistory = onNavigateToHistory // Pasar callback
                            )
                        }

                        // Botones de Acción
                        item {
                            ActionButtonsSection(
                                user = state.user,
                                onNavigateToRequestCredits = onNavigateToRequestCredits,
                                onNavigateToAdminDashboard = onNavigateToAdminDashboard
                            )
                        }

                        // Reservas Activas
                        item {
                            GlassCardSection(title = "Próximas Reservas") {
                                if (state.activeBookings.isEmpty()) {
                                    Text(
                                        "No tienes reservas activas.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = ColorTextSecondary.copy(alpha = 0.5f),
                                        fontStyle = FontStyle.Italic,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    state.activeBookings.forEachIndexed { index, booking ->
                                        ActiveReservationRow(booking)
                                        if (index < state.activeBookings.size - 1) {
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = ColorBorder)
                                        }
                                    }
                                }
                            }
                        }

                        // Historiales
                        item {
                            GlassCardSection(title = "Historial") {
                                ExpandableHistorySection(
                                    title = "Solicitudes de Créditos",
                                    icon = Icons.Default.LocalActivity,
                                    state = profileViewModel.creditHistoryState.collectAsState().value
                                ) { req -> CreditHistoryItemRow(req as CreditRequest) }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = ColorBorder)

                                ExpandableHistorySection(
                                    title = "Movimientos de Cuenta",
                                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                                    state = profileViewModel.transactionHistoryState.collectAsState().value
                                ) { tx -> TransactionHistoryItemRow(tx as CreditTransaction) }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = ColorBorder)

                                ExpandableHistorySection(
                                    title = "Historial de Clases",
                                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                                    state = profileViewModel.attendanceHistoryState.collectAsState().value
                                ) { rec -> AttendanceHistoryItemRow(rec as EnrichedAttendanceRecord) }
                            }
                        }

                        // Cerrar Sesión
                        item {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                TextButton(onClick = onLogout) {
                                    Text("Cerrar Sesión", color = ColorError, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =====================================================
// NUEVO COMPONENTE DE CABECERA (Estilo iOS)
// =====================================================

@Composable
fun ProfileHeaderIOSStyle(
    user: User,
    activeBookingsCount: Int,
    attendanceCount: Int,
    onEditClick: () -> Unit,
    isLoadingImage: Boolean,
    onNavigateToRules: () -> Unit,
    onNavigateToHistory: () -> Unit
) {
    val nextLevelXP = LevelSystem.getNextLevelXp(user.xp)
    val prevLevelLimit = LevelSystem.getPreviousLevelLimit(user.xp)
    val range = (nextLevelXP - prevLevelLimit).toFloat().coerceAtLeast(1f)
    val progress = ((user.xp - prevLevelLimit) / range).coerceIn(0f, 1f)
    val xpNeeded = nextLevelXP - user.xp

    val dateFormatMonth = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-ES"))
    val memberSinceStr = user.registrationDate?.let { dateFormatMonth.format(it) } ?: "Enero 2025"
    val validUntilStr = user.creditValidUntil?.let { dateFormatMonth.format(it) } ?: "n/a"

    // Animated counters for stats
    val animAttendance by animatedCounter(attendanceCount)
    val animBookings by animatedCounter(activeBookingsCount)
    val animCredits by animatedCounter(user.credits)

    // Animated XP progress bar
    val animProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "xpBar"
    )

    // Decoration icon rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "profileHeader")
    val decorRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "decorRot"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {

            // 1. Botón Editar Flotante
            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 4.dp, top = 0.dp)
                    .shadow(elevation = 8.dp, shape = CircleShape)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .size(44.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.White, modifier = Modifier.size(20.dp))
            }

            // 2. Contenido Central
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                // Avatar con anillo XP animado + aura pulsante
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(170.dp)
                ) {
                    // Anillo XP animado
                    AnimatedXpRing(
                        progress = progress,
                        xp = user.xp,
                        level = user.level,
                        size = 168.dp,
                        trackColor = Color.White.copy(alpha = 0.1f),
                        progressColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    // Avatar con glow pulsante
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(148.dp)
                            .pulsingGlow(
                                color = MaterialTheme.colorScheme.primary,
                                minAlpha = 0.15f,
                                maxAlpha = 0.45f,
                                durationMs = 2200
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoadingImage) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            } else {
                                SubcomposeAsyncImage(
                                    model = user.profileImageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    error = {
                                        Box(Modifier.fillMaxSize().background(Color.Gray)) {
                                            Icon(Icons.Default.Person, null,
                                                modifier = Modifier.align(Alignment.Center),
                                                tint = Color.White)
                                        }
                                    }
                                )
                            }
                        }
                    }

                    // Decoration icon con rotación
                    val decorationName = LevelSystem.getAvatarDecoration(user.level)
                    if (decorationName != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-4).dp, y = 4.dp)
                        ) {
                            Icon(
                                imageVector = getIconByName(decorationName),
                                contentDescription = "Rango ${user.level}",
                                tint = if (user.level == "Elite") Color(0xFFFFD700)
                                       else MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(36.dp)
                                    .shadow(8.dp, CircleShape)
                                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                                    .rotate(decorRotation * 0.05f) // giño sutil
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // TARJETA CRISTALINA CON GRADIENTE ANIMADO
                com.aquiles.crosschapp.presentation.components.GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .animatedGlowGradient(
                            color1 = MaterialTheme.colorScheme.primary,
                            color2 = Color(0xFF8B00FF),
                            durationMs = 7000
                        ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box {
                        // Partículas sutiles en la card de perfil
                        FloatingParticlesBackground(
                            modifier = Modifier.matchParentSize().height(200.dp),
                            particleColor = MaterialTheme.colorScheme.primary,
                            particleCount = 8
                        )
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = user.fullName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = ColorTextPrimary,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Miembro desde: $memberSinceStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorTextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Válidos hasta el $validUntilStr",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(8.dp))

                            // XP PROGRESS BAR ANIMADA
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Nivel ${user.level.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${user.xp} XP",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ColorTextSecondary
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.White.copy(alpha = 0.2f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(animProgress)
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        Color(0xFFFFD700)
                                                    )
                                                )
                                            )
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Próximo nivel en $xpNeeded XP",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ColorTextSecondary,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            // BOTONES
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                OutlinedButton(
                                    onClick = onNavigateToRules,
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, ColorBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Text("Reglas", style = MaterialTheme.typography.labelSmall)
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = { onNavigateToHistory() },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    Icon(Icons.Default.History, null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Historial", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ESTADÍSTICAS CON CONTADORES ANIMADOS
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ProfileStatCard(animAttendance.toString(), "Clases", Icons.AutoMirrored.Filled.DirectionsRun, Modifier.weight(1f))
            ProfileStatCard(animBookings.toString(), "Reservas", Icons.Default.Event, Modifier.weight(1f))
            ProfileStatCard(animCredits.toString(), "Créditos", Icons.Default.ConfirmationNumber, Modifier.weight(1f))
        }
    }
}

// =====================================================
// COMPONENTES EXISTENTES (Sin cambios mayores)
// =====================================================

@Composable
fun ProfileStatCard(value: String, label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    com.aquiles.crosschapp.presentation.components.GlassCard(
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.Bold, color = ColorTextPrimary, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.labelSmall, color = ColorTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun ActionButtonsSection(user: User, onNavigateToRequestCredits: () -> Unit, onNavigateToAdminDashboard: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onNavigateToRequestCredits,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.AddCard, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Solicitar Créditos", fontWeight = FontWeight.Bold)
        }
            if (user.role == "owner" || user.isAdmin) {
                Button(
                    onClick = onNavigateToAdminDashboard,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary), // ROJO/COLOR TEMA
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AdminPanelSettings, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Panel de Administrador", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

@Composable
fun GlassCardSection(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    com.aquiles.crosschapp.presentation.components.GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            if (title != null) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
                Spacer(modifier = Modifier.height(16.dp))
            }
            content()
        }
    }
}

@Composable
fun ActiveReservationRow(gymClass: GymClass) {
    val dateFormat = SimpleDateFormat("EEE d, HH:mm", Locale.forLanguageTag("es-ES"))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(4.dp).height(48.dp).background(ColorPrimaryAction, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(gymClass.name, fontWeight = FontWeight.Bold, color = ColorTextPrimary, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, null, tint = ColorTextSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(gymClass.dateTime?.let { dateFormat.format(it).uppercase() } ?: "", style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)
            }
        }
        Surface(color = ColorSuccess.copy(alpha = 0.2f), shape = RoundedCornerShape(6.dp)) {
            Text("CONFIRMADA", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), color = ColorSuccess, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Suppress("UNCHECKED_CAST")
@Composable
fun ExpandableHistorySection(title: String, icon: ImageVector, state: Any, content: @Composable (Any) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (isExpanded) 90f else 0f, label = "arrow")

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) { isExpanded = !isExpanded }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = ColorTextSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, color = ColorTextPrimary, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.KeyboardArrowDown, null, tint = ColorTextSecondary, modifier = Modifier.rotate(rotation))
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(modifier = Modifier.padding(start = 8.dp, top = 8.dp)) {
                when (state) {
                    is CreditHistoryState.Loading, is TransactionHistoryState.Loading, is AttendanceHistoryState.Loading ->
                        Box(Modifier.fillMaxWidth().padding(12.dp), Alignment.Center) { CircularProgressIndicator(Modifier.size(20.dp), color = ColorPrimaryAction) }
                    is CreditHistoryState.Empty, is TransactionHistoryState.Empty, is AttendanceHistoryState.Empty ->
                        Text("No hay registros.", Modifier.padding(8.dp), color = ColorTextSecondary.copy(alpha = 0.5f), fontStyle = FontStyle.Italic)
                    is CreditHistoryState.Success -> state.requests.forEach { content(it) }
                    is TransactionHistoryState.Success -> state.transactions.forEach { content(it) }
                    is AttendanceHistoryState.Success -> state.records.forEach { content(it) }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun TransactionHistoryItemRow(tx: CreditTransaction) {
    val isPositive = tx.amount > 0
    val amountColor = if (isPositive) ColorSuccess else ColorTextPrimary
    val sign = if (isPositive) "+" else ""
    val dateFormat = SimpleDateFormat("dd/MM/yy", Locale.forLanguageTag("es-ES"))
    val icon = when {
        tx.type.uppercase().contains("RESERVA") -> Icons.Default.ConfirmationNumber
        tx.type.uppercase().contains("DEVOLUCION") || tx.type.uppercase().contains("CANCEL") -> Icons.Default.Restore
        tx.type.uppercase().contains("COMPRA") || tx.type.uppercase().contains("CARGA") -> Icons.Default.CreditCard
        else -> Icons.AutoMirrored.Filled.List
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).background(Color.White.copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = ColorPrimaryAction, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(text = tx.description, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = ColorTextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            tx.date?.let { Text(text = dateFormat.format(it), style = MaterialTheme.typography.labelSmall, color = ColorTextSecondary) }
        }
        Text(text = "$sign${tx.amount}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = amountColor, modifier = Modifier.background(amountColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
fun CreditHistoryItemRow(req: CreditRequest) {
    val (statusText, statusColor) = when(req.status) {
        "APPROVED" -> "Aprobado" to ColorSuccess
        "REJECTED" -> "Rechazado" to ColorError
        else -> "Pendiente" to Color(0xFFFFD600)
    }
    val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("es-ES"))
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(text = statusText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = statusColor)
            req.requestDate?.let { Text(text = dateFormat.format(it), style = MaterialTheme.typography.labelSmall, color = ColorTextSecondary) }
        }
        Spacer(Modifier.height(4.dp))
        Text(text = req.comboName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = ColorTextPrimary)
        Text(text = "${req.creditsRequested} créditos", style = MaterialTheme.typography.labelSmall, color = ColorTextSecondary)
    }
}

// Helper para mapear strings de LevelSystem a Iconos
private fun getIconByName(name: String): ImageVector {
    return when(name) {
        "military_tech" -> Icons.Default.MilitaryTech
        "fitness_center" -> Icons.Default.FitnessCenter
        "workspace_premium" -> Icons.Default.WorkspacePremium
        "emoji_events" -> Icons.Default.EmojiEvents
        else -> Icons.Default.Star
    }
}

@Composable
fun AttendanceHistoryItemRow(rec: EnrichedAttendanceRecord) {
    val dateFormat = SimpleDateFormat("EEEE dd 'de' MMMM, HH:mm", Locale.forLanguageTag("es-ES"))
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(
            text = rec.classDetails?.name ?: "Clase (Sin detalles)",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = ColorTextPrimary
        )
        rec.record.classDate?.let {
            Text(
                text = dateFormat.format(it).replaceFirstChar { c -> c.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = ColorTextSecondary
            )
        }
    }
}