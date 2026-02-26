package com.aquiles.crosschapp.presentation.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.aquiles.crosschapp.presentation.viewmodel.NoticeViewModel
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import com.aquiles.crosschapp.presentation.viewmodel.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.45f)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
private val ColorInputBackground = Color(0xFF2C2C2E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateNoticeScreen(
    navController: NavHostController,
    noticeViewModel: NoticeViewModel
) {
    val gymPrimaryColor = try { 
        Color(android.graphics.Color.parseColor(UserSession.currentGym.value?.primaryColor ?: "#FC5200")) 
    } catch (e: Exception) { 
        Color(0xFFFC5200) 
    }
    val ColorPrimaryAction = gymPrimaryColor

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedImageUri = uri }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Publicar Novedad", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ColorTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f))
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Form Card (iOS Style - Simple)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = ColorGlassSurface),
                    border = BorderStroke(1.dp, ColorBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Text(
                            "NUEVA NOVEDAD",
                            style = MaterialTheme.typography.labelMedium,
                            color = ColorPrimaryAction,
                            fontWeight = FontWeight.Bold
                        )

                        // Image Picker (OBLIGATORIO como iOS)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(ColorInputBackground)
                                .clickable { imagePickerLauncher.launch("image/*") }
                                .border(
                                    width = 2.dp,
                                    color = if (selectedImageUri != null) ColorPrimaryAction else ColorBorder,
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedImageUri != null) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Imagen seleccionada",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                // Remove Button
                                IconButton(
                                    onClick = { selectedImageUri = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(0.7f), androidx.compose.foundation.shape.CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Color.White)
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.AddPhotoAlternate,
                                        null,
                                        tint = ColorTextSecondary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "Toca para agregar imagen",
                                        color = ColorTextSecondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        "(Obligatorio)",
                                        color = ColorPrimaryAction,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Title Input (Opcional como iOS)
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = {
                                Text(
                                    "Título o descripción corta (Opcional)",
                                    color = ColorTextSecondary.copy(0.5f)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ColorTextPrimary,
                                unfocusedTextColor = ColorTextPrimary,
                                cursorColor = ColorPrimaryAction,
                                focusedBorderColor = ColorPrimaryAction,
                                unfocusedBorderColor = ColorBorder,
                                focusedContainerColor = ColorInputBackground,
                                unfocusedContainerColor = ColorInputBackground
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Publish Button
                        Button(
                            onClick = {
                                if (selectedImageUri == null) {
                                    Toast.makeText(context, "Selecciona una imagen", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isUploading = true
                                
                                // Upload image and create notice
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        val user = UserSession.currentUser.value
                                        if (user == null) {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "Usuario no encontrado", Toast.LENGTH_SHORT).show()
                                                isUploading = false
                                            }
                                            return@launch
                                        }

                                        // Upload to Firebase Storage
                                        val storage = FirebaseStorage.getInstance()
                                        val filename = java.util.UUID.randomUUID().toString()
                                        val ref = storage.reference.child("gyms/${user.gym_id}/news/$filename.jpg")
                                        
                                        ref.putFile(selectedImageUri!!).await()
                                        val downloadUrl = ref.downloadUrl.await().toString()



                                        // Create notice with uploaded URL (Suspend function, waits for completion)
                                        noticeViewModel.createNotice(
                                            title = if (title.isBlank()) null else title,
                                            imageUrl = downloadUrl
                                        )

                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "¡Publicado!", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                            isUploading = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedImageUri != null) ColorPrimaryAction else Color.Gray
                            ),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isUploading && selectedImageUri != null
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(Icons.Default.Check, null)
                                Spacer(Modifier.width(8.dp))
                                Text("PUBLICAR AVISO", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Info Text
                Text(
                    "Estilo iOS: Simple y rápido. Solo imagen + título opcional.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ColorTextSecondary.copy(0.6f),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}
