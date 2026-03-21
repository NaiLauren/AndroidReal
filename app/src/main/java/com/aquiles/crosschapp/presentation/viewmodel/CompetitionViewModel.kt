package com.aquiles.crosschapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.Competition
import com.aquiles.crosschapp.data.model.CompetitionType
import com.aquiles.crosschapp.data.model.RankingCriteria
import com.aquiles.crosschapp.data.model.ScoreStrategy
import com.aquiles.crosschapp.data.model.ValidationRule
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class CompetitionViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _competitions = MutableStateFlow<List<Competition>>(emptyList())
    val competitions: StateFlow<List<Competition>> = _competitions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadCompetitions() {
        val gymId = UserSession.currentUser.value?.gym_id ?: return
        if (gymId.isBlank()) return

        _isLoading.value = true
        viewModelScope.launch {
            try {
                val snapshot = db.collection("competitions")
                    .whereEqualTo("gym_id", gymId)
                    // Sin filtro isActive — el owner ve todas
                    // Sin orderBy para evitar índice compuesto en Firestore
                    .get()
                    .await()

                // Ordenamos client-side por endDate descendente
                val comps = snapshot.toObjects(Competition::class.java)
                    .sortedByDescending { it.endDate }
                _competitions.value = comps
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar competencias: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createCompetitionWithEvent(
        title: String,
        description: String,
        type: CompetitionType,
        criteria: RankingCriteria,
        startDate: Date,
        endDate: Date,
        prizeDescription: String?,
        xpReward: Int?,
        scoreStrategy: ScoreStrategy,
        validationRule: ValidationRule,
        eventTime: String,
        eventCapacity: Int
    ) {
        val gymId = com.aquiles.crosschapp.presentation.viewmodel.UserSession.currentUser.value?.gym_id ?: return
        if (title.isBlank()) {
            _errorMessage.value = "El título es obligatorio"
            return
        }

        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                // 1. Create Competition Reference
                val compRef = db.collection("competitions").document()
                
                // Parse Time for Event Date
                val timeParts = eventTime.split(":")
                val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 10
                val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0
                val eventDateTime = java.util.Calendar.getInstance().apply {
                    time = startDate
                    set(java.util.Calendar.HOUR_OF_DAY, hour)
                    set(java.util.Calendar.MINUTE, minute)
                    set(java.util.Calendar.SECOND, 0)
                }.time

                // 2. Create GymClass and WOD References
                val classRef = db.collection("gymClasses").document()
                val wodRef = db.collection("wods").document()

                val eventClass = com.aquiles.crosschapp.data.model.GymClass(
                    documentId = classRef.id,
                    gym_id = gymId,
                    name = title,
                    dateTime = eventDateTime, // Usamos la fecha de inicio del torneo combinada con la hora elegida
                    durationMinutes = 60,
                    maxCapacity = eventCapacity,
                    classType = "COMPETITION",
                    competitionId = compRef.id,
                    isOpenGym = false,
                    isCancelled = false,
                    coachName = "Organización",
                    enrolledUserIds = emptyList(), 
                    waitingList = emptyList(),    
                    hexColor = "#FFD700", // Dorado
                    wodId = wodRef.id
                )

                // 3. Create WOD Object (Reference already created above)
                val eventWod = com.aquiles.crosschapp.data.model.Wod(
                    id = wodRef.id,
                    gym_id = gymId,
                    title = title,
                    type = "Daily",
                    description = description,
                    scoreType = criteria.value,
                    date = eventDateTime,
                    notes = "WOD oficial de la competencia",
                    isCompetitionEnabled = true
                )

                // 4. Assemble Competition Object with Linked Class
                val newComp = Competition(
                    id = compRef.id,
                    gymId = gymId,
                    title = title,
                    description = description,
                    type = type.value, 
                    criteria = criteria.value, 
                    startDate = startDate,
                    endDate = endDate,
                    isActive = true,
                    prizeDescription = if (prizeDescription.isNullOrBlank()) null else prizeDescription,
                    xpReward = xpReward,
                    isIntergym = false, // Solo Super Admin puede crear competencias inter-gym
                    scoreStrategy = scoreStrategy.value,
                    validationRule = validationRule.value,
                    linkedClassIds = listOf(classRef.id) 
                )

                // 5. Batch Commit Both
                db.runBatch { batch ->
                    batch.set(compRef, newComp)
                    batch.set(classRef, eventClass)
                    batch.set(wodRef, eventWod)
                }.await()

                markSetupStepComplete("torneo_listo")
                loadCompetitions() // Reload list
            } catch (e: Exception) {
                _errorMessage.value = "Error al crear competencia: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteCompetition(competitionId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                // Hard-delete: borra el documento permanentemente
                db.collection("competitions").document(competitionId)
                    .delete()
                    .await()
                loadCompetitions()
            } catch (e: Exception) {
                _errorMessage.value = "Error al eliminar: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // MARK: - Setup Wizard Progress
    private fun markSetupStepComplete(stepKey: String) {
        val gymId = com.aquiles.crosschapp.presentation.viewmodel.UserSession.currentUser.value?.gym_id ?: return
        viewModelScope.launch {
            try {
                db.collection("gyms").document(gymId)
                    .set(mapOf("setupProgress" to mapOf(stepKey to true)), com.google.firebase.firestore.SetOptions.merge())
            } catch (e: Exception) {
                android.util.Log.e("CompetitionViewModel", "Error marking setup step $stepKey", e)
            }
        }
    }
}
