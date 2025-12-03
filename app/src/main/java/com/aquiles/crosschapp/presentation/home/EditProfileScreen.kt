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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.aquiles.crosschapp.R
import com.aquiles.crosschapp.presentation.viewmodel.ProfileState
import com.aquiles.crosschapp.presentation.viewmodel.ProfileUpdateState
import com.aquiles.crosschapp.presentation.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// --- CONSTANTES DE DISEÑO MEJORADAS ---
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.15f) // Borde un poco más visible
// Fondo Glass más oscuro para mejor lectura sobre la imagen de fondo
private val ColorGlassSurface = Color(0xFF151517).copy(alpha = 0.90f)
private val ColorDestructive = Color(0xFFFF3B30)

// TEXTO LEGAL
private const val TERMINOS_LEGALES_TEXTO = """
DECLARACIÓN DE ASUNCIÓN DE RIESGOS Y RENUNCIA DE RESPONSABILIDAD:

1. VOLUNTARIEDAD: Declaro que participo en las actividades de entrenamiento físico de forma totalmente voluntaria y bajo mi propia responsabilidad.

2. RIESGOS: Entiendo que la actividad física conlleva riesgos significativos. ASUMO TOTALMENTE ESTOS RIESGOS.

3. ESTADO DE SALUD: Certifico que estoy en condiciones físicas y mentales óptimas.

4. RENUNCIA: Por la presente, LIBERO Y EXIMO DE TODA RESPONSABILIDAD a este centro de entrenamiento.

Al marcar "Acepto", firmo digitalmente este documento.
"""

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    innerPadding: PaddingValues,
    profileViewModel: ProfileViewModel,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val profileState by profileViewModel.userState.collectAsState()
    val updateState by profileViewModel.profileUpdateState.collectAsState()
    val context = LocalContext.current

    var showDeleteDialog by remember { mutableStateOf(false) }

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

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("¿Eliminar cuenta permanentemente?", color = ColorTextPrimary) },
            text = { Text("Esta acción no se puede deshacer.", color = ColorTextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        profileViewModel.deleteAccount()
                        onLogout()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ColorDestructive)
                ) { Text("Sí, eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar", color = ColorTextSecondary) }
            },
            containerColor = ColorGlassSurface,
            textContentColor = ColorTextSecondary,
            titleContentColor = ColorTextPrimary
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f))) {
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

                    // --- ESTADOS LOCALES ---
                    var name by remember(user.id) { mutableStateOf(user.name) }
                    var lastName by remember(user.id) { mutableStateOf(user.lastName) }
                    var phoneNumber by remember(user.id) { mutableStateOf(user.phoneNumber) }
                    var emergencyContact by remember(user.id) { mutableStateOf(user.emergencyContact ?: "") }
                    var birthDate by remember(user.id) { mutableStateOf(user.birthDate) }

                    // Médicos
                    var hasHeartCondition by remember(user.id) { mutableStateOf(user.hasHeartCondition ?: false) }
                    var hasInjuries by remember(user.id) { mutableStateOf(user.hasInjuries ?: false) }
                    var medicalNotes by remember(user.id) { mutableStateOf(user.medicalNotes ?: "") }
                    var waiverAccepted by remember(user.id) { mutableStateOf(user.waiverAccepted ?: false) }

                    val isWaiverActive = remember(user) { profileViewModel.isWaiverActive(user) }

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
                            .padding(horizontal = 24.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Avatar
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SubcomposeAsyncImage(
                                model = user.profileImageUrl?.ifBlank { R.drawable.ic_launcher_foreground } ?: R.drawable.ic_launcher_foreground,
                                contentDescription = "Foto",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, ColorPrimaryAction, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "La foto se cambia desde el Perfil principal",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorTextSecondary.copy(alpha = 0.5f)
                            )
                        }

                        // --- INPUTS MEJORADOS (Fondo Oscuro) ---
                        ProfileInputTextField(value = name, onValueChange = { name = it }, label = "Nombre")
                        ProfileInputTextField(value = lastName, onValueChange = { lastName = it }, label = "Apellido")
                        ProfileInputTextField(value = phoneNumber ?: "", onValueChange = { phoneNumber = it }, label = "Teléfono", keyboardType = KeyboardType.Phone)
                        ProfileInputTextField(value = emergencyContact, onValueChange = { emergencyContact = it }, label = "Contacto Emergencia", keyboardType = KeyboardType.Phone)

                        // DatePicker
                        Box(modifier = Modifier.clickable { showDatePicker = true }) {
                            ProfileInputTextField(
                                value = birthDateString,
                                onValueChange = {},
                                label = "Fecha de Nacimiento",
                                readOnly = true,
                                trailingIcon = { Icon(Icons.Default.CalendarMonth, null, tint = ColorPrimaryAction) }
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // --- FICHA MÉDICA ---
                        ProfileMedicalSection(
                            isActive = isWaiverActive,
                            waiverDate = user.waiverDate,
                            hasHeartCondition = hasHeartCondition,
                            onHeartChange = { hasHeartCondition = it },
                            hasInjuries = hasInjuries,
                            onInjuriesChange = { hasInjuries = it },
                            medicalNotes = medicalNotes,
                            onNotesChange = { medicalNotes = it },
                            waiverAccepted = waiverAccepted,
                            onWaiverChange = { waiverAccepted = it }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        val hasChanges = name != user.name || lastName != user.lastName || phoneNumber != user.phoneNumber || emergencyContact != (user.emergencyContact ?: "") || birthDate != user.birthDate ||
                                hasHeartCondition != (user.hasHeartCondition ?: false) || hasInjuries != (user.hasInjuries ?: false) || medicalNotes != (user.medicalNotes ?: "") || waiverAccepted != (user.waiverAccepted ?: false)

                        // BOTÓN GUARDAR
                        Button(
                            onClick = {
                                profileViewModel.updateUserProfile(
                                    name, lastName, phoneNumber, emergencyContact, birthDate,
                                    hasHeartCondition, hasInjuries, medicalNotes, waiverAccepted
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .shadow(10.dp, RoundedCornerShape(12.dp), spotColor = ColorPrimaryAction),
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
                                Text("Guardar Cambios", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        TextButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Eliminar mi cuenta", color = ColorDestructive)
                        }
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }
    }
}

// --- COMPONENTES PRIVADOS ---

@Composable
private fun ProfileInputTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    // CAMBIO IMPORTANTE: Usamos un TextField con colores sólidos semitransparentes
    // para que destaque sobre el fondo, en lugar de solo un borde.
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, ColorBorder), RoundedCornerShape(12.dp)),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        readOnly = readOnly,
        trailingIcon = trailingIcon,
        colors = TextFieldDefaults.colors(
            // Fondo oscuro semitransparente (Glass)
            focusedContainerColor = ColorGlassSurface,
            unfocusedContainerColor = ColorGlassSurface,
            disabledContainerColor = ColorGlassSurface,

            focusedTextColor = ColorTextPrimary,
            unfocusedTextColor = ColorTextPrimary,
            disabledTextColor = ColorTextSecondary,

            focusedLabelColor = ColorPrimaryAction,
            unfocusedLabelColor = ColorTextSecondary,
            disabledLabelColor = ColorTextSecondary.copy(alpha = 0.5f),

            cursorColor = ColorPrimaryAction,
            focusedIndicatorColor = Color.Transparent, // Sin línea inferior
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
private fun ProfileMedicalSection(
    isActive: Boolean,
    waiverDate: java.util.Date?,
    hasHeartCondition: Boolean,
    onHeartChange: (Boolean) -> Unit,
    hasInjuries: Boolean,
    onInjuriesChange: (Boolean) -> Unit,
    medicalNotes: String,
    onNotesChange: (String) -> Unit,
    waiverAccepted: Boolean,
    onWaiverChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface),
        border = BorderStroke(1.dp, ColorBorder),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MedicalServices, contentDescription = null, tint = ColorPrimaryAction)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Situación Médica y Legal", fontWeight = FontWeight.Bold, color = ColorTextPrimary, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (isActive) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Declaración Jurada Vigente", color = Color.Green, fontWeight = FontWeight.Bold)
                }

                // ... (Resto de la lógica de visualización igual, pero con mejores colores)

                val dateStr = waiverDate?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) } ?: "-"
                Text("Firmado el: $dateStr", style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = ColorBorder)
                Spacer(modifier = Modifier.height(12.dp))

                ProfileReadOnlyRow("Cardíaco:", hasHeartCondition, Color(0xFFFF3B30))
                ProfileReadOnlyRow("Lesiones:", hasInjuries, Color(0xFFFFA500))

                if (medicalNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Notas:", color = ColorTextSecondary, style = MaterialTheme.typography.bodySmall)
                    Text(medicalNotes, color = ColorTextPrimary, fontStyle = FontStyle.Italic)
                }
                Text(
                    "🔒 Documento bloqueado por seguridad.",
                    color = ColorTextSecondary.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 12.dp)
                )

            } else {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onHeartChange(!hasHeartCondition) }) {
                    Checkbox(checked = hasHeartCondition, onCheckedChange = onHeartChange, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF3B30), uncheckedColor = ColorTextSecondary))
                    Text("¿Afecciones Cardíacas?", color = ColorTextPrimary)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onInjuriesChange(!hasInjuries) }) {
                    Checkbox(checked = hasInjuries, onCheckedChange = onInjuriesChange, colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFFA500), uncheckedColor = ColorTextSecondary))
                    Text("¿Lesiones Recientes?", color = ColorTextPrimary)
                }

                Spacer(modifier = Modifier.height(8.dp))

                ProfileInputTextField(value = medicalNotes, onValueChange = onNotesChange, label = "Detalles médicos (opcional)")

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .height(140.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, ColorBorder), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = TERMINOS_LEGALES_TEXTO,
                        color = ColorTextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onWaiverChange(!waiverAccepted) }) {
                    Checkbox(checked = waiverAccepted, onCheckedChange = onWaiverChange, colors = CheckboxDefaults.colors(checkedColor = ColorPrimaryAction, uncheckedColor = ColorTextSecondary))
                    Text("ACEPTO y Firmo digitalmente", fontWeight = FontWeight.Bold, color = ColorTextPrimary)
                }
            }
        }
    }
}

@Composable
private fun ProfileReadOnlyRow(label: String, value: Boolean, alertColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = ColorTextSecondary)
        Text(
            if (value) "SÍ" else "NO",
            color = if (value) alertColor else Color.Green,
            fontWeight = FontWeight.Bold
        )
    }
}