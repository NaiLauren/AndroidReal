package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel

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
fun AdminDashboardScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    adminViewModel: AdminViewModel = viewModel()
) {
    // --- UI STRUCTURE ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Panel Admin", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ColorTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { localScaffoldPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = localScaffoldPadding.calculateTopPadding() + 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ManagementToolsSection(navController = navController)
                }
            }
        }
    }
}

@Composable
private fun ManagementToolsSection(navController: NavController) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "GESTIÓN",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
            color = ColorTextSecondary,
            fontWeight = FontWeight.Bold
        )

        AdminOptionCardGlass(
            title = "Reportes & Finanzas",
            subtitle = "Ingresos mensuales y actividad",
            icon = Icons.Default.AttachMoney,
            onClick = { navController.navigate("admin_reports_screen") }
        )

        AdminOptionCardGlass(
            title = "Solicitudes de Crédito",
            subtitle = "Aprobar pagos y renovaciones",
            icon = Icons.Default.CreditScore,
            onClick = { navController.navigate("admin_credit_requests") }
        )

        AdminOptionCardGlass(
            title = "Alumnos",
            subtitle = "Base de datos, mensajes y créditos",
            icon = Icons.Default.Groups,
            onClick = { navController.navigate("admin_manage_users") }
        )

        AdminOptionCardGlass(
            title = "Agenda & WODs",
            subtitle = "Crear clases y entrenamientos",
            icon = Icons.Default.CalendarMonth,
            onClick = { navController.navigate("admin_manage_classes") }
        )

        // NEW: Batch Planner
        AdminOptionCardGlass(
            title = "Planificador Masivo",
            subtitle = "Autocompletar mes/semana",
            icon = Icons.Default.Schedule,
            onClick = { navController.navigate("admin_schedule_planner") }
        )

        Text(
            "CONFIGURACIÓN",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp),
            color = ColorTextSecondary,
            fontWeight = FontWeight.Bold
        )

        AdminOptionCardGlass(
            title = "Packs de Crédito",
            subtitle = "Precios y reglas de facturación",
            icon = Icons.Default.Style,
            onClick = { navController.navigate("admin_manage_packs_screen") }
        )

        AdminOptionCardGlass(
            title = "Métodos de Pago",
            subtitle = "Configurar CBU, Alias y MP",
            icon = Icons.Default.Payments,
            onClick = { navController.navigate("admin_payment_config") }
        )

        AdminOptionCardGlass(
            title = "Personalización",
            subtitle = "Color de marca y apariencia",
            icon = Icons.Default.Palette,
            onClick = { navController.navigate("admin_gym_settings") }
        )

        AdminOptionCardGlass(
            title = "Benchmarks",
            subtitle = "WODs de referencia (Fran, Murph...)",
            icon = Icons.Default.Leaderboard,
            onClick = { navController.navigate("admin_manage_benchmarks") }
        )

        AdminOptionCardGlass(
            title = "Plantilla Horaria",
            subtitle = "Definir bloques horarios base",
            icon = Icons.Default.Schedule,
            onClick = { navController.navigate("admin_manage_schedules") }
        )

        AdminOptionCardGlass(
            title = "Novedades / Tablón",
            subtitle = "Publicar anuncios y noticias",
            icon = Icons.Default.Campaign, // Using Campaign icon for announcements
            onClick = { navController.navigate("admin_news_screen") }
        )
    }
}

@Composable
private fun AdminOptionCardGlass(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ColorBorder),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icono con fondo sutil
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(ColorPrimaryAction.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ColorPrimaryAction,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTextSecondary,
                    maxLines = 1
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "Ir",
                modifier = Modifier.size(16.dp),
                tint = ColorTextSecondary
            )
        }
    }
}