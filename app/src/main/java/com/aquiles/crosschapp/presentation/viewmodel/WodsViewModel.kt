package com.aquiles.crosschapp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.GymClass
import com.aquiles.crosschapp.data.model.GlobalChallenge
import com.aquiles.crosschapp.data.model.Wod
import com.aquiles.crosschapp.data.model.Competition
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

// Estado para el Pager de Clases
data class DailyClassesState(
    val classes: List<GymClass> = emptyList(),
    val initialScrollIndex: Int = 0,
    val isLoading: Boolean = false
)

// Estado general
sealed class WodsState {
    data object Loading : WodsState()
    data class Success(val wods: List<Wod>) : WodsState()
    data class Error(val message: String) : WodsState()
}

sealed class ChallengeSaveState {
    object Idle : ChallengeSaveState()
    object Loading : ChallengeSaveState()
    data class Success(val message: String) : ChallengeSaveState()
    data class Error(val message: String) : ChallengeSaveState()
}

class WodsViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    // Estado principal
    private val _wodsState = MutableStateFlow<WodsState>(WodsState.Loading)
    val wodsState: StateFlow<WodsState> = _wodsState.asStateFlow()

    // Estado Guardar Resultado Desafío
    private val _saveChallengeResultState = MutableStateFlow<ChallengeSaveState>(ChallengeSaveState.Idle)
    val saveChallengeResultState: StateFlow<ChallengeSaveState> = _saveChallengeResultState.asStateFlow()

    // Pager de Clases
    private val _dailyClassesState = MutableStateFlow(DailyClassesState())
    val dailyClassesState: StateFlow<DailyClassesState> = _dailyClassesState.asStateFlow()

    // --- DASHBOARD WODS ---
    private val _todayWod = MutableStateFlow<Wod?>(null)
    val todayWod: StateFlow<Wod?> = _todayWod.asStateFlow()

    private val _tomorrowWod = MutableStateFlow<Wod?>(null)
    val tomorrowWod: StateFlow<Wod?> = _tomorrowWod.asStateFlow()

    // --- ACTIVE COMPETITIONS FOR PREMIUM UI ---
    private val _hasActiveCompetition = MutableStateFlow(false)
    val hasActiveCompetition: StateFlow<Boolean> = _hasActiveCompetition.asStateFlow()

    private var classesListenerRegistration: ListenerRegistration? = null
    private var wodsListenerRegistration: ListenerRegistration? = null
    private var competitionsListenerRegistration: ListenerRegistration? = null

    fun listenForDashboardWods() {
        val currentUserGymId = UserSession.currentUserGymId.value
        if (currentUserGymId.isNullOrBlank()) {
            _wodsState.value = WodsState.Error("ID del gimnasio no disponible.")
            return
        }
        // 1. Cargar Clases para el carrusel (Consistencia unificada)
        loadClassesForTodayPager()
        // 2. Escuchar WODs (Hoy y Mañana) en tiempo real
        listenToWodsForDashboard()
        // 3. Cargar desafíos globales
        loadGlobalChallenges()
        // 4. Escuchar por competiciones activas
        listenForActiveCompetitions()
    }

    private fun loadClassesForTodayPager() {
        val gymId = UserSession.currentUserGymId.value ?: return

        viewModelScope.launch {
            _dailyClassesState.update { it.copy(isLoading = true) }

            try {
                val calendar = Calendar.getInstance()
                val startOfToday = getStartOfDay(calendar.time)
                val endOfToday = getEndOfDay(calendar.time)

                val snapshot = firestore.collection("gymClasses")
                    .whereEqualTo("gym_id", gymId)
                    .whereGreaterThanOrEqualTo("dateTime", startOfToday)
                    .whereLessThan("dateTime", endOfToday)
                    .orderBy("dateTime", Query.Direction.ASCENDING)
                    .get().await()

                val classes = snapshot.toObjects(GymClass::class.java)

                val now = Date()
                var scrollIndex = classes.indexOfFirst { gymClass ->
                    // --- CORRECCIÓN: Null Safety para Date? ---
                    val startTime = gymClass.dateTime?.time ?: return@indexOfFirst false
                    val endTime = Date(startTime + (gymClass.durationMinutes * 60 * 1000))
                    endTime.after(now)
                }

                if (scrollIndex == -1) {
                    scrollIndex = if(classes.isNotEmpty()) classes.lastIndex else 0
                }

                _dailyClassesState.update {
                    it.copy(classes = classes, initialScrollIndex = scrollIndex, isLoading = false)
                }

                // _wodsState.value = WodsState.Success(emptyList()) // This line was removed as it's not directly related to classes loading success

            } catch (e: Exception) {
                Log.e("WodsViewModel", "Error cargando clases", e)
                _dailyClassesState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun listenToWodsForDashboard() {
        val gymId = UserSession.currentUserGymId.value ?: return
        wodsListenerRegistration?.remove()

        val calendar = Calendar.getInstance()
        val startOfToday = getStartOfDay(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfTomorrow = getEndOfDay(calendar.time)

        val todayCal = Calendar.getInstance()
        val dayOfYearToday = todayCal.get(Calendar.DAY_OF_YEAR)

        wodsListenerRegistration = firestore.collection("wods")
            .whereEqualTo("gym_id", gymId)
            .whereGreaterThanOrEqualTo("date", startOfToday)
            .whereLessThan("date", endOfTomorrow)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("WodsViewModel", "Error en listener de WODs", e)
                    _wodsState.value = WodsState.Error(e.localizedMessage ?: "Error al cargar WODs")
                    return@addSnapshotListener
                }

                val wods = snapshot?.toObjects(Wod::class.java) ?: emptyList()
                
                // Separar Hoy y Mañana con mayor precisión
                _todayWod.value = wods.find { wod ->
                    val wCal = Calendar.getInstance().apply { time = wod.date ?: Date() }
                    wCal.get(Calendar.DAY_OF_YEAR) == dayOfYearToday
                }

                _tomorrowWod.value = wods.find { wod ->
                    val wCal = Calendar.getInstance().apply { time = wod.date ?: Date() }
                    wCal.get(Calendar.DAY_OF_YEAR) != dayOfYearToday
                }

                _wodsState.value = WodsState.Success(wods)
            }
    }

    // --- NUEVO: GLOBAL CHALLENGES (Para competir entre gimnasios) ---
    private val _globalChallenges = MutableStateFlow<List<GlobalChallenge>>(emptyList())
    val globalChallenges: StateFlow<List<GlobalChallenge>> = _globalChallenges.asStateFlow()

    fun loadGlobalChallenges() {
        val gymId = UserSession.currentUserGymId.value ?: return
        viewModelScope.launch {
            try {
                // Cargar desafíos globales
                val globalSnapshot = firestore.collection("challenges")
                    .whereEqualTo("isGlobal", true)
                    .get().await()
                
                // Cargar desafíos locales del gimnasio
                val localSnapshot = firestore.collection("challenges")
                    .whereEqualTo("gym_id", gymId)
                    .get().await()

                val allChallenges = (globalSnapshot.toObjects(GlobalChallenge::class.java) + 
                                     localSnapshot.toObjects(GlobalChallenge::class.java))
                    .distinctBy { it.id }
                
                // Filtrar por gimnasios permitidos (especialmente para los globales restringidos)
                val filtered = allChallenges.filter { bench ->
                    if (bench.isGlobal) {
                        bench.sponsorIds.isNullOrEmpty() || bench.sponsorIds!!.contains(gymId)
                    } else {
                        true // Ya filtrado por query
                    }
                }
                
                _globalChallenges.value = filtered
            } catch (e: Exception) {
                Log.e("WodsViewModel", "Error cargando desafíos (globales/locales)", e)
            }
        }
    }

    private fun listenForActiveCompetitions() {
        val currentUserGymId = UserSession.currentUserGymId.value ?: return
        competitionsListenerRegistration?.remove()

        competitionsListenerRegistration = firestore.collection("competitions")
            .whereEqualTo("gym_id", currentUserGymId)
            // Note: iOS check does not filter by isActive, but typically it checks for ANY valid competition
            // We just need to know if there's any competition to show the premium glow.
            .addSnapshotListener { snapshot, error ->
                 if (error != null) { return@addSnapshotListener }
                 if (snapshot != null) {
                      _hasActiveCompetition.value = snapshot.documents.isNotEmpty()
                 }
            }
    }

    fun logChallengeResult(
        challengeId: String,
        challengeName: String,
        score: String,
        isRx: Boolean,
        notes: String,
        scoreType: String = "reps"
    ) {
        val user = UserSession.currentUser.value ?: run {
            _saveChallengeResultState.value = ChallengeSaveState.Error("Usuario no autenticado")
            return
        }
        val gymId = user.gym_id
        if (gymId.isBlank()) {
            _saveChallengeResultState.value = ChallengeSaveState.Error("Gimnasio no configurado")
            return
        }

        viewModelScope.launch {
            _saveChallengeResultState.value = ChallengeSaveState.Loading
            try {
                // Cálculo de Score Numérico Inteligente
                val numericScore = calculateNumericScore(score, scoreType)

                val resultData = hashMapOf(
                    "challengeId" to challengeId,
                    "challengeName" to challengeName,
                    "userId" to user.id,
                    "userName" to user.name,
                    "userLastName" to user.lastName,
                    "userProfileImage" to (user.profileImageUrl ?: ""),
                    "userLevel" to user.level,
                    "score" to score.trim(),
                    "numericScore" to numericScore,
                    "isRx" to isRx,
                    "notes" to notes.trim(),
                    "gym_id" to gymId,
                    "date" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "validationStatus" to "pending",
                    "facilityName" to (user.facilityName ?: "")
                )

                firestore.collection("challenge_results")
                    .add(resultData)
                    .await()

                // Award XP (iOS Parity)
                try {
                    com.aquiles.crosschapp.data.service.GamificationService.addXp(
                        userId = user.id,
                        gymId = gymId,
                        amount = 15, // Desafíos dan más XP que WODs (Paridad iOS)
                        type = "CHALLENGE",
                        title = "Desafío Registrado",
                        description = "Cargaste marca en: $challengeName",
                        relatedId = challengeId
                    )
                } catch (e: Exception) {
                    Log.e("WodsViewModel", "Error awarding XP for challenge", e)
                }

                _saveChallengeResultState.value = ChallengeSaveState.Success("¡Desafío guardado! (+15 XP)")
            } catch (e: Exception) {
                Log.e("WodsViewModel", "Error saving challenge result", e)
                _saveChallengeResultState.value = ChallengeSaveState.Error("Error al guardar: ${e.localizedMessage}")
            }
        }
    }

    private fun calculateNumericScore(scoreInput: String, unit: String): Double {
        return try {
            when (unit.uppercase()) {
                "TIME", "TIEMPO" -> parseTimeToSeconds(scoreInput)
                else -> {
                    // Extraer solo dígitos y decimales para REPS, WEIGHT, etc.
                    scoreInput.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: 0.0
                }
            }
        } catch (e: Exception) {
            0.0
        }
    }

    private fun parseTimeToSeconds(timeStr: String): Double {
        val parts = timeStr.trim().split(":").mapNotNull { it.trim().toDoubleOrNull() }
        return when (parts.size) {
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            2 -> parts[0] * 60 + parts[1]
            1 -> parts[0]
            else -> 0.0
        }
    }

    fun resetChallengeSaveState() {
        _saveChallengeResultState.value = ChallengeSaveState.Idle
    }

    private fun getStartOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }

    private fun getEndOfDay(date: Date): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        return calendar.time
    }

    override fun onCleared() {
        super.onCleared()
        classesListenerRegistration?.remove()
        wodsListenerRegistration?.remove()
        competitionsListenerRegistration?.remove()
    }
}