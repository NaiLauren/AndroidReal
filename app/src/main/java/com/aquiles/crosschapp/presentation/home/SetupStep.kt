package com.aquiles.crosschapp.presentation.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class SetupStep(
    val title: String,
    val description: String,
    val educationalLocation: String,
    val icon: ImageVector,
    val destination: String
) {
    HORARIOS_LISTOS(
        "Define tus Horarios",
        "Define los horarios de apertura libre para saber cuándo está abierto tu centro.",
        "💡 Ubicación en el futuro:\nBusca 'Horarios Fijos' en la sección principal del mando.",
        Icons.Default.Schedule,
        "admin_manage_schedules"
    ),
    PACKS_LISTOS(
        "Crea tus Paquetes",
        "Configura qué planes van a comprar tus alumnos (Pase Libre, 12 Clases, etc.).",
        "💡 Ubicación en el futuro:\nBusca 'Gestionar Packs' en Configuración Avanzada.",
        Icons.Default.Sell,
        "admin_manage_packs_screen"
    ),
    CLASES_LISTAS(
        "Arma tu Plantilla",
        "Añade las clases guiadas en los horarios fijos de tu semana.",
        "💡 Ubicación en el futuro:\nBusca 'Horarios Fijos' en la sección principal del mando.",
        Icons.Default.ViewList,
        "admin_schedule_planner"
    ),
    TORNEO_LISTO(
        "Organiza una Competencia",
        "Dale vida al gimnasio. Crea un torneo o ranking.",
        "💡 Ubicación en el futuro:\nBusca 'Torneos y Compe' en Configuración Avanzada.",
        Icons.Default.EmojiEvents,
        "admin_competition_manager"
    ),
    NOVEDAD_LISTA(
        "Publica una Novedad",
        "Comunícate con tus alumnos avisando de eventos u ofertas.",
        "💡 Ubicación en el futuro:\nBusca 'Novedades y Avisos' en Configuración Avanzada.",
        Icons.Default.Campaign,
        "admin_news_screen"
    ),
    GAMIFICACION_LISTA(
        "Personaliza tu App",
        "Elige los colores de tu marca y las imágenes de portada de tus alumnos.",
        "💡 Ubicación en el futuro:\nBusca 'Ajustes de Marca' en Configuración Avanzada.",
        Icons.Default.Palette,
        "admin_gym_settings"
    ),
    GESTION_ALUMNOS(
        "Gestiona tus Alumnos",
        "Visualiza a tus alumnos y administra sus accesos.",
        "💡 Ubicación en el futuro:\nBusca el botón 'Alumnos' morado en el panel central.",
        Icons.Default.People,
        "admin_manage_users"
    );

    fun toKey(): String {
        return name.lowercase()
    }
}
