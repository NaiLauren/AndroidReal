package com.aquiles.crosschapp.presentation.home

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.aquiles.crosschapp.data.model.ChallengeResult
import com.aquiles.crosschapp.data.service.VideoUploadService
import com.aquiles.crosschapp.presentation.components.GlassCard
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import com.aquiles.crosschapp.presentation.viewmodel.UserSession
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitChallengeResultScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    challengeId: String,
    challengeName: String,
    adminViewModel: AdminViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var score by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isRx by remember { mutableStateOf(false) }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadedVideoUrl by remember { mutableStateOf<String?>(null) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileSize = getFileSize(context, uri)
            val fileName = getFileName(context, uri) ?: "video"
            val (isValid, errorMsg) = VideoUploadService.validateVideoFile(fileSize, fileName)

            if (isValid) {
                selectedVideoUri = uri
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar(errorMsg ?: "Video inválido")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.height(72.dp),
                title = {
                    Column {
                        Text(
                            "Enviar Resultado",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                        Text(
                            challengeName,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Score Input
            OutlinedTextField(
                value = score,
                onValueChange = { score = it },
                label = { Text("Tu Resultado") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedBorderColor = Color(0xFFFC5200)
                )
            )

            // Rx Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0xFF1C1C1E),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("¿Hiciste el Rx?", color = Color.White)
                Switch(
                    checked = isRx,
                    onCheckedChange = { isRx = it },
                    modifier = Modifier.scale(0.8f)
                )
            }

            // Notes Input
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas (opcional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedBorderColor = Color(0xFFFC5200)
                )
            )

            Divider(color = Color.White.copy(alpha = 0.1f))

            // Video Upload Section
            Text(
                "Prueba en Video (opcional pero recomendado)",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )

            if (selectedVideoUri == null && uploadedVideoUrl == null) {
                Button(
                    onClick = { videoPickerLauncher.launch("video/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFC5200)
                    )
                ) {
                    Icon(
                        Icons.Default.CloudUpload,
                        "Subir",
                        modifier = Modifier.size(20.dp),
                        tint = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Seleccionar Video", color = Color.White)
                }
            } else {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.PlayCircle,
                                "Video",
                                modifier = Modifier.size(32.dp),
                                tint = Color(0xFFFC5200)
                            )
                            Column {
                                Text(
                                    if (uploadedVideoUrl != null) "Video Subido ✅" else "Video Seleccionado",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 12.sp
                                )
                                Text(
                                    getFileName(context, selectedVideoUri) ?: "video.mp4",
                                    color = Color.White.copy(alpha = 0.6f),
                                    fontSize = 10.sp
                                )
                            }
                        }

                        if (isUploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color(0xFFFC5200)
                            )
                        } else {
                            IconButton(onClick = {
                                selectedVideoUri = null
                                uploadedVideoUrl = null
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    "Remover",
                                    tint = Color.White.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            // Submit Button
            Button(
                onClick = {
                    scope.launch {
                        if (score.isEmpty()) {
                            snackbarHostState.showSnackbar("Ingresa tu resultado")
                            return@launch
                        }

                        // Upload video if selected
                        if (selectedVideoUri != null && uploadedVideoUrl == null) {
                            isUploading = true
                            val userId = UserSession.currentUser.value?.id ?: ""
                            val result = VideoUploadService.uploadChallengeVideo(
                                selectedVideoUri!!,
                                challengeId,
                                userId
                            )
                            isUploading = false

                            when (result) {
                                is VideoUploadService.UploadResult.Success -> {
                                    uploadedVideoUrl = result.videoUrl
                                    snackbarHostState.showSnackbar("Video subido exitosamente")
                                }
                                is VideoUploadService.UploadResult.Error -> {
                                    snackbarHostState.showSnackbar(
                                        "Error al subir video: ${result.exception.message}"
                                    )
                                    return@launch
                                }
                                else -> {}
                            }
                        }

                        // Submit the result
                        val numericScore = score.toDoubleOrNull() ?: 0.0
                        adminViewModel.submitChallengeResult(
                            challengeId = challengeId,
                            score = score,
                            numericScore = numericScore,
                            isRx = isRx,
                            notes = notes,
                            videoUrl = uploadedVideoUrl
                        )

                        snackbarHostState.showSnackbar("Resultado enviado para validación")
                        navController.popBackStack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFC5200)
                ),
                enabled = !isUploading
            ) {
                Text("Enviar Resultado", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// Helper functions
fun getFileSize(context: Context, uri: Uri): Long {
    return try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
            it.moveToFirst()
            it.getLong(sizeIndex)
        } ?: 0
    } catch (e: Exception) {
        0
    }
}

fun getFileName(context: Context, uri: Uri?): String? {
    if (uri == null) return null
    return try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            it.moveToFirst()
            it.getString(nameIndex)
        }
    } catch (e: Exception) {
        null
    }
}
