package com.aquiles.crosschapp.presentation.home

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import com.aquiles.crosschapp.presentation.viewmodel.BillingRulesState
import com.aquiles.crosschapp.presentation.viewmodel.CreditPack
import com.aquiles.crosschapp.presentation.viewmodel.CreditPacksState
import com.aquiles.crosschapp.presentation.viewmodel.PaymentSettingsState
import java.text.NumberFormat
import java.util.*
import com.aquiles.crosschapp.presentation.components.GlassCard
import androidx.compose.foundation.layout.height

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.65f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
private val ColorBackgroundGradientStart = Color(0xFF000000)
private val ColorBackgroundGradientEnd = Color(0xFF121212)
private val ColorDialogSurface = Color(0xFF1C1C1E)
private val ColorSuccess = Color(0xFF4CAF50)
private val ColorError = Color(0xFFEF5350)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManagePacksScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    adminViewModel: AdminViewModel = viewModel(),
    setupStepKey: String? = null
) {
    var showSetupPopup by remember { mutableStateOf(setupStepKey != null) }
    
    if (showSetupPopup) {
        SetupStep.entries.find { it.toKey() == setupStepKey }?.let { step ->
            AlertDialog(
                onDismissRequest = { showSetupPopup = false },
                title = { Text(step.title, color = ColorTextPrimary) },
                text = { Text(step.description, color = ColorTextSecondary) },
                confirmButton = {
                    Button(onClick = { showSetupPopup = false }, colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction)) { 
                        Text("Entendido", color = Color.White) 
                    }
                },
                containerColor = ColorDialogSurface
            )
        }
    }
    
    val packsState by adminViewModel.creditPacksState.collectAsState()
    val paymentSettingsState by adminViewModel.paymentSettingsState.collectAsState()
    val billingRulesState by adminViewModel.billingRulesState.collectAsState()
    
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedPack by remember { mutableStateOf<CreditPack?>(null) }
    var packToDelete by remember { mutableStateOf<CreditPack?>(null) }

    LaunchedEffect(Unit) {
        adminViewModel.loadAllCreditPacks()
        adminViewModel.loadAllCreditPacks()
        adminViewModel.loadBillingRules()
        adminViewModel.loadGymPaymentSettings()
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
                    title = { Text("Gestionar Packs", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ColorTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f))
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        selectedPack = null
                        showEditDialog = true
                    },
                    containerColor = ColorPrimaryAction,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, "Añadir")
                }
            },
            containerColor = Color.Transparent
        ) { localScaffoldPadding ->

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = localScaffoldPadding.calculateTopPadding())
            ) {

                GlassPaymentConfigCard(
                     adminViewModel = adminViewModel,
                     paymentSettingsState = paymentSettingsState,
                     billingRulesState = billingRulesState
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    when (val state = packsState) {
                        is CreditPacksState.Loading, CreditPacksState.Idle -> {
                            CircularProgressIndicator(color = ColorPrimaryAction, modifier = Modifier.align(Alignment.Center))
                        }
                        is CreditPacksState.Error -> {
                            Text("Error: ${state.message}", modifier = Modifier.align(Alignment.Center), color = ColorError)
                        }
                        is CreditPacksState.Empty -> {
                            GlassCard(modifier = Modifier.fillMaxWidth().padding(32.dp)) {
                                Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("No hay packs creados.", color = ColorTextSecondary)
                                }
                            }
                        }
                        is CreditPacksState.Success -> {
                            LazyColumn(
                                contentPadding = PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 16.dp,
                                    bottom = innerPadding.calculateBottomPadding() + 80.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(state.packs, key = { it.id }) { pack ->
                                    GlassCreditPackItem(
                                        pack = pack,
                                        onEditClick = {
                                            selectedPack = pack
                                            showEditDialog = true
                                        },
                                        onDeleteClick = {
                                            packToDelete = pack
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

    if (showEditDialog) {
        val currentPlan = (billingRulesState as? BillingRulesState.Success)?.rules?.billingPlan ?: "STANDARD"
        GlassEditPackDialog(
            pack = selectedPack,
            billingPlan = currentPlan,
            onDismiss = { showEditDialog = false },
            onSave = { packToSave ->
                adminViewModel.saveCreditPack(packToSave)
                showEditDialog = false
            }
        )
    }

    packToDelete?.let { pack ->
        AlertDialog(
            onDismissRequest = { packToDelete = null },
            containerColor = ColorDialogSurface,
            title = { Text("Eliminar Pack", color = ColorTextPrimary) },
            text = { Text("¿Eliminar '${pack.name}'? Esta acción es permanente.", color = ColorTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.deleteCreditPack(pack.id)
                        packToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorError)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { packToDelete = null }) {
                    Text("Cancelar", color = ColorTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun GlassPaymentConfigCard(
    adminViewModel: AdminViewModel,
    paymentSettingsState: PaymentSettingsState,
    billingRulesState: BillingRulesState
) {
    var alias by remember { mutableStateOf("") }
    var cbu by remember { mutableStateOf("") }
    var hasLoaded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Sync state to local variables when loaded
    LaunchedEffect(paymentSettingsState) {
        if (paymentSettingsState is PaymentSettingsState.Success) {
            alias = paymentSettingsState.bankInfo
            cbu = paymentSettingsState.mpInfo
            hasLoaded = true
        }
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Configuración de Recepción", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
                    Text("Datos para que tus alumnos te paguen.", style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))
            
            if (paymentSettingsState is PaymentSettingsState.Loading) {
                 CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = ColorPrimaryAction)
            } else {
                 GlassDialogTextField(value = alias, onValueChange = { alias = it }, label = "Alias (CBU/CVU)")
                 Spacer(Modifier.height(8.dp))
                 GlassDialogTextField(value = cbu, onValueChange = { cbu = it }, label = "CVU / CBU (Numérico)", keyboardType = KeyboardType.Number)
                 
                 Spacer(Modifier.height(16.dp))
                 
                 Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                 ) {
                     Button(
                         onClick = {
                             adminViewModel.saveGymPaymentSettings(alias, cbu)
                             Toast.makeText(context, "Datos de pago guardados.", Toast.LENGTH_SHORT).show()
                         },
                         colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction),
                         shape = RoundedCornerShape(8.dp),
                         modifier = Modifier.height(40.dp)
                     ) {
                         Text("Guardar Cambios")
                     }
                 }
            }
            
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = ColorBorder)
            Spacer(Modifier.height(16.dp))

            // Surcharge Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                 Column(Modifier.weight(1f)) {
                    Text("Recargos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
                    Text("Activa para usar precios con mora.", style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)
                }
                Spacer(Modifier.width(16.dp))

                when (val state = billingRulesState) {
                    is BillingRulesState.Loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp), color = ColorPrimaryAction)
                    is BillingRulesState.Error -> Switch(checked = false, onCheckedChange = null, enabled = false)
                    is BillingRulesState.Success -> {
                        Switch(
                            checked = state.rules.isSurchargeActive,
                            onCheckedChange = { isEnabled -> adminViewModel.setSurchargeStatus(isEnabled) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ColorPrimaryAction,
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.Transparent,
                                uncheckedBorderColor = ColorTextSecondary
                            )
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun GlassCreditPackItem(
    pack: CreditPack,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR")) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(pack.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorTextPrimary)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (pack.isUnlimited) {
                        Text("∞ Pase Libre", style = MaterialTheme.typography.bodyMedium, color = ColorSuccess, fontWeight = FontWeight.Bold)
                    } else {
                        Text("${pack.credits} Créditos", style = MaterialTheme.typography.bodyMedium, color = ColorSuccess, fontWeight = FontWeight.Bold)
                    }
                    if(pack.description.isNotBlank()) {
                        Text(" • ${pack.description}", style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary, maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column {
                        Text("NORMAL", style = MaterialTheme.typography.labelSmall, color = ColorTextSecondary)
                        Text(currencyFormat.format(pack.price), style = MaterialTheme.typography.bodyLarge, color = ColorTextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("RECARGO", style = MaterialTheme.typography.labelSmall, color = ColorTextSecondary)
                        Text(currencyFormat.format(pack.surchargePrice), style = MaterialTheme.typography.bodyLarge, color = ColorPrimaryAction, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column {
                IconButton(onClick = onEditClick) {
                    Icon(Icons.Default.Edit, "Editar", tint = ColorPrimaryAction)
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(Icons.Default.Delete, "Eliminar", tint = ColorError.copy(alpha = 0.8f))
                }
            }
        }
    }
}

@Composable
private fun GlassEditPackDialog(
    pack: CreditPack?,
    billingPlan: String,
    onDismiss: () -> Unit,
    onSave: (CreditPack) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(pack?.name ?: "") }
    var description by remember { mutableStateOf(pack?.description ?: "") }
    var credits by remember { mutableStateOf(pack?.credits?.toString() ?: "") }
    var basePrice by remember { mutableStateOf("") } // Precio que el admin quiere ganar
    var baseSurchargePrice by remember { mutableStateOf("") }
    var order by remember { mutableStateOf(pack?.order?.toString() ?: "0") }
    var isUnlimited by remember { mutableStateOf(pack?.isUnlimited ?: false) }
    var durationDays by remember { mutableStateOf(pack?.durationDays?.toString() ?: "30") }
    val isEditMode = pack != null

    // Cálculo de recargo según plan
    val monthlyFee = if (billingPlan == "PARTNER") 2000.0 else 1500.0
    val durationFactor = (durationDays.toDoubleOrNull() ?: 30.0) / 30.0
    val totalServiceFee = monthlyFee * durationFactor

    // Inicializar precios base restando el fee si estamos editando
    LaunchedEffect(pack) {
        if (pack != null) {
            val factor = pack.durationDays.toDouble() / 30.0
            val currentFee = (if (billingPlan == "PARTNER") 2000.0 else 1500.0) * factor
            basePrice = (pack.price - currentFee).coerceAtLeast(0.0).toString()
            baseSurchargePrice = (pack.surchargePrice - currentFee).coerceAtLeast(0.0).toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ColorDialogSurface,
        title = { Text(if (isEditMode) "Editar Pack" else "Nuevo Pack", color = ColorTextPrimary) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GlassDialogTextField(value = name, onValueChange = { name = it }, label = "Nombre *")
                GlassDialogTextField(value = description, onValueChange = { description = it }, label = "Descripción")
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isUnlimited, onCheckedChange = { isUnlimited = it }, colors = CheckboxDefaults.colors(checkedColor = ColorPrimaryAction))
                    Text("Es Pase Libre (Ilimitado)", color = ColorTextPrimary)
                }

                if (!isUnlimited) {
                    GlassDialogTextField(value = credits, onValueChange = { credits = it.filter { c -> c.isDigit() } }, label = "Créditos *", keyboardType = KeyboardType.Number)
                }

                GlassDialogTextField(value = durationDays, onValueChange = { durationDays = it.filter { c -> c.isDigit() } }, label = "Duración en Días * (ej. 30)", keyboardType = KeyboardType.Number)
                
                Column {
                    GlassDialogTextField(value = basePrice, onValueChange = { basePrice = it.filter { c -> c.isDigit() || c == '.' } }, label = "Tu Ganancia Pack (Normal) *", keyboardType = KeyboardType.Decimal)
                    val finalPrice = (basePrice.toDoubleOrNull() ?: 0.0) + totalServiceFee
                    Text(
                        "Al alumno le figura: ${formatCurrency(finalPrice)} (Incluye +${formatCurrency(totalServiceFee)} de App)",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorPrimaryAction,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                Column {
                    GlassDialogTextField(value = baseSurchargePrice, onValueChange = { baseSurchargePrice = it.filter { c -> c.isDigit() || c == '.' } }, label = "Tu Ganancia Pack (Mora) *", keyboardType = KeyboardType.Decimal)
                    val finalSurcharge = (baseSurchargePrice.toDoubleOrNull() ?: 0.0) + totalServiceFee
                    Text(
                        "Al alumno le figura: ${formatCurrency(finalSurcharge)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorTextSecondary,
                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                    )
                }

                GlassDialogTextField(value = order, onValueChange = { order = it.filter { c -> c.isDigit() } }, label = "Orden *", keyboardType = KeyboardType.Number)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val creditsInt = if (isUnlimited) 9999 else credits.toIntOrNull()
                    val priceDouble = (basePrice.toDoubleOrNull() ?: 0.0) + totalServiceFee
                    val surchargePriceDouble = (baseSurchargePrice.toDoubleOrNull() ?: 0.0) + totalServiceFee
                    val orderInt = order.toIntOrNull()
                    val durationInt = durationDays.toIntOrNull() ?: 30

                    if (name.isBlank() || creditsInt == null || orderInt == null) {
                        Toast.makeText(context, "Completa los campos obligatorios.", Toast.LENGTH_SHORT).show()
                    } else {
                        val packToSave = CreditPack(
                            id = pack?.id ?: "",
                            name = name,
                            description = description,
                            credits = creditsInt,
                            price = priceDouble,
                            surchargePrice = surchargePriceDouble,
                            order = orderInt,
                            isActive = pack?.isActive ?: true,
                            isUnlimited = isUnlimited,
                            durationDays = durationInt
                        )
                        onSave(packToSave)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction)
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = ColorTextSecondary) }
        }
    )
}

@Composable
fun GlassDialogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = ColorTextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ColorPrimaryAction,
            unfocusedBorderColor = ColorBorder,
            focusedTextColor = ColorTextPrimary,
            unfocusedTextColor = ColorTextPrimary,
            cursorColor = ColorPrimaryAction,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

private fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR"))
    return formatter.format(amount)
}