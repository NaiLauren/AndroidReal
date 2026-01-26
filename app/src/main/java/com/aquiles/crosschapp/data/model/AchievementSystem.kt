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
        ),
        // --- NUEVOS LOGROS ---
        AchievementDefinition(
            id = "weekend_warrior",
            title = "Guerrero de Finde",
            description = "Entrenar el fin de semana demuestra tu compromiso.",
            iconName = "sports_kabaddi",
            xpReward = 70
        ),
        AchievementDefinition(
            id = "never_skip_monday",
            title = "Lunes Sagrado",
            description = "Nunca te saltas un lunes. ¡Esa es la actitud!",
            iconName = "calendar_today",
            xpReward = 30
        ),
        AchievementDefinition(
            id = "lunch_crew",
            title = "Hora del Almuerzo",
            description = "Entrenando mientras otros comen. (12:00 - 14:00)",
            iconName = "lunch_dining",
            xpReward = 40
        ),
        AchievementDefinition(
            id = "hat_trick",
            title = "Hat Trick",
            description = "¡3 días consecutivos asistiendo! Impresionante.",
            iconName = "sports_soccer",
            xpReward = 150
        ),
        AchievementDefinition(
            id = "double_trouble",
            title = "Doble Turno",
            description = "¡2 clases en un solo día! ¿Estás loco?",
            iconName = "filter_2",
            xpReward = 200
        ),
        // --- PROGRESO DE CLASES ---
        AchievementDefinition("1_class", "Primer Paso", "¡Completaste tu primera clase!", "flag", "automatic", 20),
        AchievementDefinition("5_classes", "Calentando Motores", "¡5 clases! Ya le coges el ritmo.", "local_fire_department", "automatic", 50),
        AchievementDefinition("20_classes", "Parte del Mobiliario", "¡20 clases! Ya te conocemos.", "weekend", "automatic", 100),
        AchievementDefinition("50_classes", "Lealtad de Hierro", "50 clases. Tu constancia es tu fuerza.", "military_tech", "automatic", 200),
        AchievementDefinition("100_classes", "Titán del Box", "¡100 clases! Eres una leyenda.", "shield", "automatic", 500),
        AchievementDefinition("200_classes", "Centurión", "¡200 clases! Respeto.", "workspace_premium", "automatic", 1000),
        AchievementDefinition("300_classes", "Deidad del Box", "300 clases. ¿Vives aquí?", "auto_awesome", "automatic", 2000)
    )

    fun getById(id: String): AchievementDefinition? {
        return smartList.find { it.id == id }
    }
}