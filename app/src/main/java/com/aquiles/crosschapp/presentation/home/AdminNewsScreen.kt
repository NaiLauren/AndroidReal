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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.aquiles.crosschapp.data.model.GymNotice
import com.aquiles.crosschapp.presentation.viewmodel.NoticeViewModel
import com.aquiles.crosschapp.presentation.viewmodel.UserSession
import java.text.SimpleDateFormat // Consider java.time for new code, but SimpleDateFormat is common in legacy
import java.util.Locale
import com.aquiles.crosschapp.presentation.components.GlassCard
import androidx.compose.foundation.layout.height

// --- DESIGN SYSTEM CONSTANTS (Reused for consistency) ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.65f)
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
    noticeViewModel: NoticeViewModel = viewModel(),
    setupStepKey: String? = null
) {
    var showSetupPopup by remember { mutableStateOf(setupStepKey != null) }
    
    if (showSetupPopup) {
        SetupStep.entries.find { it.toKey() == setupStepKey }?.let { step ->
            AlertDialog(
                onDismissRequest = { showSetupPopup = false },
                title = { Text(step.title, color = ColorTextPrimary) },
                text = { Text(step.description, color = ColorTextSecondary) },
                confirmButton = {
                    Button(onClick = { showSetupPopup = false }, colors = ButtonDefaults.buttonColors(containerColor = ColorPrimaryAction)) { 
                        Text("Entendido", color = Color.White) 
                    }
                },
                containerColor = ColorGlassSurface
            )
        }
    }
    
    val notices by noticeViewModel.notices.collectAsState()
    val isLoading by noticeViewModel.isLoading.collectAsState()
    
    val currentUser by UserSession.currentUser.collectAsState()
    
    // Refresh data on enter or user change
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            noticeViewModel.loadNotices()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // .background(Color.Black.copy(alpha = 0.4f)) // Removed to show AppBackground
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    modifier = Modifier.height(72.dp),
                    title = { Text("Novedades del Box", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ColorTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color(0xFF1C1C1E).copy(alpha = 0.85f))
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
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No hay avisos publicados.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = ColorTextSecondary
                                )
                            }
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
            containerColor = Color(0xFF1C1C1E).copy(alpha = 0.70f).copy(alpha = 0.70f),
            titleContentColor = ColorTextPrimary,
            textContentColor = ColorTextSecondary
        )
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Image Thumbnail
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(if (notice.actualImageUrl.isNotBlank()) notice.actualImageUrl else "https://via.placeholder.com/150") // Fallback
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, ColorBorder, RoundedCornerShape(12.dp)),
                loading = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp), color = ColorPrimaryAction)
                    }
                },
                error = {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Gray.copy(0.3f)), contentAlignment = Alignment.Center) {
                         Icon(Icons.Default.Image, null, tint = Color.Gray)
                    }
                }
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
