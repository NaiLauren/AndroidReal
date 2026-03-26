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
    @get:PropertyName("gym_id") @set:PropertyName("gym_id") var gymId: String = "",
    val title: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val type: String = "Mensual",
    val criteria: String = "Puntos (Acumulativo)",
    val startDate: Date? = null,
    val endDate: Date? = null,
    @get:PropertyName("isActive") @set:PropertyName("isActive") var isActive: Boolean = true,

    // Linked WODs/Classes
    @get:PropertyName("linked_class_ids") @set:PropertyName("linked_class_ids") var linkedClassIds: List<String> = emptyList(),

    // Rewards
    val prizeDescription: String? = null,
    val xpReward: Int? = null,

    // Intergym — un solo campo que maneja tanto "intergym" (iOS) como "isIntergym" (Android legacy).
    // @PropertyName("intergym") hace que Firestore lea/escriba la clave "intergym",
    // y Kotlin usa el nombre isIntergym internamente sin chocar en el nivel JVM.
    @get:PropertyName("intergym") @set:PropertyName("intergym") var isIntergym: Boolean? = null,

    val registrationCredits: Int = 0, // N-NUEVO: Costo en créditos para inscribirse

    // Estrategia de puntaje y validación (iOS escribe "scoreStrategy", "validationRule", "scoreStrategyEnum")
    val scoreStrategy: String? = null,
    val validationRule: String? = null,
    val scoreStrategyEnum: String? = null,  // iOS puede escribir el raw value del enum aquí

    // Status (iOS escribe "status" como string del enum)
    val status: String? = null,

    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun resolveTypeEnum(): CompetitionType = CompetitionType.fromValue(type)
    fun resolveCriteriaEnum(): RankingCriteria = RankingCriteria.fromValue(criteria)
    // Renombrado de getScoreStrategyEnum() para evitar clash con el campo scoreStrategyEnum
    fun resolveScoreStrategy(): ScoreStrategy = ScoreStrategy.fromValue(scoreStrategy ?: scoreStrategyEnum ?: "absolute")
    fun resolveValidationRule(): ValidationRule = ValidationRule.fromValue(validationRule ?: "automatic")

    fun isIntergymEvent(): Boolean = isIntergym ?: false

    // Renombrado para no chocar con ningún getter Firestore
    fun resolveStatus(): CompetitionStatus {
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

