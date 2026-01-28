// RUTA: presentation/viewmodel/AuthViewModel.kt
// VERSIÓN RESTAURADA CON LLAMADAS A UserSession.startSession

package com.aquiles.crosschapp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.Gym
import com.aquiles.crosschapp.data.model.MyFirebaseMessagingService
import com.aquiles.crosschapp.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val user: FirebaseUser?) : AuthState()
    data class NeedsProfileCompletion(val user: FirebaseUser, val name: String?, val email: String?) : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class PasswordResetState {
    object Idle : PasswordResetState()
    object Loading : PasswordResetState()
    data class Success(val message: String) : PasswordResetState()
    data class Error(val message: String) : PasswordResetState()
}

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    private val _passwordResetState = MutableStateFlow<PasswordResetState>(PasswordResetState.Idle)
    val passwordResetState = _passwordResetState.asStateFlow()

    private val _selectedGym = MutableStateFlow<Gym?>(null)
    val selectedGym = _selectedGym.asStateFlow()

    fun loadGymDetails(gymId: String) {
        viewModelScope.launch {
            try {
                val doc = firestore.collection("gyms").document(gymId).get().await()
                if (doc.exists()) {
                    _selectedGym.value = doc.toObject(Gym::class.java)?.copy(id = doc.id)
                } else {
                    Log.e("AuthViewModel", "No se encontró el gimnasio con ID: $gymId")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error al cargar detalles del gimnasio", e)
            }
        }
    }

    // --- GOOGLE SIGN IN ---
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user

                if (user != null) {
                    // Verificar si ya tiene perfil en Firestore
                    val doc = firestore.collection("users").document(user.uid).get().await()
                    if (doc.exists()) {
                        // Usuario existe y tiene perfil completo -> Login Exitoso
                        val userProfile = doc.toObject(User::class.java)
                        if (userProfile != null) {
                            UserSession.startSession(userProfile)
                            updateAndSaveFcmToken()
                            _authState.value = AuthState.Success(user)
                        } else {
                            _authState.value = AuthState.Error("Error al cargar perfil de usuario.")
                        }
                    } else {
                        // Usuario autenticado en Firebase pero sin documento en Firestore -> Necesita completar registro
                        // Extraemos datos básicos
                        val name = user.displayName?.split(" ")?.firstOrNull() ?: ""
                        val email = user.email ?: ""
                        _authState.value = AuthState.NeedsProfileCompletion(user, name, email)
                    }
                } else {
                    _authState.value = AuthState.Error("Error de autenticación con Google (Usuario nulo).")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Error al iniciar sesión con Google: ${e.message}")
            }
        }
    }

    fun completeSocialLoginRegistration(
        name: String,
        lastName: String,
        phoneNumber: String,
        gymId: String
    ) {
        val user = auth.currentUser
        if (user == null) {
            _authState.value = AuthState.Error("No hay sesión activa. Intenta ingresar con Google nuevamente.")
            return
        }
        if (gymId.isBlank()) {
            _authState.value = AuthState.Error("El ID del gimnasio es obligatorio.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val newUser = User(
                    id = user.uid,
                    email = user.email ?: "",
                    name = name.trim(),
                    lastName = lastName.trim(),
                    phoneNumber = phoneNumber.trim(),
                    gym_id = gymId,
                    role = "member",
                    profileImageUrl = user.photoUrl?.toString() // Usar foto de Google si existe
                )

                firestore.collection("users").document(user.uid).set(newUser).await()
                
                UserSession.startSession(newUser)
                updateAndSaveFcmToken()
                _authState.value = AuthState.Success(user)

            } catch (e: Exception) {
               _authState.value = AuthState.Error("Error al guardar perfil: ${e.message}")
            }
        }
    }
    // --- FIN GOOGLE SIGN IN ---


    fun registerUser(
        email: String,
        password: String,
        name: String,
        lastName: String,
        phoneNumber: String,
        gymId: String
    ) {
        if (gymId.isBlank()) {
            _authState.value = AuthState.Error("Error: El identificador del gimnasio es necesario para el registro.")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val firebaseUser = authResult.user

                firebaseUser?.let { user ->
                    val newUser = User(
                        id = user.uid,
                        email = user.email ?: "",
                        name = name.trim(),
                        lastName = lastName.trim(),
                        phoneNumber = phoneNumber.trim(),
                        gym_id = gymId,
                        role = "member"
                    )
                    firestore.collection("users")
                        .document(user.uid)
                        .set(newUser)
                        .await()

                    // --- ¡LÍNEA RESTAURADA! ---
                    UserSession.startSession(newUser)

                    updateAndSaveFcmToken()
                    _authState.value = AuthState.Success(user)

                } ?: run {
                    _authState.value = AuthState.Error("Error: Usuario de autenticación nulo.")
                }

            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Error desconocido al registrar")
            }
        }
    }

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val uid = result.user!!.uid
                val userDoc = firestore.collection("users").document(uid).get().await()

                if (userDoc.exists()) {
                    val userProfileFromDoc = userDoc.toObject(User::class.java)

                    if (userProfileFromDoc != null) {
                        // --- ¡LÍNEA RESTAURADA! ---
                        UserSession.startSession(userProfileFromDoc)

                        updateAndSaveFcmToken()
                        _authState.value = AuthState.Success(result.user)
                    } else {
                        _authState.value = AuthState.Error("Error al procesar el perfil del usuario.")
                        auth.signOut()
                    }
                } else {
                    _authState.value = AuthState.Error("No se pudo encontrar el perfil del usuario.")
                    auth.signOut()
                }

            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Error desconocido al iniciar sesión")
            }
        }
    }

    fun signOut() {
        auth.signOut()
        UserSession.endSession()
        _authState.value = AuthState.Idle
    }

    private suspend fun updateAndSaveFcmToken() {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            MyFirebaseMessagingService.sendTokenToFirestore(token)
        } catch (e: Exception) {
            Log.e("AuthViewModel", "No se pudo obtener/guardar el token FCM.", e)
        }
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _passwordResetState.value = PasswordResetState.Error("Por favor, introduce un email válido.")
            return
        }
        viewModelScope.launch {
            _passwordResetState.value = PasswordResetState.Loading
            try {
                auth.sendPasswordResetEmail(email).await()
                _passwordResetState.value = PasswordResetState.Success("¡Correo enviado! Revisa tu bandeja de entrada.")
            } catch (e: Exception) {
                _passwordResetState.value = PasswordResetState.Error(e.localizedMessage ?: "Error al enviar el correo.")
            }
        }
    }

    fun resetPasswordResetState() {
        _passwordResetState.value = PasswordResetState.Idle
    }
}