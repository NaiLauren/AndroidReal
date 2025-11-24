package com.aquiles.crosschapp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.Wod
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

// Estado de la UI para WODs
sealed class WodsState {
    data object Loading : WodsState()
    data class Success(val wods: List<Wod>) : WodsState() // Lista de WODs (como en iOS)
    data class Error(val message: String) : WodsState()
}

class WodsViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    // Estado principal (Igual que @Published var wods en iOS)
    private val _wodsState = MutableStateFlow<WodsState>(WodsState.Loading)
    val wodsState: StateFlow<WodsState> = _wodsState.asStateFlow()

    // Listener para limpieza (Igual que private var listener en iOS)
    private var listener: ListenerRegistration? = null

    // Variable calculada para la UI del Dashboard (Hoy y Mañana)
    // Esto ayuda a tu WodsScreen a saber cuál es cuál
    private val _todayWod = MutableStateFlow<Wod?>(null)
    val todayWod = _todayWod.asStateFlow()

    private val _tomorrowWod = MutableStateFlow<Wod?>(null)
    val tomorrowWod = _tomorrowWod.asStateFlow()

    // MARK: - Fetch WODs (Espejo de iOS fetchWods)
    // En iOS pides una fecha. En Android, como tu Dashboard muestra HOY y MAÑANA,
    // vamos a hacer que esta función escuche un RANGO desde hoy.
    fun listenForDashboardWods() {
        listener?.remove()
        _wodsState.value = WodsState.Loading

        // 1. Obtener gym_id (Igual que iOS UserSession.shared.currentUser?.gym_id)
        val gymId = UserSession.currentUserGymId.value
        if (gymId.isNullOrBlank()) {
            _wodsState.value = WodsState.Error("No se pudo identificar el gimnasio.")
            return
        }

        // 2. Calcular rangos de fecha (Start of Day)
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startOfToday = calendar.time

        // Queremos ver hasta el final de MAÑANA (48 horas de rango)
        calendar.add(Calendar.DAY_OF_YEAR, 2)
        val endOfTomorrow = calendar.time

        // Resetear calendario para comparaciones abajo
        calendar.add(Calendar.DAY_OF_YEAR, -2)

        // 3. Query con Listener (Igual que iOS addSnapshotListener)
        listener = firestore.collection("wods")
            .whereEqualTo("gym_id", gymId) // REGLA 1
            .whereGreaterThanOrEqualTo("date", startOfToday)
            .whereLessThan("date", endOfTomorrow)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _wodsState.value = WodsState.Error(error.localizedMessage ?: "Error cargando WODs")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val wods = snapshot.toObjects(Wod::class.java)
                    _wodsState.value = WodsState.Success(wods)

                    // Lógica extra para separar Hoy vs Mañana en la UI de Android
                    processWodsForDashboard(wods, startOfToday)
                } else {
                    _wodsState.value = WodsState.Success(emptyList())
                }
            }
    }

    private fun processWodsForDashboard(wods: List<Wod>, todayStart: Date) {
        val calendar = Calendar.getInstance()
        calendar.time = todayStart
        val dayOfYearToday = calendar.get(Calendar.DAY_OF_YEAR)

        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val dayOfYearTomorrow = calendar.get(Calendar.DAY_OF_YEAR)

        // Buscar WOD de hoy
        val today = wods.find { wod ->
            val wodCal = Calendar.getInstance().apply { time = wod.date }
            wodCal.get(Calendar.DAY_OF_YEAR) == dayOfYearToday
        }

        // Buscar WOD de mañana
        val tomorrow = wods.find { wod ->
            val wodCal = Calendar.getInstance().apply { time = wod.date }
            wodCal.get(Calendar.DAY_OF_YEAR) == dayOfYearTomorrow
        }

        _todayWod.value = today
        _tomorrowWod.value = tomorrow
    }

    override fun onCleared() {
        listener?.remove()
        super.onCleared()
    }
}