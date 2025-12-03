package com.aquiles.crosschapp.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.R
import com.aquiles.crosschapp.data.model.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

sealed class SendMessageState {
    object Idle : SendMessageState()
    object Loading : SendMessageState()
    data class Success(val message: String) : SendMessageState()
    data class Error(val message: String) : SendMessageState()
}

sealed class BenchmarkOperationState {
    object Idle : BenchmarkOperationState()
    object Loading : BenchmarkOperationState()
    data class Success(val message: String) : BenchmarkOperationState()
    data class Error(val message: String) : BenchmarkOperationState()
}

class AdminViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private var userListListener: com.google.firebase.firestore.ListenerRegistration? = null
    private val storage = FirebaseStorage.getInstance()

    private val currentUserGymId: String?
        get() = UserSession.currentUserGymId.value

    private val _requestsState = MutableStateFlow<CreditRequestsListState>(CreditRequestsListState.Idle)
    val requestsState: StateFlow<CreditRequestsListState> = _requestsState.asStateFlow()
    private val _updateState = MutableStateFlow<RequestUpdateState>(RequestUpdateState.Idle)
    val updateState: StateFlow<RequestUpdateState> = _updateState.asStateFlow()
    private val _classListState = MutableStateFlow<ClassListState>(ClassListState.Idle)
    val classListState: StateFlow<ClassListState> = _classListState.asStateFlow()
    private val _classOperationState = MutableStateFlow<ClassOperationState>(ClassOperationState.Idle)
    val classOperationState: StateFlow<ClassOperationState> = _classOperationState.asStateFlow()
    private val _wodOperationState = MutableStateFlow<WodOperationState>(WodOperationState.Idle)
    val wodOperationState: StateFlow<WodOperationState> = _wodOperationState.asStateFlow()
    private val _wodDetailsState = MutableStateFlow<WodDetailsState>(WodDetailsState.Idle)
    val wodDetailsState: StateFlow<WodDetailsState> = _wodDetailsState.asStateFlow()
    private val _appConfigState = MutableStateFlow<AppConfigState>(AppConfigState.Loading)
    val appConfigState: StateFlow<AppConfigState> = _appConfigState.asStateFlow()
    private val _scheduleTemplateState = MutableStateFlow<List<String>>(emptyList())
    val scheduleTemplateState = _scheduleTemplateState.asStateFlow()
    private val _scheduleOperationState = MutableStateFlow<String?>(null)
    val scheduleOperationState = _scheduleOperationState.asStateFlow()
    private val _benchmarkWodsState = MutableStateFlow<BenchmarkWodsState>(BenchmarkWodsState.Loading)
    val benchmarkWodsState: StateFlow<BenchmarkWodsState> = _benchmarkWodsState.asStateFlow()
    private val _pendingRequestsCount = MutableStateFlow(0)
    val pendingRequestsCount = _pendingRequestsCount.asStateFlow()
    private val _wodsDashboardState = MutableStateFlow<WodsDashboardState>(WodsDashboardState.Idle)
    val wodsDashboardState: StateFlow<WodsDashboardState> = _wodsDashboardState.asStateFlow()
    private val _userListState = MutableStateFlow<UserListState>(UserListState.Loading)
    val userListState: StateFlow<UserListState> = _userListState.asStateFlow()
    private val _attendeeListState = MutableStateFlow<AttendeeListState>(AttendeeListState.Loading)
    val attendeeListState: StateFlow<AttendeeListState> = _attendeeListState.asStateFlow()
    private val _userDetailsState = MutableStateFlow<UserDetailsState>(UserDetailsState.Idle)
    val userDetailsState: StateFlow<UserDetailsState> = _userDetailsState.asStateFlow()
    private val _classForEditState = MutableStateFlow<ClassForEditState>(ClassForEditState.Idle)
    val classForEditState: StateFlow<ClassForEditState> = _classForEditState.asStateFlow()
    private val _reportsState = MutableStateFlow<ReportsState>(ReportsState.Idle)
    val reportsState: StateFlow<ReportsState> = _reportsState.asStateFlow()
    private val _creditPacksState = MutableStateFlow<CreditPacksState>(CreditPacksState.Idle)
    val creditPacksState: StateFlow<CreditPacksState> = _creditPacksState.asStateFlow()
    private val _billingRulesState = MutableStateFlow<BillingRulesState>(BillingRulesState.Loading)
    val billingRulesState: StateFlow<BillingRulesState> = _billingRulesState.asStateFlow()
    private val _sendMessageState = MutableStateFlow<SendMessageState>(SendMessageState.Idle)
    val sendMessageState = _sendMessageState.asStateFlow()
    private val _benchmarkOperationState = MutableStateFlow<BenchmarkOperationState>(BenchmarkOperationState.Idle)
    val benchmarkOperationState: StateFlow<BenchmarkOperationState> = _benchmarkOperationState.asStateFlow()

    init {
        listenForPendingRequests()
    }

    private fun executeAdminAction(
        errorStateSetter: (String) -> Unit,
        action: suspend CoroutineScope.(adminUser: User, gymId: String) -> Unit
    ) {
        viewModelScope.launch {
            val adminUser = UserSession.currentUser.value
            val gymId = currentUserGymId

            if (adminUser != null && (adminUser.role == "owner" || adminUser.role == "coach") && !gymId.isNullOrBlank()) {
                action(this, adminUser, gymId)
            } else {
                errorStateSetter("Error de permisos o de identificación del gimnasio. Tu sesión pudo haber expirado.")
            }
        }
    }

    fun sendPersonalMessage(
        targetUserId: String,
        content: String,
        attachmentUri: Uri?,
        context: Context
    ) {
        executeAdminAction(
            errorStateSetter = { _sendMessageState.value = SendMessageState.Error(it) }
        ) { adminUser, gymId ->
            _sendMessageState.value = SendMessageState.Loading

            if (content.isBlank() && attachmentUri == null) {
                _sendMessageState.value = SendMessageState.Error("El mensaje debe tener texto o un archivo adjunto.")
                return@executeAdminAction
            }

            try {
                var attachmentUrl: String? = null
                var attachmentType: String? = null

                if (attachmentUri != null) {
                    val fileExtension = getFileExtension(context, attachmentUri)
                    val fileName = "${System.currentTimeMillis()}.$fileExtension"
                    val storageRef = storage.reference.child("personal_message_attachments/$gymId/$targetUserId/$fileName")

                    storageRef.putFile(attachmentUri).await()
                    attachmentUrl = storageRef.downloadUrl.await().toString()

                    attachmentType = when {
                        fileExtension?.contains("pdf", ignoreCase = true) == true -> "pdf"
                        fileExtension?.contains("jp", ignoreCase = true) == true -> "image"
                        fileExtension?.contains("png", ignoreCase = true) == true -> "image"
                        else -> "file"
                    }
                }

                val messageDocRef = firestore.collection("personal_messages").document()
                val newMessage = PersonalMessage(
                    id = messageDocRef.id,
                    gym_id = gymId,
                    userId = targetUserId,
                    sender_id = adminUser.id,
                    sender_name = adminUser.name,
                    content = content.trim(),
                    isRead = false,
                    attachmentUrl = attachmentUrl,
                    attachmentType = attachmentType
                )

                messageDocRef.set(newMessage).await()
                _sendMessageState.value = SendMessageState.Success("Mensaje enviado correctamente.")

            } catch (e: Exception) {
                _sendMessageState.value = SendMessageState.Error(e.localizedMessage ?: "Error al enviar el mensaje.")
                Log.e("AdminViewModel", "Error en sendPersonalMessage", e)
            }
        }
    }

    fun sendBroadcastMessage(
        targetUserIds: List<String>,
        content: String,
        attachmentUri: Uri?,
        context: Context
    ) {
        executeAdminAction(
            errorStateSetter = { _sendMessageState.value = SendMessageState.Error(it) }
        ) { adminUser, gymId ->
            _sendMessageState.value = SendMessageState.Loading

            if (targetUserIds.isEmpty()) {
                _sendMessageState.value = SendMessageState.Error("No se seleccionó ningún destinatario.")
                return@executeAdminAction
            }
            if (content.isBlank() && attachmentUri == null) {
                _sendMessageState.value = SendMessageState.Error("El mensaje debe tener texto o un archivo adjunto.")
                return@executeAdminAction
            }

            try {
                var attachmentUrl: String? = null
                var attachmentType: String? = null

                if (attachmentUri != null) {
                    val fileExtension = getFileExtension(context, attachmentUri)
                    val fileName = "broadcast_${System.currentTimeMillis()}.$fileExtension"
                    val storageRef = storage.reference.child("personal_message_attachments/$gymId/broadcasts/$fileName")

                    storageRef.putFile(attachmentUri).await()
                    attachmentUrl = storageRef.downloadUrl.await().toString()

                    attachmentType = when {
                        fileExtension?.contains("pdf", ignoreCase = true) == true -> "pdf"
                        fileExtension?.contains("jp", ignoreCase = true) == true -> "image"
                        fileExtension?.contains("png", ignoreCase = true) == true -> "image"
                        else -> "file"
                    }
                }

                val batch = firestore.batch()
                targetUserIds.forEach { targetUserId ->
                    val messageDocRef = firestore.collection("personal_messages").document()
                    val newMessage = PersonalMessage(
                        id = messageDocRef.id,
                        gym_id = gymId,
                        userId = targetUserId,
                        sender_id = adminUser.id,
                        sender_name = adminUser.name,
                        content = content.trim(),
                        isRead = false,
                        attachmentUrl = attachmentUrl,
                        attachmentType = attachmentType
                    )
                    batch.set(messageDocRef, newMessage)
                }

                batch.commit().await()
                _sendMessageState.value = SendMessageState.Success("Mensaje enviado a ${targetUserIds.size} alumnos.")

            } catch (e: Exception) {
                _sendMessageState.value = SendMessageState.Error(e.localizedMessage ?: "Error al enviar la difusión.")
                Log.e("AdminViewModel", "Error en sendBroadcastMessage", e)
            }
        }
    }

    private fun getFileExtension(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri)?.let {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
        }
    }

    fun resetSendMessageState() {
        _sendMessageState.value = SendMessageState.Idle
    }

    fun saveAttendance(classId: String, attendedUserIds: List<String>) {
        executeAdminAction(
            errorStateSetter = { _classOperationState.value = ClassOperationState.Error(it) }
        ) { _, gymId ->
            if (attendedUserIds.isEmpty()) {
                _classOperationState.value = ClassOperationState.Error("Ningún asistente seleccionado.")
                return@executeAdminAction
            }
            _classOperationState.value = ClassOperationState.Loading
            try {
                firestore.runTransaction { transaction ->
                    val classRef = firestore.collection("gymClasses").document(classId)
                    val classDoc = transaction.get(classRef)
                    if (classDoc.getBoolean("attendanceTaken") == true) {
                        throw IllegalStateException("La asistencia para esta clase ya fue registrada.")
                    }

                    val classDate = classDoc.getDate("dateTime")
                    val calendar = Calendar.getInstance()
                    var dayOfWeek = 0
                    var hourOfDay = 0
                    if (classDate != null) {
                        calendar.time = classDate
                        dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                        hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
                    }

                    attendedUserIds.forEach { userId ->
                        val newAttendanceRecord = AttendanceRecord(
                            classId = classId,
                            classDate = classDate,
                            dayOfWeek = dayOfWeek,
                            hourOfDay = hourOfDay,
                            userId = userId,
                            gym_id = gymId
                        )
                        val attendanceDocRef = firestore.collection("attendance_history").document()
                        transaction.set(attendanceDocRef, newAttendanceRecord)

                        val userRef = firestore.collection("users").document(userId)
                        transaction.update(userRef, "totalClassesAttended", FieldValue.increment(1))
                    }

                    transaction.update(classRef, "attendanceTaken", true)
                    transaction.update(classRef, "attendedUserIds", attendedUserIds)
                }.await()

                _classOperationState.value = ClassOperationState.Success("Asistencia guardada con éxito.")
            } catch (e: Exception) {
                _classOperationState.value = ClassOperationState.Error(e.localizedMessage ?: "Error al guardar la asistencia.")
            }
        }
    }

    fun approveRequest(request: CreditRequest) {
        executeAdminAction(
            errorStateSetter = { _updateState.value = RequestUpdateState.Error(it) }
        ) { adminUser, gymId ->
            _updateState.value = RequestUpdateState.Loading
            try {
                val userRef = firestore.collection("users").document(request.userId)
                val userDoc = userRef.get().await()
                val currentValidUntil = userDoc.getDate("creditValidUntil")

                val calendar = Calendar.getInstance()
                if (currentValidUntil != null && currentValidUntil.after(Date())) {
                    calendar.time = currentValidUntil
                } else {
                    calendar.time = Date()
                }
                calendar.add(Calendar.DAY_OF_YEAR, 30)
                val newValidUntilDate = calendar.time

                val batch = firestore.batch()

                val requestRef = firestore.collection("creditRequests").document(request.id)
                val requestUpdates = mapOf(
                    "status" to CreditRequestStatus.APPROVED.name,
                    "processedByAdminId" to adminUser.id,
                    "processedByAdminName" to adminUser.fullName,
                    "processedDate" to FieldValue.serverTimestamp()
                )
                batch.update(requestRef, requestUpdates)

                batch.update(userRef, "credits", FieldValue.increment(request.creditsRequested.toLong()))
                batch.update(userRef, "creditValidUntil", newValidUntilDate)

                val newTransaction = CreditTransaction(
                    userId = request.userId,
                    gym_id = gymId,
                    amount = request.creditsRequested,
                    type = "Adquisición",
                    description = "Aprobación de pack '${request.comboName}'"
                )
                val transactionRef = firestore.collection("credit_transactions").document()
                batch.set(transactionRef, newTransaction)

                val formattedDate = SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "ES")).format(newValidUntilDate)
                val notificationMessage = "Tu solicitud de \"${request.comboName}\" fue aprobada. Vencen el $formattedDate."
                val notification = Notification(
                    userId = request.userId,
                    gym_id = gymId,
                    title = "¡Créditos Aprobados!",
                    message = notificationMessage,
                    type = NotificationType.CREDIT_APPROVED.name
                )
                val notificationRef = firestore.collection("notifications").document()
                batch.set(notificationRef, notification)

                batch.commit().await()
                _updateState.value = RequestUpdateState.Success("Solicitud aprobada con éxito.")

            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error al aprobar la solicitud", e)
                _updateState.value = RequestUpdateState.Error(e.localizedMessage ?: "Error desconocido.")
            }
        }
    }

    fun rejectRequest(request: CreditRequest) {
        executeAdminAction(
            errorStateSetter = { _updateState.value = RequestUpdateState.Error(it) }
        ) { adminUser, gymId ->
            _updateState.value = RequestUpdateState.Loading
            try {
                val batch = firestore.batch()

                val requestRef = firestore.collection("creditRequests").document(request.id)
                val updates = mapOf(
                    "status" to CreditRequestStatus.REJECTED.name,
                    "processedByAdminId" to adminUser.id,
                    "processedByAdminName" to adminUser.fullName,
                    "processedDate" to FieldValue.serverTimestamp()
                )
                batch.update(requestRef, updates)

                val notificationMessage = "Tu solicitud de \"${request.comboName}\" fue rechazada."
                val notification = Notification(
                    userId = request.userId,
                    gym_id = gymId,
                    title = "Solicitud Rechazada",
                    message = notificationMessage,
                    type = NotificationType.CREDIT_REJECTED.name
                )
                val notificationRef = firestore.collection("notifications").document()
                batch.set(notificationRef, notification)

                batch.commit().await()
                _updateState.value = RequestUpdateState.Success("Solicitud rechazada.")

            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error al rechazar la solicitud", e)
                _updateState.value = RequestUpdateState.Error(e.localizedMessage ?: "Error desconocido.")
            }
        }
    }

    fun loadBillingRules() {
        viewModelScope.launch {
            _billingRulesState.value = BillingRulesState.Loading
            try {
                val doc = firestore.collection("settings").document("billing_rules").get().await()
                val rules = doc.toObject(BillingRules::class.java) ?: BillingRules()
                _billingRulesState.value = BillingRulesState.Success(rules)
            } catch (e: Exception) {
                _billingRulesState.value = BillingRulesState.Error(e.localizedMessage ?: "Error al cargar configuración")
            }
        }
    }

    fun setSurchargeStatus(isEnabled: Boolean) {
        viewModelScope.launch {
            _billingRulesState.value = BillingRulesState.Loading
            try {
                val newRules = BillingRules(isSurchargeActive = isEnabled)
                firestore.collection("settings").document("billing_rules").set(newRules).await()
                _billingRulesState.value = BillingRulesState.Success(newRules)
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error al actualizar las reglas de recargo", e)
                _billingRulesState.value = BillingRulesState.Error(e.localizedMessage ?: "Error al guardar")
                loadBillingRules()
            }
        }
    }

    fun loadAllCreditPacks() {
        viewModelScope.launch {
            _creditPacksState.value = CreditPacksState.Loading
            val gymId = currentUserGymId ?: run {
                _creditPacksState.value = CreditPacksState.Error("No se pudo identificar tu gimnasio.")
                return@launch
            }
            try {
                val snapshot = firestore.collection("creditPacks")
                    .whereEqualTo("gym_id", gymId)
                    .orderBy("order", Query.Direction.ASCENDING)
                    .get().await()

                val packs = snapshot.documents.mapNotNull { it.toObject(CreditPack::class.java)?.copy(id = it.id) }

                if (packs.isEmpty()) {
                    _creditPacksState.value = CreditPacksState.Empty
                } else {
                    _creditPacksState.value = CreditPacksState.Success(packs)
                }
            } catch (e: Exception) {
                _creditPacksState.value = CreditPacksState.Error(e.localizedMessage ?: "Error al cargar los packs.")
            }
        }
    }

    fun saveCreditPack(pack: CreditPack) {
        executeAdminAction(
            errorStateSetter = { Log.e("AdminViewModel", "Error al guardar pack: $it") }
        ) { _, gymId ->
            try {
                val docRef = if (pack.id.isNotBlank()) {
                    firestore.collection("creditPacks").document(pack.id)
                } else {
                    firestore.collection("creditPacks").document()
                }
                docRef.set(pack.copy(id = docRef.id, gym_id = gymId)).await()
                loadAllCreditPacks()
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error al guardar el pack", e)
            }
        }
    }

    fun deleteCreditPack(packId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("creditPacks").document(packId).delete().await()
                loadAllCreditPacks()
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error al eliminar el pack", e)
            }
        }
    }

    private fun listenForPendingRequests() {
        val gymId = currentUserGymId
        if (UserSession.currentUser.value?.role == "owner" && !gymId.isNullOrBlank()) {
            firestore.collection("creditRequests")
                .whereEqualTo("gym_id", gymId)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener { snapshots, e ->
                    if (e != null) {
                        Log.w("AdminViewModel", "Listen failed for admin.", e)
                        return@addSnapshotListener
                    }
                    _pendingRequestsCount.value = snapshots?.size() ?: 0
                }
        } else {
            _pendingRequestsCount.value = 0
        }
    }

    fun loadReportsForMonth(year: Int, month: Int) {
        executeAdminAction(
            errorStateSetter = { _reportsState.value = ReportsState.Error(it) }
        ) { _, gymId ->
            _reportsState.value = ReportsState.Loading
            try {
                val calendar = Calendar.getInstance()
                calendar.set(year, month - 1, 1, 0, 0, 0)
                val startOfMonth = calendar.time
                calendar.add(Calendar.MONTH, 1)
                val startOfNextMonth = calendar.time

                val revenueSnapshot = firestore.collection("creditRequests")
                    .whereEqualTo("gym_id", gymId)
                    .whereEqualTo("status", "APPROVED")
                    .whereGreaterThanOrEqualTo("processedDate", startOfMonth)
                    .whereLessThan("processedDate", startOfNextMonth)
                    .orderBy("processedDate", Query.Direction.DESCENDING)
                    .get().await()

                val transactions = revenueSnapshot.toObjects(CreditRequest::class.java)
                val totalRevenue = transactions.sumOf { it.amountPaid }
                val monthlyActiveUsers = transactions.map { it.userId }.distinct().count()

                _reportsState.value = ReportsState.Success(
                    totalRevenue = totalRevenue,
                    monthlyActiveUsers = monthlyActiveUsers,
                    monthlyTransactions = transactions
                )

            } catch (e: Exception) {
                _reportsState.value = ReportsState.Error(e.localizedMessage ?: "Error al cargar los reportes.")
                Log.e("AdminViewModel", "Error en loadReportsForMonth. ¿Faltan índices en Firestore?", e)
            }
        }
    }

    fun loadAllCreditRequests() {
        executeAdminAction(
            errorStateSetter = { _requestsState.value = CreditRequestsListState.Error(it) }
        ) { _, gymId ->
            _requestsState.value = CreditRequestsListState.Loading
            try {
                val pendingDeferred = async {
                    firestore.collection("creditRequests")
                        .whereEqualTo("gym_id", gymId)
                        .whereEqualTo("status", "PENDING")
                        .orderBy("requestDate", Query.Direction.ASCENDING)
                        .get().await()
                }

                val processedDeferred = async {
                    firestore.collection("creditRequests")
                        .whereEqualTo("gym_id", gymId)
                        .whereIn("status", listOf("APPROVED", "REJECTED"))
                        .orderBy("processedDate", Query.Direction.DESCENDING)
                        .limit(20)
                        .get().await()
                }

                val pendingSnapshot = pendingDeferred.await()
                val processedSnapshot = processedDeferred.await()

                val pendingList = pendingSnapshot.toObjects(CreditRequest::class.java)
                val processedList = processedSnapshot.toObjects(CreditRequest::class.java)

                if (pendingList.isEmpty() && processedList.isEmpty()) {
                    _requestsState.value = CreditRequestsListState.Empty
                } else {
                    _requestsState.value = CreditRequestsListState.Success(
                        pendingRequests = pendingList,
                        processedRequests = processedList
                    )
                }
            } catch (e: Exception) {
                _requestsState.value = CreditRequestsListState.Error(e.localizedMessage ?: "Error al cargar solicitudes.")
            }
        }
    }

    fun resetUpdateState() {
        _updateState.value = RequestUpdateState.Idle
    }

    fun loadWodsForDashboard() {
        viewModelScope.launch {
            _wodsDashboardState.value = WodsDashboardState.Loading
            val gymId = currentUserGymId ?: run {
                _wodsDashboardState.value = WodsDashboardState.Error("No se pudo identificar tu gimnasio.")
                return@launch
            }
            try {
                val today = LocalDate.now()
                val tomorrow = today.plusDays(1)
                val todayStart = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant())
                val tomorrowEnd = Date.from(tomorrow.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())
                val snapshot = firestore.collection("wods")
                    .whereEqualTo("gym_id", gymId)
                    .whereGreaterThanOrEqualTo("date", todayStart)
                    .whereLessThan("date", tomorrowEnd)
                    .get().await()
                val wods = snapshot.toObjects(Wod::class.java)
                val todayWod = wods.find { it.date?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()?.isEqual(today) == true }
                val tomorrowWod = wods.find { it.date?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()?.isEqual(tomorrow) == true }
                _wodsDashboardState.value = WodsDashboardState.Success(todayWod, tomorrowWod)
            } catch (e: Exception) {
                _wodsDashboardState.value = WodsDashboardState.Error(e.localizedMessage ?: "Error")
            }
        }
    }

    fun saveWod(originalWodId: String?, title: String, type: String, description: String, date: Date, scoreType: String, notes: String) {
        executeAdminAction(
            errorStateSetter = { _wodOperationState.value = WodOperationState.Error(it) }
        ) { _, gymId ->
            _wodOperationState.value = WodOperationState.Loading
            if (title.isBlank() || type.isBlank() || description.isBlank() || scoreType.isBlank()) {
                _wodOperationState.value = WodOperationState.Error("Todos los campos son obligatorios.")
                return@executeAdminAction
            }
            val wodId = originalWodId ?: firestore.collection("wods").document().id
            val wodData = Wod(id = wodId, title = title, type = type, description = description, date = date, scoreType = scoreType, notes = notes, gym_id = gymId)
            try {
                firestore.collection("wods").document(wodId).set(wodData).await()
                _wodOperationState.value = WodOperationState.Success(if (originalWodId == null) "WOD creado." else "WOD actualizado.")
                loadWodsForDashboard()
            } catch (e: Exception) {
                _wodOperationState.value = WodOperationState.Error(e.localizedMessage ?: "Error")
            }
        }
    }

    fun loadWodDetails(wodId: String?) {
        if (wodId == null) {
            _wodDetailsState.value = WodDetailsState.Success(null)
            return
        }
        viewModelScope.launch {
            _wodDetailsState.value = WodDetailsState.Loading
            try {
                val doc = firestore.collection("wods").document(wodId).get().await()
                _wodDetailsState.value = if (doc.exists()) WodDetailsState.Success(doc.toObject(Wod::class.java)) else WodDetailsState.Error("WOD no encontrado.")
            } catch (e: Exception) {
                _wodDetailsState.value = WodDetailsState.Error(e.localizedMessage ?: "Error")
            }
        }
    }

    fun deleteWod(wodId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("wods").document(wodId).delete().await()
                loadWodsForDashboard()
            } catch (e: Exception) {
                _wodOperationState.value = WodOperationState.Error(e.localizedMessage ?: "Error")
            }
        }
    }

    fun resetWodOperationState() {
        _wodOperationState.value = WodOperationState.Idle
    }

    fun clearWodDetails() {
        _wodDetailsState.value = WodDetailsState.Idle
    }

    private fun getTemplateRef() = firestore.collection("settings").document("schedule_template")

    fun loadScheduleTemplate() {
        viewModelScope.launch {
            try {
                val doc = getTemplateRef().get().await()
                _scheduleTemplateState.value = if (doc.exists()) (doc.get("available_times") as? List<*>)?.filterIsInstance<String>()?.sorted() ?: emptyList() else emptyList()
            } catch (e: Exception) {
                _scheduleOperationState.value = "Error: ${e.localizedMessage}"
            }
        }
    }

    fun addTimeToTemplate(time: String) {
        viewModelScope.launch {
            try {
                getTemplateRef().update("available_times", FieldValue.arrayUnion(time)).await()
                _scheduleOperationState.value = "Horario añadido."
                loadScheduleTemplate()
            } catch (e: Exception) {
                getTemplateRef().set(mapOf("available_times" to listOf(time))).await()
                _scheduleOperationState.value = "Horario añadido."
                loadScheduleTemplate()
            }
        }
    }

    fun removeTimeFromTemplate(time: String) {
        viewModelScope.launch {
            try {
                getTemplateRef().update("available_times", FieldValue.arrayRemove(time)).await()
                _scheduleOperationState.value = "Horario eliminado."
                loadScheduleTemplate()
            } catch (e: Exception) {
                _scheduleOperationState.value = "Error: ${e.localizedMessage}"
            }
        }
    }

    fun clearScheduleOperationMessage() {
        _scheduleOperationState.value = null
    }

    fun loadAppConfig() {
        viewModelScope.launch {
            _appConfigState.value = AppConfigState.Loading
            try {
                val doc = firestore.collection("settings").document("app_config").get().await()
                _appConfigState.value = if (doc.exists()) AppConfigState.Success(doc.get("wod_images_by_day") as? Map<String, String> ?: emptyMap()) else AppConfigState.Success(emptyMap())
            } catch (e: Exception) {
                _appConfigState.value = AppConfigState.Error("Error: ${e.message}")
            }
        }
    }

    fun loadBenchmarkWods() {
        viewModelScope.launch {
            _benchmarkWodsState.value = BenchmarkWodsState.Loading
            val gymId = currentUserGymId ?: run {
                _benchmarkWodsState.value = BenchmarkWodsState.Error("No se pudo identificar tu gimnasio.")
                return@launch
            }
            try {
                val snapshot = firestore.collection("benchmark_wods")
                    .whereEqualTo("gym_id", gymId)
                    .orderBy("name").get().await()
                _benchmarkWodsState.value = BenchmarkWodsState.Success(snapshot.toObjects(BenchmarkWod::class.java))
            } catch (e: Exception) {
                _benchmarkWodsState.value = BenchmarkWodsState.Error(e.localizedMessage ?: "Error al cargar Benchmarks.")
            }
        }
    }

    fun saveBenchmark(benchmark: BenchmarkWod) {
        executeAdminAction(
            errorStateSetter = { _benchmarkOperationState.value = BenchmarkOperationState.Error(it) }
        ) { _, gymId ->
            _benchmarkOperationState.value = BenchmarkOperationState.Loading
            if (benchmark.name.isBlank() || benchmark.description.isBlank()) {
                _benchmarkOperationState.value = BenchmarkOperationState.Error("El nombre y la descripción son obligatorios.")
                return@executeAdminAction
            }

            try {
                val docRef = if (benchmark.id.isNotBlank()) {
                    firestore.collection("benchmark_wods").document(benchmark.id)
                } else {
                    firestore.collection("benchmark_wods").document()
                }

                val benchmarkToSave = benchmark.copy(id = docRef.id, gym_id = gymId)

                docRef.set(benchmarkToSave).await()
                _benchmarkOperationState.value = BenchmarkOperationState.Success("Benchmark guardado correctamente.")
                loadBenchmarkWods()
            } catch (e: Exception) {
                _benchmarkOperationState.value = BenchmarkOperationState.Error(e.localizedMessage ?: "Error al guardar el benchmark.")
                Log.e("AdminViewModel", "Error en saveBenchmark", e)
            }
        }
    }

    fun deleteBenchmark(benchmarkId: String) {
        viewModelScope.launch {
            _benchmarkOperationState.value = BenchmarkOperationState.Loading
            try {
                firestore.collection("benchmark_wods").document(benchmarkId).delete().await()
                _benchmarkOperationState.value = BenchmarkOperationState.Success("Benchmark eliminado correctamente.")
                loadBenchmarkWods()
            } catch (e: Exception) {
                _benchmarkOperationState.value = BenchmarkOperationState.Error(e.localizedMessage ?: "Error al eliminar el benchmark.")
                Log.e("AdminViewModel", "Error en deleteBenchmark", e)
            }
        }
    }

    fun resetBenchmarkOperationState() {
        _benchmarkOperationState.value = BenchmarkOperationState.Idle
    }

    fun createWodAndClassesForDay(
        isWodType: Boolean,
        wodTitle: String,
        wodDescription: String,
        wodScoreType: String,
        otherClassName: String,
        otherClassDescription: String,
        date: Date,
        coachName: String,
        durationMinutes: Int,
        maxCapacity: Int,
        selectedTimes: List<String>,
        wodColor: String,   // <--- NUEVO PARÁMETRO
        otherColor: String  // <--- NUEVO PARÁMETRO
    ) {
        viewModelScope.launch {
            _classOperationState.value = ClassOperationState.Loading
            try {
                val user = UserSession.currentUser.value ?: throw IllegalStateException("No user")
                val batch = firestore.batch()

                var newWodId: String? = null
                if (isWodType) {
                    val wodRef = firestore.collection("wods").document()
                    newWodId = wodRef.id
                    val newWod = Wod(
                        id = newWodId,
                        gym_id = user.gym_id,
                        title = wodTitle,
                        description = wodDescription,
                        type = "WOD",
                        date = date,
                        scoreType = wodScoreType
                        // Se eliminó createdBy para evitar el error
                    )
                    batch.set(wodRef, newWod)
                }

                selectedTimes.forEach { time ->
                    val classRef = firestore.collection("gymClasses").document()

                    val calendar = Calendar.getInstance()
                    calendar.time = date
                    val (hour, minute) = time.split(":").map { it.toInt() }
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)
                    calendar.set(Calendar.SECOND, 0)
                    val combinedDateTime = calendar.time

                    val newClass = GymClass(
                        id = classRef.id,
                        gym_id = user.gym_id,
                        name = if (isWodType) "WOD" else otherClassName,
                        description = if (isWodType) "" else otherClassDescription,
                        dateTime = combinedDateTime,
                        durationMinutes = durationMinutes,
                        maxCapacity = maxCapacity,
                        coachName = coachName,
                        wodId = newWodId,
                        classType = if (isWodType) "WOD" else "Otra Clase",

                        hexColor = if (isWodType) wodColor else otherColor
                    )
                    batch.set(classRef, newClass)
                }
                batch.commit().await()
                _classOperationState.value = ClassOperationState.Success("Clases creadas correctamente")
                loadFutureClasses()
            } catch (e: Exception) {
                _classOperationState.value = ClassOperationState.Error(e.message ?: "Error al crear clases")
            }
        }
    }

    fun loadFutureClasses() {
        viewModelScope.launch {
            _classListState.value = ClassListState.Loading
            val gymId = currentUserGymId ?: run {
                _classListState.value = ClassListState.Error("No se pudo identificar tu gimnasio.")
                return@launch
            }
            try {
                val snapshot = firestore.collection("gymClasses")
                    .whereEqualTo("gym_id", gymId)
                    .whereGreaterThanOrEqualTo("dateTime", Date())
                    .orderBy("dateTime", Query.Direction.ASCENDING)
                    .get().await()
                _classListState.value = ClassListState.Success(snapshot.documents.mapNotNull { it.toObject(GymClass::class.java)?.copy(id = it.id) })
            } catch (e: Exception) {
                _classListState.value = ClassListState.Error(e.localizedMessage ?: "Error al cargar clases.")
            }
        }
    }

    fun updateClass(
        classId: String,
        wodId: String?,
        isWodType: Boolean,
        wodTitle: String,
        wodDescription: String,
        wodScoreType: String,
        otherClassName: String,
        otherClassDescription: String,
        date: Date,
        coachName: String,
        durationMinutes: Int,
        maxCapacity: Int,
        colorCode: String // <--- NUEVO PARÁMETRO
    ) {
        viewModelScope.launch {
            _classOperationState.value = ClassOperationState.Loading
            try {
                val batch = firestore.batch()

                val classRef = firestore.collection("gymClasses").document(classId)
                // Usamos HashMap<String, Any?> para permitir nulos si fuera necesario
                val classUpdates = hashMapOf<String, Any?>(
                    "dateTime" to date,
                    "coachName" to coachName,
                    "durationMinutes" to durationMinutes,
                    "maxCapacity" to maxCapacity,
                    "classType" to if (isWodType) "WOD" else "Otra Clase",
                    "name" to if (isWodType) "WOD" else otherClassName,
                    "description" to if (isWodType) "" else otherClassDescription,
                    "hexColor" to colorCode
                )

                if (!isWodType) {
                    classUpdates["wodId"] = null
                }

                batch.update(classRef, classUpdates)

                if (isWodType && wodId != null) {
                    val wodRef = firestore.collection("wods").document(wodId)
                    val wodUpdates = mapOf(
                        "title" to wodTitle,
                        "description" to wodDescription,
                        "scoreType" to wodScoreType
                    )
                    batch.update(wodRef, wodUpdates)
                }

                batch.commit().await()
                _classOperationState.value = ClassOperationState.Success("Clase actualizada")
                loadFutureClasses()
            } catch (e: Exception) {
                _classOperationState.value = ClassOperationState.Error(e.message ?: "Error al actualizar")
            }
        }
    }

    fun deleteClass(classId: String) {
        viewModelScope.launch {
            _classOperationState.value = ClassOperationState.Loading
            try {
                firestore.runTransaction { transaction ->
                    val classRef = firestore.collection("gymClasses").document(classId)
                    val classDoc = transaction.get(classRef)

                    if (!classDoc.exists()) {
                        throw IllegalStateException("La clase que intentas borrar ya no existe.")
                    }

                    val gymClass = classDoc.toObject(GymClass::class.java)!!
                    val enrolledUserIds = gymClass.enrolledUserIds

                    if (enrolledUserIds.isNotEmpty()) {
                        val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
                        val classDateFormatted = gymClass.dateTime?.let { dateFormat.format(it) } ?: "Fecha desconocida"
                        val transactionDescription = "Clase cancelada por admin: ${gymClass.name} - $classDateFormatted"

                        enrolledUserIds.forEach { userId ->
                            val userRef = firestore.collection("users").document(userId)

                            transaction.update(userRef, "credits", FieldValue.increment(1))
                            transaction.update(userRef, "currentClassesReserved", FieldValue.increment(-1))

                            val newTransaction = CreditTransaction(
                                userId = userId,
                                gym_id = gymClass.gym_id,
                                amount = 1,
                                type = "Reembolso por cancelación",
                                description = transactionDescription
                            )
                            val transactionRef = firestore.collection("credit_transactions").document()
                            transaction.set(transactionRef, newTransaction)
                        }
                    }

                    transaction.delete(classRef)

                }.await()

                _classOperationState.value = ClassOperationState.Success("Clase eliminada y créditos devueltos a los inscriptos.")
                loadFutureClasses()

            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error al eliminar la clase y reembolsar", e)
                _classOperationState.value = ClassOperationState.Error(e.localizedMessage ?: "Error al eliminar la clase.")
            }
        }
    }

    fun resetClassOperationState() {
        _classOperationState.value = ClassOperationState.Idle
    }

    // --- FUNCIÓN ACTUALIZADA: CARGA EN TIEMPO REAL ---
    fun loadAllUsers() {
        // 1. Obtenemos el ID y validamos
        val gymId = currentUserGymId
        if (gymId.isNullOrBlank()) {
            _userListState.value = UserListState.Error("No se pudo identificar tu gimnasio.")
            return
        }

        // 2. Estado de carga
        _userListState.value = UserListState.Loading

        // 3. Limpiamos listener anterior si existe (para no duplicar)
        userListListener?.remove()

        // 4. Iniciamos la escucha en vivo (SnapshotListener)
        userListListener = firestore.collection("users")
            .whereEqualTo("gym_id", gymId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _userListState.value = UserListState.Error(e.localizedMessage ?: "Error al cargar usuarios.")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val users = snapshot.toObjects(User::class.java)
                        .filter { it.id != UserSession.getCurrentUserId() }

                    _userListState.value = UserListState.Success(users)
                } else {
                    _userListState.value = UserListState.Success(emptyList())
                }
            }
    }

    fun loadUserDetails(userId: String) {
        viewModelScope.launch {
            _userDetailsState.value = UserDetailsState.Loading
            try {
                val document = firestore.collection("users").document(userId).get().await()
                if (document.exists()) {
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        _userDetailsState.value = UserDetailsState.Success(user)
                    } else {
                        _userDetailsState.value = UserDetailsState.Error("Error al deserializar los datos del usuario.")
                    }
                } else {
                    _userDetailsState.value = UserDetailsState.Error("No se encontró al usuario.")
                }
            } catch (e: Exception) {
                _userDetailsState.value = UserDetailsState.Error(e.localizedMessage ?: "Error al cargar los detalles del usuario.")
            }
        }
    }

    fun clearUserDetails() {
        _userDetailsState.value = UserDetailsState.Idle
    }


    fun loadAttendeesDetails(userIds: List<String>) {
        if (userIds.isEmpty()) {
            _attendeeListState.value = AttendeeListState.Success(emptyList())
            return
        }
        viewModelScope.launch {
            _attendeeListState.value = AttendeeListState.Loading
            try {
                val deferredUsers = userIds.map { userId ->
                    async {
                        try {
                            val doc = firestore.collection("users").document(userId).get().await()
                            doc.toObject(User::class.java)
                        } catch (e: Exception) {
                            Log.e("AdminViewModel", "Error fetching user $userId", e)
                            null
                        }
                    }
                }
                val users = deferredUsers.awaitAll().filterNotNull()

                _attendeeListState.value = AttendeeListState.Success(users)
            } catch (e: Exception) {
                _attendeeListState.value = AttendeeListState.Error(e.localizedMessage ?: "Error al cargar asistentes.")
            }
        }
    }

    fun loadClassForEditing(classId: String) {
        viewModelScope.launch {
            _classForEditState.value = ClassForEditState.Loading
            try {
                val classDoc = firestore.collection("gymClasses").document(classId).get().await()
                val gymClass = classDoc.toObject(GymClass::class.java)?.copy(id = classDoc.id)

                if (gymClass == null) {
                    _classForEditState.value = ClassForEditState.Error("No se encontró la clase para editar.")
                    return@launch
                }

                var wod: Wod? = null
                if (!gymClass.wodId.isNullOrBlank()) {
                    val wodDoc = firestore.collection("wods").document(gymClass.wodId!!).get().await()
                    if (wodDoc.exists()) {
                        wod = wodDoc.toObject(Wod::class.java)
                    }
                }
                _classForEditState.value = ClassForEditState.Success(gymClass, wod)

            } catch (e: Exception) {
                _classForEditState.value = ClassForEditState.Error(e.localizedMessage ?: "Error al cargar datos para edición.")
            }
        }
    }

    fun generateAndShareImage(context: Context, baseImageUri: Uri, wodResult: WodResult?) {
        viewModelScope.launch {
            val wod = (wodsDashboardState.value as? WodsDashboardState.Success)?.todayWod
            if (wod == null) {
                Toast.makeText(context, "No se encontró el WOD de hoy.", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val shareableImageUri = generateShareableImage(context, baseImageUri, wod, wodResult)

            if (shareableImageUri != null) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, shareableImageUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(shareIntent, "Compartir WOD del día")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } else {
                Toast.makeText(context, "Error al generar la imagen.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun generateShareableImage(context: Context, baseImageUri: Uri, wod: Wod, wodResult: WodResult?): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                var inputStream = context.contentResolver.openInputStream(baseImageUri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                inputStream = context.contentResolver.openInputStream(baseImageUri)
                val exif = inputStream?.let { androidx.exifinterface.media.ExifInterface(it) }
                inputStream?.close()

                val orientation = exif?.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, androidx.exifinterface.media.ExifInterface.ORIENTATION_UNDEFINED)

                val matrix = Matrix()
                when (orientation) {
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }

                val baseBitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)

                val imageWidth = baseBitmap.width.toFloat()
                val imageHeight = baseBitmap.height.toFloat()
                val resultBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(resultBitmap)

                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = imageWidth * 0.07f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.LTGRAY
                    textSize = imageWidth * 0.04f
                }
                val descriptionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    alpha = 230
                    textSize = imageWidth * 0.045f
                }
                val scoreColor = Color.rgb(255, 165, 0)
                val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = scoreColor
                    textSize = imageWidth * 0.05f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val rectPaint = Paint().apply {
                    color = Color.BLACK
                    alpha = 150
                }

                val textRectHeight = imageHeight * 0.40f
                canvas.drawRect(0f, imageHeight - textRectHeight, imageWidth, imageHeight, rectPaint)

                val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.app_logo)
                val logoSize = imageWidth * 0.15f
                val logoPadding = 30f
                val logoRect = RectF(imageWidth - logoSize - logoPadding, logoPadding, imageWidth - logoPadding, logoPadding + logoSize)
                canvas.drawBitmap(logoBitmap, null, logoRect, null)

                var currentY = imageHeight - textRectHeight + (logoPadding * 1.5f)
                val textLeftPadding = 40f
                val lineSpacing = 15f

                fun getTextHeight(paint: Paint): Float {
                    val fm = paint.fontMetrics
                    return fm.descent - fm.ascent
                }

                val dateFormatter = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es", "ES"))
                val dateText = wod.date?.let { dateFormatter.format(it).uppercase() } ?: "FECHA NO DISPONIBLE"
                canvas.drawText(dateText, textLeftPadding, currentY, subTextPaint)
                currentY += getTextHeight(subTextPaint) + lineSpacing

                canvas.drawText(wod.title.uppercase(), textLeftPadding, currentY, textPaint)
                currentY += getTextHeight(textPaint) + lineSpacing

                if (wod.description.isNotBlank()) {
                    val descriptionLines = wod.description.split("\n")
                    descriptionLines.forEach { line ->
                        canvas.drawText(line, textLeftPadding, currentY, descriptionPaint)
                        currentY += getTextHeight(descriptionPaint) + (lineSpacing * 0.5f)
                    }
                    currentY += lineSpacing
                }

                if (!wod.scoreType.isNullOrEmpty()) {
                    canvas.drawText(wod.scoreType, textLeftPadding, currentY, subTextPaint)
                    currentY += getTextHeight(subTextPaint) + (lineSpacing * 2)
                }

                if (wodResult != null) {
                    val scoreLabel = "TU MARCA: "
                    canvas.drawText(scoreLabel, textLeftPadding, currentY, subTextPaint)

                    val scoreLabelWidth = subTextPaint.measureText(scoreLabel)
                    val scoreXPosition = textLeftPadding + scoreLabelWidth

                    canvas.drawText(wodResult.score, scoreXPosition, currentY, scorePaint)

                    if (wodResult.isRx) {
                        val scoreWidth = scorePaint.measureText(wodResult.score)
                        val rxText = " (Rx)"
                        canvas.drawText(rxText, scoreXPosition + scoreWidth, currentY, subTextPaint)
                    }
                }
                val newUri = createImageUri(context)
                val outputStream = context.contentResolver.openOutputStream(newUri) ?: return@withContext null
                resultBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                outputStream.close()

                return@withContext newUri

            } catch (e: Exception) {
                e.printStackTrace()
                return@withContext null
            }
        }
    }

    private fun createImageUri(context: Context): Uri {
        val imageFolder = File(context.cacheDir, "images")
        imageFolder.mkdirs()
        val file = File(imageFolder, "shared_image_${System.currentTimeMillis()}.jpg")
        val authority = "${context.packageName}.provider"
        return FileProvider.getUriForFile(context, authority, file)
    }
    override fun onCleared() {
        super.onCleared()
        userListListener?.remove() // <--- Agrega esta línea para detener la escucha
    }
}