package com.aquiles.crosschapp.presentation.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.data.model.CreditRequest
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import com.aquiles.crosschapp.presentation.viewmodel.ReportsState
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import com.aquiles.crosschapp.presentation.components.GlassCard
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.layout.height

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.45f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
private val ColorBackgroundGradientStart = Color(0xFF000000)
private val ColorBackgroundGradientEnd = Color(0xFF121212)
private val ColorSuccess = Color(0xFF4CAF50)
private val ColorWarning = Color(0xFFFFC107)
private val ColorError = Color(0xFFEF5350)
private val ColorInfo = Color(0xFF2196F3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportsScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    adminViewModel: AdminViewModel = viewModel()
) {
    val reportsState by adminViewModel.reportsState.collectAsState()
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }

    LaunchedEffect(selectedDate) {
        val year = selectedDate.get(Calendar.YEAR)
        val month = selectedDate.get(Calendar.MONTH) + 1
        adminViewModel.loadReportsForMonth(year, month)
    }

    // --- UI STRUCTURE ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            // .background(Color.Black.copy(alpha = 0.4f)) // Removed for glass background
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    modifier = Modifier.height(72.dp),
                    title = { Text("Reportes", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ColorTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f))
                )
            },
            containerColor = Color.Transparent
        ) { localScaffoldPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = localScaffoldPadding.calculateTopPadding())
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Selector de Mes
                GlassMonthSelector(
                    selectedDate = selectedDate.time,
                    onPreviousMonth = {
                        val newCalendar = selectedDate.clone() as Calendar
                        newCalendar.add(Calendar.MONTH, -1)
                        selectedDate = newCalendar
                    },
                    onNextMonth = {
                        val newCalendar = selectedDate.clone() as Calendar
                        newCalendar.add(Calendar.MONTH, 1)
                        selectedDate = newCalendar
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                when (val state = reportsState) {
                    is ReportsState.Loading, is ReportsState.Idle -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ColorPrimaryAction)
                        }
                    }
                    is ReportsState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    is ReportsState.Success -> {
                        ReportsContent(state, innerPadding, adminViewModel, selectedDate)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsContent(
    state: ReportsState.Success, 
    innerPadding: PaddingValues, 
    adminViewModel: AdminViewModel, 
    selectedDate: Calendar
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Tarjetas de Métricas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GlassMetricCard(
                title = "Ingresos",
                value = formatCurrency(state.totalRevenue),
                icon = Icons.Default.AttachMoney,
                modifier = Modifier.weight(1f),
                isMoney = true
            )
            GlassMetricCard(
                title = "Activos",
                value = state.monthlyActiveUsers.toString(),
                icon = Icons.Default.People,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        // --- APP COSTS SECTION ---
        Text(
            "COSTOS DEL SERVICIO",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = ColorTextSecondary,
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        GlassAppCostCard(state, adminViewModel, selectedDate)
        
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "TRANSACCIONES",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = ColorTextSecondary,
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (state.monthlyTransactions.isEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Sin movimientos este mes.", color = ColorTextSecondary.copy(alpha = 0.5f))
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.monthlyTransactions, key = { it.id }) { transaction ->
                    GlassTransactionItem(transaction)
                }
            }
        }
    }
}

@Composable
private fun GlassMonthSelector(
    selectedDate: Date,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val dateFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.Builder().setLanguage("es").setRegion("ES").build()) }

    GlassCard(
        shape = RoundedCornerShape(50), // Pill shape
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onPreviousMonth) {
                Icon(Icons.AutoMirrored.Filled.ArrowLeft, null, tint = ColorTextPrimary)
            }

            Text(
                text = dateFormatter.format(selectedDate).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ColorTextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            IconButton(onClick = onNextMonth) {
                Icon(Icons.AutoMirrored.Filled.ArrowRight, null, tint = ColorTextPrimary)
            }
        }
    }
}

@Composable
private fun GlassMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isMoney: Boolean = false
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = ColorTextPrimary)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(text = title.uppercase(), style = MaterialTheme.typography.labelSmall, color = ColorTextSecondary, fontWeight = FontWeight.Bold)

            // Ajuste dinámico de fuente
            val baseStyle = MaterialTheme.typography.headlineSmall
            var fontSize by remember(value) { mutableStateOf(baseStyle.fontSize) }

            Text(
                text = value,
                style = baseStyle.copy(fontSize = fontSize),
                fontWeight = FontWeight.ExtraBold,
                color = if (isMoney) ColorSuccess else ColorTextPrimary,
                maxLines = 1,
                softWrap = false,
                onTextLayout = { result ->
                    if (result.didOverflowWidth) fontSize *= 0.9f
                }
            )
        }
    }
}

@Composable
private fun GlassTransactionItem(transaction: CreditRequest) {
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale.Builder().setLanguage("es").setRegion("ES").build()) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = transaction.userName, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
                Text(text = transaction.comboName, style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCurrency(transaction.amountPaid),
                    fontWeight = FontWeight.Bold,
                    color = ColorSuccess,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = transaction.processedDate?.let { dateFormat.format(it) } ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorTextSecondary
                )
            }
        }
    }
}

private fun formatCurrency(amount: Double, currency: String = "ARS"): String {
    val locale = if (currency == "USD") Locale.US else Locale.Builder().setLanguage("es").setRegion("AR").build()
    return NumberFormat.getCurrencyInstance(locale).format(amount)
}

