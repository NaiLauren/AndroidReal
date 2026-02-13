package com.aquiles.crosschapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class BenchmarkWod(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val scoreType: String = "", // TIME, REPS, WEIGHT, ROUNDS, POINTS, DISTANCE
    val measurementUnit: String = "TIME",
    val sortOrder: String = "ASC",
    val strategy: String = "",
    val gym_id: String = "",
    
    // NUEVOS CAMPOS: Benchmarks Globales Multi-Gym & Competencias Temporales
    val allowedGymIds: List<String>? = null,
    val isCompetition: Boolean? = null,
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null
) {
    // Helper: Verifica si el usuario puede registrar un resultado
    fun canRegisterResult(): Boolean {
        if (isCompetition != true) return true // Ranking permanente
        
        val start = startDate ?: return false
        val end = endDate ?: return false
        
        val now = Timestamp.now()
        return now >= start && now <= end
    }
    
    // Helper: Estado de la competencia
    fun getCompetitionStatus(): CompetitionStatus {
        if (isCompetition != true) return CompetitionStatus.PERMANENT
        
        val start = startDate ?: return CompetitionStatus.PERMANENT
        val end = endDate ?: return CompetitionStatus.PERMANENT
        
        val now = Timestamp.now()
        return when {
            now < start -> CompetitionStatus.UPCOMING
            now > end -> CompetitionStatus.FINISHED
            else -> CompetitionStatus.ACTIVE
        }
    }
    
    enum class CompetitionStatus {
        PERMANENT,  // Ranking permanente
        ACTIVE,     // Competencia en curso
        UPCOMING,   // Competencia próximamente
        FINISHED    // Competencia finalizada
    }
}