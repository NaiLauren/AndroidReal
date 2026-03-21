package com.aquiles.crosschapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.Achievement
import com.aquiles.crosschapp.data.model.AchievementDefinition
import com.aquiles.crosschapp.data.model.AchievementSystem
import com.aquiles.crosschapp.data.model.RankingEntry
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Categoría de logro (paridad con iOS AchievementCategory)
 */
enum class AchievementCategory(val displayName: String) {
    MILESTONE("Hitos de Clases"),
    STREAK("Rachas"),
    TIME("Tiempo y Horarios"),
    SPECIAL("Especiales")
}

/**
 * Modelo de presentación: combina la definición del logro con el estado desbloqueado del usuario.
 */
data class AchievementDisplayModel(
    val definition: AchievementDefinition,
    val isUnlocked: Boolean,
    val unlockedAt: java.util.Date? = null,
    /** Progreso 0.0–1.0 para logros de milestone (clases) */
    val progress: Float = 0f,
    /** Texto de progreso, ej. "20 / 50 clases" */
    val progressLabel: String? = null,
    val category: AchievementCategory
)

sealed class AchievementsState {
    object Loading : AchievementsState()
    data class Success(
        val byCategory: Map<AchievementCategory, List<AchievementDisplayModel>>,
        val totalUnlocked: Int,
        val trophies: List<CompetitionTrophy>
    ) : AchievementsState()
    data class Error(val message: String) : AchievementsState()
}

/** Trofeo ganado en una competencia (1°, 2°, 3° puesto) */
data class CompetitionTrophy(
    val competitionTitle: String,
    val position: Int, // 1, 2 or 3
    val competitionId: String
)

class AchievementsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _state = MutableStateFlow<AchievementsState>(AchievementsState.Loading)
    val state: StateFlow<AchievementsState> = _state.asStateFlow()

    fun loadAchievements(userId: String, gymId: String, totalClassesAttended: Int) {
        _state.value = AchievementsState.Loading
        viewModelScope.launch {
            try {
                // 1. Fetch logros desbloqueados del usuario en Firestore
                val unlockedSnap = db.collection("achievements")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                val unlockedAchievements = unlockedSnap.toObjects(Achievement::class.java)
                val unlockedIds = unlockedAchievements.map { it.achievementId }.toSet()
                val unlockedDates = unlockedAchievements.associateBy({ it.achievementId }, { it.unlockedAt })

                // 2. Fetch trofeos de competencias (resultados aprobados con rank 1-3)
                val trophies = fetchCompetitionTrophies(userId, gymId)

                // 3. Mapear definiciones a modelos de presentación
                val allDefs = AchievementSystem.smartList
                val displayModels = allDefs.map { def ->
                    val isUnlocked = def.id in unlockedIds
                    val category = resolveCategory(def)
                    val (progress, progressLabel) = resolveProgress(def, totalClassesAttended)
                    AchievementDisplayModel(
                        definition = def,
                        isUnlocked = isUnlocked,
                        unlockedAt = unlockedDates[def.id],
                        progress = progress,
                        progressLabel = progressLabel,
                        category = category
                    )
                }

                // 4. Agrupar por categoría: primero desbloqueados, luego bloqueados
                val byCategory = displayModels
                    .groupBy { it.category }
                    .mapValues { (_, items) ->
                        items.sortedWith(
                            compareByDescending<AchievementDisplayModel> { it.isUnlocked }
                                .thenBy { it.definition.id }
                        )
                    }
                    .toSortedMap(compareBy { it.ordinal })

                val totalUnlocked = displayModels.count { it.isUnlocked }

                _state.value = AchievementsState.Success(
                    byCategory = byCategory,
                    totalUnlocked = totalUnlocked,
                    trophies = trophies
                )

            } catch (e: Exception) {
                _state.value = AchievementsState.Error("Error al cargar logros: ${e.message}")
            }
        }
    }

    private suspend fun fetchCompetitionTrophies(userId: String, gymId: String): List<CompetitionTrophy> {
        return try {
            // Buscar resultados del usuario aprobados con rank 1-3
            val resultsSnap = db.collection("wod_results")
                .whereEqualTo("userId", userId)
                .whereEqualTo("gym_id", gymId)
                .whereEqualTo("validationStatus", "approved")
                .get()
                .await()

            val trophies = mutableListOf<CompetitionTrophy>()
            val competitionIds = resultsSnap.documents
                .mapNotNull { it.getString("competitionId") }
                .filter { it.isNotBlank() }
                .distinct()

            // Para cada competencia, verificar si el usuario estuvo en podio
            for (compId in competitionIds.take(10)) {
                try {
                    val allResultsSnap = db.collection("wod_results")
                        .whereEqualTo("competitionId", compId)
                        .whereEqualTo("validationStatus", "approved")
                        .get()
                        .await()

                    val competitionTitleSnap = db.collection("competitions")
                        .document(compId)
                        .get()
                        .await()
                    val competitionTitle = competitionTitleSnap.getString("title") ?: "Competencia"

                    // Ordenar por numericScore (desc) para determinar puesto
                    val sorted = allResultsSnap.documents
                        .mapNotNull { doc ->
                            val uid = doc.getString("userId") ?: return@mapNotNull null
                            val score = (doc.get("numericScore") as? Number)?.toDouble() ?: 0.0
                            uid to score
                        }
                        .sortedByDescending { it.second }

                    val podiumPosition = sorted.indexOfFirst { it.first == userId }.takeIf { it >= 0 }?.plus(1)
                    if (podiumPosition != null && podiumPosition <= 3) {
                        trophies.add(
                            CompetitionTrophy(
                                competitionTitle = competitionTitle,
                                position = podiumPosition,
                                competitionId = compId
                            )
                        )
                    }
                } catch (_: Exception) {}
            }
            trophies
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Determina la categoría de un logro basado en su ID */
    private fun resolveCategory(def: AchievementDefinition): AchievementCategory {
        return when (def.id) {
            "1_class", "5_classes", "20_classes", "50_classes",
            "100_classes", "200_classes", "300_classes" -> AchievementCategory.MILESTONE
            "hat_trick", "week_fire", "double_trouble" -> AchievementCategory.STREAK
            "early_bird", "night_owl", "lunch_crew" -> AchievementCategory.TIME
            else -> AchievementCategory.SPECIAL
        }
    }

    /** Calcula progreso y label para logros de milestone */
    private fun resolveProgress(def: AchievementDefinition, totalClasses: Int): Pair<Float, String?> {
        val target = when (def.id) {
            "1_class" -> 1
            "5_classes" -> 5
            "20_classes" -> 20
            "50_classes" -> 50
            "100_classes" -> 100
            "200_classes" -> 200
            "300_classes" -> 300
            else -> return Pair(0f, null)
        }
        val progress = (totalClasses.toFloat() / target).coerceIn(0f, 1f)
        val label = "$totalClasses / $target clases"
        return Pair(progress, label)
    }
}
