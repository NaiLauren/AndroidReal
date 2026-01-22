package com.aquiles.crosschapp.data.model

data class AchievementDefinition(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val type: String = "smart",
    val xpReward: Int = 50
)

object AchievementSystem {
    // Definimos los logros "Inteligentes"
    val smartList = listOf(
        AchievementDefinition(
            id = "early_bird",
            title = "Madrugador",
            description = "Entrenar antes de las 9 AM requiere valor.",
            iconName = "wb_sunny",
            xpReward = 50
        ),
        AchievementDefinition(
            id = "night_owl",
            title = "Ave Nocturna",
            description = "Cerrando el box a pura potencia (+20hs).",
            iconName = "dark_mode",
            xpReward = 50
        ),
        AchievementDefinition(
            id = "week_fire",
            title = "Semana de Fuego",
            description = "¡5 clases en los últimos 7 días! Estás on fire.",
            iconName = "whatshot",
            xpReward = 100
        )
        // Aquí puedes agregar más en el futuro (ej: "Fin de semana guerrero")
    )

    fun getById(id: String): AchievementDefinition? {
        return smartList.find { it.id == id }
    }
}