@Composable
private fun GlassAppCostCard(
    state: ReportsState.Success,
    viewModel: AdminViewModel,
    selectedDate: Calendar
) {
    val context = LocalContext.current
    val totalCostUSD = state.monthlyActiveUsers * 1.13
    val dollarRate = state.dollarRate ?: 0.0
    val estimatedCostARS = totalCostUSD * dollarRate

    val statusColor = when (state.paymentStatus) {
        "PAID" -> ColorSuccess
        "PENDING_REVIEW" -> ColorInfo
        "REJECTED" -> ColorError
        else -> ColorWarning
    }

    val statusText = when (state.paymentStatus) {
        "PAID" -> "PAGADO"
        "PENDING_REVIEW" -> "EN REVISIÓN"
        "REJECTED" -> "RECHAZADO"
        else -> "PENDIENTE"
    }
    
    val monthFormat = remember { SimpleDateFormat("MM/yyyy", Locale.US) }
    val invoiceNumber = "RF-${monthFormat.format(selectedDate.time).replace("/", "")}"

    // Image Picker
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.uploadProofOfPayment(
                uri, 
                selectedDate.get(Calendar.MONTH) + 1,
                selectedDate.get(Calendar.YEAR),
                totalCostUSD, 
                context
            )
        }
    }

    Box(Modifier.fillMaxWidth()) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161618).copy(alpha = 0.8f)) // Fondo "papel negro" de la factura
            ) {
                // INVOICE HEADER
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "LIQUIDACIÓN MENSUAL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = ColorTextPrimary,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "Factura N° $invoiceNumber",
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorTextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                DashedDivider()

                // INVOICE BODY (Desglose)
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("DESGLOSE DE SERVICIO", style = MaterialTheme.typography.labelSmall, color = ColorPrimaryAction, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    InvoiceRow("Alumnos Activos Mensuales", "${state.monthlyActiveUsers} x $1.13")
                    InvoiceRow("Licencia Base App", "Incluida")
                    InvoiceRow("Soporte Técnico", "Incluido")
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // TOTAL USD
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("TOTAL USD", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = ColorTextPrimary)
                        Text(
                            formatCurrency(totalCostUSD, "USD"),
                            style = MaterialTheme.typography.headlineMedium,
                            color = ColorTextPrimary,
                            fontWeight = FontWeight.Black
                        )
                    }
                    
                    // CONVERSIÓN ARS
                    if (dollarRate > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Abono en Pesos (ARS)", style = MaterialTheme.typography.labelSmall, color = ColorTextSecondary)
                                Text("Cotización Dólar Oficial: ${formatCurrency(dollarRate)}", style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary.copy(alpha = 0.7f))
                            }
                            Text(formatCurrency(estimatedCostARS), style = MaterialTheme.typography.titleMedium, color = ColorSuccess, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                DashedDivider()

                // INVOICE FOOTER (Payment section)
                Column(modifier = Modifier.padding(20.dp)) {
                    if (state.paymentStatus != "PAID") {
                        if (state.paymentInfo != null) {
                            PaymentInfoSection(state.paymentInfo)
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Button(
                            onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (state.paymentStatus == "REJECTED") ColorError else ColorPrimaryAction,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (state.paymentStatus == "REJECTED") "REENVIAR COMPROBANTE" else "ADJUNTAR COMPROBANTE")
                        }
                        
                        if (state.paymentStatus == "PENDING_REVIEW") {
                            Text(
                                "Comprobante en revisión.", 
                                style = MaterialTheme.typography.labelSmall, 
                                color = ColorWarning, 
                                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp)
                            )
                        }
                    } else {
                        // Pagado, mostrar gracias
                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Liquidación abonada exitosamente.\nGracias por confiar en RealFitness.",
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorTextSecondary,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
        
        // WATERMARK STAMP
        InvoiceStamp(text = statusText, color = statusColor)
    }
}

@Composable
private fun InvoiceRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = ColorTextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = ColorTextPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DashedDivider() {
    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(1.dp).padding(horizontal = 20.dp)) {
        drawLine(
            color = ColorTextSecondary.copy(alpha = 0.3f),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(size.width, 0f),
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
    }
}

@Composable
private fun BoxScope.InvoiceStamp(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Black,
        color = color.copy(alpha = 0.25f),
        modifier = Modifier
            .align(Alignment.Center)
            .rotate(-25f)
            .padding(16.dp)
    )
}

@Composable
private fun PaymentInfoSection(info: com.aquiles.crosschapp.presentation.viewmodel.AppPaymentInfo) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("DATOS DE TRANSFERENCIA", style = MaterialTheme.typography.labelSmall, color = ColorTextSecondary, fontWeight = FontWeight.Bold)
        
        PaymentDataItem("Banco", info.bankName)
        PaymentDataItem("Titular", info.holder)
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Alias", style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)
                Text(info.alias, style = MaterialTheme.typography.bodyMedium, color = ColorTextPrimary, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { clipboardManager.setPrimaryClip(ClipData.newPlainText("Copied Text", info.alias)) }) {
                Icon(Icons.Filled.FileCopy, null, tint = ColorPrimaryAction, modifier = Modifier.size(20.dp))
            }
        }
        
         Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("CBU / CVU", style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)
                Text(info.cbu, style = MaterialTheme.typography.bodyMedium, color = ColorTextPrimary, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = { clipboardManager.setPrimaryClip(ClipData.newPlainText("Copied Text", info.cbu)) }) {
                Icon(Icons.Filled.FileCopy, null, tint = ColorPrimaryAction, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun PaymentDataItem(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = ColorTextPrimary, fontWeight = FontWeight.Bold)
    }
}