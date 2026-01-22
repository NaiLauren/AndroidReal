package com.aquiles.crosschapp.data.model

object LevelSystem {

    fun getLevelName(xp: Int): String {
        return when {
            xp < 100 -> "Novato"
            xp < 500 -> "Constante"
            xp < 1500 -> "Atleta"
            xp < 5000 -> "RX"
            else -> "Elite"
        }
    }

    fun getNextLevelXp(xp: Int): Int {
        return when {
            xp < 100 -> 100
            xp < 500 -> 500
            xp < 1500 -> 1500
            xp < 5000 -> 5000
            else -> 100000 // Elite max
        }
    }
    
    // XP Rewards Constants
    const val XP_ATTENDANCE = 10
}
