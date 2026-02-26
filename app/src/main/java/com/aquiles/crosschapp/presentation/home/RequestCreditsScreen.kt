package com.aquiles.crosschapp.presentation.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.aquiles.crosschapp.ui.theme.LocalPrimaryColor
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aquiles.crosschapp.presentation.components.FeedbackDialog
import com.aquiles.crosschapp.presentation.components.FeedbackType
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.viewmodel.*
import java.text.NumberFormat
import java.util.*

// --- DESIGN SYSTEM CONSTANTS (PRO) ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.45f)
private val ColorDialogSurface = Color(0xFF121212)
// LocalPrimaryColor.current ahora usa LocalPrimaryColor.current (dinámico por gym)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.6f)
private val ColorBorder = Color.White.copy(alpha = 0.15f)
private val ColorGold = Color(0xFFFFD700)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestCreditsScreen(
    innerPadding: PaddingValues,
    creditsViewModel: CreditsViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val offeringsState by creditsViewModel.offeringsState.collectAsState()
    val requestOperationState by creditsViewModel.creditRequestOperationState.collectAsState()

    var showConfirmationDialog by remember { mutableStateOf(false) }
    var selectedPackForRequest by remember { mutableStateOf<CreditPack?>(null) }

    // Estados para Feedback Dialogs
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("") }

    // Feedback Handler
    LaunchedEffect(requestOperationState) {
        when (val state = requestOperationState) {
            is CreditRequestOperationState.Success -> {
                showSuccessDialog = true
            }
            is CreditRequestOperationState.Error -> {
                feedbackMessage = state.message
                showErrorDialog = true
            }
            else -> {}
        }
    }

    // Dialogs de Feedback
    FeedbackDialog(
        show = showSuccessDialog,
        type = FeedbackType.SUCCESS,
        title = "¡Solicitud Recibida!",
        message = "Hemos recibido tu comprobante. Procesaremos tu solicitud a la brevedad.",
        onDismiss = {
            showSuccessDialog = false
            creditsViewModel.resetCreditRequestOperationState()
            onNavigateBack()
        }
    )

    FeedbackDialog(
        show = showErrorDialog,
        type = FeedbackType.ERROR,
        title = "Error en la solicitud",
        message = feedbackMessage,
        onDismiss = {
            showErrorDialog = false
            creditsViewModel.resetCreditRequestOperationState()
        }
    )

    // --- UI STRUCTURE ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2C2C2E),
                        Color.Black
                    ),
                    center = Offset.Unspecified,
                    radius = 1500f
                )
            )
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {}, // Título custom en el body
                    navigationIcon = {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .padding(8.dp)
                                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        ) {
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
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // HEADER PRO
                Text(
                    text = "TIENDA",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = ColorTextPrimary,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Invierte en tu rendimiento",
                    style = MaterialTheme.typography.titleMedium,
                    color = LocalPrimaryColor.current,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                when (val state = offeringsState) {
                    is OfferingsState.Loading, OfferingsState.Idle -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = LocalPrimaryColor.current)
                        }
                    }
                    is OfferingsState.Success -> {
                        // Warning Banner
                        if (state.surchargeApplied) {
                            GlassWarningBanner("Precios actualizados por fecha de pago.")
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (state.packs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No hay packs disponibles.", color = ColorTextSecondary)
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 20.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                items(state.packs, key = { it.id }) { pack ->
                                    val isBestValue = pack.credits >= 12 // Lógica simple para destacar packs grandes
                                    ProCreditCard(
                                        pack = pack,
                                        isBestValue = isBestValue,
                                        onClick = {
                                            selectedPackForRequest = pack
                                            showConfirmationDialog = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                    is OfferingsState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CloudQueue, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                                Spacer(Modifier.height(16.dp))
                                Text("No pudimos cargar los packs", color = ColorTextSecondary)
                                TextButton(onClick = { /* Retry logic if available */ }) {
                                    Text("Reintentar", color = LocalPrimaryColor.current)
                                }
                            }
                        }
                    }
                }

                // DIALOGO PRO
                if (showConfirmationDialog && selectedPackForRequest != null) {
                    val isLoading = requestOperationState is CreditRequestOperationState.Loading
                    ProPaymentDialog(
                        pack = selectedPackForRequest!!,
                        onDismiss = { if (!isLoading) showConfirmationDialog = false },
                        onConfirm = { paymentMethod, imageUri ->
                            creditsViewModel.requestCredit(
                                pack = selectedPackForRequest!!,
                                paymentMethod = paymentMethod,
                                paymentProofUri = imageUri
                            )
                        },
                        isLoading = isLoading,
                        creditsViewModel = creditsViewModel
                    )
                }
            }
        }
    }
}

// =====================================================
// COMPONENTES PRO (UI ELEVADA)
// =====================================================

