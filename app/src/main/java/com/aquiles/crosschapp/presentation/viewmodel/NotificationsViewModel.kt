package com.aquiles.crosschapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.Notification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AllNotificationsState {
    data object Loading : AllNotificationsState()
    data class Success(val notifications: List<Notification>) : AllNotificationsState()
    data class Error(val message: String) : AllNotificationsState()
    data object Empty : AllNotificationsState()
}

class NotificationsViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _notificationsState = MutableStateFlow<AllNotificationsState>(AllNotificationsState.Loading)
    val notificationsState = _notificationsState.asStateFlow()

    private var notificationsListener: ListenerRegistration? = null

    init {
        loadAllNotifications()
    }

    private fun loadAllNotifications() {
        val userId = UserSession.getCurrentUserId()
        val gymId = UserSession.currentUserGymId.value

        if (userId == null || gymId == null) {
            _notificationsState.value = AllNotificationsState.Error("Usuario no autenticado o gimnasio no encontrado.")
            return
        }

        notificationsListener?.remove()
        _notificationsState.value = AllNotificationsState.Loading

        // Sin orderBy para evitar índice compuesto — ordenamos client-side
        notificationsListener = firestore.collection("notifications")
            .whereEqualTo("gym_id", gymId)
            .whereEqualTo("userId", userId)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _notificationsState.value = AllNotificationsState.Error(
                        error.localizedMessage ?: "Error al cargar notificaciones."
                    )
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val notifications = snapshot.toObjects(Notification::class.java)
                        .sortedByDescending { it.timestamp }
                    if (notifications.isEmpty()) {
                        _notificationsState.value = AllNotificationsState.Empty
                    } else {
                        _notificationsState.value = AllNotificationsState.Success(notifications)
                    }
                }
            }
    }

    fun markNotificationAsRead(notificationId: String) {
        if (notificationId.isBlank()) return

        // Optimistic update: actualizar estado local inmediatamente sin esperar a Firestore
        val currentState = _notificationsState.value
        if (currentState is AllNotificationsState.Success) {
            val updated = currentState.notifications.map { notification ->
                if (notification.id == notificationId) notification.copy(isRead = true)
                else notification
            }
            _notificationsState.value = AllNotificationsState.Success(updated)
        }

        // Persistir en Firestore de fondo
        viewModelScope.launch {
            try {
                firestore.collection("notifications").document(notificationId)
                    .update("isRead", true)
                    .await()
            } catch (e: Exception) {
                // Si falla, el snapshot listener va a restaurar el estado real
                android.util.Log.e("NotificationsVM", "Error marcando leída: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        notificationsListener?.remove()
    }
}