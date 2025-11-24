package com.aquiles.crosschapp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// Estado para la UI: Cargando, Éxito (con o sin URL), o Error.
sealed class SplashState {
    object Idle : SplashState()
    object Loading : SplashState()
    data class Success(val videoUrl: String?) : SplashState() // La URL puede ser nula
    data class Error(val message: String) : SplashState()
}

class DynamicSplashViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _splashState = MutableStateFlow<SplashState>(SplashState.Idle)
    val splashState: StateFlow<SplashState> = _splashState.asStateFlow()

    fun loadVideoUrl() {
        // Evita que se cargue múltiples veces si la función es llamada de nuevo
        if (_splashState.value != SplashState.Idle) return

        viewModelScope.launch {
            _splashState.value = SplashState.Loading

            val gymId = UserSession.currentUser.value?.gym_id
            if (gymId.isNullOrBlank()) {
                Log.w("DynamicSplashVM", "No se encontró gym_id en la sesión del usuario. Saltando video.")
                // Es un éxito, pero sin video. La UI sabrá cómo manejar esto.
                _splashState.value = SplashState.Success(null)
                return@launch
            }

            try {
                Log.d("DynamicSplashVM", "Buscando video para el gym_id: $gymId")
                val document = firestore.collection("gyms").document(gymId).get().await()

                if (document.exists()) {
                    val url = document.getString("splashVideoUrl")
                    Log.d("DynamicSplashVM", "URL encontrada: $url")
                    _splashState.value = SplashState.Success(url)
                } else {
                    Log.w("DynamicSplashVM", "No se encontró el documento para el gym_id: $gymId. Saltando video.")
                    _splashState.value = SplashState.Success(null)
                }
            } catch (e: Exception) {
                Log.e("DynamicSplashVM", "Error al obtener la URL del video.", e)
                // En caso de error de red, también saltamos el video para no bloquear al usuario.
                _splashState.value = SplashState.Error("Error de red. Saltando video.")
            }
        }
    }
}