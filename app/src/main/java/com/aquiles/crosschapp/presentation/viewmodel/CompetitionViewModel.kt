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
import android.net.Uri
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class CompetitionViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val storage = com.google.firebase.storage.FirebaseStorage.getInstance()

    private val _competitions = MutableStateFlow<List<Competition>>(emptyList())
    val competitions: StateFlow<List<Competition>> = _competitions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

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
        eventCapacity: Int,
        registrationCredits: Int,
        imageUri: Uri?
    ) {
        val gymId = com.aquiles.crosschapp.presentation.viewmodel.UserSession.currentUser.value?.gym_id ?: return
        if (title.isBlank()) {
            _errorMessage.value = "El título es obligatorio"
            return
        }

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val compRef = db.collection("competitions").document()

                var finalImageUrl: String? = null
                if (imageUri != null) {
                    val storageRef = storage.reference.child("competitions/$gymId/${UUID.randomUUID()}.jpg")
                    storageRef.putFile(imageUri).await()
                    finalImageUrl = storageRef.downloadUrl.await().toString()
                }

                val timeParts = eventTime.split(":")
                val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 10
                val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

                // Generar lista de días para la competencia
                val calendar = java.util.Calendar.getInstance()
                val days = mutableListOf<Date>()
                calendar.time = startDate
                val endCal = java.util.Calendar.getInstance().apply { time = endDate }
                while (!calendar.after(endCal)) {
                    days.add(calendar.time)
                    calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
                }

                // Crear una GymClass + WOD por cada día
                val classRefs = mutableListOf<com.google.firebase.firestore.DocumentReference>()
                val classObjects = mutableListOf<com.aquiles.crosschapp.data.model.GymClass>()
                val wodObjects = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, com.aquiles.crosschapp.data.model.Wod>>()

                days.forEachIndexed { index, day ->
                    val classRef = db.collection("gymClasses").document()
                    val wodRef = db.collection("wods").document()

                    val eventDateTime = java.util.Calendar.getInstance().apply {
                        time = day
                        set(java.util.Calendar.HOUR_OF_DAY, hour)
                        set(java.util.Calendar.MINUTE, minute)
                        set(java.util.Calendar.SECOND, 0)
                    }.time

                    val dayLabel = if (days.size > 1) "$title - Día ${index + 1}" else title

                    classRefs.add(classRef)
                    classObjects.add(
                        com.aquiles.crosschapp.data.model.GymClass(
                            documentId = classRef.id,
                            gym_id = gymId,
                            name = dayLabel,
                            dateTime = eventDateTime,
                            durationMinutes = 60,
                            maxCapacity = eventCapacity,
                            classType = "COMPETITION",
                            competitionId = compRef.id,
                            isOpenGym = false,
                            isCancelled = false,
                            coachName = "Organización",
                            enrolledUserIds = emptyList(),
                            waitingList = emptyList(),
                            hexColor = "#FFD700",
                            imageUrl = finalImageUrl,
                            wodId = wodRef.id
                        )
                    )
                    wodObjects.add(
                        wodRef to com.aquiles.crosschapp.data.model.Wod(
                            id = wodRef.id,
                            gym_id = gymId,
                            title = dayLabel,
                            type = "Daily",
                            description = description,
                            scoreType = criteria.value,
                            date = eventDateTime,
                            notes = "WOD oficial de la competencia",
                            isCompetitionEnabled = true
                        )
                    )
                }

                val newComp = Competition(
                    id = compRef.id,
                    gymId = gymId,
                    title = title,
                    description = description,
                    type = type.value,
                    criteria = criteria.value,
                    imageUrl = finalImageUrl,
                    registrationCredits = registrationCredits,
                    startDate = startDate,
                    endDate = endDate,
                    isActive = true,
                    prizeDescription = if (prizeDescription.isNullOrBlank()) null else prizeDescription,
                    xpReward = xpReward,
                    isIntergym = false,
                    scoreStrategy = scoreStrategy.value,
                    validationRule = validationRule.value,
                    linkedClassIds = classRefs.map { it.id }
                )

                db.runBatch { batch ->
                    batch.set(compRef, newComp)
                    classRefs.forEachIndexed { i, ref ->
                        batch.set(ref, classObjects[i])
                    }
                    wodObjects.forEach { (ref, wod) ->
                        batch.set(ref, wod)
                    }
                }.await()

                _successMessage.value = "¡Competencia creada exitosamente!"
                markSetupStepComplete("torneo_listo")
                loadCompetitions()
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
                _successMessage.value = "Competencia eliminada"
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

    fun clearSuccess() {
        _successMessage.value = null
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
