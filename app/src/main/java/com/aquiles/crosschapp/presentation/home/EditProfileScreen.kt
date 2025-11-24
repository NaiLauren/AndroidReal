package com.aquiles.crosschapp.presentation.home

import android.app.DatePickerDialog
import android.widget.DatePicker
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.aquiles.crosschapp.R
import com.aquiles.crosschapp.presentation.viewmodel.ProfileState
import com.aquiles.crosschapp.presentation.viewmodel.ProfileUpdateState
import com.aquiles.crosschapp.presentation.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
private val ColorBackgroundGradientStart = Color(0xFF000000)
private val ColorBackgroundGradientEnd = Color(0xFF121212)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    innerPadding: PaddingValues,
    profileViewModel: ProfileViewModel,
    onNavigateBack: () -> Unit
) {
    val profileState by profileViewModel.userState.collectAsState()
    val updateState by profileViewModel.profileUpdateState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(updateState) {
        if (updateState is ProfileUpdateState.Success) {
            Toast.makeText(context, "Perfil actualizado", Toast.LENGTH_SHORT).show()
            profileViewModel.resetProfileUpdateState()
            onNavigateBack()
        } else if (updateState is ProfileUpdateState.Error) {
            Toast.makeText(context, (updateState as ProfileUpdateState.Error).message, Toast.LENGTH_LONG).show()
            profileViewModel.resetProfileUpdateState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Editar Perfil", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
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

            when (val state = profileState) {
                ProfileState.Idle, is ProfileState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ColorPrimaryAction) }
                }
                is ProfileState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is ProfileState.Success -> {
                    val user = state.user
                    var name by remember(user.id) { mutableStateOf(user.name) }
                    var lastName by remember(user.id) { mutableStateOf(user.lastName) }
                    var phoneNumber by remember(user.id) { mutableStateOf(user.phoneNumber) }
                    var emergencyContact by remember(user.id) { mutableStateOf(user.emergencyContact ?: "") }
                    var birthDate by remember(user.id) { mutableStateOf(user.birthDate) }

                    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
                    val birthDateString = remember(birthDate) { birthDate?.let { dateFormat.format(it) } ?: "" }
                    var showDatePicker by remember { mutableStateOf(false) }

                    if (showDatePicker) {
                        val calendar = Calendar.getInstance()
                        birthDate?.let { calendar.time = it }

                        DatePickerDialog(
                            context,
                            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                                calendar.set(year, month, dayOfMonth)
                                birthDate = calendar.time
                                showDatePicker = false
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).apply {
                            setOnDismissListener { showDatePicker = false }
                            show()
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = localScaffoldPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
                            .padding(24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Avatar (Visual Only - edit happens in ProfileScreen)
                        Box(contentAlignment = Alignment.BottomEnd) {
                            SubcomposeAsyncImage(
                                model = user.profileImageUrl?.ifBlank { R.drawable.ic_launcher_foreground } ?: R.drawable.ic_launcher_foreground,
                                contentDescription = "Foto de perfil",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, ColorPrimaryAction, CircleShape),
                                contentScale = ContentScale.Crop,
                                error = {
                                    Box(Modifier.fillMaxSize().background(Color.Gray), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Edit, null, tint = Color.White)
                                    }
                                }
                            )
                        }

                        Text(
                            "La foto se cambia desde la pantalla principal de Perfil",
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorTextSecondary
                        )

                        // Inputs
                        GlassEditTextField(value = name, onValueChange = { name = it }, label = "Nombre")
                        GlassEditTextField(value = lastName, onValueChange = { lastName = it }, label = "Apellido")
                        GlassEditTextField(value = phoneNumber ?: "", onValueChange = { phoneNumber = it }, label = "Teléfono (WhatsApp)", keyboardType = KeyboardType.Phone)
                        GlassEditTextField(value = emergencyContact, onValueChange = { emergencyContact = it }, label = "Contacto de Emergencia", keyboardType = KeyboardType.Phone)

                        // Date Picker Field Custom
                        OutlinedTextField(
                            value = birthDateString,
                            onValueChange = { },
                            label = { Text("Fecha de Nacimiento", color = ColorTextSecondary) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDatePicker = true },
                            readOnly = true,
                            enabled = false, // Deshabilitar input directo, manejar click en modifier
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = ColorPrimaryAction)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = ColorTextPrimary,
                                disabledBorderColor = ColorBorder,
                                disabledLabelColor = ColorTextSecondary,
                                disabledTrailingIconColor = ColorPrimaryAction
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        val hasChanges = name != user.name || lastName != user.lastName || phoneNumber != user.phoneNumber || emergencyContact != (user.emergencyContact ?: "") || birthDate != user.birthDate

                        Button(
                            onClick = {
                                profileViewModel.updateUserProfile(
                                    name = name,
                                    lastName = lastName,
                                    phoneNumber = phoneNumber,
                                    emergencyContact = emergencyContact,
                                    birthDate = birthDate
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            enabled = updateState !is ProfileUpdateState.Loading && hasChanges,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorPrimaryAction,
                                disabledContainerColor = ColorPrimaryAction.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (updateState is ProfileUpdateState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            } else {
                                Text("Guardar Cambios", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GlassEditTextField(
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
        shape = RoundedCornerShape(12.dp)
    )
}