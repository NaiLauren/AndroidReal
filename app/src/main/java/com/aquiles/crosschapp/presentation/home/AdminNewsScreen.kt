package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aquiles.crosschapp.data.model.GymNotice
import com.aquiles.crosschapp.presentation.viewmodel.NoticeViewModel
import java.text.SimpleDateFormat // Consider java.time for new code, but SimpleDateFormat is common in legacy
import java.util.Locale

// --- DESIGN SYSTEM CONSTANTS (Reused for consistency) ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.75f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
private val ColorDanger = Color(0xFFFF3B30)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminNewsScreen(
    innerPadding: PaddingValues = PaddingValues(), // Default to empty if not passed, though usually passed from Scaffold
    navController: NavController,
    noticeViewModel: NoticeViewModel = viewModel()
) {
    val notices by noticeViewModel.notices.collectAsState()
    val isLoading by noticeViewModel.isLoading.collectAsState()
    
    // Refresh data on enter
    LaunchedEffect(Unit) {
        noticeViewModel.loadNotices()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)) // Overlay dim
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Novedades del Box", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ColorTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navController.navigate("create_notice") }, // Reuse existing route
                    containerColor = ColorPrimaryAction,
                    contentColor = ColorTextPrimary
                ) {
                    Icon(Icons.Default.Add, "Crear Nueva")
                }
            },
            containerColor = Color.Transparent
        ) { localPadding ->
             LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = localPadding.calculateTopPadding() + 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 80.dp, // Space for FAB
                    start = 16.dp,
                    end = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (notices.isEmpty() && !isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No hay avisos publicados.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = ColorTextSecondary
                            )
                        }
                    }
                } else {
                    items(notices) { notice ->
                        AdminNoticeItem(
                            notice = notice,
                            onDelete = { noticeViewModel.deleteNotice(notice.id) }
                        )
                    }
                }
            }
            
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ColorPrimaryAction)
                }
            }
        }
    }
}

@Composable
fun AdminNoticeItem(
    notice: GymNotice,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Aviso") },
            text = { Text("¿Estás seguro de que deseas eliminar este aviso? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ColorDanger)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            },
            containerColor = Color(0xFF1C1C1E),
            titleContentColor = ColorTextPrimary,
            textContentColor = ColorTextSecondary
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface),
        border = BorderStroke(1.dp, ColorBorder)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Image Thumbnail
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(if (notice.imageUrl.isNotBlank()) notice.imageUrl else "https://via.placeholder.com/150") // Fallback
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, ColorBorder, RoundedCornerShape(12.dp))
            )

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (!notice.title.isNullOrBlank()) notice.title else "Sin Título",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary,
                    maxLines = 1
                )
                
                // Date formatting
                val dateStr = remember(notice.createdAt) {
                    notice.createdAt?.toDate()?.let { date ->
                        SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(date)
                    } ?: "Fecha desconocida"
                }
                
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = ColorTextSecondary
                )
                
                if (notice.message.isNotBlank()) {
                     Text(
                        text = notice.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorTextSecondary,
                        maxLines = 2
                    )
                }
            }

            // Delete Action
            IconButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Eliminar",
                    tint = ColorDanger, // Red for destructive action
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
