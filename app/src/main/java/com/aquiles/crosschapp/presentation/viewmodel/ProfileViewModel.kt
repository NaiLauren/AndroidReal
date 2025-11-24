package com.aquiles.crosschapp.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.AttendanceRecord
import com.aquiles.crosschapp.data.model.CreditRequest
import com.aquiles.crosschapp.data.model.CreditTransaction
import com.aquiles.crosschapp.data.model.GymClass
import com.aquiles.crosschapp.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

// --- ESTADOS ---
sealed class ProfileState {
    data object Idle : ProfileState()
    data object Loading : ProfileState()
    data class Success(val user: User, val activeBookings: List<GymClass>) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

sealed class ProfileUpdateState {
    data object Idle : ProfileUpdateState()
    data object Loading : ProfileUpdateState()
    data object Success : ProfileUpdateState()
    data class Error(val message: String) : ProfileUpdateState()
}

sealed class TransactionHistoryState {
    data object Loading : TransactionHistoryState()
    data class Success(val transactions: List<CreditTransaction>) : TransactionHistoryState()
    data object Empty : TransactionHistoryState()
}

sealed class CreditHistoryState {
    data object Loading : CreditHistoryState()
    data class Success(val requests: List<CreditRequest>) : CreditHistoryState()
    data object Empty : CreditHistoryState()
}

sealed class AttendanceHistoryState {
    data object Loading : AttendanceHistoryState()
    data class Success(val records: List<EnrichedAttendanceRecord>) : AttendanceHistoryState()
    data object Empty : AttendanceHistoryState()
}

// Wrapper para mostrar datos combinados en la UI
data class EnrichedAttendanceRecord(
    val record: AttendanceRecord,
    val classDetails: GymClass?
)

class ProfileViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _userState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val userState: StateFlow<ProfileState> = _userState.asStateFlow()

    private val _profileUpdateState = MutableStateFlow<ProfileUpdateState>(ProfileUpdateState.Idle)
    val profileUpdateState: StateFlow<ProfileUpdateState> = _profileUpdateState.asStateFlow()

    private val _transactionHistoryState = MutableStateFlow<TransactionHistoryState>(TransactionHistoryState.Loading)
    val transactionHistoryState = _transactionHistoryState.asStateFlow()

    private val _creditHistoryState = MutableStateFlow<CreditHistoryState>(CreditHistoryState.Loading)
    val creditHistoryState = _creditHistoryState.asStateFlow()

    private val _attendanceHistoryState = MutableStateFlow<AttendanceHistoryState>(AttendanceHistoryState.Loading)
    val attendanceHistoryState = _attendanceHistoryState.asStateFlow()

    private var userListener: ListenerRegistration? = null
    private var bookingsListener: ListenerRegistration? = null
    private var transactionsListener: ListenerRegistration? = null
    private var requestsListener: ListenerRegistration? = null

    private var currentListeningUserId: String? = null

    init {
        listenToUserProfile()
    }

    private fun listenToUserProfile() {
        viewModelScope.launch {
            UserSession.currentUser.collect { currentUser ->
                if (currentUser != null) {
                    if (currentListeningUserId != currentUser.id) {
                        currentListeningUserId = currentUser.id
                        setupRealtimeListeners(currentUser.id, currentUser.gym_id)
                    }
                } else {
                    onCleared()
                    currentListeningUserId = null
                }
            }
        }
    }

