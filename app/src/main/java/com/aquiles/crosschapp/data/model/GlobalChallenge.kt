package com.aquiles.crosschapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class GlobalChallenge(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val scoreType: String = "", // TIME, REPS, WEIGHT, ROUNDS, POINTS, DISTANCE
    val measurementUnit: String = "TIME",
    val sortOrder: String = "ASC",
    val strategy: String = "",
    val sponsorIds: List<String>? = null,
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    @get:com.google.firebase.firestore.PropertyName("isGlobal")
    @set:com.google.firebase.firestore.PropertyName("isGlobal")
    var isGlobal: Boolean = true,
    val gym_id: String? = null // Null if strictly global for everyone, or a specific gym if it's a private challenge for a gym
) {
    // Helper: Verifica si el usuario puede registrar un resultado
    fun canRegisterResult(): Boolean {
        // Si no tiene fechas, es un desafío "permanente" (siempre activo)
        val start = startDate ?: return true
        val end = endDate ?: return true
        
        val now = Timestamp.now()
        return now >= start && now <= end
    }
    
    // Helper: Estado del desafío
    fun getChallengeStatus(): ChallengeStatus {
        val start = startDate ?: return ChallengeStatus.ACTIVE
        val end = endDate ?: return ChallengeStatus.ACTIVE
        
        val now = Timestamp.now()
        return when {
            now < start -> ChallengeStatus.UPCOMING
            now > end -> ChallengeStatus.FINISHED
            else -> ChallengeStatus.ACTIVE
        }
    }
    
    enum class ChallengeStatus {
        ACTIVE,     // Desafío en curso
        UPCOMING,   // Desafío próximamente
        FINISHED    // Desafío finalizado
    }
}
