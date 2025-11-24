package com.aquiles.crosschapp.presentation.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aquiles.crosschapp.R
import com.aquiles.crosschapp.presentation.viewmodel.AuthViewModel
import com.aquiles.crosschapp.presentation.viewmodel.AuthState
import com.aquiles.crosschapp.presentation.viewmodel.PasswordResetState
import com.aquiles.crosschapp.ui.theme.CrossChAppTheme

// Colores Design System
private val OrangePrimary = Color(0xFFFC5200)
private val SurfaceDark = Color(0xFF2C2C2E)
private val TextWhite = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xB3FFFFFF)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val passwordResetState by authViewModel.passwordResetState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }

    // Manejo de estados (Login y Reset)
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> { onLoginSuccess(); authViewModel.resetAuthState() }
            is AuthState.Error -> { Toast.makeText(context, state.message, Toast.LENGTH_LONG).show(); authViewModel.resetAuthState() }
            else -> {}
        }
    }
    LaunchedEffect(passwordResetState) {
        when (val state = passwordResetState) {
            is PasswordResetState.Success -> { Toast.makeText(context, state.message, Toast.LENGTH_LONG).show(); showResetDialog = false; authViewModel.resetPasswordResetState() }
            is PasswordResetState.Error -> { Toast.makeText(context, state.message, Toast.LENGTH_LONG).show(); authViewModel.resetPasswordResetState() }
            else -> {}
        }
    }

    // UI Principal
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Fondo
        Image(
            painter = painterResource(id = R.drawable.fondo5), // Asegúrate de tener esta imagen
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Capa oscura para legibilidad
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))

        // 2. Contenido Scrolleable (Para teclados pequeños)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Espaciador Superior (Simula el Spacer(minLength: 220) de iOS)
            Spacer(modifier = Modifier.height(220.dp))

            // Título con Sombra
            Text(
                text = "Inicia Sesión",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    shadow = Shadow(color = Color.Black.copy(alpha = 0.8f), offset = Offset(0f, 4f), blurRadius = 8f)
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Campo Email
            // Campo Email
            GlassTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "Email",
                icon = androidx.compose.material.icons.Icons.Default.Email, // USAMOS VECTOR
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(16.dp))

// Campo Password
            GlassTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Contraseña",
                icon = androidx.compose.material.icons.Icons.Default.Lock, // USAMOS VECTOR
                keyboardType = KeyboardType.Password,
                isPassword = true
            )

            // Error Message (Si existe)
            if (authState is AuthState.Error) {
                Text(
                    text = (authState as AuthState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón Entrar
            Button(
                onClick = { authViewModel.loginUser(email, password) },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OrangePrimary,
                    disabledContainerColor = OrangePrimary.copy(alpha = 0.5f)
                ),
                enabled = email.isNotBlank() && password.isNotBlank() && authState !is AuthState.Loading
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = TextWhite)
                } else {
                    Text("Entrar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Links Inferiores
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    color = TextWhite.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { showResetDialog = true }
                )

                Text(
                    text = "Regístrate",
                    color = OrangePrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }

            Spacer(modifier = Modifier.height(40.dp)) // Espacio final
        }
    }

    // Dialog Reset Password
    if (showResetDialog) {
        PasswordResetDialog(
            onDismiss = { showResetDialog = false },
            onSendEmail = { authViewModel.sendPasswordResetEmail(it) },
            isLoading = passwordResetState is PasswordResetState.Loading
        )
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    // CAMBIO: Aceptamos ImageVector
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceDark.copy(alpha = 0.6f))
            .height(56.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TextWhite.copy(alpha = 0.5f)) },
            textStyle = TextStyle(color = TextWhite, fontSize = 16.sp),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                cursorColor = OrangePrimary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            singleLine = true,
            // CAMBIO: Usamos leadingIcon con el vector
            leadingIcon = if (icon != null) {
                { Icon(imageVector = icon, contentDescription = null, tint = TextWhite.copy(alpha = 0.7f)) }
            } else null
        )
    }
}

@Composable
fun PasswordResetDialog(
    onDismiss: () -> Unit,
    onSendEmail: (String) -> Unit,
    isLoading: Boolean
) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("Restablecer Contraseña", color = TextWhite) },
        text = {
            Column {
                Text("Introduce tu email:", color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                GlassTextField(value = email, onValueChange = { email = it }, placeholder = "Email", keyboardType = KeyboardType.Email)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSendEmail(email) },
                colors = ButtonDefaults.buttonColors(containerColor = OrangePrimary),
                enabled = email.isNotBlank() && !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TextWhite) else Text("Enviar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextSecondary) }
        }
    )
}