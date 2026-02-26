package com.aquiles.crosschapp.presentation.home

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
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
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.45f)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.15f)
private val ColorPrimaryAction = Color(0xFFFC5200)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminActivityImagesScreen(
    navController: NavController,
    adminViewModel: AdminViewModel
) {
    val context = LocalContext.current
    val activityImages by adminViewModel.activityImagesState.collectAsState()
    
    // Estado para el diálogo de añadir/editar
    var showDialog by remember { mutableStateOf(false) }
    var selectedDayName by remember { mutableStateOf("") }
    
    // Carga inicial
    LaunchedEffect(Unit) {
        adminViewModel.loadActivityImages()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Imágenes de Actividad", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ColorTextPrimary)
                        }
                    },
                    actions = {
                         IconButton(onClick = { 
                             selectedDayName = "" // Nuevo
                             showDialog = true 
                         }) {
                             Icon(Icons.Default.Add, "Añadir", tint = ColorTextPrimary)
                         }
                         IconButton(onClick = { adminViewModel.generateDayPlaceholders() }) {
                             Icon(Icons.Default.Add, "Generar Placeholders", tint = ColorPrimaryAction)
                         }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f))
                )
            },
            containerColor = Color.Transparent
        ) { localPadding ->
            
            if (activityImages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(localPadding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ColorPrimaryAction)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Cargando imágenes...", color = ColorTextSecondary)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 150.dp),
                    contentPadding = PaddingValues(
                        top = localPadding.calculateTopPadding() + 16.dp,
                        bottom = 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Ordenar por días de la semana (Lunes primero)
                    val daysOrder = listOf("LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES", "SÁBADO", "DOMINGO")
                    val sortedImages = activityImages.toList().sortedBy { daysOrder.indexOf(it.first).takeIf { idx -> idx >= 0 } ?: 999 }
                    
                    items(sortedImages) { (day, url) ->
                        ActivityImageCard(
                            dayName = day,
                            imageUrl = url,
                            onTap = { 
                                selectedDayName = day
                                showDialog = true 
                            },
                            onDelete = {
                                adminViewModel.deleteActivityImage(day)
                            }
                        )
                    }
                }
            }
        }

        if (showDialog) {
            GlassActivityImageDialog(
                initialName = selectedDayName,
                onDismiss = { showDialog = false },
                onSave = { name, uri ->
                    adminViewModel.saveActivityImage(name, uri, context) { success ->
                        if (success) {
                             Toast.makeText(context, "Imagen guardada exitosamente", Toast.LENGTH_SHORT).show()
                             showDialog = false
                        } else {
                             Toast.makeText(context, "Error al guardar imagen", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun ActivityImageCard(
    dayName: String,
    imageUrl: String,
    onTap: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable(onClick = onTap),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, ColorBorder),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageUrl == "placeholder") {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Image, null, tint = ColorTextSecondary, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Toca para subir", style = MaterialTheme.typography.labelSmall, color = ColorTextSecondary)
                }
            } else {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = dayName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Overlay oscuro para que el texto resalte
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
            }

            // Nombre del Día
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = dayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
            }
            
            // Botón Eliminar
            if (imageUrl != "placeholder") {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(onClick = onDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassActivityImageDialog(
    initialName: String = "",
    onDismiss: () -> Unit,
    onSave: (String, Uri) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    val isEditMode = initialName.isNotBlank()
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) selectedUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E).copy(alpha = 0.70f).copy(alpha = 0.70f), // ColorDialogSurface
        titleContentColor = ColorTextPrimary,
        textContentColor = ColorTextSecondary,
        title = { Text(if (isEditMode) "Editar Imagen" else "Nueva Imagen", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // 1. Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (!isEditMode) name = it.uppercase() },
                    label = { Text("Nombre del Día / Actividad", color = ColorTextSecondary) },
                    enabled = !isEditMode,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorPrimaryAction,
                        unfocusedBorderColor = ColorBorder,
                        focusedTextColor = ColorTextPrimary,
                        unfocusedTextColor = ColorTextPrimary, // Even if disabled, should be readable?
                        disabledTextColor = ColorTextSecondary,
                        disabledBorderColor = ColorBorder.copy(alpha = 0.5f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // 2. Image Picker Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(ColorGlassSurface)
                        .border(1.dp, ColorBorder, RoundedCornerShape(12.dp))
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedUri != null) {
                        AsyncImage(
                            model = selectedUri,
                            contentDescription = "Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Overlay to indicate clickability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, null, tint = Color.White.copy(alpha = 0.8f))
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Add, null, tint = ColorPrimaryAction, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Toca para seleccionar imagen", style = MaterialTheme.typography.bodySmall, color = ColorTextSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && selectedUri != null) {
                        onSave(name, selectedUri!!)
                    } else if (isEditMode && selectedUri == null) {
                         // If editing and no new image selected, maybe we just dismiss or treat as no-op?
                         // Currently saveActivityImage requires (name, uri). AdminViewModel doesn't seem to support "rename only" or "keep image".
                         // So we require an image for now, or we would need to fetch the existing one which complexity.
                         // Let's enforce picking an image for now as per previous logic.
                         Toast.makeText(context, "Debes seleccionar una imagen.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Completa todos los campos.", Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = (name.isNotBlank() && selectedUri != null),
                colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = ColorTextSecondary)
            }
        }
    )
}
