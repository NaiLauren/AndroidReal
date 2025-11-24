package com.aquiles.crosschapp.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.aquiles.crosschapp.data.model.User
import com.aquiles.crosschapp.presentation.viewmodel.AdminViewModel
import com.aquiles.crosschapp.presentation.viewmodel.UserListState
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.75f)
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)
private val ColorBackgroundGradientStart = Color(0xFF000000)
private val ColorBackgroundGradientEnd = Color(0xFF121212)
private val ColorError = Color(0xFFEF5350)
private val ColorSuccess = Color(0xFF4CAF50)
private val ColorWarning = Color(0xFFFFA500)

private enum class SortOrder {
    EXPIRATION_SOON, ALPHABETICAL, MOST_CREDITS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManageUsersScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    adminViewModel: AdminViewModel = viewModel()
) {
    var selectedUserIds by remember { mutableStateOf(setOf<String>()) }
    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(SortOrder.EXPIRATION_SOON) }
    val userListState by adminViewModel.userListState.collectAsState()

    LaunchedEffect(Unit) {
        adminViewModel.loadAllUsers()
    }

    // --- UI STRUCTURE ---
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Alumnos", fontWeight = FontWeight.Bold, color = ColorTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = ColorTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            floatingActionButton = {
                AnimatedVisibility(visible = selectedUserIds.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            val ids = selectedUserIds.joinToString(",")
                            navController.navigate("admin_broadcast_message/$ids")
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.Send, "Enviar difusión") },
                        text = { Text("Difusión (${selectedUserIds.size})") },
                        containerColor = ColorPrimaryAction,
                        contentColor = Color.White
                    )
                }
            },
            containerColor = Color.Transparent
        ) { localScaffoldPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = localScaffoldPadding.calculateTopPadding())
                    .padding(horizontal = 16.dp)
            ) {
                // Buscador Glass
                GlassSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filtros
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GlassFilterChip(
                        selected = sortOrder == SortOrder.EXPIRATION_SOON,
                        onClick = { sortOrder = SortOrder.EXPIRATION_SOON },
                        label = "Por Vencer"
                    )
                    GlassFilterChip(
                        selected = sortOrder == SortOrder.ALPHABETICAL,
                        onClick = { sortOrder = SortOrder.ALPHABETICAL },
                        label = "A-Z"
                    )
                    GlassFilterChip(
                        selected = sortOrder == SortOrder.MOST_CREDITS,
                        onClick = { sortOrder = SortOrder.MOST_CREDITS },
                        label = "Más Créditos"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (val state = userListState) {
                    is UserListState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = ColorPrimaryAction) }
                    is UserListState.Error -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Error: ${state.message}", color = ColorError) }
                    is UserListState.Success -> {
                        val filteredAndSortedUsers = remember(searchQuery, sortOrder, state.users) {
                            val filtered = state.users.filter { user -> user.fullName.contains(searchQuery, ignoreCase = true) }
                            when (sortOrder) {
                                SortOrder.EXPIRATION_SOON -> filtered.sortedBy { it.creditValidUntil ?: Date(Long.MAX_VALUE) }
                                SortOrder.ALPHABETICAL -> filtered.sortedBy { it.fullName }
                                SortOrder.MOST_CREDITS -> filtered.sortedByDescending { it.credits }
                            }
                        }

                        if (filteredAndSortedUsers.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No se encontraron alumnos.", color = ColorTextSecondary) }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding() + 80.dp)
                            ) {
                                items(filteredAndSortedUsers, key = { it.id }) { user ->
                                    UserStatusItemGlass(
                                        user = user,
                                        isSelected = selectedUserIds.contains(user.id),
                                        onSelectionChange = {
                                            selectedUserIds = if (selectedUserIds.contains(user.id)) selectedUserIds - user.id else selectedUserIds + user.id
                                        },
                                        onUserClick = { userId -> navController.navigate("admin_user_details/$userId") },
                                        onSendMessageClick = { userId, userName -> navController.navigate("admin_send_message/$userId/$userName") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =====================================================
// COMPONENTES UI (GLASS STYLE)
// =====================================================

@Composable
fun GlassSearchBar(query: String, onQueryChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Buscar por nombre...", color = ColorTextSecondary) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ColorTextSecondary) },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ColorPrimaryAction,
            unfocusedBorderColor = ColorBorder,
            focusedTextColor = ColorTextPrimary,
            cursorColor = ColorPrimaryAction,
            focusedContainerColor = ColorGlassSurface,
            unfocusedContainerColor = ColorGlassSurface
        ),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun GlassFilterChip(selected: Boolean, onClick: () -> Unit, label: String) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, color = if(selected) Color.White else ColorTextSecondary) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = ColorGlassSurface,
            selectedContainerColor = ColorPrimaryAction,
            labelColor = ColorTextSecondary,
            selectedLabelColor = Color.White
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = ColorBorder,
            selectedBorderColor = Color.Transparent,
            enabled = true,
            selected = selected
        )
    )
}

@Composable
fun UserStatusItemGlass(
    user: User,
    isSelected: Boolean,
    onSelectionChange: () -> Unit,
    onUserClick: (String) -> Unit,
    onSendMessageClick: (String, String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale("es", "ES")) }
    val today = Calendar.getInstance()
    val expirationDate = user.creditValidUntil?.let { Calendar.getInstance().apply { time = it } }
    val daysUntilExpiration = expirationDate?.let { TimeUnit.MILLISECONDS.toDays(it.timeInMillis - today.timeInMillis) }

    val statusColor = when {
        expirationDate == null || expirationDate.before(today) -> ColorError
        daysUntilExpiration != null && daysUntilExpiration <= 7 -> ColorWarning
        else -> ColorSuccess
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onUserClick(user.id) },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isSelected) ColorPrimaryAction else ColorBorder),
        colors = CardDefaults.cardColors(containerColor = ColorGlassSurface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox area
            Box(modifier = Modifier.clickable { onSelectionChange() }) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectionChange() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = ColorPrimaryAction,
                        uncheckedColor = ColorTextSecondary,
                        checkmarkColor = Color.White
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Avatar
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(user.profileImageUrl).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (painter.state is AsyncImagePainter.State.Loading || painter.state is AsyncImagePainter.State.Error) {
                        Icon(Icons.Default.Person, null, tint = ColorTextSecondary, modifier = Modifier.size(24.dp))
                    } else {
                        SubcomposeAsyncImageContent()
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
                Text(
                    text = "Créditos: ${user.credits}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorTextSecondary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = user.creditValidUntil?.let { dateFormat.format(it) } ?: "Sin membresía",
                        style = MaterialTheme.typography.labelSmall,
                        color = ColorTextSecondary.copy(alpha = 0.8f)
                    )
                }
            }

            // Botón Mensaje Directo
            IconButton(onClick = { onSendMessageClick(user.id, user.fullName) }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Mensaje",
                    tint = ColorPrimaryAction
                )
            }
        }
    }
}