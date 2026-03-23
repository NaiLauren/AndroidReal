package com.aquiles.crosschapp.presentation.home

import com.aquiles.crosschapp.presentation.components.GlassCard
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import com.aquiles.crosschapp.presentation.viewmodel.PaymentSettingsState

// --- COLORES (Consistentes con tu App) ---
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorBackground = Color(0xFF121212)
private val ColorSurface = Color(0xFF1E1E1E)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.Gray
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.65f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPaymentConfigScreen(
    viewModel: AdminViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.paymentSettingsState.collectAsState()
    val context = LocalContext.current

    // Variables locales para los campos de texto
    var bankInfo by remember { mutableStateOf("") }
    var mpInfo by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Cargar datos al iniciar
    LaunchedEffect(Unit) {
        viewModel.loadGymPaymentSettings()
    }

    // Reaccionar a cambios en el estado
    LaunchedEffect(state) {
        when (state) {
            is PaymentSettingsState.Loading -> {
                isLoading = true
            }
            is PaymentSettingsState.Success -> {
                isLoading = false
                val successState = state as PaymentSettingsState.Success
                // Solo actualizamos los campos si están vacíos (para no sobrescribir lo que escribe el usuario mientras edita)
                if (bankInfo.isEmpty() && mpInfo.isEmpty()) {
                    bankInfo = successState.bankInfo
                    mpInfo = successState.mpInfo
                }
            }
            is PaymentSettingsState.Error -> {
                isLoading = false
                Toast.makeText(context, (state as PaymentSettingsState.Error).message, Toast.LENGTH_LONG).show()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurar Pagos", color = ColorTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = ColorTextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f))
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Tarjeta de Información
                GlassCard(
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Instrucciones",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorTextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ingresa los datos bancarios y de Mercado Pago que verán tus alumnos al solicitar créditos. Estos datos son exclusivos para tu gimnasio.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ColorTextSecondary
                        )
                    }
                }

                // Campo: Transferencia Bancaria
                Column {
                    Text("Transferencia Bancaria (CBU/Alias)", color = ColorTextPrimary, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bankInfo,
                        onValueChange = { bankInfo = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        placeholder = { Text("Ej: Banco Galicia\nCBU: 000000...\nAlias: MI.GIMNASIO", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ColorGlassSurface,
                            unfocusedContainerColor = ColorGlassSurface,
                            focusedTextColor = ColorTextPrimary,
                            unfocusedTextColor = ColorTextPrimary,
                            cursorColor = ColorPrimaryAction,
                            focusedBorderColor = ColorPrimaryAction,
                            unfocusedBorderColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Campo: Mercado Pago
                Column {
                    Text("Mercado Pago (CVU/Alias/Link)", color = ColorTextPrimary, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = mpInfo,
                        onValueChange = { mpInfo = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        placeholder = { Text("Ej: Enviar dinero a...\nCVU: 0000...\nAlias: mp.gimnasio", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = ColorGlassSurface,
                            unfocusedContainerColor = ColorGlassSurface,
                            focusedTextColor = ColorTextPrimary,
                            unfocusedTextColor = ColorTextPrimary,
                            cursorColor = ColorPrimaryAction,
                            focusedBorderColor = ColorPrimaryAction,
                            unfocusedBorderColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón Guardar
                Button(
                    onClick = {
                        viewModel.saveGymPaymentSettings(bankInfo, mpInfo)
                        Toast.makeText(context, "Guardando información...", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Guardar Cambios", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}