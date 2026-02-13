package com.aquiles.crosschapp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.common.Resource
import com.aquiles.crosschapp.data.model.Notification
import com.aquiles.crosschapp.data.model.PersonalMessage
import com.aquiles.crosschapp.data.repository.MessageRepository
import com.aquiles.crosschapp.data.repository.NotificationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

// --- Los Sealed Class se mantienen igual por ahora (se podrían mapear o reemplazar) ---
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

    // Inyección manual de dependencias (TODO: Usar Hilt en el futuro)
    private val notificationRepository = NotificationRepository()
    private val messageRepository = MessageRepository()
    
    private val TAG = "HomeViewModel_DEBUG"

    private val _notificationsState = MutableStateFlow<NotificationsState>(NotificationsState.Loading)
    val notificationsState: StateFlow<NotificationsState> = _notificationsState.asStateFlow()
    private val _personalMessageState = MutableStateFlow<PersonalMessageState>(PersonalMessageState.Loading)
    val personalMessageState: StateFlow<PersonalMessageState> = _personalMessageState.asStateFlow()

    private var notificationsJob: Job? = null
    private var personalMessageJob: Job? = null

    private val _userClasses = MutableStateFlow<List<com.aquiles.crosschapp.data.model.GymClass>>(emptyList())
    val userClasses: StateFlow<List<com.aquiles.crosschapp.data.model.GymClass>> = _userClasses.asStateFlow()

    private var classesJob: Job? = null

    init {
        Log.d(TAG, "ViewModel inicializado.")
        UserSession.currentUser
            .onEach { user ->
                Log.d(TAG, "Observando sesión. El usuario es: ${user?.id} en gym: ${user?.gym_id}")
                if (user != null && user.gym_id.isNotBlank()) {
                    Log.d(TAG, "Usuario VÁLIDO detectado. Iniciando listeners...")
                    listenForUnreadNotifications(user.id, user.gym_id)
                    listenForPersonalMessage(user.id, user.gym_id)
                    listenForUserClasses(user.id, user.gym_id)
                } else {
                    Log.d(TAG, "Usuario NULO o inválido. Limpiando listeners y estado.")
                    clearListenersAndSetEmptyState()
                }
            }
            .launchIn(viewModelScope)
    }

    private fun listenForUserClasses(userId: String, gymId: String) {
        classesJob?.cancel()
        
        // Calculate Today's Range
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDay = calendar.time
        
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.time

        // Firestore Query matches SchedulePlanner but with 'enrolledUserIds' filter locally or via query if array-contains supported
        // Array-contains is supported.
        classesJob = viewModelScope.launch {
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("gymClasses")
                .whereEqualTo("gym_id", gymId)
                .whereGreaterThanOrEqualTo("dateTime", com.google.firebase.Timestamp(startOfDay))
                .whereLessThan("dateTime", com.google.firebase.Timestamp(endOfDay))
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e(TAG, "Error listening for classes", e)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val allTodayClasses = snapshot.toObjects(com.aquiles.crosschapp.data.model.GymClass::class.java)
                        // Filter locally for enrollment to avoid complex composite index requirement (ArrayContains + Range)
                        val myClasses = allTodayClasses.filter { it.enrolledUserIds.contains(userId) && !it.isCancelled }
                            .sortedBy { it.dateTime }
                        _userClasses.value = myClasses
                    }
                }
        }
    }

    private fun listenForUnreadNotifications(userId: String, gymId: String) {
        notificationsJob?.cancel()
        notificationsJob = notificationRepository.getUnreadNotifications(userId, gymId)
            .onEach { result ->
                when(result) {
                    is Resource.Success -> {
                        _notificationsState.value = NotificationsState.Success(result.data ?: emptyList())
                    }
                    is Resource.Error -> {
                        _notificationsState.value = NotificationsState.Error(result.message ?: "Error desconocido")
                    }
                    is Resource.Loading -> {
                        // Opcional: _notificationsState.value = NotificationsState.Loading
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun listenForPersonalMessage(userId: String, gymId: String) {
        personalMessageJob?.cancel()
        personalMessageJob = messageRepository.getUnreadPersonalMessage(userId, gymId)
            .onEach { result ->
                when(result) {
                    is Resource.Success -> {
                        val msg = result.data
                        if (msg != null) {
                            _personalMessageState.value = PersonalMessageState.Success(msg)
                        } else {
                            _personalMessageState.value = PersonalMessageState.Empty
                        }
                    }
                    is Resource.Error -> {
                        _personalMessageState.value = PersonalMessageState.Error(result.message ?: "Error desconocido")
                    }
                    is Resource.Loading -> {
                        _personalMessageState.value = PersonalMessageState.Loading
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun markNotificationAsRead(notificationId: String) {
        if (notificationId.isBlank()) return
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }

    fun markPersonalMessageAsRead(messageId: String) {
        if (messageId.isBlank()) return
        viewModelScope.launch {
            messageRepository.markAsRead(messageId)
        }
    }

    private fun clearListenersAndSetEmptyState() {
        notificationsJob?.cancel()
        personalMessageJob?.cancel()
        classesJob?.cancel()
        _notificationsState.value = NotificationsState.Success(emptyList())
        _personalMessageState.value = PersonalMessageState.Empty
        _userClasses.value = emptyList()
    }

    override fun onCleared() {
        super.onCleared()
        clearListenersAndSetEmptyState()
    }
}