    private fun setupRealtimeListeners(userId: String, gymId: String) {
        // 1. User Data
        userListener?.remove()
        userListener = firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _userState.value = ProfileState.Error(e.message ?: "Error al cargar perfil")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val updatedUser = snapshot.toObject(User::class.java)
                    if (updatedUser != null) {
                        listenToActiveBookings(updatedUser)
                    }
                }
            }

        // 2. Transactions
        transactionsListener?.remove()
        transactionsListener = firestore.collection("credit_transactions")
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshots, _ ->
                val list = snapshots?.toObjects(CreditTransaction::class.java) ?: emptyList()
                _transactionHistoryState.value = if (list.isEmpty()) TransactionHistoryState.Empty else TransactionHistoryState.Success(list)
            }

        // 3. Requests
        requestsListener?.remove()
        requestsListener = firestore.collection("creditRequests")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                val rawList = snapshots?.toObjects(CreditRequest::class.java) ?: emptyList()
                val sortedList = rawList.sortedByDescending { it.requestDate }
                _creditHistoryState.value = if (sortedList.isEmpty()) CreditHistoryState.Empty else CreditHistoryState.Success(sortedList)
            }

        // 4. Attendance (CORREGIDO: Pasamos gymId)
        loadAttendanceHistory(userId, gymId)
    }

    private fun listenToActiveBookings(user: User) {
        bookingsListener?.remove()
        bookingsListener = firestore.collection("gymClasses")
            .whereEqualTo("gym_id", user.gym_id)
            .whereArrayContains("enrolledUserIds", user.id)
            .whereGreaterThan("dateTime", Date())
            .orderBy("dateTime", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    _userState.value = ProfileState.Success(user, emptyList())
                    return@addSnapshotListener
                }
                try {
                    val bookings = snapshots?.documents?.mapNotNull { doc ->
                        doc.toObject(GymClass::class.java)?.copy(id = doc.id)
                    } ?: emptyList()
                    _userState.value = ProfileState.Success(user, bookings)
                } catch (e: Exception) {
                    _userState.value = ProfileState.Success(user, emptyList())
                }
            }
    }

    // --- CORRECCIÓN: Agregar gym_id a la consulta ---
    private fun loadAttendanceHistory(userId: String, gymId: String) {
        viewModelScope.launch {
            _attendanceHistoryState.value = AttendanceHistoryState.Loading
            try {
                Log.d("ProfileVM", "Consultando asistencias -> Gym: $gymId, User: $userId")

                // 1. Obtener historial con FILTRO COMPLETO (Esto arregla el Permission Denied)
                val snapshot = firestore.collection("attendance_history")
                    .whereEqualTo("gym_id", gymId) // <--- ESTO FALTABA
                    .whereEqualTo("userId", userId)
                    .limit(50)
                    .get().await()

                Log.d("ProfileVM", "Asistencias encontradas: ${snapshot.documents.size}")

                val rawRecords = snapshot.toObjects(AttendanceRecord::class.java)
                val sortedRecords = rawRecords.sortedByDescending { it.classDate }

                if (sortedRecords.isEmpty()) {
                    _attendanceHistoryState.value = AttendanceHistoryState.Empty
                    return@launch
                }

                // 2. Enriquecer datos en PARALELO
                val enrichedDeferred = sortedRecords.map { record ->
                    async {
                        var classDetails: GymClass? = null
                        if (record.classId.isNotBlank()) {
                            try {
                                val classDoc = firestore.collection("gymClasses").document(record.classId).get().await()
                                classDetails = classDoc.toObject(GymClass::class.java)
                            } catch (e: Exception) {
                                Log.w("ProfileVM", "No se pudo cargar detalle clase: ${record.classId}")
                            }
                        }
                        EnrichedAttendanceRecord(record, classDetails)
                    }
                }

                val enrichedList = enrichedDeferred.awaitAll()

                _attendanceHistoryState.value = if (enrichedList.isEmpty()) AttendanceHistoryState.Empty else AttendanceHistoryState.Success(enrichedList)

            } catch (e: Exception) {
                Log.e("ProfileVM", "Error cargando asistencias", e)
                _attendanceHistoryState.value = AttendanceHistoryState.Empty
            }
        }
    }

    fun uploadProfileImage(uri: Uri) {
        viewModelScope.launch {
            _profileUpdateState.value = ProfileUpdateState.Loading
            val user = (userState.value as? ProfileState.Success)?.user ?: return@launch
            try {
                val ref = storage.reference.child("profile_images/${user.id}.jpg")
                ref.putFile(uri).await()
                val url = ref.downloadUrl.await().toString()
                firestore.collection("users").document(user.id).update("profileImageUrl", url).await()
                _profileUpdateState.value = ProfileUpdateState.Success
            } catch (e: Exception) {
                _profileUpdateState.value = ProfileUpdateState.Error(e.message ?: "Error al subir imagen")
            }
        }
    }

    fun updateUserProfile(name: String, lastName: String, phoneNumber: String?, emergencyContact: String?, birthDate: Date?) {
        viewModelScope.launch {
            _profileUpdateState.value = ProfileUpdateState.Loading
            val user = (userState.value as? ProfileState.Success)?.user ?: return@launch
            try {
                val updates = hashMapOf<String, Any?>(
                    "name" to name,
                    "lastName" to lastName,
                    "phoneNumber" to phoneNumber,
                    "emergencyContact" to emergencyContact,
                    "birthDate" to birthDate
                )
                firestore.collection("users").document(user.id).update(updates).await()
                _profileUpdateState.value = ProfileUpdateState.Success
            } catch (e: Exception) {
                _profileUpdateState.value = ProfileUpdateState.Error(e.message ?: "Error al actualizar")
            }
        }
    }

    fun resetProfileUpdateState() {
        _profileUpdateState.value = ProfileUpdateState.Idle
    }

    override fun onCleared() {
        userListener?.remove()
        bookingsListener?.remove()
        transactionsListener?.remove()
        requestsListener?.remove()
        super.onCleared()
    }
}