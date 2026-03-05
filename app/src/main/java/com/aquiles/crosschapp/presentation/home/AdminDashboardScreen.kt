package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import com.aquiles.crosschapp.presentation.viewmodel.UserSession

import com.aquiles.crosschapp.presentation.components.GlassCard
import androidx.compose.foundation.layout.height

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.45f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    adminViewModel: AdminViewModel = viewModel()
) {
    // Collect pending requests count
    val pendingRequests by adminViewModel.pendingRequestsCount.collectAsState()
    
    // Collect current gym for setup progress
    val currentGym by UserSession.currentGym.collectAsState()

    // Trigger loading of requests on enter
    LaunchedEffect(Unit) {
        adminViewModel.loadPendingRequests()
        adminViewModel.checkAndCloseYesterday()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Removed .background(Color.Black) to allow AppBackground to show through
    ) {
        // Fondo con imagen o gradiente podría ir aquí si se desea igualar "fondo2" de iOS exactamente

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    modifier = Modifier.height(72.dp),
                    title = { Text("Centro de Mando", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Atrás",
                                tint = ColorTextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f)
                    )
                )
            },
            containerColor = Color.Transparent
        ) { localScaffoldPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = localScaffoldPadding.calculateTopPadding() + 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 20.dp, 
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. HEADER (Texto estático, sin GlassCard para no parecer botón)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "Bienvenido al Panel de Control",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = ColorTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Gestiona tu gimnasio en tiempo real",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorTextSecondary
                        )
                    }
                }
                
                // 1.5 ASISTENTE DE CONFIGURACIÓN
                currentGym?.let { gym ->
                    item {
                        SetupProgressCard(
                            progress = gym.setupProgress ?: emptyMap(),
                            skipped = gym.setupSkipped ?: emptyMap(),
                            onStepTapped = { step ->
                                navController.navigate("${step.destination}?setupStep=${step.toKey()}")
                            },
                            onStepSkipped = { step ->
                                adminViewModel.markSetupStepSkipped(step.toKey())
                            }
                        )
                    }
                }

                // 2. TARJETA FINANCIERA PRINCIPAL (Full Width)
                item {
                    FinancialCard(
                        onClick = { navController.navigate("admin_reports_screen") }
                    )
                }

                // 3. ACCIONES CRÍTICAS (Grid 2x2)
                // Usamos un item que contiene un Layout Flow o columnas manuales ya que LazyVerticalGrid anidado puede dar problemas
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Solicitudes
                            DashboardCard(
                                title = "Solicitudes",
                                icon = Icons.Default.Payment, // Was CreditCard
                                color = Color.Blue,
                                badgeCount = pendingRequests,
                                modifier = Modifier.weight(1f),
                                onClick = { navController.navigate("admin_credit_requests") }
                            )
                            // Alumnos
                            DashboardCard(
                                title = "Alumnos",
                                icon = Icons.Default.Groups,
                                color = Color(0xFFAF52DE), // Purple equivalent
                                modifier = Modifier.weight(1f),
                                onClick = { navController.navigate("admin_manage_users") }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Planificar
                            DashboardCard(
                                title = "Planificar",
                                icon = Icons.Default.DateRange, // Was CalendarToday
                                color = Color(0xFFFF9500), // Orange
                                modifier = Modifier.weight(1f),
                                onClick = { navController.navigate("admin_schedule_planner") }
                            )
                            // Horarios
                            DashboardCard(
                                title = "Horarios",
                                icon = Icons.Default.Schedule,
                                color = Color(0xFFFF2D55), // Pink
                                modifier = Modifier.weight(1f),
                                onClick = { navController.navigate("admin_manage_schedules") }
                            )
                        }
                    }
                }

                // 4. HERRAMIENTAS SECUNDARIAS
                item {
                    Column(spacing = 0.dp) {
                        Text(
                            "Configuración Avanzada",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )

                        GlassCard(
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column {
                            AdminListRow(
                                    title = "Gestionar Packs",
                                    icon = Icons.AutoMirrored.Filled.Label, // Fixed deprecation
                                    onClick = { navController.navigate("admin_manage_packs_screen") }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
                                
                                AdminListRow(
                                    title = "Torneos y Compe",
                                    icon = Icons.Default.EmojiEvents, // Trophy icon
                                    onClick = { navController.navigate("admin_competition_manager") }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)

                                AdminListRow(
                                    title = "Benchmarks (WODs)",
                                    icon = Icons.AutoMirrored.Filled.List, // List.star equivalent
                                    onClick = { navController.navigate("admin_manage_benchmarks") }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)

                                AdminListRow(
                                    title = "Ajustes de Marca",
                                    icon = Icons.Default.Palette,
                                    onClick = { navController.navigate("admin_gym_settings") }
                                )
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)

                                AdminListRow(
                                    title = "Novedades y Avisos",
                                    icon = Icons.Default.Campaign,
                                    onClick = { navController.navigate("admin_news_screen") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- COMPONENTS ---

@Composable
fun FinancialCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color.Green.copy(alpha = 0.3f))
    ) {
        Box(
            modifier = Modifier.background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Green.copy(alpha = 0.2f),
                        Color.Black.copy(alpha = 0.6f)
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List, // Fixed deprecation
                            contentDescription = null,
                            tint = Color.Green,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Finanzas y Reportes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Revisa tus ingresos mensuales, estado de cuenta y paga el servicio de la app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        lineHeight = 16.sp
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    icon: ImageVector,
    color: Color,
    badgeCount: Int = 0,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = modifier
            .height(140.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box {
                    // Icon base
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    
                    // Badge
                    if (badgeCount > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 6.dp, y = (-6).dp)
                                .background(Color.Red, CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeCount.toString(),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun AdminListRow(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title, 
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = Color.White.copy(alpha = 0.3f)
        )
    }
}

// Extension function for creating spacing in Column
@Composable
fun Column(spacing: androidx.compose.ui.unit.Dp, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(spacing), content = content)
}