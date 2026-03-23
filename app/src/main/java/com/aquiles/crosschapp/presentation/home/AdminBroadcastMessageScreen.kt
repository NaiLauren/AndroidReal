package com.aquiles.crosschapp.presentation.home

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import com.aquiles.crosschapp.presentation.viewmodel.SendMessageState
import kotlinx.coroutines.delay
import com.aquiles.crosschapp.presentation.components.GlassCard
import kotlinx.coroutines.launch

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.65f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
private val ColorBackgroundGradientStart = Color(0xFF000000)
private val ColorBackgroundGradientEnd = Color(0xFF121212)
private val ColorError = Color(0xFFEF5350)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBroadcastMessageScreen(
    navController: NavController,
    userIds: List<String>,
    adminViewModel: AdminViewModel = viewModel()
) {
    var messageContent by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    val sendMessageState by adminViewModel.sendMessageState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> selectedFileUri = uri }
    )

    LaunchedEffect(sendMessageState) {
        when (val state = sendMessageState) {
            is SendMessageState.Success -> {
                scope.launch {
                    snackbarHostState.showSnackbar("¡Difusión enviada con éxito!")
                    delay(500)
                    navController.popBackStack("admin_manage_users", inclusive = false)
                }
            }
            is SendMessageState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar("Error: ${state.message}")
                    adminViewModel.resetSendMessageState()
                }
            }
            else -> {}
        }
    }

    // --- UI STRUCTURE ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            // .background(Color.Black.copy(alpha = 0.4f)) // Removed for glass background
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Nueva Difusión", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = ColorTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f))
                )
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Enviando a ${userIds.size} alumno(s)",
                    style = MaterialTheme.typography.titleMedium,
                    color = ColorPrimaryAction,
                    fontWeight = FontWeight.Bold
                )

                // Área de Texto Glass
                OutlinedTextField(
                    value = messageContent,
                    onValueChange = { messageContent = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = { Text("Mensaje para todos...", color = ColorTextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorPrimaryAction,
                        unfocusedBorderColor = ColorBorder,
                        focusedTextColor = ColorTextPrimary,
                        unfocusedTextColor = ColorTextPrimary,
                        cursorColor = ColorPrimaryAction,
                        focusedContainerColor = ColorGlassSurface,
                        unfocusedContainerColor = ColorGlassSurface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                if (selectedFileUri != null) {
                    GlassFileChipBroadcast(
                        uri = selectedFileUri!!,
                        context = context,
                        onClear = { selectedFileUri = null }
                    )
                }

                // Botones
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = sendMessageState !is SendMessageState.Loading,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ColorTextPrimary),
                        border = BorderStroke(1.dp, ColorBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Adjuntar Archivo")
                    }

                    Button(
                        onClick = {
                            adminViewModel.sendBroadcastMessage(userIds, messageContent, selectedFileUri, context)
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        enabled = sendMessageState !is SendMessageState.Loading && (messageContent.isNotBlank() || selectedFileUri != null),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ENVIAR DIFUSIÓN", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Overlay de Carga
        if (sendMessageState is SendMessageState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable(enabled = false, onClick = {}),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = ColorPrimaryAction)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Enviando masivamente...",
                        color = ColorTextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassFileChipBroadcast(uri: Uri, context: Context, onClear: () -> Unit) {
    val fileName = getFileNameBroadcast(context, uri)
    val isPdf = fileName.endsWith(".pdf", ignoreCase = true)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPdf) Icons.Default.PictureAsPdf else Icons.Default.Image,
                    contentDescription = null,
                    tint = ColorTextPrimary
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text("Archivo Adjunto", style = MaterialTheme.typography.labelSmall, color = ColorTextSecondary)
                Text(fileName, style = MaterialTheme.typography.bodyMedium, color = ColorTextPrimary, maxLines = 1)
            }

            IconButton(onClick = onClear) {
                Icon(Icons.Default.Close, contentDescription = "Quitar", tint = ColorError)
            }
        }
    }
}

private fun getFileNameBroadcast(context: Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (columnIndex != -1) {
                    result = cursor.getString(columnIndex)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != -1) {
            result = result?.substring(cut!! + 1)
        }
    }
    return result ?: "Archivo desconocido"
}