package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.data.model.GymPaymentDashboardItem
import com.aquiles.crosschapp.data.model.PaymentStatus
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.viewmodel.SuperAdminPaymentsViewModel
import com.aquiles.crosschapp.presentation.viewmodel.UserSession
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperAdminPaymentsScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: SuperAdminPaymentsViewModel = viewModel()
) {
    val dashboardItems by viewModel.dashboardItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val totalEstRevenue by viewModel.totalEstRevenue.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var itemToConfirm: GymPaymentDashboardItem? by remember { mutableStateOf(null) }

    val dateFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        viewModel.checkPermissionAndLoad()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(72.dp),
                title = {
                    Text(
                        "Panel de Cobranza",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, "Configuración", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f)
                )
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header with total revenue
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111111))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Facturación Potencial",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    formatUSD(totalEstRevenue),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFC5200)
                )
                
                // Month selector
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .background(
                            Color.White.opacity(0.1f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.changeMonth(-1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("←", color = Color.White, fontSize = 18.sp)
                    }
                    
                    Text(
                        dateFormatter.format(selectedDate).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.width(160.dp),
                        fontSize = 14.sp
                    )
                    
                    IconButton(
                        onClick = { viewModel.changeMonth(1) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Text("→", color = Color.White, fontSize = 18.sp)
                    }
                }
            }

            // Content
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFC5200))
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            errorMessage ?: "",
                            color = Color.Red,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(dashboardItems) { item ->
                            GymDashboardCard(
                                item = item,
                                onProofClick = { /* TODO: Show proof modal */ },
                                onActionClick = {
                                    if (item.status != PaymentStatus.PAID) {
                                        itemToConfirm = item
                                        showConfirmDialog = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Confirm Dialog
    if (showConfirmDialog && itemToConfirm != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirmar Pago") },
            text = {
                Text("¿Estás seguro de marcar como PAGADO a ${itemToConfirm?.gymName}?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        itemToConfirm?.let { viewModel.markAsPaid(it) }
                        showConfirmDialog = false
                        itemToConfirm = null
                    }
                ) {
                    Text("Confirmar", color = Color(0xFFFC5200))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    // Settings Dialog
    if (showSettingsDialog) {
        PaymentSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@Composable
private fun GymDashboardCard(
    item: GymPaymentDashboardItem,
    onProofClick: () -> Unit,
    onActionClick: () -> Void
) {
    val statusColor = when (item.status) {
        PaymentStatus.PAID -> Color.Green
        PaymentStatus.PENDING_REVIEW -> Color.Yellow
        PaymentStatus.REJECTED -> Color.Red
        PaymentStatus.UNPAID -> Color.Red.copy(alpha = 0.7f)
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Status indicator bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(statusColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp)
            ) {
                // Gym name and last payment
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        item.gymName,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    if (item.status != PaymentStatus.PAID) {
                        Text(
                            "Últ. Pago: ${item.lastPaymentDateStr}",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            modifier = Modifier
                                .background(
                                    Color.White.opacity(0.05f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(4.dp)
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Metrics
                Row(
                    horizontalArrangement = Arrangement.spacedBy(15.dp)
                ) {
                    Text(
                        "👥 ${item.studentCount} alumnos",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    
                    Text(
                        "💵 ${formatUSD(item.estimatedDebt)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Status and actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        when (item.status) {
                            PaymentStatus.PAID -> "AL DÍA"
                            PaymentStatus.PENDING_REVIEW -> "REQUIERE REVISIÓN"
                            PaymentStatus.UNPAID -> "PENDIENTE DE PAGO"
                            PaymentStatus.REJECTED -> "RECHAZADO"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (!item.proofUrl.isNullOrEmpty()) {
                            IconButton(
                                onClick = onProofClick,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.DocumentScanner,
                                    "Ver comprobante",
                                    tint = Color.Blue
                                )
                            }
                        }

                        if (item.status != PaymentStatus.PAID) {
                            IconButton(
                                onClick = onActionClick,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    "Marcar pagado",
                                    tint = if (item.status == PaymentStatus.PENDING_REVIEW)
                                        Color.Green
                                    else
                                        Color.Gray
                                )
                            }
                        } else {
                            Icon(
                                Icons.Default.CheckCircle,
                                "Pagado",
                                tint = Color.Green.copy(alpha = 0.5f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentSettingsDialog(
    viewModel: SuperAdminPaymentsViewModel,
    onDismiss: () -> Unit
) {
    val paymentSettings by viewModel.paymentSettings.collectAsState()
    val isSaving by viewModel.isSavingSettings.collectAsState()

    var alias by remember { mutableStateOf(paymentSettings.alias) }
    var cbu by remember { mutableStateOf(paymentSettings.cbu) }
    var bankName by remember { mutableStateOf(paymentSettings.bankName) }
    var accountHolder by remember { mutableStateOf(paymentSettings.accountHolder) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurar Pagos") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text("Alias") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = cbu,
                    onValueChange = { cbu = it },
                    label = { Text("CBU / CVU") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Banco / Billetera") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = accountHolder,
                    onValueChange = { accountHolder = it },
                    label = { Text("Titular de la Cuenta") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.savePaymentSettings(alias, cbu, bankName, accountHolder)
                    onDismiss()
                },
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color(0xFFFC5200)
                    )
                } else {
                    Text("Guardar", color = Color(0xFFFC5200))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

private fun formatUSD(value: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    return formatter.format(value)
}
