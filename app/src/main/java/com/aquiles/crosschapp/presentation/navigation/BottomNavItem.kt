// Archivo: presentation/navigation/BottomNavItem.kt

package com.aquiles.crosschapp.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle // Para Perfil
import androidx.compose.material.icons.filled.CalendarToday // Para Horarios
import androidx.compose.material.icons.filled.FitnessCenter // Para WODs
import androidx.compose.material.icons.filled.Home // Para Inicio
import androidx.compose.material.icons.filled.Leaderboard // Para Rendimiento
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Esta 'sealed class' es como una lista cerrada y definida de todos los posibles
 * destinos que pueden aparecer en nuestra barra de navegación inferior.
 *
 * Cada 'object' dentro de la clase representa un botón en la barra.
 *
 * ¿Por qué 'sealed'? Porque nos aseguramos de que SÓLO los objetos definidos aquí
 * pueden ser un BottomNavItem. No se pueden crear más en otros archivos.
 * Esto nos da seguridad y control sobre nuestra navegación.
 */
sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Home : BottomNavItem(
        route = "home", // <-- CORREGIDO
        label = "Inicio",
        icon = Icons.Filled.Home
    )

    object Schedule : BottomNavItem(
        route = "schedule", // <-- CORREGIDO
        label = "Horarios",
        icon = Icons.Filled.CalendarToday
    )

    object Wods : BottomNavItem(
        route = "wods", // <-- CORREGIDO
        label = "WODs",
        icon = Icons.Filled.FitnessCenter
    )

    object Performance : BottomNavItem(
        route = "performance", // <-- CORREGIDO
        label = "Rendimiento",
        icon = Icons.Filled.Leaderboard
    )

    object Profile : BottomNavItem(
        route = "profile", // <-- CORREGIDO
        label = "Perfil",
        icon = Icons.Filled.AccountCircle
    )
}