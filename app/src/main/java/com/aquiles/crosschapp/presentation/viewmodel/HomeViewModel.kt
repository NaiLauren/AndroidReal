// RUTA: presentation/viewmodel/HomeViewModel.kt
// VERSIÓN ACTUALIZADA PARA NUEVA LÓGICA DE MENSAJERÍA (isRead)

package com.aquiles.crosschapp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.Notification
import com.aquiles.crosschapp.data.model.PersonalMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

// --- Los Sealed Class se mantienen igual ---
sealed class PersonalMessageState {
    object Loading : PersonalMessageState()
    data class Success(val message: PersonalMessage) : PersonalMessageState()
    object Empty : PersonalMessageState()
    data class Error(val message: String) : PersonalMessageState()
}
sealed class NotificationsState {
    object Loading : NotificationsState()
    data class Success(val notifications: List<Notification>) : NotificationsState()
    data class Error(val message: String) : NotificationsState()
}

class HomeViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "HomeViewModel_DEBUG"

    private val _notificationsState = MutableStateFlow<NotificationsState>(NotificationsState.Loading)
    val notificationsState: StateFlow<NotificationsState> = _notificationsState.asStateFlow()
    private val _personalMessageState = MutableStateFlow<PersonalMessageState>(PersonalMessageState.Loading)
    val personalMessageState: StateFlow<PersonalMessageState> = _personalMessageState.asStateFlow()

    private var notificationsListener: ListenerRegistration? = null
    private var personalMessageListener: ListenerRegistration? = null

    init {
        Log.d(TAG, "ViewModel inicializado.")
        UserSession.currentUser
            .onEach { user ->
                Log.d(TAG, "Observando sesión. El usuario es: ${user?.id} en gym: ${user?.gym_id}")
                if (user != null && user.gym_id.isNotBlank()) {
                    Log.d(TAG, "Usuario VÁLIDO detectado. Iniciando listeners...")
                    listenForUnreadNotifications(user.id, user.gym_id)
                    listenForPersonalMessage(user.id, user.gym_id)
                } else {
                    Log.d(TAG, "Usuario NULO o inválido. Limpiando listeners y estado.")
                    clearListenersAndSetEmptyState()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun listenForUnreadNotifications(userId: String, gymId: String) {
        notificationsListener?.remove()
        notificationsListener = firestore.collection("notifications")
            .whereEqualTo("gym_id", gymId)
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    _notificationsState.value = NotificationsState.Error(e.localizedMessage ?: "Error")
                    return@addSnapshotListener
                }
                val notifications = snapshots?.toObjects(Notification::class.java) ?: emptyList()
                _notificationsState.value = NotificationsState.Success(notifications)
            }
    }

    private fun listenForPersonalMessage(userId: String, gymId: String) {
        Log.d(TAG, "-> listenForPersonalMessage llamado con userId: $userId, gymId: $gymId")
        personalMessageListener?.remove()

        // --- CAMBIO 1: La consulta ahora busca 'isRead' en lugar de '_archived' ---
        // Esto nos mostrará el último mensaje NO LEÍDO como una notificación.
        personalMessageListener = firestore.collection("personal_messages")
            .whereEqualTo("gym_id", gymId)
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false) // <-- El cambio clave
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshots, e ->
                Log.d(TAG, "-> Listener de mensajes personales DISPARADO.")
                if (e != null) {
                    Log.e(TAG, "-> ERROR en el listener: ", e)
                    _personalMessageState.value = PersonalMessageState.Error(e.localizedMessage ?: "Error al cargar mensaje")
                    return@addSnapshotListener
                }

                if (snapshots == null || snapshots.isEmpty) {
                    Log.d(TAG, "-> El Snapshot es NULO o VACÍO. No se encontraron mensajes no leídos.")
                    _personalMessageState.value = PersonalMessageState.Empty
                } else {
                    Log.d(TAG, "-> Snapshot recibido con ${snapshots.size()} documento(s).")
                    val document = snapshots.documents.first()
                    Log.d(TAG, "-> Datos del documento: ${document.data}")
                    val message = try {
                        document.toObject(PersonalMessage::class.java)
                    } catch (ex: Exception) {
                        Log.e(TAG, "-> EXCEPCIÓN durante toObject():", ex)
                        null
                    }

                    if (message != null) {
                        Log.d(TAG, "-> CONVERSIÓN A OBJETO EXITOSA. Objeto: $message")
                        _personalMessageState.value = PersonalMessageState.Success(message)
                    } else {
                        Log.e(TAG, "-> ¡FALLO CRÍTICO! La conversión a objeto devolvió NULL.")
                        _personalMessageState.value = PersonalMessageState.Empty
                    }
                }
            }
    }

    fun markNotificationAsRead(notificationId: String) {
        if (notificationId.isBlank()) return
        viewModelScope.launch {
            try {
                firestore.collection("notifications").document(notificationId).update("isRead", true).await()
            } catch (e: Exception) { Log.e(TAG, "Error al marcar notif como leída", e) }
        }
    }

    // --- CAMBIO 2: Renombramos la función y cambiamos su lógica ---
    // Ya no archivamos, solo marcamos como leído.
    fun markPersonalMessageAsRead(messageId: String) {
        if (messageId.isBlank()) return
        viewModelScope.launch {
            try {
                firestore.collection("personal_messages").document(messageId).update("isRead", true).await()
                Log.d(TAG, "Mensaje $messageId marcado como leído.")
            } catch (e: Exception) {
                Log.e(TAG, "Error al marcar mensaje como leído", e)
            }
        }
    }

    private fun clearListenersAndSetEmptyState() {
        notificationsListener?.remove()
        personalMessageListener?.remove()
        _notificationsState.value = NotificationsState.Success(emptyList())
        _personalMessageState.value = PersonalMessageState.Empty
    }

    override fun onCleared() {
        super.onCleared()
        clearListenersAndSetEmptyState()
    }
}