package com.aquiles.crosschapp.presentation.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.aquiles.crosschapp.data.model.*
import com.aquiles.crosschapp.presentation.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.75f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorAdminAction = Color(0xFF673AB7)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
private val ColorSuccess = Color(0xFF4CAF50)
private val ColorError = Color(0xFFEF5350)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    innerPadding: PaddingValues,
    profileViewModel: ProfileViewModel = viewModel(),
    onEditProfileClicked: () -> Unit,
    onNavigateToRequestCredits: () -> Unit,
    onNavigateToAdminDashboard: () -> Unit,
    onLogout: () -> Unit
) {
    val profileState by profileViewModel.userState.collectAsState()
    val profileUpdateState by profileViewModel.profileUpdateState.collectAsState()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri ->
        uri?.let { profileViewModel.uploadProfileImage(it) }
    }
    val requestPermissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) imagePickerLauncher.launch("image/*")
        else Toast.makeText(context, "Permiso denegado.", Toast.LENGTH_SHORT).show()
    }

    fun launchImagePicker() {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED) {
            imagePickerLauncher.launch("image/*")
        } else {
            requestPermissionLauncher.launch(perm)
        }
    }

    LaunchedEffect(profileUpdateState) {
        if (profileUpdateState is ProfileUpdateState.Success) {
            Toast.makeText(context, "Foto actualizada correctamente", Toast.LENGTH_SHORT).show()
            profileViewModel.resetProfileUpdateState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.1f))
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Mi Perfil", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    actions = {
                        IconButton(onClick = onEditProfileClicked) {
                            Icon(Icons.Default.Edit, "Editar", tint = ColorPrimaryAction)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
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
                        contentPadding = PaddingValues(top = localPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding() + 20.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        item {
                            ProfileHeader(
                                user = state.user,
                                activeBookingsCount = state.activeBookings.size,
                                onImageClick = { launchImagePicker() },
                                isLoadingImage = profileUpdateState is ProfileUpdateState.Loading
                            )
                        }

                        item {
                            ActionButtonsSection(
                                user = state.user,
                                onNavigateToRequestCredits = onNavigateToRequestCredits,
                                onNavigateToAdminDashboard = onNavigateToAdminDashboard
                            )
                        }

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
                                    icon = Icons.Default.ReceiptLong,
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
// COMPONENTES UI (GLASS STYLE)
// =====================================================

@Composable
fun ProfileHeader(user: User, activeBookingsCount: Int, onImageClick: () -> Unit, isLoadingImage: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .border(2.dp, ColorPrimaryAction, CircleShape)
                .clickable { onImageClick() }
        ) {
            if (isLoadingImage) {
                CircularProgressIndicator(color = ColorPrimaryAction)
            } else {
                SubcomposeAsyncImage(
                    model = user.profileImageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = { Box(Modifier.fillMaxSize().background(Color.Gray)) { Icon(Icons.Default.Person, null, modifier = Modifier.align(Alignment.Center), tint = Color.White) } }
                )
            }
            Box(
                modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape).padding(4.dp)
            ) { Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(12.dp)) }
        }
        Spacer(Modifier.height(16.dp))
        Text(user.fullName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
        Text(user.email, style = MaterialTheme.typography.bodyMedium, color = ColorTextSecondary)
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ProfileStatCard(user.totalClassesAttended.toString(), "Asistencias", Icons.AutoMirrored.Filled.DirectionsRun, Modifier.weight(1f))
            ProfileStatCard(activeBookingsCount.toString(), "Reservas", Icons.Default.Event, Modifier.weight(1f))
            ProfileStatCard(user.credits.toString(), "Créditos", Icons.Default.ConfirmationNumber, Modifier.weight(1f))
        }
    }
}

@Composable
fun ProfileStatCard(value: String, label: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface),
        border = BorderStroke(1.dp, ColorBorder)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = ColorPrimaryAction, modifier = Modifier.size(20.dp))
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
            colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction),
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
                colors = ButtonDefaults.buttonColors(containerColor = ColorAdminAction),
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface),
        border = BorderStroke(1.dp, ColorBorder)
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
    val dateFormat = SimpleDateFormat("EEE d, HH:mm", Locale("es", "ES"))
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
    val dateFormat = SimpleDateFormat("dd/MM/yy", Locale("es", "ES"))
    val icon = when {
        tx.type.uppercase().contains("RESERVA") -> Icons.Default.ConfirmationNumber
        tx.type.uppercase().contains("DEVOLUCION") || tx.type.uppercase().contains("CANCEL") -> Icons.Default.Restore
        tx.type.uppercase().contains("COMPRA") || tx.type.uppercase().contains("CARGA") -> Icons.Default.CreditCard
        else -> Icons.Default.List
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
    val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale("es", "ES"))
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

@Composable
fun AttendanceHistoryItemRow(rec: EnrichedAttendanceRecord) {
    val dateFormat = SimpleDateFormat("EEEE dd 'de' MMMM, HH:mm", Locale("es", "ES"))
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