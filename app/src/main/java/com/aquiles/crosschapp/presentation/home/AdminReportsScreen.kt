package com.aquiles.crosschapp.presentation.home

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
import androidx.compose.material.icons.filled.People
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
import com.aquiles.crosschapp.data.model.CreditRequest
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import com.aquiles.crosschapp.presentation.viewmodel.ReportsState
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.75f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
private val ColorBackgroundGradientStart = Color(0xFF000000)
private val ColorBackgroundGradientEnd = Color(0xFF121212)
private val ColorSuccess = Color(0xFF4CAF50)

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
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Reportes", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
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
                        ReportsContent(state, innerPadding)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportsContent(state: ReportsState.Success, innerPadding: PaddingValues) {
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

        Text(
            "TRANSACCIONES",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = ColorTextSecondary,
            modifier = Modifier.fillMaxWidth().padding(start = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (state.monthlyTransactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sin movimientos este mes.", color = ColorTextSecondary.copy(alpha = 0.5f))
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
    val dateFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale("es", "ES")) }

    Card(
        shape = RoundedCornerShape(50), // Pill shape
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface),
        border = BorderStroke(1.dp, ColorBorder)
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
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ColorBorder),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface)
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
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale("es", "ES")) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ColorBorder),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface)
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

private fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("es", "AR")).format(amount)
}