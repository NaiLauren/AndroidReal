package com.aquiles.crosschapp.presentation.home

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.data.model.CreditRequest
import com.aquiles.crosschapp.data.model.CreditRequestStatus
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import com.aquiles.crosschapp.presentation.viewmodel.CreditRequestsListState
import com.aquiles.crosschapp.presentation.viewmodel.RequestUpdateState
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import com.aquiles.crosschapp.presentation.components.GlassCard

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.75f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
private val ColorBackgroundGradientStart = Color(0xFF000000)
private val ColorBackgroundGradientEnd = Color(0xFF121212)
private val ColorError = Color(0xFFEF5350)
private val ColorSuccess = Color(0xFF4CAF50)
private val ColorDialogSurface = Color(0xFF1C1C1E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCreditRequestsScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    adminViewModel: AdminViewModel = viewModel()
) {
    val requestsState by adminViewModel.requestsState.collectAsState()
    val updateState by adminViewModel.updateState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(key1 = Unit) {
        adminViewModel.loadAllCreditRequests()
    }

    LaunchedEffect(updateState) {
        when (val state = updateState) {
            is RequestUpdateState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                adminViewModel.loadAllCreditRequests()
                adminViewModel.resetUpdateState()
            }
            is RequestUpdateState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                adminViewModel.resetUpdateState()
            }
            else -> {}
        }
    }

    // --- UI STRUCTURE ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            // .background(Color.Black.copy(alpha = 0.4f)) // Removed to show AppBackground
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Solicitudes", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = ColorTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { localScaffoldPadding ->
            when (val state = requestsState) {
                is CreditRequestsListState.Loading, is CreditRequestsListState.Idle -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ColorPrimaryAction)
                    }
                }
                is CreditRequestsListState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${state.message}", color = ColorError)
                    }
                }
                is CreditRequestsListState.Empty -> {
                    GlassCard(modifier = Modifier.align(Alignment.Center).padding(32.dp)) {
                        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No hay solicitudes pendientes.", color = ColorTextSecondary)
                        }
                    }
                }
                is CreditRequestsListState.Success -> {
                    val isOperationInProgress = updateState is RequestUpdateState.Loading
                    var showHistoryDetailDialog by remember { mutableStateOf(false) }
                    var selectedHistoryRequest by remember { mutableStateOf<CreditRequest?>(null) }

                    if (showHistoryDetailDialog && selectedHistoryRequest != null) {
                        GlassProcessedRequestDetailDialog(
                            request = selectedHistoryRequest!!,
                            onDismiss = { showHistoryDetailDialog = false }
                        )
                    }

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
                        if (state.pendingRequests.isNotEmpty()) {
                            item {
                                Text(
                                    "PENDIENTES",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
                                    color = ColorTextSecondary
                                )
                            }
                            items(state.pendingRequests, key = { "pending-${it.id}" }) { request ->
                                GlassCreditRequestItem(
                                    request = request,
                                    onApprove = { adminViewModel.approveRequest(it) },
                                    onReject = { adminViewModel.rejectRequest(it) },
                                    isEnabled = !isOperationInProgress
                                )
                            }
                        }

                        if (state.processedRequests.isNotEmpty()) {
                            item {
                                Text(
                                    "HISTORIAL",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp, start = 4.dp),
                                    color = ColorTextSecondary
                                )
                            }
                            items(state.processedRequests, key = { "processed-${it.id}" }) { request ->
                                GlassProcessedRequestItem(
                                    request = request,
                                    onClick = {
                                        selectedHistoryRequest = request
                                        showHistoryDetailDialog = true
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
fun GlassCreditRequestItem(
    request: CreditRequest,
    onApprove: (CreditRequest) -> Unit,
    onReject: (CreditRequest) -> Unit,
    isEnabled: Boolean
) {
    val dateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "AR"))
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = request.userName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = ColorTextPrimary)
                    Text(
                        text = request.requestDate?.let { dateFormat.format(it) } ?: "N/A",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorTextSecondary
                    )
                }
                Text(
                    text = currencyFormat.format(request.amountPaid),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorPrimaryAction
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = ColorBorder)

            InfoRow(label = "Combo:", value = request.comboName)
            InfoRow(label = "Créditos:", value = request.creditsRequested.toString())
            InfoRow(label = "Pago:", value = request.paymentMethod)

            if (!request.contactInfo.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text("Contacto:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(80.dp), color = ColorTextSecondary)
                    Text(request.contactInfo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), color = ColorTextPrimary, maxLines = 1)
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(request.contactInfo))
                            Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = ColorPrimaryAction)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!request.paymentProofUrl.isNullOrBlank()) {
                    GlassViewProofButton(paymentProofPath = request.paymentProofUrl, isEnabled = isEnabled)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { onReject(request) },
                        enabled = isEnabled,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorError),
                        border = BorderStroke(1.dp, ColorError.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Rechazar")
                    }
                    Button(
                        onClick = { onApprove(request) },
                        enabled = isEnabled,
                        colors = ButtonDefaults.buttonColors(containerColor = ColorSuccess),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Aprobar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun GlassProcessedRequestItem(
    request: CreditRequest,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
    val isApproved = request.status == CreditRequestStatus.APPROVED.name
    val icon = if (isApproved) Icons.Default.CheckCircle else Icons.Default.DoNotDisturbOn
    val iconColor = if (isApproved) ColorSuccess else ColorError

    GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(request.userName, fontWeight = FontWeight.SemiBold, color = ColorTextPrimary)
                    Text(
                        "${request.comboName} • ${request.processedDate?.let { dateFormat.format(it) } ?: ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun GlassProcessedRequestDetailDialog(
    request: CreditRequest,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "AR"))

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorDialogSurface,
        titleContentColor = ColorTextPrimary,
        textContentColor = ColorTextSecondary,
        title = { Text("Detalle Histórico", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoRow("Usuario:", request.userName)
                InfoRow("Combo:", request.comboName)
                InfoRow("Monto:", currencyFormat.format(request.amountPaid))
                InfoRow("Estado:", request.status)
                InfoRow("Admin:", request.processedByAdminName ?: "N/A")
                InfoRow("Fecha:", request.processedDate?.let { dateFormat.format(it) } ?: "N/A")

                if (!request.paymentProofUrl.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    GlassViewProofButton(paymentProofPath = request.paymentProofUrl, isEnabled = true)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = ColorPrimaryAction)
            }
        }
    )
}

@Composable
fun GlassViewProofButton(paymentProofPath: String, isEnabled: Boolean) {
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paymentProofPath))
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "No se pudo abrir el comprobante.", Toast.LENGTH_SHORT).show()
            }
        },
        enabled = isEnabled,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, ColorPrimaryAction.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorTextPrimary),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, modifier = Modifier.size(18.dp), tint = ColorPrimaryAction)
        Spacer(Modifier.width(8.dp))
        Text("Ver Comprobante")
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(80.dp), color = ColorTextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = ColorTextPrimary)
    }
}