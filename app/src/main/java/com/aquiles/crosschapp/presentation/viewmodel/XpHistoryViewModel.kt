package com.aquiles.crosschapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.XpLog
import com.aquiles.crosschapp.presentation.viewmodel.UserSession
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class XpHistoryState {
    data object Loading : XpHistoryState()
    data class Success(val logs: List<XpLog>) : XpHistoryState()
    data object Empty : XpHistoryState()
    data class Error(val message: String) : XpHistoryState()
}

class XpHistoryViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _state = MutableStateFlow<XpHistoryState>(XpHistoryState.Loading)
    val state = _state.asStateFlow()

    init {
        fetchHistory()
    }

    fun fetchHistory() {
        val currentUser = UserSession.currentUser.value ?: return

        viewModelScope.launch {
            _state.value = XpHistoryState.Loading
            try {
                // Consultar colección xp_logs
                // Requiere índice compuesto: gym_id + userId + timestamp DESC
                val snapshot = firestore.collection("xp_logs")
                    .whereEqualTo("gym_id", currentUser.gym_id)
                    .whereEqualTo("userId", currentUser.id)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(50) // Paginación simple por ahora
                    .get()
                    .await()

                val logs = snapshot.toObjects(XpLog::class.java)

                if (logs.isEmpty()) {
                    _state.value = XpHistoryState.Empty
                } else {
                    _state.value = XpHistoryState.Success(logs)
                }

            } catch (e: Exception) {
                // Si falla (ej. falta indice), mostrar error amigable o lista vacía
                _state.value = XpHistoryState.Error("Error cargando historial: ${e.localizedMessage}")
            }
        }
    }
}