@Composable
fun GlassWarningBanner(text: String) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .background(Color(0xFFFF5252).copy(alpha = 0.1f))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, null, tint = Color(0xFFFF5252))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ProCreditCard(
    pack: CreditPack,
    isBestValue: Boolean,
    onClick: () -> Unit
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR")) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() }
    ) {
        // Fondo con gradiente y borde activo
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            ColorGlassSurface,
                            ColorGlassSurface.copy(alpha = 0.4f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = if (isBestValue) Brush.linearGradient(listOf(ColorGold, LocalPrimaryColor.current)) else Brush.linearGradient(listOf(ColorBorder, Color.Transparent)),
                    shape = RoundedCornerShape(24.dp)
                )
        )

        // Marca de agua
        Icon(
            imageVector = if(isBestValue) Icons.Default.WorkspacePremium else Icons.Default.ConfirmationNumber,
            contentDescription = null,
            tint = (if(isBestValue) ColorGold else Color.White).copy(alpha = 0.05f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 20.dp, y = 20.dp)
                .size(140.dp)
                .rotate(-15f)
        )

        // Contenido
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Info Pack
                Column(modifier = Modifier.weight(1f)) {
                    if (isBestValue) {
                        Surface(
                            color = ColorGold,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Text(
                                "MÁS POPULAR",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = pack.name.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextSecondary,
                        letterSpacing = 1.sp
                    )
                    if (pack.isUnlimited) {
                        Text(
                            text = "∞ PASE LIBRE",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = ColorTextPrimary
                        )
                    } else {
                        Text(
                            text = "${pack.credits} CRÉDITOS",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = ColorTextPrimary
                        )
                    }
                }
                
                // Precio
                Text(
                    text = currencyFormatter.format(pack.price),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isBestValue) ColorGold else LocalPrimaryColor.current
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (pack.description.isNotBlank()) {
                Text(
                    text = pack.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorTextSecondary,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Botón Falso (Visual)
            Button(
                onClick = onClick, // Redundante pero buen feedback tactil
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBestValue) LocalPrimaryColor.current else Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "COMPRAR AHORA",
                    fontWeight = FontWeight.Bold,
                    color = if (isBestValue) Color.White else ColorTextPrimary
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProPaymentDialog(
    pack: CreditPack,
    onDismiss: () -> Unit,
    onConfirm: (String, Uri?) -> Unit,
    isLoading: Boolean,
    creditsViewModel: CreditsViewModel
) {
    var selectedPaymentMethod by remember { mutableStateOf("") }
    val paymentMethods = listOf("MercadoPago", "Transferencia Bancaria", "Efectivo")
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showPaymentMethodError by remember { mutableStateOf(false) }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR")) }
    val paymentDetailsState by creditsViewModel.paymentDetailsState.collectAsState()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? -> if (uri != null) imageUri = uri }

    BasicAlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // TICKET SHAPE CARD
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = ColorDialogSurface,
            border = BorderStroke(1.dp, ColorBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Ticket
                Icon(Icons.Default.Description, null, tint = LocalPrimaryColor.current, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(16.dp))
                Text("Resumen de Compra", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
                Spacer(Modifier.height(24.dp))

                // Detalles
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Producto", color = ColorTextSecondary)
                    Text(pack.name, color = ColorTextPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Créditos", color = ColorTextSecondary)
                    if (pack.isUnlimited) {
                        Text("∞ (Pase Libre)", color = ColorTextPrimary, fontWeight = FontWeight.Bold)
                    } else {
                        Text("${pack.credits}", color = ColorTextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.fillMaxWidth(), color = ColorBorder, thickness = 1.dp) // Podría ser dashed
                Spacer(Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ColorTextPrimary)
                    Text(currencyFormatter.format(pack.price), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = LocalPrimaryColor.current)
                }

                Spacer(Modifier.height(24.dp))

                // Selector de Pago
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (!isLoading) expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedPaymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Método de Pago") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = !isLoading)
                            .fillMaxWidth(),
                        isError = showPaymentMethodError,
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LocalPrimaryColor.current,
                            unfocusedBorderColor = ColorBorder,
                            focusedLabelColor = LocalPrimaryColor.current,
                            unfocusedLabelColor = ColorTextSecondary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color(0xFF2C2C2E))
                    ) {
                        paymentMethods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method, color = Color.White) },
                                onClick = {
                                    selectedPaymentMethod = method
                                    showPaymentMethodError = false
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Datos Dinámicos
                if (selectedPaymentMethod == "Transferencia Bancaria" || selectedPaymentMethod == "MercadoPago") {
                    Spacer(Modifier.height(16.dp))
                    when (val state = paymentDetailsState) {
                        is PaymentDetailsState.Loading -> CircularProgressIndicator(Modifier.size(24.dp), color = LocalPrimaryColor.current)
                        is PaymentDetailsState.Success -> {
                            PaymentInfoCardGlass(label = "Alias / CBU", value = if(selectedPaymentMethod == "MercadoPago") state.details.mercadoPagoInfo else state.details.bankTransferInfo)
                        }
                        else -> {}
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorTextPrimary),
                        border = BorderStroke(1.dp, if (imageUri != null) ColorSuccess else ColorBorder)
                    ) {
                         Icon(if(imageUri != null) Icons.Default.CheckCircle else Icons.Default.UploadFile, null, tint = if(imageUri != null) ColorSuccess else ColorTextPrimary)
                         Spacer(Modifier.width(8.dp))
                         Text(if (imageUri != null) "Comprobante Cargado" else "Subir Comprobante")
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Botones Acción
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorTextSecondary),
                        border = BorderStroke(1.dp, ColorBorder)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            if (selectedPaymentMethod.isNotBlank()) onConfirm(selectedPaymentMethod, imageUri)
                            else showPaymentMethodError = true
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = LocalPrimaryColor.current)
                    ) {
                        if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        else Text("Confirmar")
                    }
                }
            }
        }
    }
}

// Reutilizamos PaymentInfoCardGlass pero mejorado
@Composable
fun PaymentInfoCardGlass(label: String, value: String) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .border(1.dp, ColorBorder, RoundedCornerShape(8.dp))
            .padding(12.dp)
            .clickable {
                clipboardManager.setPrimaryClip(ClipData.newPlainText("Copied Text", value))
                Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = ColorTextSecondary)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = ColorTextPrimary, fontWeight = FontWeight.Bold)
        }
        Icon(Icons.Default.ContentCopy, null, tint = LocalPrimaryColor.current, modifier = Modifier.size(18.dp))
    }
}

// Helpers
private val ColorSuccess = Color(0xFF4CAF50)