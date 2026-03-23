package com.aquiles.crosschapp.presentation.auth

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import com.aquiles.crosschapp.R
import com.aquiles.crosschapp.presentation.viewmodel.AuthViewModel
import com.aquiles.crosschapp.presentation.viewmodel.AuthState
import com.aquiles.crosschapp.ui.theme.CrossChAppTheme

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f) // Borde muy sutil
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.65f)
private val ColorInputBackground = Color.Black.copy(alpha = 0.3f) // Fondo de los inputs (dentro de la tarjeta)

@Composable
fun RegisterScreen(
    gymId: String?,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Detección de Modo: Completar Perfil (Social Login)
    val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    val isSocialRegistration = firebaseUser != null

    LaunchedEffect(gymId) {
        if (!gymId.isNullOrBlank()) {
            authViewModel.loadGymDetails(gymId)
        }
    }
    
    // Efecto para pre-llenar datos si venimos de Google
    
    val selectedGym by authViewModel.selectedGym.collectAsState()

    // Form States
    var name by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Not Specified") } // [Fix] Gender selection

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var passwordsDoNotMatch by remember { mutableStateOf(false) }

    // Efecto para pre-llenar datos si venimos de Google
    // Efecto para pre-llenar datos si venimos de Google
    LaunchedEffect(firebaseUser) {
        if (firebaseUser != null) {
            val splitName = firebaseUser.displayName?.split(" ")
            if (name.isBlank()) name = splitName?.firstOrNull() ?: ""
            // Intento básico de apellido
            if (lastName.isBlank() && (splitName?.size ?: 0) > 1) lastName = splitName?.lastOrNull() ?: ""
            if (email.isBlank()) email = firebaseUser.email ?: ""
        }
    }

    // Handlers
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Success -> {
                Toast.makeText(context, "¡Bienvenido!", Toast.LENGTH_SHORT).show()
                onRegisterSuccess()
                authViewModel.resetAuthState()
            }
            is AuthState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                authViewModel.resetAuthState()
            }
            else -> {}
        }
    }

    // --- UI STRUCTURE ---
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Fondo e Imagen
        Image(
            painter = painterResource(id = R.drawable.fondo5),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Gradiente oscuro
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.9f))))
        )

        // 2. Contenido Scrolleable
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(50.dp)) // Espacio superior

            // Tarjeta Glass Contenedora (Formulario)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = ColorGlassSurface),
                border = BorderStroke(1.dp, ColorBorder)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Logo del Gimnasio
                    if (!selectedGym?.logoUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = selectedGym!!.logoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(2.dp, ColorPrimaryAction, CircleShape),
                            loading = { CircularProgressIndicator(modifier = Modifier.padding(20.dp), color = ColorPrimaryAction) }
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isSocialRegistration) "Completa tu perfil" else "Únete a",
                            style = MaterialTheme.typography.labelMedium,
                            color = ColorTextSecondary
                        )
                        Text(
                            text = selectedGym?.name ?: "TribeOnMove",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = ColorTextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // --- INPUTS ---
                    // Usamos el nuevo estilo "Filled Glass" para mejor contraste dentro de la tarjeta
                    GlassRegisterTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Nombre",
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )

                    GlassRegisterTextField(
                        value = lastName,
                        onValueChange = { lastName = it },
                        label = "Apellido",
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )

                    GlassRegisterTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = "Celular",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    )

                    // [Fix] Gender Selector
                    GenderSelector(selectedGender = gender, onGenderSelected = { gender = it })

                    // Email (Deshabilitado si es Social Login y ya tenemos el email)
                    GlassRegisterTextField(
                        value = email,
                        onValueChange = { if (!isSocialRegistration) email = it },
                        label = "Email",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = if (isSocialRegistration) ImeAction.Done else ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                    ) // TODO: Visualmente indicar si está readonly

                    if (!isSocialRegistration) {
                        GlassRegisterTextField(
                            value = password,
                            onValueChange = { password = it; passwordsDoNotMatch = false },
                            label = "Contraseña",
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = ColorTextSecondary)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )

                        GlassRegisterTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; passwordsDoNotMatch = false },
                            label = "Confirmar Contraseña",
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = ColorTextSecondary)
                                }
                            },
                            isError = passwordsDoNotMatch,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )

                        if (passwordsDoNotMatch) {
                            Text(
                                text = "Las contraseñas no coinciden.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // --- BOTÓN CON GLOW ---
                    Button(
                        onClick = {
                            if (gymId == null) {
                                Toast.makeText(context, "Error: ID de gimnasio no válido.", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            
                            if (isSocialRegistration) {
                                authViewModel.completeSocialLoginRegistration(name, lastName, phoneNumber, gymId, gender)
                            } else {
                                if (password == confirmPassword) {
                                    authViewModel.registerUser(email, password, name, lastName, phoneNumber, gymId, gender)
                                } else {
                                    passwordsDoNotMatch = true
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .shadow(10.dp, RoundedCornerShape(12.dp), spotColor = ColorPrimaryAction), // Glow
                        enabled = name.isNotBlank() && lastName.isNotBlank() && email.isNotBlank() && (isSocialRegistration || (password.length >= 6 && confirmPassword.isNotBlank())) && authState !is AuthState.Loading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorPrimaryAction,
                            disabledContainerColor = ColorPrimaryAction.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (authState is AuthState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text(if (isSocialRegistration) "FINALIZAR REGISTRO" else "REGISTRARSE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    TextButton(
                        onClick = onNavigateToLogin,
                        enabled = authState !is AuthState.Loading
                    ) {
                        Text("¿Ya tienes cuenta? Inicia Sesión", color = ColorTextSecondary)
                    }
                }
            }

            Spacer(Modifier.height(50.dp)) // Espacio inferior generoso
        }
    }
}

// --- INPUT MEJORADO (Estilo Login) ---
@Composable
fun GlassRegisterTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false
) {
    // Usamos TextField estándar en lugar de Outlined para que se vea como un bloque sólido
    // Esto es más consistente con el "Box" style del LoginScreen
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, if (isError) MaterialTheme.colorScheme.error else ColorBorder),
                RoundedCornerShape(12.dp)
            ),
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = ColorInputBackground, // Fondo oscuro semitransparente
            unfocusedContainerColor = ColorInputBackground,
            disabledContainerColor = ColorInputBackground,
            focusedTextColor = ColorTextPrimary,
            unfocusedTextColor = ColorTextPrimary,
            focusedLabelColor = ColorPrimaryAction, // Label Naranja al enfocar
            unfocusedLabelColor = ColorTextSecondary,
            cursorColor = ColorPrimaryAction,
            focusedIndicatorColor = Color.Transparent, // Sin linea inferior
            unfocusedIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
            errorLabelColor = MaterialTheme.colorScheme.error
        ),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        isError = isError
    )
}

@Composable
fun GenderSelector(selectedGender: String, onGenderSelected: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Género",
            style = MaterialTheme.typography.labelMedium,
            color = ColorTextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("male" to "Masculino", "female" to "Femenino", "other" to "Otro").forEach { (value, label) ->
                val isSelected = selectedGender == value || (selectedGender == "Not Specified" && value == "male") // Default to male if not specified
                OutlinedButton(
                    onClick = { onGenderSelected(value) },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, if (isSelected) ColorPrimaryAction else ColorBorder),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isSelected) ColorPrimaryAction.copy(alpha = 0.2f) else Color.Transparent,
                        contentColor = if (isSelected) ColorPrimaryAction else ColorTextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(label, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RegisterScreenPreview() {
    CrossChAppTheme {
        RegisterScreen(gymId = "test_id", onRegisterSuccess = {}, onNavigateToLogin = {})
    }
}