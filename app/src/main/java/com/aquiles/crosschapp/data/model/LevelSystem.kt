package com.aquiles.crosschapp.data.model

object LevelSystem {

    fun getLevelName(xp: Int): String {
        return when {
            xp < 350 -> "Novato"
            xp < 1000 -> "Constante"
            xp < 3000 -> "Atleta"
            xp < 10000 -> "RX"
            else -> "Elite"
        }
    }

    fun getNextLevelXp(xp: Int): Int {
        return when {
            xp < 350 -> 350
            xp < 1000 -> 1000
            xp < 3000 -> 3000
            xp < 10000 -> 10000
            else -> 100000 // Elite max
        }
    }
    
    // XP Rewards Constants
    const val XP_ATTENDANCE = 10

    // Gamification Helpers
    fun getAvatarDecoration(level: String): String? {
        return when (level) {
            "Novato" -> null // Sin decoración
            "Constante" -> "military_tech" // Medalla Bronce
            "Atleta" -> "fitness_center" // Pesas
            "RX" -> "workspace_premium" // Medalla Oro / Trofeo
            "Elite" -> "emoji_events" // Corona / Trofeo Elite
            else -> null
        }
    }

    fun getPreviousLevelLimit(xp: Int): Int {
        return when {
            xp < 350 -> 0
            xp < 1000 -> 350
            xp < 3000 -> 1000
            xp < 10000 -> 3000
            else -> 10000
        }
    }
}
