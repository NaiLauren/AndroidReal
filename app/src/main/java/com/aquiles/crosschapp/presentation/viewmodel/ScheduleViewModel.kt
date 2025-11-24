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

    private val _bookingState = MutableStateFlow<BookingState>(BookingState.Idle)
    val bookingState: StateFlow<BookingState> = _bookingState.asStateFlow()

    private val _nextBookingState = MutableStateFlow<NextBookingState>(NextBookingState.Loading)
    val nextBookingState: StateFlow<NextBookingState> = _nextBookingState.asStateFlow()

    private val _classDetailsState = MutableStateFlow<ClassDetailsState>(ClassDetailsState.Idle)
    val classDetailsState: StateFlow<ClassDetailsState> = _classDetailsState.asStateFlow()

    // Listeners
    private var classesListener: ListenerRegistration? = null
    private var nextBookingListener: ListenerRegistration? = null
    private var classDetailsListener: ListenerRegistration? = null

    init {
        // Iniciar escucha de la próxima reserva apenas tengamos usuario
        viewModelScope.launch {
            UserSession.currentUser.collect { u ->
                if (u != null && u.gym_id.isNotEmpty()) {
                    listenForNextBooking(u.id, u.gym_id)
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
                    // Mientras tanto, mostramos error o null.
                    _nextBookingState.value = NextBookingState.Error(e.message ?: "Error loading next booking")
                    return@addSnapshotListener
                }

                val nextClass = s?.documents?.firstOrNull()?.toObject(GymClass::class.java)?.copy(id = s.documents.first().id)
                _nextBookingState.value = NextBookingState.Success(nextClass)
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
                    val gymClass = snapshot.toObject(GymClass::class.java)?.copy(id = snapshot.id)

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
                if (gymClass.enrolledUserIds.size >= gymClass.maxCapacity) throw IllegalStateException("Clase llena")

                val isAdminOrCoach = user.role == "owner" || user.role == "coach"
                if (!user.hasValidCredits && !isAdminOrCoach) throw IllegalStateException("Sin créditos")

                val now = Date()
                val thirtyMinBefore = Date(gymClass.dateTime.time - (30 * 60 * 1000))
                if (now.after(gymClass.dateTime)) throw IllegalStateException("Clase finalizada")

                // Permitir reservar tarde si hay cupo (opcional, ajustado a tu regla de negocio)
                // if (now.after(thirtyMinBefore) && gymClass.enrolledUserIds.isEmpty()) throw IllegalStateException("Clase cerrada")

                firestore.runTransaction { tx ->
                    val userRef = firestore.collection("users").document(user.id)
                    val classRef = firestore.collection("gymClasses").document(classId)
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
                    null
                }.await()

                _bookingState.value = BookingState.Success("Reserva exitosa")
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
                val gymClass = classDoc.toObject(GymClass::class.java) ?: throw IllegalStateException("Clase no encontrada")

                val now = Date()
                val thirtyMinBefore = Date(gymClass.dateTime!!.time - (30 * 60 * 1000))
                val isAdminOrCoach = user.role == "owner" || user.role == "coach"

                if (now.after(thirtyMinBefore) && !isAdminOrCoach) throw IllegalStateException("Fuera de tiempo para cancelar")

                firestore.runTransaction { tx ->
                    val userRef = firestore.collection("users").document(user.id)
                    val classRef = firestore.collection("gymClasses").document(classId)
                    val txRef = firestore.collection("credit_transactions").document()

                    tx.update(userRef, "credits", FieldValue.increment(1))
                    tx.update(userRef, "currentClassesReserved", FieldValue.increment(-1))
                    tx.update(classRef, "enrolledUserIds", FieldValue.arrayRemove(user.id))

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
                val list = s?.toObjects(GymClass::class.java)?.mapNotNull { it.copy(id = it.id) } ?: emptyList()
                _classesState.value = ClassesState.Success(list)
            }
    }

    fun resetBookingState() { _bookingState.value = BookingState.Idle }

    override fun onCleared() {
        classesListener?.remove()
        nextBookingListener?.remove()
        classDetailsListener?.remove()
        super.onCleared()
    }
}