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
    val isDesafio: Boolean? = null,
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val isGlobal: Boolean = false
) {
    // Helper: Verifica si el usuario puede registrar un resultado
    fun canRegisterResult(): Boolean {
        if (isDesafio != true) return true // Ranking permanente
        
        // Si es desafío pero no tiene fechas, es un desafío "permanente" (siempre activo)
        val start = startDate ?: return true
        val end = endDate ?: return true
        
        val now = Timestamp.now()
        return now >= start && now <= end
    }
    
    // Helper: Estado del desafío
    fun getDesafioStatus(): DesafioStatus {
        if (isDesafio != true) return DesafioStatus.PERMANENT
        
        // Si es desafío pero no tiene fechas, es un desafío activo de forma permanente
        val start = startDate ?: return DesafioStatus.ACTIVE
        val end = endDate ?: return DesafioStatus.ACTIVE
        
        val now = Timestamp.now()
        return when {
            now < start -> DesafioStatus.UPCOMING
            now > end -> DesafioStatus.FINISHED
            else -> DesafioStatus.ACTIVE
        }
    }
    
    enum class DesafioStatus {
        PERMANENT,  // Ranking permanente
        ACTIVE,     // Desafío en curso
        UPCOMING,   // Desafío próximamente
        FINISHED    // Desafío finalizado
    }
}