package com.aquiles.crosschapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.aquiles.crosschapp.data.model.PersonalMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Esta clase sellada define los posibles estados de la pantalla de archivo de mensajes
sealed class MessageHistoryState {
    object Loading : MessageHistoryState()
    data class Success(val messages: List<PersonalMessage>) : MessageHistoryState()
    object Empty : MessageHistoryState()
    data class Error(val message: String) : MessageHistoryState()
}

class MessageArchiveViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private var messagesListener: ListenerRegistration? = null

    private val _messagesState = MutableStateFlow<MessageHistoryState>(MessageHistoryState.Loading)
    val messagesState: StateFlow<MessageHistoryState> = _messagesState.asStateFlow()

    init {
        listenForMessages()
    }

    private fun listenForMessages() {
        viewModelScope.launch {
            val userId = UserSession.getCurrentUserId()
            if (userId.isNullOrBlank()) {
                _messagesState.value = MessageHistoryState.Error("No se pudo identificar al usuario.")
                return@launch
            }

            messagesListener?.remove()

            val query = firestore.collection("personal_messages")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)

            messagesListener = query.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _messagesState.value = MessageHistoryState.Error(error.localizedMessage ?: "Error al cargar mensajes.")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.toObjects(PersonalMessage::class.java)
                    if (messages.isEmpty()) {
                        _messagesState.value = MessageHistoryState.Empty
                    } else {
                        _messagesState.value = MessageHistoryState.Success(messages)
                    }
                }
            }
        }
    }
    fun deleteMessage(messageId: String) {
        if (messageId.isBlank()) {
            Log.e("MessageArchiveVM", "Se intentó borrar un mensaje con ID vacío.")
            return
        }

        viewModelScope.launch {
            firestore.collection("personal_messages").document(messageId)
                .delete()
                .addOnSuccessListener {
                    Log.d("MessageArchiveVM", "Mensaje $messageId borrado exitosamente.")
                    // ¡No necesitas hacer nada más! El listener se encargará de actualizar la lista automáticamente.
                }
                .addOnFailureListener { e ->
                    Log.w("MessageArchiveVM", "Error al borrar el mensaje $messageId", e)
                    // Aquí podrías mostrar un mensaje de error al usuario si quisieras.
                }
        }
    }
    override fun onCleared() {
        super.onCleared()
        messagesListener?.remove()
    }
}