package com.aquiles.crosschapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.GymClass
import com.aquiles.crosschapp.data.model.User
import com.aquiles.crosschapp.data.model.Wod
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.HttpsCallableResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

// --- ESTADOS UI ---
sealed class ClassesState {
    data object Idle : ClassesState()
    data object Loading : ClassesState()
    data class Success(val classes: List<GymClass>) : ClassesState()
    data class Error(val message: String) : ClassesState()
}

sealed class BookingState {
    data object Idle : BookingState()
    data class Loading(val classId: String) : BookingState()
    data class Success(val message: String) : BookingState()
    data class Error(val message: String) : BookingState()
}

sealed class NextBookingState {
    data object Loading : NextBookingState()
    data class Success(val nextClass: GymClass?) : NextBookingState()
    data class Error(val message: String) : NextBookingState()
}

sealed class ClassDetailsState {
    data object Idle : ClassDetailsState()
    data object Loading : ClassDetailsState()
    data class Success(val gymClass: GymClass, val wod: Wod?) : ClassDetailsState()
    data class Error(val message: String) : ClassDetailsState()
}

class ScheduleViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    // Estados
    private val _classesState = MutableStateFlow<ClassesState>(ClassesState.Idle)
    val classesState: StateFlow<ClassesState> = _classesState.asStateFlow()

    // [NEW] Open Gym Hours
    data class TimeRange(val startTime: String, val endTime: String)
    data class GymOperatingHours(val dayOfWeek: Int, val ranges: List<TimeRange>)
    private val _gymOperatingHours = MutableStateFlow<Map<Int, GymOperatingHours>>(emptyMap())
    val gymOperatingHours: StateFlow<Map<Int, GymOperatingHours>> = _gymOperatingHours.asStateFlow()

    private val _bookingState = MutableStateFlow<BookingState>(BookingState.Idle)
    val bookingState: StateFlow<BookingState> = _bookingState.asStateFlow()

    private val _nextBookingState = MutableStateFlow<NextBookingState>(NextBookingState.Loading)
    val nextBookingState: StateFlow<NextBookingState> = _nextBookingState.asStateFlow()

    private val _classDetailsState = MutableStateFlow<ClassDetailsState>(ClassDetailsState.Idle)
    val classDetailsState: StateFlow<ClassDetailsState> = _classDetailsState.asStateFlow()

    // [NEW] Exclusive Today Card State
    private val _todayBookingState = MutableStateFlow<NextBookingState>(NextBookingState.Loading)
    val todayBookingState: StateFlow<NextBookingState> = _todayBookingState.asStateFlow()

    // Listeners
    private var classesListener: ListenerRegistration? = null
    private var nextBookingListener: ListenerRegistration? = null
    private var todayBookingListener: ListenerRegistration? = null // [NEW]
    private var classDetailsListener: ListenerRegistration? = null
    private var settingsListener: ListenerRegistration? = null

    init {
        // Iniciar escucha de la próxima reserva apenas tengamos usuario
        viewModelScope.launch {
            UserSession.currentUser.collect { u ->
                if (u != null && u.gym_id.isNotEmpty()) {
                    listenForNextBooking(u.id, u.gym_id)
                    listenForTodayBooking(u.id, u.gym_id) // [NEW]
                    fetchGymOperatingHours(u.gym_id)
                }
            }
        }
    }

    // ========================================================================
    // PRÓXIMA RESERVA (WODS SCREEN CARD)
    // ========================================================================
    private fun listenForNextBooking(uid: String, gid: String) {
        nextBookingListener?.remove()
        _nextBookingState.value = NextBookingState.Loading

        // Esta consulta requiere un índice compuesto en Firestore:
        // collection: gymClasses
        // fields: gym_id (ASC), enrolledUserIds (ARRAY_CONTAINS), dateTime (ASC)
        nextBookingListener = firestore.collection("gymClasses")
            .whereEqualTo("gym_id", gid)
            .whereArrayContains("enrolledUserIds", uid)
            .whereGreaterThan("dateTime", Date()) // Solo futuras
            .orderBy("dateTime", Query.Direction.ASCENDING)
            .limit(1) // Solo queremos la más próxima
            .addSnapshotListener { s, e ->
                if (e != null) {
                    // Si falla por falta de índice, el logcat te dará el link para crearlo.
                    _nextBookingState.value = NextBookingState.Error(e.message ?: "Error loading next booking")
                    return@addSnapshotListener
                }

                val nextClass = s?.documents?.firstOrNull()?.toObject(GymClass::class.java)?.copy(documentId = s.documents.first().id)
                _nextBookingState.value = NextBookingState.Success(nextClass)
            }
    }


    // ========================================================================
    // RESERVA DE HOY (EXCLUSIVE CARD)
    // ========================================================================
    private fun listenForTodayBooking(uid: String, gid: String) {
        todayBookingListener?.remove()
        _todayBookingState.value = NextBookingState.Loading

        val now = Date()
        val calendar = Calendar.getInstance()
        calendar.time = now
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.time
        
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.time

        // Busca clase donde el usuario esté inscrito Y sea HOY
        todayBookingListener = firestore.collection("gymClasses")
            .whereEqualTo("gym_id", gid)
            .whereArrayContains("enrolledUserIds", uid)
            .whereGreaterThanOrEqualTo("dateTime", startOfDay)
            .whereLessThan("dateTime", endOfDay)
            .limit(1) // Asumimos 1 clase principal por día o tomamos la primera
            .addSnapshotListener { s, e ->
                if (e != null) {
                    _todayBookingState.value = NextBookingState.Error(e.message ?: "Error loading today booking")
                    return@addSnapshotListener
                }

                val todayClass = s?.documents?.firstOrNull()?.toObject(GymClass::class.java)?.copy(documentId = s.documents.first().id)
                _todayBookingState.value = NextBookingState.Success(todayClass)
            }
    }

    // ========================================================================
    // CARGAR DETALLES DE CLASE
    // ========================================================================
    fun loadClassDetails(classId: String) {
        _classDetailsState.value = ClassDetailsState.Loading
        classDetailsListener?.remove()

        classDetailsListener = firestore.collection("gymClasses").document(classId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _classDetailsState.value = ClassDetailsState.Error(error.localizedMessage ?: "Error al cargar clase")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val gymClass = snapshot.toObject(GymClass::class.java)?.copy(documentId = snapshot.id)

                    if (gymClass != null) {
                        if (!gymClass.wodId.isNullOrBlank()) {
                            loadWodForClass(gymClass)
                        } else {
                            _classDetailsState.value = ClassDetailsState.Success(gymClass, null)
                        }
                    } else {
                        _classDetailsState.value = ClassDetailsState.Error("Error al procesar datos de la clase")
                    }
                } else {
                    _classDetailsState.value = ClassDetailsState.Error("La clase no existe")
                }
            }
    }

    private fun loadWodForClass(gymClass: GymClass) {
        viewModelScope.launch {
            try {
                val wodDoc = firestore.collection("wods").document(gymClass.wodId!!).get().await()
                val wod = if (wodDoc.exists()) wodDoc.toObject(Wod::class.java) else null
                _classDetailsState.value = ClassDetailsState.Success(gymClass, wod)
            } catch (e: Exception) {
                _classDetailsState.value = ClassDetailsState.Success(gymClass, null)
            }
        }
    }

    // ========================================================================
    // RESERVA
    // ========================================================================
    fun bookClass(classId: String, user: User) {
        viewModelScope.launch {
            _bookingState.value = BookingState.Loading(classId)
            try {
                val classDoc = firestore.collection("gymClasses").document(classId).get().await()
                val gymClass = classDoc.toObject(GymClass::class.java) ?: throw IllegalStateException("Clase no encontrada")

                if (gymClass.dateTime == null) throw IllegalStateException("Fecha inválida")
                if (gymClass.enrolledUserIds.contains(user.id)) throw IllegalStateException("Ya estás inscrito")
                val isWaitingList = gymClass.enrolledUserIds.size >= gymClass.maxCapacity
                if (isWaitingList && gymClass.waitingList.contains(user.id)) throw IllegalStateException("Ya estás en lista de espera")

                val isAdminOrCoach = user.role == "owner" || user.role == "coach"
                if (!user.hasValidCredits && !isAdminOrCoach) throw IllegalStateException("Sin créditos")

                val now = Date()
                if (now.after(gymClass.dateTime)) throw IllegalStateException("Clase finalizada")

                firestore.runTransaction { tx ->
                    val classRef = firestore.collection("gymClasses").document(classId)

                    if (isWaitingList) {
                        // Lista de espera: no cobramos crédito aún.
                        tx.update(classRef, "waitingList", FieldValue.arrayUnion(user.id))
                    } else {
                        val userRef = firestore.collection("users").document(user.id)
                        val txRef = firestore.collection("credit_transactions").document()

                        tx.update(userRef, "credits", FieldValue.increment(-1))
                        tx.update(userRef, "currentClassesReserved", FieldValue.increment(1))
                        tx.update(classRef, "enrolledUserIds", FieldValue.arrayUnion(user.id))

                        val df = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                        val txData = hashMapOf(
                            "id" to txRef.id,
                            "userId" to user.id,
                            "gym_id" to user.gym_id,
                            "type" to "Reserva de Clase",
                            "amount" to -1,
                            "description" to "Reserva: ${gymClass.name} - ${df.format(gymClass.dateTime)}",
                            "date" to FieldValue.serverTimestamp(),
                            "relatedClassId" to classId
                        )
                        tx.set(txRef, txData)
                    }
                    null
                }.await()

                val successMsg = if (isWaitingList) "Estás en Lista de Espera" else "Reserva exitosa"
                _bookingState.value = BookingState.Success(successMsg)
                // El listener de nextBooking se actualizará automáticamente
            } catch (e: Exception) {
                _bookingState.value = BookingState.Error(e.message ?: "Error al reservar")
            }
        }
    }

    // ========================================================================
    // CANCELACIÓN
    // ========================================================================
    fun cancelBooking(classId: String, user: User) {
        viewModelScope.launch {
            _bookingState.value = BookingState.Loading(classId)
            try {
                val classDoc = firestore.collection("gymClasses").document(classId).get().await()
                val gymClass = classDoc.toObject(GymClass::class.java)
                    ?: throw IllegalStateException("Clase no encontrada")

                if (gymClass.dateTime == null) throw IllegalStateException("Fecha inválida")

                // --- INICIO DE VALIDACIÓN 30 MINUTOS ---
                val now = Date()
                val thirtyMinBefore = Date(gymClass.dateTime.time - (30 * 60 * 1000))
                val isAdminOrCoach = user.role == "owner" || user.role == "coach"

                // Si NO es admin/entrenador Y ya pasó el límite de tiempo (faltan menos de 30 min)
                if (now.after(thirtyMinBefore) && !isAdminOrCoach) {
                    throw IllegalStateException("No se puede cancelar con menos de 30 minutos de antelación.")
                }
                // --- FIN DE VALIDACIÓN ---

                val isWaitingListCancellation = gymClass.waitingList.contains(user.id)

                firestore.runTransaction { tx ->
                    val classRef = firestore.collection("gymClasses").document(classId)
                    val freshClassDoc = tx.get(classRef)
                    val currentEnrolled = freshClassDoc.get("enrolledUserIds") as? MutableList<String> ?: mutableListOf()
                    val currentWaiting = freshClassDoc.get("waitingList") as? MutableList<String> ?: mutableListOf()

                    if (isWaitingListCancellation) {
                        currentWaiting.remove(user.id)
                    } else {
                        currentEnrolled.remove(user.id)
                        
                        val userRef = firestore.collection("users").document(user.id)
                        val txRef = firestore.collection("credit_transactions").document()

                        tx.update(userRef, "credits", FieldValue.increment(1))
                        tx.update(userRef, "currentClassesReserved", FieldValue.increment(-1))

                        val txData = hashMapOf(
                            "id" to txRef.id,
                            "userId" to user.id,
                            "gym_id" to user.gym_id,
                            "type" to "Cancelación",
                            "amount" to 1,
                            "description" to "Devolución: ${gymClass.name}",
                            "date" to FieldValue.serverTimestamp(),
                            "relatedClassId" to classId
                        )
                        tx.set(txRef, txData)

                        // Promoción desde la lista de espera
                        if (currentWaiting.isNotEmpty()) {
                            val promotedId = currentWaiting.removeAt(0)
                            currentEnrolled.add(promotedId)
                            
                            val promotedRef = firestore.collection("users").document(promotedId)
                            tx.update(promotedRef, "credits", FieldValue.increment(-1))
                            tx.update(promotedRef, "currentClassesReserved", FieldValue.increment(1))

                            val pTxRef = firestore.collection("credit_transactions").document()
                            val pTxData = hashMapOf(
                                "id" to pTxRef.id,
                                "userId" to promotedId,
                                "gym_id" to user.gym_id,
                                "type" to "Reserva desde Espera",
                                "amount" to -1,
                                "description" to "Pase de Waitlist a Titular: ${gymClass.name}",
                                "date" to FieldValue.serverTimestamp(),
                                "relatedClassId" to classId
                            )
                            tx.set(pTxRef, pTxData)
                        }
                    }

                    tx.update(classRef, "enrolledUserIds", currentEnrolled)
                    tx.update(classRef, "waitingList", currentWaiting)
                    null
                }.await()

                _bookingState.value = BookingState.Success("Cancelación exitosa")
                // El listener de nextBooking se actualizará automáticamente
            } catch (e: Exception) {
                _bookingState.value = BookingState.Error(e.message ?: "Error al cancelar")
            }
        }
    }

    // Listeners generales
    fun listenForClassesOnDate(date: LocalDate) {
        classesListener?.remove()
        _classesState.value = ClassesState.Loading
        val gymId = UserSession.currentUserGymId.value ?: return

        val start = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val end = Date.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant())

        classesListener = firestore.collection("gymClasses")
            .whereEqualTo("gym_id", gymId)
            .whereGreaterThanOrEqualTo("dateTime", start)
            .whereLessThan("dateTime", end)
            .orderBy("dateTime", Query.Direction.ASCENDING)
            .addSnapshotListener { s, e ->
                if (e != null) { _classesState.value = ClassesState.Error(e.message ?: ""); return@addSnapshotListener }
                val list = s?.toObjects(GymClass::class.java) ?: emptyList()
                _classesState.value = ClassesState.Success(list)
            }
    }

    private fun fetchGymOperatingHours(gymId: String) {
        settingsListener?.remove()
        settingsListener = firestore.collection("gyms").document(gymId)
            .collection("settings").document("weekly_schedule_template")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val result = mutableMapOf<Int, GymOperatingHours>()
                    val data = snapshot.data ?: return@addSnapshotListener

                    for (day in 1..7) {
                        val key = "${day}_open_gym_ranges"
                        val rangesList = data[key] as? List<String>
                        if (rangesList != null) {
                            val parsedRanges = rangesList.mapNotNull { rangeStr ->
                                val parts = rangeStr.split("-")
                                if (parts.size == 2) TimeRange(parts[0], parts[1]) else null
                            }
                            if (parsedRanges.isNotEmpty()) {
                                result[day] = GymOperatingHours(day, parsedRanges)
                            }
                        }
                    }
                    _gymOperatingHours.value = result
                }
            }
    }

    fun resetBookingState() { _bookingState.value = BookingState.Idle }

    override fun onCleared() {
        classesListener?.remove()
        nextBookingListener?.remove()
        todayBookingListener?.remove() // [NEW]
        classDetailsListener?.remove()
        settingsListener?.remove()
        super.onCleared()
    }

    // ========================================================================
    // CHECK-IN CON QR
    // ========================================================================

    private val _checkInResult = MutableStateFlow<CheckInState>(CheckInState.Idle)
    val checkInResult = _checkInResult.asStateFlow()

    fun resetCheckInState() { _checkInResult.value = CheckInState.Idle }

    fun validateCheckIn(scannedGymId: String) {
        val currentUser = UserSession.currentUser.value ?: return

        // 1. Validar que sea el gimnasio correcto (Validación previa UI)
        if (scannedGymId != currentUser.gym_id) {
            _checkInResult.value = CheckInState.Error("Este código QR no es de tu gimnasio.")
            return
        }

        viewModelScope.launch {
            _checkInResult.value = CheckInState.Loading
            try {
                // 2. Buscar la clase ACTIVA
                val now = Date()
                val calendar = Calendar.getInstance()
                calendar.time = now; calendar.add(Calendar.MINUTE, -30); val timeStart = calendar.time
                calendar.time = now; calendar.add(Calendar.MINUTE, 60); val timeEnd = calendar.time

                val querySnapshot = firestore.collection("gymClasses")
                    .whereEqualTo("gym_id", currentUser.gym_id)
                    .whereArrayContains("enrolledUserIds", currentUser.id)
                    .whereGreaterThan("dateTime", timeStart)
                    .whereLessThan("dateTime", timeEnd)
                    .get().await()

                if (querySnapshot.isEmpty) {
                    _checkInResult.value = CheckInState.Error("No tienes clase agendada en este horario (+/- 30min).")
                    return@launch
                }

                val classId = querySnapshot.documents.first().id

                // 3. LLAMAR A CLOUD FUNCTION
                val functions = com.google.firebase.functions.FirebaseFunctions.getInstance("southamerica-east1")
                
                val data = hashMapOf(
                    "classId" to classId,
                    "scannedCode" to scannedGymId
                )

                // Usamos await() en lugar de continueWith para simplificar y usar corrutinas
                val task = functions.getHttpsCallable("registerAttendance").call(data).await()
                
                val result = task.data as? Map<String, Any>
                val success = result?.get("success") as? Boolean ?: false
                val message = result?.get("message") as? String ?: "Error desconocido"
                
                if (success) {
                    _checkInResult.value = CheckInState.Success(message)
                } else {
                    _checkInResult.value = CheckInState.Error(message)
                }

            } catch (e: Exception) {
                // Manejo de errores de Cloud Functions
                val errorMsg = if (e is com.google.firebase.functions.FirebaseFunctionsException) {
                    // Accedemos a las propiedades con el tipo correcto ya comprobado
                    "Error del servidor (${e.code}): ${e.message}"
                } else {
                    "Error de conexión: ${e.localizedMessage}"
                }
                _checkInResult.value = CheckInState.Error(errorMsg)
            }
        }
    }
}

// Coloca esto FUERA de la clase ScheduleViewModel (al final del archivo)
sealed class CheckInState {
    data object Idle : CheckInState()
    data object Loading : CheckInState()
    data class Success(val message: String) : CheckInState()
    data class Error(val message: String) : CheckInState()
}