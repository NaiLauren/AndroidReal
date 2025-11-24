package com.aquiles.crosschapp.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

// --- DESIGN SYSTEM CONSTANTS ---
private val ColorNavBarSurface = Color(0xFF121212).copy(alpha = 0.95f) // Casi sólido para legibilidad
private val ColorPrimaryAction = Color(0xFFFC5200)
private val ColorTextUnselected = Color.White.copy(alpha = 0.5f)
private val ColorBorder = Color.White.copy(alpha = 0.1f)

@Composable
fun AppBottomNavigationBar(
    navController: NavController,
    items: List<BottomNavItem>
) {
    // Eliminamos el "Box con Blur" porque es costoso en rendimiento y a veces no renderiza bien en Android.
    // Usamos la aproximación de "Vidrio Ahumado Oscuro" que es el estándar moderno.

    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            // Borde superior sutil para separar del contenido
            .border(width = 1.dp, color = ColorBorder, shape = RectangleShape),
        containerColor = ColorNavBarSurface,
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
                    selectedIconColor = ColorPrimaryAction,
                    selectedTextColor = ColorPrimaryAction,
                    indicatorColor = ColorPrimaryAction.copy(alpha = 0.1f), // Fondo sutil al seleccionar (Pill)
                    unselectedIconColor = ColorTextUnselected,
                    unselectedTextColor = ColorTextUnselected
                )
            )
        }
    }
}