package com.aquiles.crosschapp.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.WindowInsets
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.HazeStyle
import com.aquiles.crosschapp.LocalHazeState

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorNavBarSurface = Color.Transparent // Ahora Haze pone el fondo
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextUnselected = Color.White.copy(alpha = 0.5f)
private val ColorBorder = Color.White.copy(alpha = 0.2f)
private val GlassTopStart = Color.White.copy(alpha = 0.15f)
private val GlassTopEnd = Color.White.copy(alpha = 0.05f)

@Composable
fun AppBottomNavigationBar(
    navController: NavController,
    items: List<BottomNavItem>
) {
    val hazeState = LocalHazeState.current
    
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp) // Solo margen superior para el efecto
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)) // Redondeo tipo bóveda
                .then(
                    if (hazeState != null) {
                        Modifier.hazeChild(
                            state = hazeState,
                            style = HazeStyle(
                                blurRadius = 20.dp,
                                tint = Color(0xFF1C1C1E).copy(alpha = 0.55f)
                            )
                        )
                    } else Modifier.background(Color(0xFF1C1C1E).copy(alpha = 0.88f))
                )
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(GlassTopStart, GlassTopEnd)
                    )
                )
                .border(width = 1.dp, color = ColorBorder, shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
        ) {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding() // Pushes content above system bar
                    .height(68.dp),
                windowInsets = WindowInsets(0, 0, 0, 0),
                containerColor = Color.Transparent, // El Box padre aporta el cristal
                tonalElevation = 0.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                items.forEach { item ->
                    // Lógica de selección corregida para rutas anidadas
                    val isSelected = currentRoute?.startsWith(item.route) == true

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                overflow = TextOverflow.Ellipsis,
                                maxLines = 1,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), // Fondo sutil al seleccionar (Pill)
                            unselectedIconColor = ColorTextUnselected,
                            unselectedTextColor = ColorTextUnselected
                        )
                    )
                }
            } // End NavigationBar
        } // End Box
    } // End Column
}