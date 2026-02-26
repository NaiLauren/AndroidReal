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
                    .whereEqualTo("isActive", true)
                    .orderBy("endDate", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val comps = snapshot.toObjects(Competition::class.java)
                _competitions.value = comps
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar competencias: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createCompetition(
        title: String,
        description: String,
        type: CompetitionType,
        criteria: RankingCriteria,
        startDate: Date,
        endDate: Date,
        prizeDescription: String?,
        xpReward: Int?,
        isIntergym: Boolean,
        scoreStrategy: ScoreStrategy,
        validationRule: ValidationRule
    ) {
        val gymId = UserSession.currentUser.value?.gym_id ?: return
        if (title.isBlank()) {
            _errorMessage.value = "El título es obligatorio"
            return
        }

        _isLoading.value = true
        val newComp = Competition(
            gymId = gymId,
            title = title,
            description = description,
            type = type.value, // Store raw String value
            criteria = criteria.value, // Store raw String value
            startDate = startDate,
            endDate = endDate,
            isActive = true,
            prizeDescription = if (prizeDescription.isNullOrBlank()) null else prizeDescription,
            xpReward = xpReward,
            isIntergym = isIntergym,
            scoreStrategy = scoreStrategy.value,
            validationRule = validationRule.value,
            linkedClassIds = emptyList() // Default empty
        )

        viewModelScope.launch {
            try {
                db.collection("competitions").add(newComp).await()
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
                db.collection("competitions").document(competitionId)
                    .update("isActive", false) // Soft delete matches iOS
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
