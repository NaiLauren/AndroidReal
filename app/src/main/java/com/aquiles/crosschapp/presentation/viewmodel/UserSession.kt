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
        // Actualizamos el objeto user antes de ponerlo en el StateFlow
        _currentUser.value = user.copy(isAdmin = hasAdminPermissions)
    }

    /**
     * Termina la sesión.
     */
    fun endSession() {
        _currentUser.value = null
    }

    /**
     * Función de ayuda para obtener el ID del usuario actual.
     */
    fun getCurrentUserId(): String? = _currentUser.value?.id
}