package com.aquiles.crosschapp.presentation.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.graphics.toColorInt // Necesario para la corrección de color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.R
import com.aquiles.crosschapp.data.model.*
import com.aquiles.crosschapp.data.service.GamificationService
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.SetOptions
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
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

// --- ESTADOS QUE NO ESTÁN EN ADMINSTATES.KT (Se mantienen aquí) ---

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

sealed class PaymentSettingsState {
    object Idle : PaymentSettingsState()
    object Loading : PaymentSettingsState()
    data class Success(val bankInfo: String, val mpInfo: String) : PaymentSettingsState()
    data class Error(val message: String) : PaymentSettingsState()
}
// --- VIEWMODEL PRINCIPAL ---

class AdminViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var userListListener: com.google.firebase.firestore.ListenerRegistration? = null

    private val currentUserGymId: String?
        get() = UserSession.currentUserGymId.value

    // --- StateFlows (Usando los estados de AdminStates.kt) ---

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

    // Nota: Si AppConfigState está en AdminStates como 'Loading' por defecto, ajusta el inicial aquí si es necesario
    private val _appConfigState = MutableStateFlow<AppConfigState>(AppConfigState.Loading)
    val appConfigState: StateFlow<AppConfigState> = _appConfigState.asStateFlow()

    private val _scheduleTemplateState = MutableStateFlow<List<String>>(emptyList())
    val scheduleTemplateState = _scheduleTemplateState.asStateFlow()

    private val _scheduleOperationState = MutableStateFlow<String?>(null)
    val scheduleOperationState = _scheduleOperationState.asStateFlow()

    private val _benchmarkWodsState = MutableStateFlow<BenchmarkWodsState>(BenchmarkWodsState.Loading)
    val benchmarkWodsState: StateFlow<BenchmarkWodsState> = _benchmarkWodsState.asStateFlow()

    private val _benchmarkOperationState = MutableStateFlow<BenchmarkOperationState>(BenchmarkOperationState.Idle)
    val benchmarkOperationState: StateFlow<BenchmarkOperationState> = _benchmarkOperationState.asStateFlow()

    private val _pendingRequestsCount = MutableStateFlow(0)
    val pendingRequestsCount = _pendingRequestsCount.asStateFlow()

    // Suprimimos la advertencia "unused" porque probablemente la UI observa esto
    @Suppress("unused")
    private val _wodsDashboardState = MutableStateFlow<WodsDashboardState>(WodsDashboardState.Idle)
    @Suppress("unused")
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

    private val _paymentSettingsState = MutableStateFlow<PaymentSettingsState>(PaymentSettingsState.Idle)
    val paymentSettingsState = _paymentSettingsState.asStateFlow()

    private val _activityImagesState = MutableStateFlow<Map<String, String>>(emptyMap())
    val activityImagesState: StateFlow<Map<String, String>> = _activityImagesState.asStateFlow()

    init {
        listenForPendingRequests()
    }

    // Helper para verificar permisos de admin
    private fun executeAdminAction(
        errorStateSetter: (String) -> Unit,
        action: suspend CoroutineScope.(adminUser: User, gymId: String) -> Unit
    ) {
        viewModelScope.launch {
            val adminUser = UserSession.currentUser.value
            val gymId = currentUserGymId

            if (adminUser != null && (adminUser.role == "owner" || adminUser.role == "coach" || adminUser.isAdmin) && !gymId.isNullOrBlank()) {
                action(this, adminUser, gymId)
            } else {
                errorStateSetter("Error de permisos o de identificación del gimnasio. Tu sesión pudo haber expirado.")
            }
        }
    }

    fun loadGymPaymentSettings() {
        viewModelScope.launch {
            _paymentSettingsState.value = PaymentSettingsState.Loading
            val gymId = currentUserGymId ?: return@launch
            try {
                // UNIFICADO: payment_info
                val doc = firestore.collection("gyms").document(gymId)
                    .collection("settings").document("payment_info")
                    .get().await()

                if (doc.exists()) {
                    val bank = doc.getString("bankTransferInfo") ?: ""
                    val mp = doc.getString("mercadoPagoInfo") ?: ""
                    _paymentSettingsState.value = PaymentSettingsState.Success(bank, mp)
                } else {
                    _paymentSettingsState.value = PaymentSettingsState.Success("", "")
                }
            } catch (e: Exception) {
                _paymentSettingsState.value = PaymentSettingsState.Error(e.message ?: "Error")
            }
        }
    }

    fun saveGymPaymentSettings(bankInfo: String, mpInfo: String) {
        executeAdminAction({ _paymentSettingsState.value = PaymentSettingsState.Error(it) }) { _, gymId ->
            _paymentSettingsState.value = PaymentSettingsState.Loading
            try {
                // IMPORTANTE: Merge para no borrar el estado del recargo (isSurchargeActive)
                val data = mapOf("bankTransferInfo" to bankInfo, "mercadoPagoInfo" to mpInfo)
                firestore.collection("gyms").document(gymId)
                    .collection("settings").document("payment_info")
                    .set(data, SetOptions.merge())
                    .await()

                _paymentSettingsState.value = PaymentSettingsState.Success(bankInfo, mpInfo)
            } catch (e: Exception) {
                _paymentSettingsState.value = PaymentSettingsState.Error(e.message ?: "Error")
            }
        }
    }

    // --- ACTIVITY IMAGES & APP CONFIG ---

    fun loadActivityImages() {
        viewModelScope.launch {
            val gymId = currentUserGymId ?: return@launch
            try {
                val doc = firestore.collection("gyms").document(gymId)
                    .collection("settings").document("activity_images")
                    .get().await()
                
                if (doc.exists()) {
                     val data = doc.data ?: emptyMap()
                     val map = data.mapValues { it.value.toString() }
                     _activityImagesState.value = map
                } else {
                     _activityImagesState.value = emptyMap()
                }
            } catch (e: Exception) {
                 e.printStackTrace()
                 _activityImagesState.value = emptyMap()
            }
        }
    }



    // --- NEW: Dynamic Theming Update (Sync with iOS) ---
    fun updateGymPrimaryColor(hexColor: String, onResult: (Boolean) -> Unit) {
        executeAdminAction({ onResult(false) }) { _, gymId ->
            try {
                firestore.collection("gyms").document(gymId)
                    .update("primary_color", hexColor)
                    .await()
                
                markSetupStepComplete("gamificacion_lista")
                
                // Update local session if needed (optional, assuming UserSession re-fetches or observes)
                onResult(true)
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error updating primary color", e)
                onResult(false)
            }
        }
    }
    // =================================================================
    // 1. GESTIÓN DE HORARIOS (CORREGIDO PARA IGUALAR IOS)
    // =================================================================

    fun loadScheduleTemplate() {
        viewModelScope.launch {
            val gymId = currentUserGymId ?: return@launch
            try {
                // CORRECCIÓN: Ruta exacta usada en iOS
                val doc = firestore.collection("gyms").document(gymId)
                    .collection("settings").document("schedule_template")
                    .get().await()

                if (doc.exists()) {
                    // CORREGIDO: Casteo seguro usando filterIsInstance
                    val rawList = doc.get("available_times") as? List<*>
                    val times = rawList?.filterIsInstance<String>()?.sorted() ?: emptyList()
                    _scheduleTemplateState.value = times
                } else {
                    _scheduleTemplateState.value = emptyList()
                }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error loading template", e)
                _scheduleTemplateState.value = emptyList()
            }
        }
    }

    fun addTimeToTemplate(time: String) {
        viewModelScope.launch {
            val gymId = currentUserGymId ?: return@launch
            try {
                val docRef = firestore.collection("gyms").document(gymId)
                    .collection("settings").document("schedule_template")

                // CORRECCIÓN: Usar arrayUnion para evitar duplicados y set con merge
                // CORREGIDO: Eliminado calificador redundante, se usa SetOptions importado
                val data = hashMapOf("available_times" to FieldValue.arrayUnion(time))
                docRef.set(data, SetOptions.merge()).await()

                _scheduleOperationState.value = "Horario añadido: $time"
                loadScheduleTemplate()
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error adding time", e)
                _scheduleOperationState.value = "Error al guardar: ${e.message}"
            }
        }
    }

    fun removeTimeFromTemplate(time: String) {
        viewModelScope.launch {
            val gymId = currentUserGymId ?: return@launch
            try {
                val docRef = firestore.collection("gyms").document(gymId)
                    .collection("settings").document("schedule_template")

                docRef.update("available_times", FieldValue.arrayRemove(time)).await()
                _scheduleOperationState.value = "Horario eliminado."
                loadScheduleTemplate()
            } catch (_: Exception) { // CORREGIDO: 'e' no usado se renombra a '_'
                _scheduleOperationState.value = "Error al eliminar."
            }
        }
    }

    fun clearScheduleOperationMessage() { _scheduleOperationState.value = null }

    // --- WEEKLY SCHEDULE TEMPLATE (Smart Scheduling) ---

    data class TemplateSlot(
        val time: String,
        val isOpenGym: Boolean = false
    ) {
        val firestoreString: String
            get() = if (isOpenGym) "$time|OPEN" else time

        companion object {
            fun fromFirestoreString(str: String): TemplateSlot {
                return if (str.endsWith("|OPEN")) {
                    TemplateSlot(time = str.removeSuffix("|OPEN"), isOpenGym = true)
                } else {
                    TemplateSlot(time = str, isOpenGym = false)
                }
            }
        }
    }

    // Map: DayOfWeek (1=Sun, 2=Mon...) -> List of TemplateSlot
    private val _weeklyScheduleState = MutableStateFlow<Map<Int, List<TemplateSlot>>>(emptyMap())
    val weeklyScheduleState = _weeklyScheduleState.asStateFlow()

    data class TimeRange(val startTime: String, val endTime: String)
    data class GymOperatingHours(val dayOfWeek: Int, val ranges: List<TimeRange>)

    // Map: DayOfWeek -> GymOperatingHours
    private val _gymOperatingHoursState = MutableStateFlow<Map<Int, GymOperatingHours>>(emptyMap())
    val gymOperatingHoursState = _gymOperatingHoursState.asStateFlow()

    fun loadWeeklyScheduleTemplate() {
        viewModelScope.launch {
            val gymId = currentUserGymId ?: return@launch
            try {
                val doc = firestore.collection("gyms").document(gymId)
                    .collection("settings").document("weekly_schedule_template")
                    .get().await()

                if (doc.exists()) {
                    val data = doc.data ?: emptyMap()
                    // Convert Map<String, Any> to Map<Int, List<String>>
                    // Firestore keys are Strings. We assume keys are "1", "2", ... "7"
                    // Values should be List<String>
                    val result = mutableMapOf<Int, List<TemplateSlot>>()
                    data.forEach { (key, value) ->
                        val dayInt = key.toIntOrNull()
                        val timesList = (value as? List<*>)?.filterIsInstance<String>()
                        if (dayInt != null && timesList != null) {
                            val slots = timesList.map { TemplateSlot.fromFirestoreString(it) }.sortedBy { it.time }
                            result[dayInt] = slots
                        }
                    }
                    _weeklyScheduleState.value = result
                } else {
                    _weeklyScheduleState.value = emptyMap()
                }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error loading weekly template", e)
                _weeklyScheduleState.value = emptyMap()
                _gymOperatingHoursState.value = emptyMap()
            }
        }
    }

    fun loadGymOperatingHours() {
        viewModelScope.launch {
            val gymId = currentUserGymId ?: return@launch
            try {
                val doc = firestore.collection("gyms").document(gymId)
                    .collection("settings").document("weekly_schedule_template")
                    .get().await()

                if (doc.exists()) {
                    val data = doc.data ?: emptyMap()
                    val result = mutableMapOf<Int, GymOperatingHours>()
                    
                    for (day in 1..7) {
                        val key = "${day}_open_gym_ranges"
                        val rangesArray = (data[key] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        
                        val parsedRanges = rangesArray.mapNotNull { rangeStr ->
                            val parts = rangeStr.split("-")
                            if (parts.size == 2) TimeRange(parts[0], parts[1]) else null
                        }
                        
                        if (parsedRanges.isNotEmpty()) {
                            result[day] = GymOperatingHours(day, parsedRanges)
                        }
                    }
                    _gymOperatingHoursState.value = result
                } else {
                    _gymOperatingHoursState.value = emptyMap()
                }
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error loading open gym ranges", e)
                _gymOperatingHoursState.value = emptyMap()
            }
        }
    }

    fun saveTimeForDay(day: Int, time: String, isOpenGym: Boolean = false) {
        viewModelScope.launch {
            val gymId = currentUserGymId ?: return@launch
            try {
                // Key is string "1", "2", etc.
                val docRef = firestore.collection("gyms").document(gymId)
                    .collection("settings").document("weekly_schedule_template")

                val slot = TemplateSlot(time, isOpenGym)
                val updateMap = mapOf(
                    day.toString() to FieldValue.arrayUnion(slot.firestoreString)
                )
                // Set with merge ensures document exists and we update only that field
                docRef.set(updateMap, SetOptions.merge()).await()
                
                // Independientemente del tipo de horario, logramos el objetivo del asistente
                markSetupStepComplete("horarios_listos")
                if (!isOpenGym) {
                    markSetupStepComplete("clases_listas")
                }
                
                _scheduleOperationState.value = "Horario semanal añadido."
                loadWeeklyScheduleTemplate()
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error saving time for day $day", e)
                _scheduleOperationState.value = "Error: ${e.message}"
            }
        }
    }

    fun removeTimeFromDay(day: Int, slot: TemplateSlot) {
        viewModelScope.launch {
            val gymId = currentUserGymId ?: return@launch
            try {
                val docRef = firestore.collection("gyms").document(gymId)
                    .collection("settings").document("weekly_schedule_template")

                val updateMap = mapOf(
                    day.toString() to FieldValue.arrayRemove(slot.firestoreString)
                )
                docRef.update(updateMap).await()
                
                _scheduleOperationState.value = "Horario eliminado."
                loadWeeklyScheduleTemplate()
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error removing time from day $day", e)
                _scheduleOperationState.value = "Error: ${e.message}"
            }
        }
    }

    fun saveGymOperatingHours(day: Int, ranges: List<TimeRange>) {
        viewModelScope.launch {
            val gymId = currentUserGymId ?: return@launch
            try {
                val docRef = firestore.collection("gyms").document(gymId)
                    .collection("settings").document("weekly_schedule_template")

                val rangeStrings = ranges.map { "${it.startTime}-${it.endTime}" }
                val updateMap = mapOf(
                    "${day}_open_gym_ranges" to rangeStrings
                )
                
                docRef.set(updateMap, SetOptions.merge()).await()
                
                _scheduleOperationState.value = "Horario de Centro Abierto guardado."
                loadGymOperatingHours()
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error saving open gym ranges for day $day", e)
                _scheduleOperationState.value = "Error: ${e.message}"
            }
        }
    }


    // =================================================================
    // 2. GESTIÓN DE WODS Y CLASES (BATCH)
    // =================================================================

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
        wodColor: String,
        otherColor: String
    ) {
        viewModelScope.launch {
            _classOperationState.value = ClassOperationState.Loading
            try {
                val user = UserSession.currentUser.value ?: throw IllegalStateException("No user")
                val gymId = user.gym_id
                val batch = firestore.batch()

                var newWodId: String? = null

                // 1. Crear WOD si corresponde
                if (isWodType) {
                    val wodRef = firestore.collection("wods").document()
                    newWodId = wodRef.id
                    val newWod = Wod(
                        id = newWodId,
                        gym_id = gymId,
                        title = wodTitle,
                        description = wodDescription,
                        type = "Daily",
                        date = date,
                        scoreType = wodScoreType,
                        notes = ""
                    )
                    batch.set(wodRef, newWod)
                }

                // 2. Crear Clases para cada horario seleccionado
                val calendar = Calendar.getInstance()
                calendar.time = date

                selectedTimes.forEach { timeString ->
                    val parts = timeString.split(":")
                    if (parts.size == 2) {
                        calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                        calendar.set(Calendar.MINUTE, parts[1].toInt())
                        calendar.set(Calendar.SECOND, 0)
                        val classDate = calendar.time

                        val classRef = firestore.collection("gymClasses").document()
                        // CORREGIDO: Uso de ifBlank para simplificar
                        val finalName = if (isWodType) wodTitle.ifBlank { "WOD" } else otherClassName

                        val newClass = GymClass(
                            documentId = classRef.id,
                            gym_id = gymId,
                            name = finalName,
                            description = if (isWodType) wodDescription else otherClassDescription,
                            dateTime = classDate,
                            durationMinutes = durationMinutes,
                            maxCapacity = maxCapacity,
                            coachName = coachName,
                            wodId = newWodId,
                            wodScoreType = if (isWodType) wodScoreType else null,
                            classType = if (isWodType) "WOD" else "Other",
                            hexColor = if (isWodType) wodColor else otherColor,
                            enrolledUserIds = emptyList()
                        )
                        batch.set(classRef, newClass)
                    }
                }

                batch.commit().await()
                _classOperationState.value = ClassOperationState.Success("${selectedTimes.size} clases creadas correctamente.")
                loadFutureClasses()
            } catch (e: Exception) {
                _classOperationState.value = ClassOperationState.Error("Error: ${e.message}")
            }
        }
    }

    // =================================================================
    // 2.1 SMART MONTH GENERATION (NUEVO)
    // =================================================================

    data class GenerationPreview(
        val startDate: Date,
        val endDate: Date,
        val totalNewClasses: Int,
        val existingClassesCount: Int,
        val conflicts: List<Date> = emptyList()
    )

    private val _generationPreviewState = MutableStateFlow<GenerationPreview?>(null)
    val generationPreviewState = _generationPreviewState.asStateFlow()

    private val _generationStatusState = MutableStateFlow<String?>(null)
    val generationStatusState = _generationStatusState.asStateFlow()

    fun clearGenerationState() {
        _generationPreviewState.value = null
        _generationStatusState.value = null
    }

    /**
     * Preview month generation with custom date range
     * @param startDate Starting date for class generation (inclusive)
     * @param endDate Ending date for class generation (inclusive)
     */
    fun previewMonthGeneration(startDate: Date, endDate: Date) {
        viewModelScope.launch {
            val gymId = currentUserGymId ?: return@launch
            
            // Validate date range
            if (endDate.before(startDate) || endDate == startDate) {
                _generationStatusState.value = "Error: La fecha fin debe ser posterior a la fecha inicio"
                return@launch
            }
            
            _generationStatusState.value = "Calculando..."

            // 1. Calculate Theoretical Classes based on Template
            var totalNew = 0
            val weeklyMap = _weeklyScheduleState.value
            
            val tempCal = Calendar.getInstance()
            tempCal.time = startDate
            
            while (tempCal.time.before(endDate)) {
                val day = tempCal.get(Calendar.DAY_OF_WEEK)
                val times = weeklyMap[day] ?: emptyList()
                totalNew += times.size
                tempCal.add(Calendar.DAY_OF_YEAR, 1)
            }

            // 2. Check Existing Classes in Range
            try {
                val snapshot = firestore.collection("gymClasses")
                    .whereEqualTo("gym_id", gymId)
                    .whereGreaterThanOrEqualTo("dateTime", startDate)
                    .whereLessThan("dateTime", endDate)
                    .get().await()
                
                val existingCount = snapshot.size()

                _generationPreviewState.value = GenerationPreview(
                    startDate = startDate,
                    endDate = endDate,
                    totalNewClasses = totalNew,
                    existingClassesCount = existingCount
                )
                _generationStatusState.value = null // Ready for UI to show dialog

            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error previewing generation", e)
                _generationStatusState.value = "Error al calcular vista previa."
            }
        }
    }

    enum class GenerationMode {
        APPEND, OVERWRITE
    }

    fun executeGeneration(mode: GenerationMode) {
        val preview = _generationPreviewState.value ?: return
        val gymId = currentUserGymId ?: return
        
        viewModelScope.launch {
            _generationStatusState.value = "Generando..."
            val batchLimit = 500
            var batch = firestore.batch()
            var operationCount = 0

            try {
                // 1. Handle Overwrite (Delete existing first)
                if (mode == GenerationMode.OVERWRITE && preview.existingClassesCount > 0) {
                     val existingDocs = firestore.collection("gymClasses")
                        .whereEqualTo("gym_id", gymId)
                        .whereGreaterThanOrEqualTo("dateTime", preview.startDate)
                        .whereLessThan("dateTime", preview.endDate)
                        .get().await()
                    
                    for (doc in existingDocs) {
                        batch.delete(doc.reference)
                        operationCount++
                        if (operationCount >= batchLimit) {
                            batch.commit().await()
                            batch = firestore.batch()
                            operationCount = 0
                        }
                    }
                }

                // 2. Generate New Classes
                val tempCal = Calendar.getInstance()
                tempCal.time = preview.startDate
                val endCal = Calendar.getInstance()
                endCal.time = preview.endDate // Loop until this date

                val weeklyMap = _weeklyScheduleState.value

                while (tempCal.time.before(endCal.time) || tempCal.time == endCal.time) { // Inclusive? implementation plan said < endDate. Let's stick to strict < 
                       if (tempCal.time.after(preview.endDate)) break
                       
                       val dayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
                       val times = weeklyMap[dayOfWeek] ?: emptyList()

                       for (slot in times) {
                           val parts = slot.time.split(":")
                           if (parts.size == 2) {
                               val classCal = tempCal.clone() as Calendar
                               classCal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
                               classCal.set(Calendar.MINUTE, parts[1].toInt())
                               classCal.set(Calendar.SECOND, 0)
                               
                               val newRef = firestore.collection("gymClasses").document()
                               val newClass = GymClass(
                                   documentId = newRef.id,
                                   gym_id = gymId,
                                   name = if (slot.isOpenGym) "Open Gym" else "WOD", // Default name
                                   description = if (slot.isOpenGym) "Espacio libre reservable" else "Clase Generada Automáticamente",
                                   dateTime = classCal.time,
                                   durationMinutes = 60,
                                   maxCapacity = 15, // Default capacity
                                   coachName = if (slot.isOpenGym) "Staff / Libre" else "Coach", // Default coach
                                   classType = if (slot.isOpenGym) "OPEN_GYM" else "WOD",
                                   hexColor = "#FC5200",
                                   isOpenGym = slot.isOpenGym,
                                   enrolledUserIds = emptyList()
                               )
                               batch.set(newRef, newClass)
                               operationCount++
                               
                               if (operationCount >= batchLimit) {
                                   batch.commit().await()
                                   batch = firestore.batch()
                                   operationCount = 0
                               }
                           }
                       }
                       tempCal.add(Calendar.DAY_OF_YEAR, 1)
                }

                if (operationCount > 0) {
                    batch.commit().await()
                }

                _generationStatusState.value = "Generación Exitosa."
                loadFutureClasses() // Refresh list

            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error executing generation", e)
                _generationStatusState.value = "Error: ${e.message}"
            }
        }
    }

    // =================================================================
    // 3. MENSAJERÍA
    // =================================================================

    fun sendPersonalMessage(targetUserId: String, content: String, attachmentUri: Uri?, context: Context) {
        executeAdminAction(errorStateSetter = { _sendMessageState.value = SendMessageState.Error(it) }) { adminUser, gymId ->
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
            }
        }
    }

    fun sendBroadcastMessage(targetUserIds: List<String>, content: String, attachmentUri: Uri?, context: Context) {
        executeAdminAction(errorStateSetter = { _sendMessageState.value = SendMessageState.Error(it) }) { adminUser, gymId ->
            _sendMessageState.value = SendMessageState.Loading
            if (targetUserIds.isEmpty()) {
                _sendMessageState.value = SendMessageState.Error("No se seleccionó ningún destinatario.")
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
                    attachmentType = if (fileExtension?.contains("pdf", true) == true) "pdf" else "image"
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
                _sendMessageState.value = SendMessageState.Success("Difusión enviada a ${targetUserIds.size} alumnos.")
            } catch (e: Exception) {
                _sendMessageState.value = SendMessageState.Error(e.localizedMessage ?: "Error al enviar difusión.")
            }
        }
    }

    private fun getFileExtension(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri)?.let {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
        }
    }

    fun resetSendMessageState() { _sendMessageState.value = SendMessageState.Idle }

    // =================================================================
    // 4. CRÉDITOS Y PACKS
    // =================================================================

    fun approveRequest(request: CreditRequest) {
        executeAdminAction(errorStateSetter = { _updateState.value = RequestUpdateState.Error(it) }) { adminUser, gymId ->
            _updateState.value = RequestUpdateState.Loading
            try {
                val batch = firestore.batch()

                // Actualizar Usuario
                val userRef = firestore.collection("users").document(request.userId)
                val userDoc = userRef.get().await()

                // N-FIX: Usar durationDays del request (paridad con iOS). Default 30 si null.
                val durationDays = request.durationDays ?: 30

                // Calcular fechas
                val currentValidUntil = userDoc.getDate("creditValidUntil")
                val calendar = Calendar.getInstance()
                if (currentValidUntil != null && currentValidUntil.after(Date())) {
                    calendar.time = currentValidUntil
                } else {
                    calendar.time = Date()
                }
                calendar.add(Calendar.DAY_OF_YEAR, durationDays)
                val newValidUntilDate = calendar.time

                // N-FIX: Soporte para Pase Libre (isUnlimited), paridad con iOS
                if (request.isUnlimited == true) {
                    // Pase Libre: activar flag, no sumar créditos numéricos
                    batch.update(userRef, mapOf(
                        "hasFreePass" to true,
                        "creditValidUntil" to newValidUntilDate
                    ))
                } else {
                    // Pack normal: sumar créditos
                    val creditsToAdd = request.creditsRequested.toLong()
                    batch.update(userRef, "credits", FieldValue.increment(creditsToAdd))
                    batch.update(userRef, "creditValidUntil", newValidUntilDate)
                }

                // Actualizar Request
                val requestRef = firestore.collection("creditRequests").document(request.id)
                batch.update(requestRef, mapOf(
                    "status" to CreditRequestStatus.APPROVED.name,
                    "processedByAdminId" to adminUser.id,
                    "processedByAdminName" to adminUser.fullName,
                    "processedDate" to FieldValue.serverTimestamp(),
                    "durationDays" to durationDays
                ))

                // Crear Transacción
                val transactionRef = firestore.collection("credit_transactions").document()
                val newTransaction = CreditTransaction(
                    userId = request.userId,
                    gym_id = gymId,
                    amount = request.creditsRequested,
                    type = "Adquisición",
                    description = "Aprobación de pack '${request.comboName}'"
                )
                batch.set(transactionRef, newTransaction)

                // Notificación UNIFICADA (Aprobación + Vencimiento + Total)
                val notificationRef = firestore.collection("notifications").document()
                val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(newValidUntilDate)
                val unifiedMessage = "Pack '${request.comboName}' activado exitosamente. Válido hasta el $formattedDate."

                /*
                batch.set(notificationRef, Notification(
                    userId = request.userId,
                    gym_id = gymId,
                    title = "¡Créditos Aprobados!",
                    message = unifiedMessage,
                    type = NotificationType.CREDIT_APPROVED.name
                ))
                */

                batch.commit().await()
                _updateState.value = RequestUpdateState.Success("Solicitud aprobada.")
                loadAllCreditRequests()
            } catch (e: Exception) {
                _updateState.value = RequestUpdateState.Error(e.localizedMessage ?: "Error.")
            }

        }
    }

    fun rejectRequest(request: CreditRequest) {
        executeAdminAction(errorStateSetter = { _updateState.value = RequestUpdateState.Error(it) }) { adminUser, gymId ->
            _updateState.value = RequestUpdateState.Loading
            try {
                val batch = firestore.batch()
                val requestRef = firestore.collection("creditRequests").document(request.id)

                batch.update(requestRef, mapOf(
                    "status" to CreditRequestStatus.REJECTED.name,
                    "processedByAdminId" to adminUser.id,
                    "processedByAdminName" to adminUser.fullName,
                    "processedDate" to FieldValue.serverTimestamp()
                ))

                val notifRef = firestore.collection("notifications").document()
                /*
                batch.set(notifRef, Notification(
                    userId = request.userId,
                    gym_id = gymId,
                    title = "Solicitud Rechazada",
                    message = "Tu solicitud de '${request.comboName}' fue rechazada.",
                    type = NotificationType.CREDIT_REJECTED.name
                ))
                */

                batch.commit().await()
                _updateState.value = RequestUpdateState.Success("Solicitud rechazada.")
                loadAllCreditRequests()
            } catch (e: Exception) {
                _updateState.value = RequestUpdateState.Error(e.localizedMessage ?: "Error.")
            }
        }
    }

    fun loadAllCreditRequests() {
        executeAdminAction(errorStateSetter = { _requestsState.value = CreditRequestsListState.Error(it) }) { _, gymId ->
            _requestsState.value = CreditRequestsListState.Loading
            try {
                // Pending
                val pendingSnapshot = firestore.collection("creditRequests")
                    .whereEqualTo("gym_id", gymId)
                    .whereEqualTo("status", "PENDING")
                    .orderBy("requestDate", Query.Direction.ASCENDING)
                    .get().await()

                // Processed (Limit 20)
                val processedSnapshot = firestore.collection("creditRequests")
                    .whereEqualTo("gym_id", gymId)
                    .whereIn("status", listOf("APPROVED", "REJECTED"))
                    .orderBy("processedDate", Query.Direction.DESCENDING)
                    .limit(20)
                    .get().await()

                val pendingList = pendingSnapshot.toObjects(CreditRequest::class.java)
                val processedList = processedSnapshot.toObjects(CreditRequest::class.java)

                if (pendingList.isEmpty() && processedList.isEmpty()) {
                    _requestsState.value = CreditRequestsListState.Empty
                } else {
                    _requestsState.value = CreditRequestsListState.Success(pendingList, processedList)
                }
            } catch (e: Exception) {
                _requestsState.value = CreditRequestsListState.Error(e.localizedMessage ?: "Error al cargar solicitudes.")
            }
        }
    }

    fun loadAllCreditPacks() {
        viewModelScope.launch {
            _creditPacksState.value = CreditPacksState.Loading
            val gymId = currentUserGymId ?: return@launch
            try {
                val snapshot = firestore.collection("creditPacks")
                    .whereEqualTo("gym_id", gymId)
                    .orderBy("order", Query.Direction.ASCENDING)
                    .get().await()
                val packs = snapshot.documents.mapNotNull { it.toObject(CreditPack::class.java)?.copy(id = it.id) }
                _creditPacksState.value = if (packs.isEmpty()) CreditPacksState.Empty else CreditPacksState.Success(packs)
            } catch (e: Exception) {
                _creditPacksState.value = CreditPacksState.Error(e.localizedMessage ?: "Error.")
            }
        }
    }

    fun saveCreditPack(pack: CreditPack) {
        executeAdminAction(errorStateSetter = {}) { _, gymId ->
            try {
                val docRef = if (pack.id.isNotBlank()) firestore.collection("creditPacks").document(pack.id) else firestore.collection("creditPacks").document()
                docRef.set(pack.copy(id = docRef.id, gym_id = gymId)).await()
                markSetupStepComplete("packs_listos")
                loadAllCreditPacks()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun deleteCreditPack(packId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("creditPacks").document(packId).delete().await()
                loadAllCreditPacks()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun resetUpdateState() { _updateState.value = RequestUpdateState.Idle }

    // =================================================================
    // 5. USUARIOS, REPORTES Y ASISTENCIA
    // =================================================================

    fun loadAllUsers() {
        val gymId = currentUserGymId ?: return
        markSetupStepComplete("gestion_alumnos")
        _userListState.value = UserListState.Loading
        userListListener?.remove()
        userListListener = firestore.collection("users")
            .whereEqualTo("gym_id", gymId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _userListState.value = UserListState.Error(e.localizedMessage ?: "Error.")
                    return@addSnapshotListener
                }
                val users = snapshot?.toObjects(User::class.java)?.filter { it.id != UserSession.getCurrentUserId() } ?: emptyList()
                _userListState.value = UserListState.Success(users)
            }
    }

    fun loadUserDetails(userId: String) {
        viewModelScope.launch {
            _userDetailsState.value = UserDetailsState.Loading
            try {
                val doc = firestore.collection("users").document(userId).get().await()
                val user = doc.toObject(User::class.java)
                _userDetailsState.value = if (user != null) UserDetailsState.Success(user) else UserDetailsState.Error("Usuario no encontrado.")
            } catch (e: Exception) {
                _userDetailsState.value = UserDetailsState.Error(e.localizedMessage ?: "Error.")
            }
        }
    }

    fun clearUserDetails() { _userDetailsState.value = UserDetailsState.Idle }

    fun loadAttendeesDetails(userIds: List<String>) {
        if (userIds.isEmpty()) {
            _attendeeListState.value = AttendeeListState.Success(emptyList())
            return
        }
        viewModelScope.launch {
            _attendeeListState.value = AttendeeListState.Loading
            try {
                // CORREGIDO: catch(e) renombrado a (_)
                val deferreds = userIds.map { id -> async { try { firestore.collection("users").document(id).get().await().toObject(User::class.java) } catch(_:Exception){null} } }
                val users = deferreds.awaitAll().filterNotNull()
                _attendeeListState.value = AttendeeListState.Success(users)
            } catch (e: Exception) {
                _attendeeListState.value = AttendeeListState.Error(e.localizedMessage ?: "Error.")
            }
        }
    }

    fun saveAttendance(classId: String, newAttendedUserIds: List<String>) {
        executeAdminAction(errorStateSetter = { _classOperationState.value = ClassOperationState.Error(it) }) { _, gymId ->
            _classOperationState.value = ClassOperationState.Loading
            try {
                val result = firestore.runTransaction { transaction ->
                    val classRef = firestore.collection("gymClasses").document(classId)
                    val classDoc = transaction.get(classRef)
                    
                    // --- CORRECCIÓN: Permitir actualizar asistencia ya tomada ---
                    // Obtenemos los IDs anteriores (si es nulo, es lista vacía)
                    @Suppress("UNCHECKED_CAST")
                    val oldAttendedUserIds = classDoc.get("attendedUserIds") as? List<String> ?: emptyList()

                    val classDate = classDoc.getDate("dateTime")
                    val calendar = Calendar.getInstance()
                    if(classDate != null) calendar.time = classDate

                    // Calcular Deltas
                    val usersToAdd = newAttendedUserIds.filter { !oldAttendedUserIds.contains(it) }
                    val usersToRemove = oldAttendedUserIds.filter { !newAttendedUserIds.contains(it) }

                    // 1. Agregar nuevos
                    usersToAdd.forEach { userId ->
                        val record = AttendanceRecord(
                            classId = classId, classDate = classDate,
                            dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK), hourOfDay = calendar.get(Calendar.HOUR_OF_DAY),
                            userId = userId, gym_id = gymId
                        )
                        // IMPORTANTE: Referencia al documento para el historial
                        // Idealmente usaríamos un ID determinístico (ej: classId_userId) para evitar duplicados si la transacción reintenta
                        // pero Firestore runTransaction maneja esto. 
                        transaction.set(firestore.collection("attendance_history").document(), record)
                        
                        // [Fix] Crear Log de XP para Historial (Simulando 10 XP base por asistencia)
                        val xpLogRef = firestore.collection("xp_logs").document()
                        val xpLogData = hashMapOf(
                            "userId" to userId,
                            "gym_id" to gymId,
                            "amount" to 10,
                            "type" to "ATTENDANCE",
                            "title" to "Clase Completada",
                            "description" to "Asistencia confirmada",
                            "icon" to "✅",
                            "timestamp" to FieldValue.serverTimestamp(),
                            "relatedId" to classId
                        )
                        transaction.set(xpLogRef, xpLogData)
                        
                        // Incrementamos contador del usuario
                        transaction.update(firestore.collection("users").document(userId), "totalClassesAttended", FieldValue.increment(1))
                    }

                    // 2. Remover desmarcados
                    usersToRemove.forEach { userId ->
                        // Buscar en attendance_history es complejo dentro de una transacción si no tenemos el ID del documento.
                        // LIMITACIÓN: update/delete en transacción requiere DocumentReference conocida.
                        // WORKAROUND: Decrementamos el contador del usuario. El registro en attendance_history quedará "huérfano" 
                        // o requeriría una query previa fuera de la transacción para borrarlo.
                        // Para simplificar y evitar fallos: solo ajustamos el contador.
                        // (En una V2, deberíamos guardar attendance_history con ID = classId_userId)
                        transaction.update(firestore.collection("users").document(userId), "totalClassesAttended", FieldValue.increment(-1))
                    }

                    // 3. Actualizar la clase
                    transaction.update(classRef, mapOf(
                        "attendanceTaken" to true, 
                        "attendedUserIds" to newAttendedUserIds
                    ))
                    
                    // Retornamos la lista de nuevos asistentes Y la fecha para procesar gamificación fuera
                    return@runTransaction Pair(usersToAdd, classDate)
                }.await()
                
                _classOperationState.value = ClassOperationState.Success("Asistencia actualizada.")
                
                // Gamificación solo para los NUEVOS asistentes
                val (newAttendees, dateOfClass) = result
                newAttendees.forEach { userId ->
                    processGamificationForUser(userId, gymId, classId, dateOfClass ?: Date())
                }
            } catch (e: Exception) {
                _classOperationState.value = ClassOperationState.Error(e.localizedMessage ?: "Error.")
            }
        }
    }

    fun resetClassOperationState() { _classOperationState.value = ClassOperationState.Idle }

    fun loadReportsForMonth(year: Int, month: Int) {
        // CORREGIDO: Se pasa la función de seteo de error correctamente
        executeAdminAction(errorStateSetter = { _reportsState.value = ReportsState.Error(it) }) { _, gymId ->
            _reportsState.value = ReportsState.Loading
            try {
                val calendar = Calendar.getInstance()
                calendar.set(year, month - 1, 1, 0, 0, 0)
                val start = calendar.time
                calendar.add(Calendar.MONTH, 1)
                val end = calendar.time
                
                // Formato MM-yyyy para búsqueda de pagos
                val monthStr = String.format(Locale.getDefault(), "%02d-%d", month, year)

                // --- Parallel Fetching ---
                val dollarDeferred = async(Dispatchers.IO) { fetchDollarRate() }
                
                // 1. Attendance (For Active Users)
                val attendanceDeferred = async { 
                    firestore.collection("attendance_history")
                        .whereEqualTo("gym_id", gymId)
                        .whereGreaterThanOrEqualTo("classDate", start)
                        .whereLessThan("classDate", end)
                        .get().await() 
                }

                // 2. Payment Status
                val paymentStatusDeferred = async {
                     firestore.collection("app_payments")
                        .whereEqualTo("gym_id", gymId)
                        .whereEqualTo("month", monthStr)
                        .get().await()
                }

                // 3. Transactions (Revenue)
                val transactionsDeferred = async {
                    firestore.collection("creditRequests")
                        .whereEqualTo("gym_id", gymId)
                        .whereEqualTo("status", "APPROVED")
                        .whereGreaterThanOrEqualTo("processedDate", start)
                        .whereLessThan("processedDate", end)
                        .get().await()
                }
                
                // 4. App Payment Info (Global)
                val paymentInfoDeferred = async {
                     firestore.collection("settings").document("payment_info").get().await()
                }

                // --- Await & Process ---
                
                val dollarRate = dollarDeferred.await()
                
                val attendanceSnap = attendanceDeferred.await()
                val activeUsersCount = attendanceSnap.documents.mapNotNull { it.getString("userId") }.distinct().count()

                val paymentStatusSnap = paymentStatusDeferred.await()
                var paymentStatus = "PENDING"
                if (!paymentStatusSnap.isEmpty) {
                    val doc = paymentStatusSnap.documents.first()
                    paymentStatus = doc.getString("status") ?: "PENDING"
                }

                val transactionsSnap = transactionsDeferred.await()
                val requests = transactionsSnap.toObjects(CreditRequest::class.java)
                
                val paymentInfoSnap = paymentInfoDeferred.await()
                val paymentInfo = if (paymentInfoSnap.exists()) {
                     AppPaymentInfo(
                        alias = paymentInfoSnap.getString("alias") ?: "No disponible",
                        cbu = paymentInfoSnap.getString("cbu") ?: "",
                        bankName = paymentInfoSnap.getString("bankName") ?: "",
                        holder = paymentInfoSnap.getString("accountHolder") ?: ""
                     )
                } else {
                    null
                }

                _reportsState.value = ReportsState.Success(
                    totalRevenue = requests.sumOf { it.amountPaid },
                    monthlyActiveUsers = activeUsersCount,
                    monthlyTransactions = requests,
                    dollarRate = dollarRate,
                    paymentStatus = paymentStatus,
                    paymentInfo = paymentInfo
                )
            } catch (e: Exception) {
                _reportsState.value = ReportsState.Error(e.localizedMessage ?: "Error.")
            }
        }
    }

    private fun fetchDollarRate(): Double? {
        return try {
            val url = java.net.URL("https://dolarapi.com/v1/dolares/oficial")
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            if (connection.responseCode == 200) {
                val stream = connection.inputStream
                val reader = java.io.BufferedReader(java.io.InputStreamReader(stream))
                val response = reader.readText()
                reader.close()
                stream.close()
                
                val json = org.json.JSONObject(response)
                json.getDouble("venta")
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun uploadProofOfPayment(uri: Uri, month: Int, year: Int, amount: Double, context: Context) {
        val gymId = currentUserGymId ?: return
        executeAdminAction({ _reportsState.value = ReportsState.Error(it) }) { _, _ ->
            try {
                _reportsState.value = ReportsState.Loading
                val monthStr = String.format(Locale.getDefault(), "%02d-%d", month, year)
                
                // 1. Upload Image
                val fileExtension = getFileExtension(context, uri) ?: "jpg"
                val ref = storage.reference.child("payments/$gymId/${monthStr}_proof.$fileExtension")
                ref.putFile(uri).await()
                val downloadUrl = ref.downloadUrl.await().toString()
                
                // 2. Create/Update Payment Document
                val paymentData = hashMapOf(
                    "gym_id" to gymId,
                    "month" to monthStr,
                    "year" to year,
                    "status" to "PENDING_REVIEW",
                    "proofUrl" to downloadUrl,
                    "amount" to amount,
                    "timestamp" to FieldValue.serverTimestamp()
                )
                
                val query = firestore.collection("app_payments")
                    .whereEqualTo("gym_id", gymId)
                    .whereEqualTo("month", monthStr)
                    .get().await()
                
                if (query.isEmpty) {
                    firestore.collection("app_payments").add(paymentData).await()
                } else {
                    val docId = query.documents.first().id
                    firestore.collection("app_payments").document(docId).update(paymentData as Map<String, Any>).await()
                }
                
                // Reload
                loadReportsForMonth(year, month)
                
            } catch (e: Exception) {
                _reportsState.value = ReportsState.Error(e.localizedMessage ?: "Error al subir comprobante")
            }
        }
    }

    fun loadBillingRules() {
        viewModelScope.launch {
            _billingRulesState.value = BillingRulesState.Loading
            val gymId = currentUserGymId ?: return@launch
            try {
                // UNIFICADO: payment_info
                val doc = firestore.collection("gyms").document(gymId)
                    .collection("settings").document("payment_info")
                    .get().await()

                val isSurcharge = doc.getBoolean("isSurchargeActive") ?: false
                _billingRulesState.value = BillingRulesState.Success(BillingRules(isSurchargeActive = isSurcharge))
            } catch (e: Exception) {
                _billingRulesState.value = BillingRulesState.Error(e.message ?: "Error")
            }
        }
    }

    fun setSurchargeStatus(active: Boolean) {
        viewModelScope.launch {
            val gymId = currentUserGymId ?: return@launch
            try {
                // IMPORTANTE: Merge para no borrar el CBU
                val data = mapOf("isSurchargeActive" to active)
                firestore.collection("gyms").document(gymId)
                    .collection("settings").document("payment_info")
                    .set(data, SetOptions.merge())
                    .await()

                _billingRulesState.value = BillingRulesState.Success(BillingRules(isSurchargeActive = active))
            } catch (e: Exception) {
                _billingRulesState.value = BillingRulesState.Error(e.message ?: "Error")
            }
        }
    }

    private fun listenForPendingRequests() {
        val gymId = currentUserGymId
        if (UserSession.currentUser.value?.role == "owner" && !gymId.isNullOrBlank()) {
            firestore.collection("creditRequests")
                .whereEqualTo("gym_id", gymId)
                .whereEqualTo("status", "PENDING")
                .addSnapshotListener { s, _ -> _pendingRequestsCount.value = s?.size() ?: 0 }
        } else {
            _pendingRequestsCount.value = 0
        }
    }

    // =================================================================
    // 6. DASHBOARD WODS, IMÁGENES Y EDICIÓN
    // =================================================================

    fun loadWodsForDashboard() {
        viewModelScope.launch {
            _wodsDashboardState.value = WodsDashboardState.Loading
            val gymId = currentUserGymId ?: return@launch
            try {
                val today = LocalDate.now()
                val tomorrow = today.plusDays(1)
                val tStart = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant())
                val tEnd = Date.from(tomorrow.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())

                val snapshot = firestore.collection("wods")
                    .whereEqualTo("gym_id", gymId)
                    .whereGreaterThanOrEqualTo("date", tStart)
                    .whereLessThan("date", tEnd)
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

    fun loadWodDetails(wodId: String?) {
        if (wodId == null) { _wodDetailsState.value = WodDetailsState.Success(null); return }
        viewModelScope.launch {
            _wodDetailsState.value = WodDetailsState.Loading
            try {
                val doc = firestore.collection("wods").document(wodId).get().await()
                _wodDetailsState.value = if(doc.exists()) WodDetailsState.Success(doc.toObject(Wod::class.java)?.copy(id = doc.id)) else WodDetailsState.Error("WOD no encontrado.")
            } catch (e: Exception) { _wodDetailsState.value = WodDetailsState.Error(e.message ?: "Error") }
        }
    }

    fun saveWod(originalId: String?, title: String, type: String, desc: String, date: Date, scoreType: String, notes: String) {
        executeAdminAction(errorStateSetter = { _wodOperationState.value = WodOperationState.Error(it) }) { _, gymId ->
            _wodOperationState.value = WodOperationState.Loading
            val id = originalId ?: firestore.collection("wods").document().id
            val wod = Wod(id = id, gym_id = gymId, title = title, description = desc, type = type, date = date, scoreType = scoreType, notes = notes)
            try {
                firestore.collection("wods").document(id).set(wod).await()
                _wodOperationState.value = WodOperationState.Success("WOD guardado.")
                loadWodsForDashboard()
            } catch (e: Exception) {
                _wodOperationState.value = WodOperationState.Error(e.message ?: "Error")
            }
        }
    }

    @Suppress("unused") // Probablemente usado en la UI
    fun deleteWod(wodId: String) {
        viewModelScope.launch {
            try { firestore.collection("wods").document(wodId).delete().await(); loadWodsForDashboard() } catch(_: Exception){}
        }
    }

    fun loadFutureClasses() {
        viewModelScope.launch {
            _classListState.value = ClassListState.Loading
            val gymId = currentUserGymId ?: return@launch
            try {
                val snapshot = firestore.collection("gymClasses")
                    .whereEqualTo("gym_id", gymId)
                    .whereGreaterThanOrEqualTo("dateTime", Date())
                    .orderBy("dateTime", Query.Direction.ASCENDING)
                    .get().await()
                val classes = snapshot.documents.mapNotNull { it.toObject(GymClass::class.java)?.copy(documentId = it.id) }
                _classListState.value = ClassListState.Success(classes)
            } catch (e: Exception) {
                _classListState.value = ClassListState.Error(e.message ?: "Error")
            }
        }
    }

    fun deleteClass(classId: String) {
        viewModelScope.launch {
            _classOperationState.value = ClassOperationState.Loading
            try {
                firestore.runTransaction { t ->
                    val ref = firestore.collection("gymClasses").document(classId)
                    val doc = t.get(ref)
                    if (!doc.exists()) return@runTransaction
                    val cls = doc.toObject(GymClass::class.java)!!
                    cls.enrolledUserIds.forEach { uid ->
                        val uRef = firestore.collection("users").document(uid)
                        t.update(uRef, "credits", FieldValue.increment(1))
                        t.update(uRef, "currentClassesReserved", FieldValue.increment(-1))
                        t.set(firestore.collection("credit_transactions").document(), CreditTransaction(userId=uid, gym_id=cls.gym_id, amount=1, type="Reembolso", description="Clase cancelada: ${cls.name}"))
                    }
                    t.delete(ref)
                }.await()
                _classOperationState.value = ClassOperationState.Success("Clase eliminada.")
                loadFutureClasses()
            } catch (e: Exception) {
                _classOperationState.value = ClassOperationState.Error(e.message ?: "Error")
            }
        }
    }

    fun updateClass(classId: String, wodId: String?, isWodType: Boolean, wodTitle: String, wodDesc: String, scoreType: String, otherName: String, otherDesc: String, date: Date, coach: String, duration: Int, capacity: Int, hexColor: String) {
        viewModelScope.launch {
            _classOperationState.value = ClassOperationState.Loading
            try {
                val batch = firestore.batch()
                val classRef = firestore.collection("gymClasses").document(classId)
                val updates = hashMapOf<String, Any?>(
                    "name" to if (isWodType) wodTitle else otherName,
                    "description" to if (isWodType) wodDesc else otherDesc,
                    "coachName" to coach, "durationMinutes" to duration, "maxCapacity" to capacity,
                    "classType" to if (isWodType) "WOD" else "Other",
                    "wodScoreType" to if (isWodType) scoreType else null,
                    "hexColor" to hexColor, "dateTime" to Timestamp(date)
                )
                if(!isWodType) updates["wodId"] = null
                batch.update(classRef, updates)

                // CORREGIDO: Eliminado !! (Assert) innecesario
                if(isWodType && !wodId.isNullOrBlank()) {
                    batch.update(firestore.collection("wods").document(wodId), mapOf("title" to wodTitle, "description" to wodDesc, "scoreType" to scoreType))
                }
                batch.commit().await()
                _classOperationState.value = ClassOperationState.Success("Clase actualizada.")
                loadFutureClasses()
            } catch (e: Exception) {
                _classOperationState.value = ClassOperationState.Error(e.message ?: "Error")
            }
        }
    }

    fun loadClassForEditing(classId: String) {
        viewModelScope.launch {
            _classForEditState.value = ClassForEditState.Loading
            try {
                val doc = firestore.collection("gymClasses").document(classId).get().await()
                val cls = doc.toObject(GymClass::class.java)?.copy(documentId = doc.id) ?: throw Exception("Clase no encontrada")
                var wod: Wod? = null
                if (!cls.wodId.isNullOrBlank()) {
                    val wDoc = firestore.collection("wods").document(cls.wodId!!).get().await()
                    wod = wDoc.toObject(Wod::class.java)
                }
                _classForEditState.value = ClassForEditState.Success(cls, wod)
            } catch (e: Exception) {
                _classForEditState.value = ClassForEditState.Error(e.message ?: "Error")
            }
        }
    }

    fun resetWodOperationState() { _wodOperationState.value = WodOperationState.Idle }
    fun clearWodDetails() { _wodDetailsState.value = WodDetailsState.Idle }

    // --- BENCHMARKS ---
    fun loadBenchmarkWods() {
        viewModelScope.launch {
            val gymId = currentUserGymId ?: return@launch
            _benchmarkWodsState.value = BenchmarkWodsState.Loading
            try {
                // 1. Cargar benchmarks locales
                val localSnap = firestore.collection("benchmark_wods")
                    .whereEqualTo("gym_id", gymId)
                    .orderBy("name")
                    .get().await()
                val local = localSnap.toObjects(BenchmarkWod::class.java)
                
                // 2. Cargar benchmarks globales
                val globalSnap = firestore.collection("benchmark_wods")
                    .whereEqualTo("gym_id", "")
                    .get().await()
                val allGlobal = globalSnap.toObjects(BenchmarkWod::class.java)
                
                // 🆕 FILTRAR por allowedGymIds
                val global = allGlobal.filter { benchmark ->
                    val allowedIds = benchmark.allowedGymIds
                    if (!allowedIds.isNullOrEmpty()) {
                        // Si tiene allowedGymIds, verificar que contenga el gymId actual
                        allowedIds.contains(gymId)
                    } else {
                        // Si es null o vacío, disponible para todos
                        true
                    }
                }
                
                // 3. Merge y ordenar
                val all = (local + global).sortedBy { it.name }
                _benchmarkWodsState.value = BenchmarkWodsState.Success(all)
            } catch (e: Exception) { 
                _benchmarkWodsState.value = BenchmarkWodsState.Error(e.message ?: "Error") 
            }
        }
    }
    fun saveBenchmark(bench: BenchmarkWod) {
        executeAdminAction({ _benchmarkOperationState.value = BenchmarkOperationState.Error(it)}) { _, gymId ->
            _benchmarkOperationState.value = BenchmarkOperationState.Loading
            val ref = if(bench.id.isEmpty()) firestore.collection("benchmark_wods").document() else firestore.collection("benchmark_wods").document(bench.id)
            ref.set(bench.copy(id = ref.id, gym_id = gymId)).await()
            _benchmarkOperationState.value = BenchmarkOperationState.Success("Guardado.")
            loadBenchmarkWods()
        }
    }
    fun deleteBenchmark(id: String) {
        viewModelScope.launch {
            try { firestore.collection("benchmark_wods").document(id).delete().await(); loadBenchmarkWods() } catch(_:Exception){}
        }
    }
    fun resetBenchmarkOperationState() { _benchmarkOperationState.value = BenchmarkOperationState.Idle }

    // --- CORRECCIÓN APLICADA AQUÍ: Ruta correcta de Firebase ---
    fun loadAppConfig() {
        viewModelScope.launch {
            _appConfigState.value = AppConfigState.Loading
            try {
                // ANTES: "app_config" -> "daily_wods_images"
                // AHORA: "settings" -> "app_config"
                val doc = firestore.collection("settings").document("app_config").get().await()

                // CORREGIDO: Casteo seguro de Map<*,*> a Map<String, String>
                val rawMap = doc.get("wod_images_by_day") as? Map<*, *>
                val map = rawMap?.entries?.associate { (k, v) -> k.toString() to v.toString() } ?: emptyMap()

                _appConfigState.value = AppConfigState.Success(map)
            } catch (e: Exception) {
                _appConfigState.value = AppConfigState.Error(e.message ?: "Error")
            }
        }
    }

    // =================================================================
    // 7. GENERACIÓN DE IMAGEN (CANVAS)
    // =================================================================

    fun generateAndShareImage(context: Context, imageUri: Uri, wod: Wod, result: WodResult? = null) {
        viewModelScope.launch {
            val uri = generateShareableImage(context, imageUri, wod, result)
            if (uri != null) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Compartir WOD").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } else {
                Toast.makeText(context, "Error al generar imagen.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun generateShareableImage(context: Context, baseImageUri: Uri, wod: Wod, wodResult: WodResult?): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                // Decode & Scale
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(baseImageUri)?.use { BitmapFactory.decodeStream(it, null, options) }
                var sample = 1
                while (options.outWidth / sample > 1080) sample *= 2
                val loadOpts = BitmapFactory.Options().apply { inSampleSize = sample }

                val original = context.contentResolver.openInputStream(baseImageUri)?.use { BitmapFactory.decodeStream(it, null, loadOpts) } ?: return@withContext null

                // Fix Rotation
                val exif = context.contentResolver.openInputStream(baseImageUri)?.use { androidx.exifinterface.media.ExifInterface(it) }
                val orient = exif?.getAttributeInt(androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, 1) ?: 1
                val matrix = Matrix().apply {
                    when(orient) {
                        6 -> postRotate(90f)
                        3 -> postRotate(180f)
                        8 -> postRotate(270f)
                    }
                }
                val base = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)

                // Draw
                val bmp = base.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(bmp)
                val w = bmp.width.toFloat(); val h = bmp.height.toFloat()

                // Gradient
                // CORREGIDO: Uso de extensión toColorInt
                val gradPaint = Paint().apply { shader = LinearGradient(0f, h*0.5f, 0f, h, Color.TRANSPARENT, "#E6000000".toColorInt(), Shader.TileMode.CLAMP) }
                canvas.drawRect(0f, h*0.5f, w, h, gradPaint)

                // Paints
                val titleP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.WHITE; textSize=w*0.08f; typeface=Typeface.DEFAULT_BOLD; setShadowLayer(10f,2f,2f,Color.BLACK) }

                // CORREGIDO: Eliminada variable 'descP' no usada

                // CORREGIDO: Uso de extensión toColorInt
                val scoreP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color="#FC5200".toColorInt(); textSize=w*0.06f; typeface=Typeface.DEFAULT_BOLD; setShadowLayer(10f,2f,2f,Color.BLACK) }
                val dateP = Paint(Paint.ANTI_ALIAS_FLAG).apply { color=Color.LTGRAY; textSize=w*0.035f; setShadowLayer(5f,1f,1f,Color.BLACK) }

                // Logo
                val logo = BitmapFactory.decodeResource(context.resources, R.drawable.app_logo)
                if (logo != null) {
                    val sz = w*0.15f
                    val pad = 50f
                    canvas.drawBitmap(logo, null, RectF(w-sz-pad, pad+60f, w-pad, pad+60f+sz), null)
                }

                var y = h - 150f
                val x = 60f
                val gap = 20f

                if (wodResult != null) {
                    val rx = if(wodResult.isRx) " (RX)" else ""
                    canvas.drawText("MARCA: ${wodResult.score}$rx", x, y, scoreP)
                    y -= (scoreP.descent()-scoreP.ascent() + gap)
                } else {
                    canvas.drawText("ENTRENAMIENTO COMPLETADO ✅", x, y, scoreP)
                    y -= (scoreP.descent()-scoreP.ascent() + gap)
                }

                if(!wod.scoreType.isNullOrEmpty()) {
                    canvas.drawText(wod.scoreType.uppercase(), x, y, dateP)
                    y -= (dateP.descent()-dateP.ascent() + gap)
                }

                canvas.drawText(wod.title.uppercase(), x, y, titleP)
                y -= (titleP.descent()-titleP.ascent() + gap)

                // CORREGIDO: Constructor de Locale deprecado, uso de forLanguageTag
                val fmt = SimpleDateFormat("EEEE d, MMMM", Locale.forLanguageTag("es-ES"))
                val dTxt = wod.date?.let { fmt.format(it).uppercase() } ?: ""
                canvas.drawText(dTxt, x, y, dateP)

                // Save
                val dir = File(context.cacheDir, "images"); dir.mkdirs()
                val f = File(dir, "shared_${System.currentTimeMillis()}.jpg")
                FileOutputStream(f).use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                FileProvider.getUriForFile(context, "${context.packageName}.provider", f)

            } catch(e: Exception) { null }
        }
    }

    private fun processGamificationForUser(userId: String, gymId: String, classId: String, classDate: Date) {
        viewModelScope.launch {
            try {
                 // Delegamos toda la lógica compleja al servicio dedicado.
                 GamificationService.processAttendanceGamification(
                    userId = userId,
                    gymId = gymId,
                    classId = classId,
                    classDate = classDate
                 )
                 
                 Log.d("AdminViewModel", "Gamification processed for $userId via Service")

            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error processing gamification", e)
            }
        }
    }

    // =================================================================
    // 6. GESTIÓN DE IMÁGENES DE ACTIVIDADES (PARIDAD IOS)
    // =================================================================



    fun saveActivityImage(name: String, imageUri: Uri, context: Context, onResult: (Boolean) -> Unit) {
        executeAdminAction({ onResult(false) }) { _, gymId ->
            try {
                val fileExtension = getFileExtension(context, imageUri) ?: "jpg"
                val path = "gyms/$gymId/activities/$name.$fileExtension"
                val ref = storage.reference.child(path)
                
                ref.putFile(imageUri).await()
                val url = ref.downloadUrl.await().toString()

                firestore.collection("gyms").document(gymId)
                    .collection("settings").document("activity_images")
                    .set(mapOf(name to url), SetOptions.merge())
                    .await()

                val currentMap = _activityImagesState.value.toMutableMap()
                currentMap[name] = url
                _activityImagesState.value = currentMap
                
                onResult(true)
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error saving activity image", e)
                onResult(false)
            }
        }
    }

    fun deleteActivityImage(name: String) {
        viewModelScope.launch {
            val gymId = currentUserGymId ?: return@launch
            try {
                firestore.collection("gyms").document(gymId)
                    .collection("settings").document("activity_images")
                    .update(name, FieldValue.delete())
                    .await()
                
                val currentMap = _activityImagesState.value.toMutableMap()
                currentMap.remove(name)
                _activityImagesState.value = currentMap
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error deleting activity image", e)
            }
        }
    }

    fun generateDayPlaceholders() {
        viewModelScope.launch {
            val gymId = currentUserGymId ?: return@launch
            val days = listOf("LUNES", "MARTES", "MIÉRCOLES", "JUEVES", "VIERNES", "SÁBADO", "DOMINGO")
            val currentMap = _activityImagesState.value.toMutableMap()
            var hasChanges = false

            val updates = hashMapOf<String, Any>()
            
            days.forEach { day ->
                if (!currentMap.containsKey(day)) {
                    updates[day] = "placeholder"
                    currentMap[day] = "placeholder"
                    hasChanges = true
                }
            }

            if (hasChanges) {
                try {
                    firestore.collection("gyms").document(gymId)
                        .collection("settings").document("activity_images")
                        .set(updates, SetOptions.merge())
                        .await()
                    
                    _activityImagesState.value = currentMap
                } catch (e: Exception) {
                    Log.e("AdminViewModel", "Error generating placeholders", e)
                }
            }
        }
    }
    
    // =================================================================
    // 7. CIERRE DIARIO (PARIDAD IOS)
    // =================================================================
    
    fun checkAndCloseYesterday() {
        viewModelScope.launch {
            // TODO: Implementar lógica completa de cierre diario
            // Por ahora solo logueamos para cumplir con la llamada de la UI
            Log.d("AdminViewModel", "Checking daily closing status (Placeholder)")
            
            val gymId = currentUserGymId ?: return@launch
            
            // Aquí iría la lógica para verificar si lastDailyClosing < yesterday
            // y ejecutar el cierre si corresponde.
        }
    }
    
    fun loadPendingRequests() {
        // Esta función es redundante si ya tenemos el listener en init, 
        // pero la UI la llama en LaunchedEffect para asegurar actualización.
        // Podemos simplemente loguear o refrescar si fuera necesario manual.
        Log.d("AdminViewModel", "Load pending requests triggered manually")
    }

    override fun onCleared() {
        super.onCleared()
        userListListener?.remove()
    }
    
    // MARK: - Setup Wizard Progress
    fun markSetupStepComplete(stepKey: String) {
        val gymId = currentUserGymId ?: return
        viewModelScope.launch {
            try {
                firestore.collection("gyms").document(gymId)
                    .set(mapOf("setupProgress" to mapOf(stepKey to true)), com.google.firebase.firestore.SetOptions.merge())
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error marking setup step $stepKey complete", e)
            }
        }
    }

    fun markSetupStepSkipped(stepKey: String) {
        val gymId = currentUserGymId ?: return
        viewModelScope.launch {
            try {
                firestore.collection("gyms").document(gymId)
                    .set(mapOf("setupSkipped" to mapOf(stepKey to true)), com.google.firebase.firestore.SetOptions.merge())
            } catch (e: Exception) {
                Log.e("AdminViewModel", "Error marking setup step $stepKey skipped", e)
            }
        }
    }
}