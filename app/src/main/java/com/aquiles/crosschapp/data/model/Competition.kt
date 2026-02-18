package com.aquiles.crosschapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class CompetitionType(val value: String) {
    DAILY("Diaria"),
    WEEKLY("Semanal"),
    MONTHLY("Mensual"),
    ANNUAL("Anual"),
    RANGE("Días / Rango"); // N-NUEVO: Paridad con iOS .range

    companion object {
        fun fromValue(value: String): CompetitionType {
            return entries.find { it.value == value } ?: MONTHLY
        }
    }
}

enum class RankingCriteria(val value: String) {
    TIME("Tiempo (Menor es mejor)"),
    REPS("Reps/Rounds (Mayor es mejor)"),
    LOAD("Carga (Mayor es mejor)"),
    POINTS("Puntos (Acumulativo)");

    companion object {
        fun fromValue(value: String): RankingCriteria {
            return entries.find { it.value == value } ?: POINTS
        }
    }
}

// N-NUEVO: Enums de estrategia de puntaje y validación (paridad con iOS)
enum class ScoreStrategy(val value: String) {
    RELATIVE("relative"),
    ABSOLUTE("absolute"),
    ROUNDS("rounds");

    companion object {
        fun fromValue(value: String): ScoreStrategy {
            return entries.find { it.value == value } ?: ABSOLUTE
        }
    }
}

enum class ValidationRule(val value: String) {
    AUTOMATIC("automatic"),
    MANUAL("manual");

    companion object {
        fun fromValue(value: String): ValidationRule {
            return entries.find { it.value == value } ?: AUTOMATIC
        }
    }
}

data class Competition(
    @DocumentId val id: String = "",
    @get:PropertyName("gym_id") val gymId: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val type: String = "Mensual", // Stored as String to match iOS rawValue
    val criteria: String = "Puntos (Acumulativo)", // Stored as String
    val startDate: Date? = null,
    val endDate: Date? = null,
    @get:PropertyName("isActive") val isActive: Boolean = true,

    // Linked WODs/Classes
    @get:PropertyName("linked_class_ids") val linkedClassIds: List<String> = emptyList(),

    // Rewards
    val prizeDescription: String? = null,
    val xpReward: Int? = null,
    val isIntergym: Boolean? = null,

    // N-NUEVO: Estrategia de puntaje y validación (paridad con iOS)
    val scoreStrategy: String? = null, // "relative", "absolute", "rounds"
    val validationRule: String? = null, // "automatic", "manual"

    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun getTypeEnum(): CompetitionType = CompetitionType.fromValue(type)
    fun getCriteriaEnum(): RankingCriteria = RankingCriteria.fromValue(criteria)
    fun getScoreStrategyEnum(): ScoreStrategy = ScoreStrategy.fromValue(scoreStrategy ?: "absolute")
    fun getValidationRuleEnum(): ValidationRule = ValidationRule.fromValue(validationRule ?: "automatic")

    // Helper property for status checks
    fun getStatus(): CompetitionStatus {
        if (!isActive) return CompetitionStatus.INACTIVE
        val now = Date()
        val start = startDate ?: now
        val end = endDate ?: now
        return when {
            now.before(start) -> CompetitionStatus.UPCOMING
            now.after(end) -> CompetitionStatus.FINISHED
            else -> CompetitionStatus.ONGOING
        }
    }
}

enum class CompetitionStatus {
    INACTIVE, UPCOMING, ONGOING, FINISHED
}

