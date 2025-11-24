package com.aquiles.crosschapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.Notification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// El Sealed Class 'AllNotificationsState' se mantiene igual, está perfecto.
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
        // --- CAMBIO 1: Obtener datos de la fuente de la verdad: UserSession ---
        val userId = UserSession.getCurrentUserId()
        val gymId = UserSession.currentUserGymId.value

        if (userId == null || gymId == null) {
            _notificationsState.value = AllNotificationsState.Error("Usuario no autenticado o gimnasio no encontrado.")
            return
        }

        notificationsListener?.remove()
        _notificationsState.value = AllNotificationsState.Loading

        // --- CAMBIO 2: Consulta corregida para la arquitectura multi-gimnasio ---
        // Escuchamos en la colección de nivel superior 'notifications'
        notificationsListener = firestore.collection("notifications")
            .whereEqualTo("gym_id", gymId) // Filtro por gimnasio
            .whereEqualTo("userId", userId) // Filtro por usuario
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _notificationsState.value = AllNotificationsState.Error(error.localizedMessage ?: "Error al cargar notificaciones.")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val notifications = snapshot.toObjects(Notification::class.java)
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

        viewModelScope.launch {
            try {
                // --- CAMBIO 3: Ruta de actualización corregida ---
                // Apuntamos directamente al documento en la colección principal
                firestore.collection("notifications").document(notificationId)
                    .update("isRead", true)
            } catch (e: Exception) {
                // Manejar el error si es necesario
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        notificationsListener?.remove()
    }
}