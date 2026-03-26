package com.aquiles.crosschapp.domain.competition

import com.aquiles.crosschapp.data.model.RankingCriteria
import java.util.Date

/**
 * Objeto puro de validación del formulario de competencia.
 * Separado de la UI y ViewModel para facilitar los tests unitarios.
 */
object CompetitionFormValidator {

    data class ValidationResult(
        val isValid: Boolean,
        val errors: Map<String, String>
    ) {
        val errorMessages: List<String> get() = errors.values.toList()
    }

    /**
     * Valida todos los campos del formulario de creación/edición de competencia.
     */
    fun validate(
        title: String,
        description: String,
        startDate: Date,
        endDate: Date,
        maxCapacity: Int,
        registrationCredits: Int,
        criteriaSpecificValue: String? = null,
        criteria: RankingCriteria? = null
    ): ValidationResult {
        val errors = mutableMapOf<String, String>()

        // --- Validaciones de campos obligatorios ---
        if (title.isBlank()) {
            errors["title"] = "El título es obligatorio"
        } else if (title.length < 3) {
            errors["title"] = "El título debe tener al menos 3 caracteres"
        }

        if (description.isBlank()) {
            errors["description"] = "La descripción es obligatoria"
        }

        // --- Validación de fechas ---
        if (endDate.before(startDate)) {
            errors["dates"] = "La fecha de fin debe ser posterior a la de inicio"
        }

        // --- Validación de capacidad ---
        if (maxCapacity <= 0) {
            errors["capacity"] = "La capacidad mínima es de 1 atleta"
        } else if (maxCapacity > 1000) {
            errors["capacity"] = "La capacidad máxima es de 1000 atletas"
        }

        // --- Validación de campo específico por criterio (dinámico) ---
        if (registrationCredits < 0) {
            errors["registrationCredits"] = "Los créditos deben ser mayores o iguales a cero"
        }

        if (criteria != null && criteriaSpecificValue != null) {
            val numericVal = criteriaSpecificValue.toDoubleOrNull()
            when (criteria) {
                RankingCriteria.TIME -> {
                    if (numericVal == null || numericVal <= 0) {
                        errors["criteriaField"] = "El tiempo límite debe ser un número positivo"
                    }
                }
                RankingCriteria.LOAD -> {
                    if (numericVal == null || numericVal <= 0) {
                        errors["criteriaField"] = "El peso de referencia debe ser un número positivo"
                    }
                }
                RankingCriteria.REPS -> {
                    if (numericVal == null || numericVal <= 0) {
                        errors["criteriaField"] = "Las rondas máximas deben ser un número positivo"
                    }
                }
                RankingCriteria.POINTS -> { /* Sin validación extra para puntos */ }
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }

    /**
     * Valida el resultado ingresado por un atleta o admin.
     */
    fun validateScore(
        score: String,
        criteria: RankingCriteria
    ): String? {
        if (score.isBlank()) return "El resultado no puede estar vacío"

        val numericValue = score.replace(",", ".").toDoubleOrNull()
        if (numericValue == null) return "El resultado debe ser un número válido"
        if (numericValue < 0) return "El resultado no puede ser negativo"

        return when (criteria) {
            RankingCriteria.TIME -> {
                if (numericValue <= 0) "El tiempo debe ser mayor a cero" else null
            }
            RankingCriteria.LOAD -> {
                if (numericValue <= 0) "El peso debe ser mayor a cero" else null
            }
            RankingCriteria.REPS -> {
                if (numericValue != numericValue.toLong().toDouble())
                    "Las repeticiones deben ser un número entero"
                else null
            }
            RankingCriteria.POINTS -> null
        }
    }

    /**
     * Obtiene la etiqueta y descripción del campo extra según el criterio.
     */
    fun getCriteriaFieldConfig(criteria: RankingCriteria): CriteriaFieldConfig? {
        return when (criteria) {
            RankingCriteria.TIME -> CriteriaFieldConfig(
                label = "Tiempo límite (minutos)",
                placeholder = "Ej: 20",
                hint = "El tiempo máximo para completar el WOD",
                inputType = InputType.DECIMAL
            )
            RankingCriteria.LOAD -> CriteriaFieldConfig(
                label = "Peso de referencia (kg)",
                placeholder = "Ej: 100",
                hint = "El peso RX o de referencia para esta prueba",
                inputType = InputType.DECIMAL
            )
            RankingCriteria.REPS -> CriteriaFieldConfig(
                label = "Rondas / Reps máximas",
                placeholder = "Ej: 10",
                hint = "El máximo de rondas o repeticiones posibles",
                inputType = InputType.INTEGER
            )
            RankingCriteria.POINTS -> null // Sin campo extra
        }
    }

    data class CriteriaFieldConfig(
        val label: String,
        val placeholder: String,
        val hint: String,
        val inputType: InputType
    )

    enum class InputType { INTEGER, DECIMAL }

    /**
     * Verifica si la lista de competidores es válida.
     */
    fun validateCompetitorList(
        enrolledUsers: List<String>,
        minRequired: Int = 2
    ): String? {
        if (enrolledUsers.isEmpty()) return "No hay atletas inscritos en esta competencia"
        if (enrolledUsers.size < minRequired) {
            return "Se requieren al menos $minRequired atletas para iniciar la competencia (hay ${enrolledUsers.size})"
        }
        return null
    }

    /**
     * Calcula el pódium (1°, 2°, 3°) desde una lista ya ordenada de resultados.
     * Retorna una lista de hasta 3 posiciones con el userId y puesto.
     */
    fun calculatePodium(
        rankedResults: List<PodiumEntry>
    ): List<PodiumEntry> {
        return rankedResults.take(3).mapIndexed { index, entry ->
            entry.copy(position = index + 1)
        }
    }

    data class PodiumEntry(
        val userId: String,
        val userName: String,
        val scoreDisplay: String,
        val numericScore: Double,
        val position: Int = 0
    )
}
