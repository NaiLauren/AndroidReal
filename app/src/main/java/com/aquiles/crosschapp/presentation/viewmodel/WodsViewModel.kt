package com.aquiles.crosschapp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.GymClass
import com.aquiles.crosschapp.data.model.Wod
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

class WodsViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    // Estado principal
    private val _wodsState = MutableStateFlow<WodsState>(WodsState.Loading)
    val wodsState: StateFlow<WodsState> = _wodsState.asStateFlow()

    // Pager de Clases
    private val _dailyClassesState = MutableStateFlow(DailyClassesState())
    val dailyClassesState = _dailyClassesState.asStateFlow()

    // --- CORRECCIÓN: Reintegrado todayWod para la UI ---
    private val _todayWod = MutableStateFlow<Wod?>(null)
    val todayWod = _todayWod.asStateFlow()

    private val _tomorrowWod = MutableStateFlow<Wod?>(null)
    val tomorrowWod = _tomorrowWod.asStateFlow()

    private var listener: ListenerRegistration? = null

    fun listenForDashboardWods() {
        // 1. Cargar Clases para el carrusel
        loadClassesForTodayPager()
        // 2. Cargar WODs (Hoy y Mañana) para info general y compartir
        loadWodsForDashboard()
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

                _wodsState.value = WodsState.Success(emptyList())

            } catch (e: Exception) {
                Log.e("WodsViewModel", "Error cargando clases", e)
                _dailyClassesState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun loadWodsForDashboard() {
        val gymId = UserSession.currentUserGymId.value ?: return

        viewModelScope.launch {
            try {
                val calendar = Calendar.getInstance()
                val startOfToday = getStartOfDay(calendar.time)

                // Buscamos hasta el final de mañana
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                val endOfTomorrow = getEndOfDay(calendar.time)

                // Reseteamos el calendario para comparar abajo
                val todayCal = Calendar.getInstance()
                val dayOfYearToday = todayCal.get(Calendar.DAY_OF_YEAR)

                val snapshot = firestore.collection("wods")
                    .whereEqualTo("gym_id", gymId)
                    .whereGreaterThanOrEqualTo("date", startOfToday)
                    .whereLessThan("date", endOfTomorrow)
                    .get().await()

                val wods = snapshot.toObjects(Wod::class.java)

                // Separar Hoy y Mañana
                _todayWod.value = wods.find { wod ->
                    val wCal = Calendar.getInstance().apply { time = wod.date ?: Date() }
                    wCal.get(Calendar.DAY_OF_YEAR) == dayOfYearToday
                }

                _tomorrowWod.value = wods.find { wod ->
                    val wCal = Calendar.getInstance().apply { time = wod.date ?: Date() }
                    wCal.get(Calendar.DAY_OF_YEAR) != dayOfYearToday // Si no es hoy, es mañana (por el filtro de query)
                }

            } catch (e: Exception) {
                Log.e("WodsViewModel", "Error cargando WODs generales", e)
            }
        }
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
        listener?.remove()
        super.onCleared()
    }
}