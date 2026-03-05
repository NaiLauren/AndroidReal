package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.* // Trae todos los iconos (Verified, Warning, etc)
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.aquiles.crosschapp.data.model.User
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import com.aquiles.crosschapp.presentation.viewmodel.UserDetailsState
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.layout.height

// CONSTANTES
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.45f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
private val ColorError = Color(0xFFEF5350)
private val ColorSuccess = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserDetailsScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    userId: String,
    adminViewModel: AdminViewModel = viewModel()
) {
    val userDetailsState by adminViewModel.userDetailsState.collectAsState()

    LaunchedEffect(key1 = userId) { adminViewModel.loadUserDetails(userId) }
    DisposableEffect(Unit) { onDispose { adminViewModel.clearUserDetails() } }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    modifier = Modifier.height(72.dp),
                    title = { Text("Perfil de Alumno", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ColorTextPrimary) } },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f))
                )
            },
            containerColor = Color.Transparent
        ) { localScaffoldPadding ->
            when (val state = userDetailsState) {
                is UserDetailsState.Loading, is UserDetailsState.Idle -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ColorPrimaryAction) }
                is UserDetailsState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(state.message, color = ColorError) }
                is UserDetailsState.Success -> UserProfileContent(user = state.user, localScaffoldPadding = localScaffoldPadding, mainScaffoldPadding = innerPadding)
            }
        }
    }
}

@Composable
private fun UserProfileContent(
    user: User,
    localScaffoldPadding: PaddingValues,
    mainScaffoldPadding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = localScaffoldPadding.calculateTopPadding(), bottom = mainScaffoldPadding.calculateBottomPadding())
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // AVATAR
        Box(
            modifier = Modifier.size(130.dp).clip(CircleShape).background(Color.Black).border(2.dp, ColorPrimaryAction, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(user.profileImageUrl ?: "").crossfade(true).build(),
                contentDescription = "Foto", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
            ) { if (painter.state is AsyncImagePainter.State.Error) Icon(Icons.Default.Person, null, modifier = Modifier.size(60.dp), tint = ColorTextSecondary) else SubcomposeAsyncImageContent() }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = user.fullName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
            Text(text = user.role.uppercase(), style = MaterialTheme.typography.bodyLarge, color = ColorPrimaryAction)
        }

        // --- 1. MEMBRESÍA ACTUAL (Mover Arriba) ---
        GlassInfoCard(title = "Membresía", icon = Icons.Default.CardMembership) {
            InfoDetailRow(icon = Icons.Default.ConfirmationNumber, label = "Créditos", value = "${user.credits}")
            val date = user.creditValidUntil?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: "-"
            InfoDetailRow(icon = Icons.Default.EventAvailable, label = "Vencimiento", value = date)
        }

        // --- 2. DATOS PERSONALES (Contacto) ---
        GlassInfoCard(title = "Contacto", icon = Icons.Default.ContactMail) {
            InfoDetailRow(icon = Icons.Default.Email, label = "Email", value = user.email)
            InfoDetailRow(icon = Icons.Default.Phone, label = "Teléfono", value = user.phoneNumber?.takeIf { it.isNotBlank() } ?: "-")
            InfoDetailRow(icon = Icons.Default.HealthAndSafety, label = "Emergencia", value = user.emergencyContact?.takeIf { it.isNotBlank() } ?: "-")
        }

        // --- 3. AUDITORÍA LEGAL Y MÉDICA ---
        GlassInfoCard(title = "Auditoría Legal y Médica", icon = Icons.Default.Shield) {

            // ESTADO DE FIRMA
            if (user.waiverAccepted == true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Verified, null, tint = ColorSuccess)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Firma VÁLIDA", color = ColorSuccess, fontWeight = FontWeight.Bold)
                        Text("Versión: ${user.waiverVersion ?: "1.0"}", style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)
                    }
                }

                // Detalles Forenses
                val dateStr = user.waiverDate?.let { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it) } ?: "-"
                InfoDetailRow(icon = Icons.Default.History, label = "Firmado el", value = dateStr)

                if (!user.waiverDevice.isNullOrBlank()) {
                    InfoDetailRow(icon = Icons.Default.Smartphone, label = "Dispositivo", value = user.waiverDevice!!)
                }

            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = ColorError)
                    Spacer(Modifier.width(8.dp))
                    Text("NO ha firmado renuncia", color = ColorError, fontWeight = FontWeight.Bold)
                }
            }

            HorizontalDivider(color = ColorBorder)

            // DATOS MÉDICOS
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Cardíaco:", color = ColorTextSecondary)
                Text(if (user.hasHeartCondition == true) "SÍ" else "NO", color = if (user.hasHeartCondition == true) ColorError else ColorSuccess, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Lesiones:", color = ColorTextSecondary)
                Text(if (user.hasInjuries == true) "SÍ" else "NO", color = if (user.hasInjuries == true) MaterialTheme.colorScheme.primary else ColorSuccess, fontWeight = FontWeight.Bold)
            }

            if (!user.medicalNotes.isNullOrBlank()) {
                Text("Notas:", style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)
                Text(user.medicalNotes!!, style = MaterialTheme.typography.bodyMedium, color = ColorTextPrimary, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun GlassInfoCard(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ColorBorder), colors = CardDefaults.cardColors(containerColor = ColorGlassSurface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = ColorPrimaryAction)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = ColorBorder)
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { content() }
        }
    }
}

@Composable
private fun InfoDetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp).padding(top = 2.dp), tint = ColorTextSecondary)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = ColorTextSecondary)
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = ColorTextPrimary)
        }
    }
}