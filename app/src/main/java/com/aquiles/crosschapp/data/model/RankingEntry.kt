package com.aquiles.crosschapp.data.model

data class RankingEntry(
    val userId: String,
    val userName: String,
    val userProfileImageUrl: String?,
    val userLevel: String?,
    val score: Double, // Valor numérico para ordenamiento
    val scoreDisplay: String, // String formateado (ej: "12:30", "150 reps")
    val rank: Int,
    // --- Campos Sociales / Validación ---
    val resultId: String = "",
    val validationStatus: String? = null,
    val reactions: Map<String, String>? = null
)
