package com.aquiles.crosschapp.presentation.auth

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aquiles.crosschapp.R
import com.aquiles.crosschapp.presentation.viewmodel.AuthViewModel
import com.aquiles.crosschapp.presentation.viewmodel.AuthState
import com.aquiles.crosschapp.presentation.viewmodel.PasswordResetState

// --- CONSTANTES DE DISEÑO (Igual que en Perfil) ---
private val ColorPrimaryAction = Color(0xFFFC5200) // Naranja
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.75f) // Negro Transparente
private val ColorTextWhite = Color(0xFFFFFFFF)
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.2f) // Borde fino

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

    // Manejo de efectos (Toast, navegación)
    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            onLoginSuccess()
            authViewModel.resetAuthState()
        }
    }
    LaunchedEffect(passwordResetState) {
        when (val state = passwordResetState) {
            is PasswordResetState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                showResetDialog = false
                authViewModel.resetPasswordResetState()
            }
            is PasswordResetState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                authViewModel.resetPasswordResetState()
            }
            else -> {}
        }
    }

    // UI Principal
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Imagen de Fondo
        Image(
            painter = painterResource(id = R.drawable.fondo5), // Asegúrate de usar el mismo fondo que en iOS
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Capa oscura para legibilidad
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))

        // 2. Contenido Scrolleable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Espaciador Superior
            Spacer(modifier = Modifier.height(180.dp))

            // --- TÍTULO EN TARJETA CRISTALINA (NUEVO) ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorGlassSurface, RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.dp, ColorBorder), RoundedCornerShape(16.dp))
                    .padding(vertical = 20.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "¡HOLA DE NUEVO!",
                        style = MaterialTheme.typography.labelMedium,
                        color = ColorPrimaryAction,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "Inicia Sesión",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = ColorTextWhite
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(25.dp))

            // (Botón Google movido abajo)


            Spacer(modifier = Modifier.height(20.dp))

            // --- CAMPO EMAIL (Icono Naranja + Borde) ---
            GlassTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "Email",
                icon = androidx.compose.material.icons.Icons.Default.Email,
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- CAMPO PASSWORD (Icono Naranja + Borde) ---
            GlassTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Contraseña",
                icon = androidx.compose.material.icons.Icons.Default.Lock,
                keyboardType = KeyboardType.Password,
                isPassword = true
            )

            // Mensaje de Error
            if (authState is AuthState.Error) {
                Surface(
                    color = ColorGlassSurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Default.Warning, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = (authState as AuthState.Error).message,
                            color = Color.Red,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- BOTÓN ENTRAR (Con Sombra/Glow) ---
            Button(
                onClick = { authViewModel.loginUser(email, password) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = RoundedCornerShape(12.dp),
                        spotColor = ColorPrimaryAction, // Color de la sombra (Android P+)
                        ambientColor = ColorPrimaryAction
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorPrimaryAction,
                    disabledContainerColor = ColorPrimaryAction.copy(alpha = 0.5f)
                ),
                enabled = email.isNotBlank() && password.isNotBlank() && authState !is AuthState.Loading
            ) {
                if (authState is AuthState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = ColorTextWhite)
                } else {
                    Text("Entrar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(25.dp))

            // Separador "O"
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = ColorBorder)
                Text(" O ", color = ColorTextSecondary, modifier = Modifier.padding(horizontal = 8.dp))
                HorizontalDivider(modifier = Modifier.weight(1f), color = ColorBorder)
            }

            Spacer(modifier = Modifier.height(25.dp))

            // --- BOTÓN GOOGLE (REUBICADO) ---
            val googleLauncher = rememberLauncherForActivityResult(
                contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
            ) { result ->
                val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                    account.idToken?.let { token ->
                        authViewModel.signInWithGoogle(token)
                    }
                } catch (e: com.google.android.gms.common.api.ApiException) {
                    Toast.makeText(context, "Error Google Sign In: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            // Estilo más sutil para Google: Outlined Button o Glass
            Button(
                onClick = {
                    val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(context.getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                    val googleSignInClient = com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
                    googleLauncher.launch(googleSignInClient.signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(BorderStroke(1.dp, Color.White.copy(alpha=0.5f)), RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), // Glass Style
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text("Continuar con Google", color = Color.White, fontWeight = FontWeight.Medium)
                }
            }

            // --- LINKS INFERIORES ---
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                // Link Olvidé Contraseña
                Text(
                    text = "¿Olvidaste tu contraseña?",
                    color = ColorTextWhite.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable { showResetDialog = true }
                )

                // Link Registro (Texto Mixto: Blanco + Naranja)
                val annotatedString = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = ColorTextSecondary)) {
                        append("¿No tienes cuenta? ")
                    }
                    withStyle(style = SpanStyle(color = ColorPrimaryAction, fontWeight = FontWeight.Bold)) {
                        append("Regístrate")
                    }
                }

                Text(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable { onNavigateToRegister() }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
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

    // --- MANEJO DE REGISTRO INCOMPLETO (Google Sign In nuevo) ---
    // Usamos un LaunchedEffect extra para navegar si se detecta que faltan datos
    // Nota: La navegación se delega al callback `onNavigateToRegister`.
    // Pero necesitamos pasar datos. En Compose Navigation básico esto es complejo.
    // Asumiremos que `onNavigateToRegister` no acepta argumentos en la firma actual, 
    // pero podemos guardar el estado en el ViewModel o cambiar la firma.
    // PRAGMATISMO: El AuthViewModel ya tiene el estado. RegisterScreen debería leer de ahí.
    
    // Aquí solo necesitamos "reaccionar" si pasa a NeedsProfileCompletion, pero la RegisterScreen 
    // necesitará los datos. Si RegisterScreen lee 'currentUser' de authViewModel, ya tenemos medio camino.
    
    // Si la pantalla de Login no navega sola, debemos instruirla. 
    // Como la firma de onNavigateToRegister es () -> Unit, asumimos que RegisterScreen leerá el estado.
    if (authState is AuthState.NeedsProfileCompletion) {
        LaunchedEffect(Unit) {
            onNavigateToRegister()
        }
    }
}

// --- COMPONENTE INPUT MEJORADO ---
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(ColorGlassSurface, RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, ColorBorder), RoundedCornerShape(12.dp)), // Borde Fino
        contentAlignment = Alignment.CenterStart
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = ColorTextSecondary) },
            textStyle = TextStyle(color = ColorTextWhite, fontSize = 16.sp),
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                cursorColor = ColorPrimaryAction,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
            singleLine = true,
            leadingIcon = if (icon != null) {
                {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = ColorPrimaryAction // Icono Naranja
                    )
                }
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
        containerColor = Color(0xFF2C2C2E), // Surface Dark sólido para el dialogo
        title = { Text("Restablecer Contraseña", color = ColorTextWhite) },
        text = {
            Column {
                Text("Introduce tu email:", color = ColorTextSecondary)
                Spacer(Modifier.height(8.dp))
                GlassTextField(value = email, onValueChange = { email = it }, placeholder = "Email", keyboardType = KeyboardType.Email)
            }
        },
        confirmButton = {
            Button(
                onClick = { onSendEmail(email) },
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction),
                enabled = email.isNotBlank() && !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ColorTextWhite) else Text("Enviar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = ColorTextSecondary) }
        }
    )
}