// RUTA: presentation/viewmodel/UserSession.kt
// VERSIÓN ORIGINAL RESTAURADA - SIMPLE Y ESTABLE

package com.aquiles.crosschapp.presentation.viewmodel

import com.aquiles.crosschapp.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import com.aquiles.crosschapp.data.model.Gym
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object UserSession {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser = _currentUser.asStateFlow()

    // Derivamos los otros valores directamente del currentUser
    val currentUserGymId: StateFlow<String?> = currentUser.map { it?.gym_id }
        .stateIn(GlobalScope, SharingStarted.Eagerly, null)

    val currentUserRole: StateFlow<String?> = currentUser.map { it?.role }
        .stateIn(GlobalScope, SharingStarted.Eagerly, null)

    // Mantenemos isAdmin por retrocompatibilidad, también derivado
    val isAdmin: StateFlow<Boolean> = currentUser.map { it?.role == "owner" || it?.role == "coach" }
        .stateIn(GlobalScope, SharingStarted.Eagerly, false)

    /**
     * Inicia la sesión. Se llama explícitamente desde los ViewModels.
     */
    fun startSession(user: User) {
        val hasAdminPermissions = user.role == "owner" || user.role == "coach"
        _currentUser.value = user.copy(isAdmin = hasAdminPermissions)
        
        // Iniciar escucha del Gym para Theming
        listenToGym(user.gym_id)

        // SYNC TOKEN: Asegurar que el dispositivo actual tenga su token en Firestore
        // (Esto cubre logins en nuevos dispositivos donde onNewToken no dispara)
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            com.aquiles.crosschapp.data.model.MyFirebaseMessagingService.sendTokenToFirestore(token)
        }
    }

    /**
     * Termina la sesión.
     */
    fun endSession() {
        _currentUser.value = null
        gymListener?.remove()
        gymListener = null
        _currentGym.value = null
    }

    /**
     * Función de ayuda para obtener el ID del usuario actual.
     */
    fun getCurrentUserId(): String? = _currentUser.value?.id
    
    // --- GYM LISTENER FOR THEMING ---
    private val _currentGym = MutableStateFlow<Gym?>(null)
    val currentGym = _currentGym.asStateFlow()
    
    private var gymListener: ListenerRegistration? = null
    
    private fun listenToGym(gymId: String) {
        if (gymId.isBlank()) return
        gymListener?.remove()
        gymListener = FirebaseFirestore.getInstance().collection("gyms").document(gymId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null && snapshot.exists()) {
                    val rawData = snapshot.data
                    val colorField = rawData?.get("primary_color")
                    android.util.Log.d("UserSession", "Gym Update Raw: $rawData")
                    android.util.Log.d("UserSession", "Gym Update Color Field: $colorField")
                    
                    val gym = snapshot.toObject(Gym::class.java)
                    android.util.Log.d("UserSession", "Gym Deserialized: $gym")
                    _currentGym.value = gym
                }
            }
    }
}