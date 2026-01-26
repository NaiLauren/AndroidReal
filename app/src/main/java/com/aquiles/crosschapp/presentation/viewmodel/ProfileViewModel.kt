package com.aquiles.crosschapp.presentation.viewmodel

import android.app.Application
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.AttendanceRecord
import com.aquiles.crosschapp.data.model.CreditRequest
import com.aquiles.crosschapp.data.model.CreditTransaction
import com.aquiles.crosschapp.data.model.GymClass
import com.aquiles.crosschapp.data.model.User
import com.google.firebase.auth.FirebaseAuth
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
import java.util.Calendar
import java.util.Date

// --- ESTADOS (Iguales que antes) ---
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

data class EnrichedAttendanceRecord(
    val record: AttendanceRecord,
    val classDetails: GymClass?
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    // --- CONFIGURACIÓN LEGAL ---
    private val currentTermsVersion = "1.0"

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _userState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val userState: StateFlow<ProfileState> = _userState.asStateFlow()

    private val _profileUpdateState = MutableStateFlow<ProfileUpdateState>(ProfileUpdateState.Idle)
    val profileUpdateState: StateFlow<ProfileUpdateState> = _profileUpdateState.asStateFlow()

    // Estados de historiales
    private val _transactionHistoryState = MutableStateFlow<TransactionHistoryState>(TransactionHistoryState.Loading)
    val transactionHistoryState = _transactionHistoryState.asStateFlow()

    private val _creditHistoryState = MutableStateFlow<CreditHistoryState>(CreditHistoryState.Loading)
    val creditHistoryState = _creditHistoryState.asStateFlow()

    private val _attendanceHistoryState = MutableStateFlow<AttendanceHistoryState>(AttendanceHistoryState.Loading)
    val attendanceHistoryState = _attendanceHistoryState.asStateFlow()

    // LISTENERS (Para limpieza)
    private var userListener: ListenerRegistration? = null
    private var bookingsListener: ListenerRegistration? = null
    private var transactionsListener: ListenerRegistration? = null
    private var requestsListener: ListenerRegistration? = null
    private var attendanceListener: ListenerRegistration? = null // NUEVO LISTENER

    private var currentListeningUserId: String? = null

    init {
        listenToUserProfile()
    }

    // --- HELPER PARA SABER SI EL WAIVER ESTÁ ACTIVO ---
    fun isWaiverActive(user: User): Boolean {
        val signedDate = user.waiverDate ?: return false
        val accepted = user.waiverAccepted ?: false
        if (!accepted) return false

        val cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, -1)
        val oneYearAgo = cal.time

        return signedDate.after(oneYearAgo)
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
                    cleanupListeners()
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
            .whereEqualTo("gym_id", gymId)
            .whereEqualTo("userId", userId)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                val list = snapshots?.toObjects(CreditTransaction::class.java) ?: emptyList()
                _transactionHistoryState.value = if (list.isEmpty()) TransactionHistoryState.Empty else TransactionHistoryState.Success(list)
            }

        // 3. Requests
        requestsListener?.remove()
        requestsListener = firestore.collection("creditRequests")
            .whereEqualTo("gym_id", gymId)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
                val rawList = snapshots?.toObjects(CreditRequest::class.java) ?: emptyList()
                val sortedList = rawList.sortedByDescending { it.requestDate }
                _creditHistoryState.value = if (sortedList.isEmpty()) CreditHistoryState.Empty else CreditHistoryState.Success(sortedList)
            }

        // 4. Attendance (AHORA EN TIEMPO REAL)
        loadAttendanceHistoryRealtime(userId, gymId)
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
                        doc.toObject(GymClass::class.java)?.copy(documentId = doc.id)
                    } ?: emptyList()
                    _userState.value = ProfileState.Success(user, bookings)
                } catch (e: Exception) {
                    _userState.value = ProfileState.Success(user, emptyList())
                }
            }
    }

    // --- CAMBIO IMPORTANTE: AHORA USA SNAPSHOT LISTENER ---
    private fun loadAttendanceHistoryRealtime(userId: String, gymId: String) {
        attendanceListener?.remove()
        _attendanceHistoryState.value = AttendanceHistoryState.Loading

        attendanceListener = firestore.collection("attendance_history")
            .whereEqualTo("gym_id", gymId)
            .whereEqualTo("userId", userId)
            // Opcional: ordenar por fecha si tienes el índice compuesto en Firebase
            // .orderBy("classDate", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    _attendanceHistoryState.value = AttendanceHistoryState.Empty
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    val rawRecords = snapshots.toObjects(AttendanceRecord::class.java)
                    // Ordenamos en memoria si no hay índice
                    val sortedRecords = rawRecords.sortedByDescending { it.classDate }

                    // Como necesitamos buscar el nombre de la clase (async), lanzamos corrutina
                    viewModelScope.launch {
                        val enrichedDeferred = sortedRecords.map { record ->
                            async {
                                var classDetails: GymClass? = null
                                if (record.classId.isNotBlank()) {
                                    try {
                                        // Esto es rápido porque Firestore cachea lecturas recientes
                                        val classDoc = firestore.collection("gymClasses").document(record.classId).get().await()
                                        classDetails = classDoc.toObject(GymClass::class.java)
                                    } catch (e: Exception) { }
                                }
                                EnrichedAttendanceRecord(record, classDetails)
                            }
                        }
                        val enrichedList = enrichedDeferred.awaitAll()
                        _attendanceHistoryState.value = AttendanceHistoryState.Success(enrichedList)
                    }
                } else {
                    _attendanceHistoryState.value = AttendanceHistoryState.Empty
                }
            }
    }

    // --- SUBIDA DE IMAGEN ---
    fun uploadProfileImage(uri: Uri) {
        viewModelScope.launch {
            _profileUpdateState.value = ProfileUpdateState.Loading
            val user = (userState.value as? ProfileState.Success)?.user ?: return@launch

            try {
                val contentResolver = getApplication<Application>().contentResolver
                val inputStream = contentResolver.openInputStream(uri)

                if (inputStream != null) {
                    val ref = storage.reference.child("profile_images/${user.id}/profile.jpg")
                    ref.putStream(inputStream).await()
                    val url = ref.downloadUrl.await().toString()
                    firestore.collection("users").document(user.id).update("profileImageUrl", url).await()
                    inputStream.close()
                    _profileUpdateState.value = ProfileUpdateState.Success
                } else {
                    _profileUpdateState.value = ProfileUpdateState.Error("No se pudo abrir la imagen.")
                }
            } catch (e: Exception) {
                _profileUpdateState.value = ProfileUpdateState.Error(e.message ?: "Error al subir imagen")
            }
        }
    }

    // --- UPDATE PERFIL ---
    fun updateUserProfile(
        name: String,
        lastName: String,
        phoneNumber: String?,
        emergencyContact: String?,
        birthDate: Date?,
        hasHeartCondition: Boolean,
        hasInjuries: Boolean,
        medicalNotes: String,
        waiverAccepted: Boolean
    ) {
        viewModelScope.launch {
            _profileUpdateState.value = ProfileUpdateState.Loading
            val user = (userState.value as? ProfileState.Success)?.user ?: return@launch

            try {
                val updates = hashMapOf<String, Any?>(
                    "name" to name,
                    "lastName" to lastName,
                    "phoneNumber" to phoneNumber,
                    "emergencyContact" to emergencyContact,
                    "birthDate" to birthDate,
                    "hasHeartCondition" to hasHeartCondition,
                    "hasInjuries" to hasInjuries,
                    "medicalNotes" to medicalNotes
                )

                val wasActive = isWaiverActive(user)
                if (waiverAccepted && (! (user.waiverAccepted ?: false) || !wasActive)) {
                    updates["waiverAccepted"] = true
                    updates["waiverDate"] = Date()
                    updates["waiverVersion"] = currentTermsVersion
                    val deviceInfo = "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})"
                    updates["waiverDevice"] = deviceInfo
                }

                firestore.collection("users").document(user.id).update(updates).await()
                _profileUpdateState.value = ProfileUpdateState.Success
            } catch (e: Exception) {
                _profileUpdateState.value = ProfileUpdateState.Error(e.message ?: "Error al actualizar")
            }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val user = (userState.value as? ProfileState.Success)?.user ?: return@launch
            try {
                firestore.collection("users").document(user.id).delete().await()
                try { storage.reference.child("profile_images/${user.id}/profile.jpg").delete().await() } catch (e: Exception) {}
                auth.currentUser?.delete()?.await()
                UserSession.endSession()
            } catch (e: Exception) {
                UserSession.endSession()
            }
        }
    }

    fun resetProfileUpdateState() {
        _profileUpdateState.value = ProfileUpdateState.Idle
    }

    private fun cleanupListeners() {
        userListener?.remove()
        bookingsListener?.remove()
        transactionsListener?.remove()
        requestsListener?.remove()
        attendanceListener?.remove() // Limpiamos el nuevo listener
    }

    override fun onCleared() {
        cleanupListeners()
        super.onCleared()
    }
}