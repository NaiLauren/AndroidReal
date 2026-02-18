package com.aquiles.crosschapp.presentation.home

import android.net.Uri
import android.util.Log
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MenuAnchorType // [Fix] Import added
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aquiles.crosschapp.presentation.components.FeedbackDialog
import com.aquiles.crosschapp.presentation.components.FeedbackType
import com.aquiles.crosschapp.presentation.viewmodel.*
import java.text.NumberFormat
import java.util.*

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.75f)
private val ColorDialogSurface = Color(0xFF1C1C1E) // Solido para dialogos para evitar transparencias raras
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
private val ColorBackgroundGradientStart = Color(0xFF000000)
private val ColorBackgroundGradientEnd = Color(0xFF121212)

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
        title = "¡Solicitud Enviada!",
        message = "Tu comprobante se ha subido correctamente. Te notificaremos cuando se acrediten tus créditos.",
        onDismiss = {
            showSuccessDialog = false
            creditsViewModel.resetCreditRequestOperationState()
            onNavigateBack()
        }
    )

    FeedbackDialog(
        show = showErrorDialog,
        type = FeedbackType.ERROR,
        title = "Hubo un problema",
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
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Comprar Créditos", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
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
                when (val state = offeringsState) {
                    is OfferingsState.Loading, OfferingsState.Idle -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ColorPrimaryAction)
                        }
                    }
                    is OfferingsState.Success -> {
                        // Warning Banner
                        if (state.surchargeApplied) {
                            GlassWarningBanner("Los precios incluyen recargo por pago fuera de término.")
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        if (state.packs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No hay packs disponibles.", color = ColorTextSecondary)
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(state.packs, key = { it.id }) { pack ->
                                    CreditPackItemGlass(
                                        pack = pack,
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
                            Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                // DIALOGO
                if (showConfirmationDialog && selectedPackForRequest != null) {
                    val isLoading = requestOperationState is CreditRequestOperationState.Loading
                    PaymentConfirmationDialogGlass(
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
// COMPONENTES UI (GLASS STYLE)
// =====================================================

@Composable
fun GlassWarningBanner(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEF5350).copy(alpha = 0.15f)), // Rojo suave
        border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, null, tint = Color(0xFFEF5350))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodySmall, color = Color.White)
        }
    }
}

@Composable
fun CreditPackItemGlass(
    pack: CreditPack,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR")) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, ColorBorder),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pack.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
                if (pack.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = pack.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorTextSecondary
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocalActivity, null, tint = ColorPrimaryAction, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${pack.credits} créditos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = currencyFormatter.format(pack.price),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = ColorPrimaryAction
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentConfirmationDialogGlass(
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

    // Selector de imagen nuevo
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            Log.d("IMAGE_PICKER", "Uri: $uri")
            imageUri = uri
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        containerColor = ColorDialogSurface, // Color solido oscuro
        textContentColor = ColorTextSecondary,
        titleContentColor = ColorTextPrimary,
        title = { Text("Confirmar Compra", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    color = ColorTextSecondary,
                    text = buildAnnotatedString {
                        append("Pack seleccionado: ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = ColorTextPrimary)) {
                            append(pack.name)
                        }
                        append("\nTotal a pagar: ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = ColorPrimaryAction, fontSize = 18.sp)) {
                            append(currencyFormatter.format(pack.price))
                        }
                    }
                )

                HorizontalDivider(color = ColorBorder)

                // Dropdown Estilizado
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { if (!isLoading) expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedPaymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Método de Pago", color = ColorTextSecondary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(
                            type = MenuAnchorType.PrimaryNotEditable,
                            enabled = !isLoading
                        ).fillMaxWidth(),
                        isError = showPaymentMethodError,
                        enabled = !isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ColorPrimaryAction,
                            unfocusedBorderColor = ColorBorder,
                            focusedTextColor = ColorTextPrimary,
                            unfocusedTextColor = ColorTextPrimary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(ColorDialogSurface)
                    ) {
                        paymentMethods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method, color = ColorTextPrimary) },
                                onClick = {
                                    selectedPaymentMethod = method
                                    showPaymentMethodError = false
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                if (showPaymentMethodError) {
                    Text("Selecciona un método de pago", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }

                // Datos de pago dinámicos
                if (selectedPaymentMethod == "Transferencia Bancaria" || selectedPaymentMethod == "MercadoPago") {
                    when (val state = paymentDetailsState) {
                        is PaymentDetailsState.Loading -> Box(Modifier.fillMaxWidth(), Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp), color = ColorPrimaryAction) }
                        is PaymentDetailsState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
                        is PaymentDetailsState.Success -> {
                            PaymentInfoCardGlass(label = "Alias", value = state.details.bankTransferInfo)
                            PaymentInfoCardGlass(label = "CVU/CBU", value = state.details.mercadoPagoInfo)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Botón Subir Comprobante
                    OutlinedButton(
                        onClick = {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorTextPrimary),
                        border = BorderStroke(1.dp, if (imageUri != null) ColorPrimaryAction else ColorBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(if(imageUri != null) Icons.Default.Check else Icons.Default.UploadFile, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (imageUri == null) "Adjuntar Comprobante" else "Comprobante Adjuntado")
                    }

                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, ColorBorder, RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Inside
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().background(ColorPrimaryAction.copy(alpha = 0.1f), RoundedCornerShape(8.dp)).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, null, tint = ColorPrimaryAction, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Se requiere verificación manual.",
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorTextSecondary
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedPaymentMethod.isNotBlank()) {
                        onConfirm(selectedPaymentMethod, imageUri)
                    } else {
                        showPaymentMethodError = true
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White) else Text("Enviar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancelar", color = ColorTextSecondary)
            }
        }
    )
}

@Composable
fun PaymentInfoCardGlass(label: String, value: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ColorBorder),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = ColorTextSecondary)
                Text(value, style = MaterialTheme.typography.bodyMedium, color = ColorTextPrimary, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = {
                clipboardManager.setText(AnnotatedString(value))
                Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = ColorPrimaryAction, modifier = Modifier.size(20.dp))
            }
        }
    }
}