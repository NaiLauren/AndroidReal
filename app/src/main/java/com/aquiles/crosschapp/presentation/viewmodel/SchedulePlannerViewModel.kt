package com.aquiles.crosschapp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.GymClass
// import com.aquiles.crosschapp.presentation.viewmodel.UserSession // Same package, not needed
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import java.util.UUID

// Matches iOS logic for Schedule Planner
class SchedulePlannerViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    
    // UI State
    private val _creationState = MutableStateFlow<String?>(null) // null = idle, else message
    val creationState = _creationState.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _scheduleTemplate = MutableStateFlow<List<AdminViewModel.TemplateSlot>>(emptyList())
    val scheduleTemplate = _scheduleTemplate.asStateFlow()

    // Assuming UserSession is available as a singleton or injected
    // We'll use UserSession.currentUserGymId.value ideally
    private val gymId: String?
        get() = UserSession.currentUserGymId.value

    fun fetchScheduleTemplate(date: Date? = null) {
        val gid = gymId ?: return
        viewModelScope.launch {
            try {
                // 1. Try to fetch Weekly Schedule First
                val weeklyDoc = db.collection("gyms").document(gid)
                    .collection("settings").document("weekly_schedule_template")
                    .get().await()

                var foundWeekly = false
                if (weeklyDoc.exists() && date != null) {
                    val data = weeklyDoc.data
                    if (data != null) {
                        val calendar = Calendar.getInstance()
                        calendar.time = date
                        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon...
                        
                        // Keys are strings "1", "2"...
                        val times = (data[dayOfWeek.toString()] as? List<*>)?.filterIsInstance<String>()
                        
                        if (times != null) {
                            _scheduleTemplate.value = times.map { AdminViewModel.TemplateSlot.fromFirestoreString(it) }.sortedBy { it.time }
                            foundWeekly = true
                        }
                    }
                }

                // 2. Fallback to general template if no weekly schedule found for this day
                if (!foundWeekly) {
                    val snapshot = db.collection("gyms").document(gid)
                        .collection("settings").document("schedule_template")
                        .get().await()
                    
                    if (snapshot.exists()) {
                        val times = (snapshot.get("available_times") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        _scheduleTemplate.value = times.map { AdminViewModel.TemplateSlot(time = it, isOpenGym = false) }.sortedBy { it.time }
                    } else {
                         _scheduleTemplate.value = emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e("SchedulePlannerVM", "Error fetching template", e)
                _scheduleTemplate.value = emptyList()
            }
        }
    }

    // --- NEW STATE FOR PARITY ---
    private val _classesForSelectedDate = MutableStateFlow<List<GymClass>>(emptyList())
    val classesForSelectedDate = _classesForSelectedDate.asStateFlow()

    private val _selectedClassIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedClassIds = _selectedClassIds.asStateFlow()

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode = _isSelectionMode.asStateFlow()

    private var classesListener: ListenerRegistration? = null
    
    // --- ACTIONS ---

    fun fetchClasses(date: Date) {
        val gid = gymId ?: return
        
        // Calculate start and end of day
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.time
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.time

        classesListener?.remove()
        classesListener = db.collection("gymClasses")
            .whereEqualTo("gym_id", gid)
            .whereGreaterThanOrEqualTo("dateTime", Timestamp(startOfDay))
            .whereLessThan("dateTime", Timestamp(endOfDay))
            // .orderBy("dateTime") // Requires index, usually fine or sort locally
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("SchedulePlannerVM", "Listen failed", e)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val classes = snapshot.toObjects(GymClass::class.java)
                    // Sort locally to avoid index issues for now
                    _classesForSelectedDate.value = classes.sortedBy { it.dateTime }
                    
                    // Auto-Cancel Logic (Parity)
                    autoCancelEmptyClassesIfNeeded(classes)
                }
            }
    }

    private fun autoCancelEmptyClassesIfNeeded(classes: List<GymClass>) {
        val now = Date()
        val calendar = Calendar.getInstance()
        calendar.time = now
        calendar.add(Calendar.MINUTE, 30) // Close enough to start
        val threshold = calendar.time
        
        val batch = db.batch()
        var ops = 0
        
        classes.forEach { gymClass ->
            if (!gymClass.isCancelled && gymClass.enrolledUserIds.isEmpty()) {
                val classTime = gymClass.dateTime ?: return@forEach
                // Logic: If class is in the past OR starting very soon (e.g. within 30 mins) and EMPTY -> Cancel
                // iOS logic might be strictly "past" or configurable. Let's assume "past or imminent"
                if (classTime.before(threshold)) {
                    val ref = db.collection("gymClasses").document(gymClass.id)
                    // CORREGIDO: Era "cancelled", ahora "isCancelled" para sincronizar con iOS
                    batch.update(ref, "isCancelled", true)
                    ops++
                }
            }
        }
        
        if (ops > 0) {
            batch.commit().addOnSuccessListener { Log.d("SchedulePlannerVM", "Auto-cancelled $ops classes") }
        }
    }

    fun toggleSelection(classId: String) {
        val current = _selectedClassIds.value.toMutableSet()
        if (current.contains(classId)) {
            current.remove(classId)
        } else {
            current.add(classId)
        }
        _selectedClassIds.value = current
        _isSelectionMode.value = current.isNotEmpty()
    }

    fun selectAll() {
        val allIds = _classesForSelectedDate.value.map { it.id }.toSet()
        _selectedClassIds.value = allIds
        _isSelectionMode.value = allIds.isNotEmpty()
    }

    fun clearSelection() {
        _selectedClassIds.value = emptySet()
        _isSelectionMode.value = false
    }
    
    fun prepareBatchEdit() {
        // Just ensures mode is on, logic is mainly in UI
        _isSelectionMode.value = true
    }
    
    fun getFirstSelectedClass(): GymClass? {
        val firstId = _selectedClassIds.value.firstOrNull() ?: return null
        return _classesForSelectedDate.value.find { it.id == firstId }
    }

    // Combined Create/Update
    fun createOrUpdateBatch(
        startDate: Date,
        selectedTimes: Set<AdminViewModel.TemplateSlot>,
        selectedWeekdays: Set<Int>,
        repeatMonths: Int,
        className: String,
        coachName: String,
        description: String,
        capacity: Int,
        durationMinutes: Int,
        createWod: Boolean,
        wodScoreType: String,
        isUpdateMode: Boolean = false
    ) {
        if (isUpdateMode) {
            updateBatchClasses(className, coachName, description, capacity, durationMinutes, createWod, wodScoreType, startDate)
        } else {
            createClassesBatch(startDate, selectedTimes, selectedWeekdays, repeatMonths, className, coachName, description, capacity, durationMinutes, createWod, wodScoreType)
        }
    }

    private fun updateBatchClasses(
        className: String,
        coachName: String,
        description: String,
        capacity: Int,
        durationMinutes: Int,
        createWod: Boolean,
        wodScoreType: String,
        dateForWod: Date // Used for WOD date if creating new
    ) {
        val idsToUpdate = _selectedClassIds.value
        if (idsToUpdate.isEmpty()) return
        
        _isLoading.value = true
        _creationState.value = "Actualizando..."
        
        viewModelScope.launch {
            try {
                // 1. Prepare WOD if needed (reuse logic or create new one for the batch?)
                // If updating multiple classes, usually they might share the WOD if they are on same day. 
                // But if we selected classes across multiple days (not possible with current UI which selects per day), they would share one WOD date?
                // Simplified: If creating WOD, create ONE for the batch (assuming same day) OR update existing?
                // Let's assume Create WOD = Create NEW WOD and link to all.
                
                var newWodId: String? = null
                if (createWod) {
                     // Check if they already have same WOD? rare. Create new.
                    val newWodRef = db.collection("wods").document()
                    val newWod = hashMapOf(
                        "title" to className,
                        "type" to "Daily",
                        "description" to description,
                        "date" to Timestamp(dateForWod), // Use the date passed (start date currently)
                        "scoreType" to wodScoreType,
                        "notes" to "Batch Updated",
                        "createdAt" to Timestamp.now(),
                        "gym_id" to (gymId ?: "")
                    )
                    newWodId = newWodRef.id
                    newWodRef.set(newWod).await()
                }

                // 2. Batch Update
                val batch = db.batch()
                var batchCount = 0
                
                for (id in idsToUpdate) {
                    val ref = db.collection("gymClasses").document(id)
                    val updates = mutableMapOf<String, Any>(
                        "name" to className,
                        "coachName" to coachName,
                        "description" to description,
                        "maxCapacity" to capacity,
                        "durationMinutes" to durationMinutes,
                        "isOpenGym" to false
                    )
                    if (newWodId != null) {
                        updates["wod_id"] = newWodId
                    }
                    // If we want to CLEAR wod? Assume if createWod=false we don't clear, just don't add new.
                    
                    batch.update(ref, updates)
                    batchCount++
                    
                    if (batchCount >= 400) {
                         batch.commit().await()
                         batchCount = 0
                    }
                }
                
                if (batchCount > 0) batch.commit().await()
                
                _creationState.value = "Success: Actualizadas ${idsToUpdate.size} clases."
                _isLoading.value = false
                clearSelection()
                
            } catch (e: Exception) {
                Log.e("SchedulePlannerVM", "Update error", e)
                _creationState.value = "Error: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    // Original Batch Creation Logic (Renamed/Kept)
    private fun createClassesBatch(
        startDate: Date,
        selectedTimes: Set<AdminViewModel.TemplateSlot>,
        selectedWeekdays: Set<Int>, // Calendar.SUNDAY = 1, etc.
        repeatMonths: Int,
        className: String,
        coachName: String,
        description: String,
        capacity: Int,
        durationMinutes: Int,
        createWod: Boolean,
        wodScoreType: String
    ) {
        val gid = gymId ?: return
        if (selectedTimes.isEmpty()) {
            _creationState.value = "Error: No has seleccionado horarios."
            return
        }

        _isLoading.value = true
        _creationState.value = "Generando..."

        viewModelScope.launch {
            try {
                // 1. Calculate Dates
                val calendar = Calendar.getInstance()
                calendar.time = startDate
                // Reset to start of day? Swift did startOfDay.
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val cleanStartDate = calendar.time

                val endDateCalendar = Calendar.getInstance()
                endDateCalendar.time = cleanStartDate
                
                if (repeatMonths > 0) {
                    endDateCalendar.add(Calendar.MONTH, repeatMonths)
                } else {
                    endDateCalendar.add(Calendar.DAY_OF_MONTH, 1)
                }
                val cleanEndDate = endDateCalendar.time

                val datesToProcess = mutableListOf<Date>()
                val tempCal = Calendar.getInstance()
                tempCal.time = cleanStartDate

                // If selectedWeekdays is empty, assuming just the specific start date (or all days? iOS seemingly defaulted to start date's weekday if empty or user selection mode. Let's assume user must select weekdays if repeating)
                // iOS logic: if empty, use current day's weekday.
                val targetWeekdays = if (selectedWeekdays.isEmpty()) {
                    val d = Calendar.getInstance(); d.time = cleanStartDate
                    setOf(d.get(Calendar.DAY_OF_WEEK))
                } else {
                    selectedWeekdays
                }

                while (tempCal.time.before(cleanEndDate)) {
                    val dayOfWeek = tempCal.get(Calendar.DAY_OF_WEEK)
                    if (targetWeekdays.contains(dayOfWeek)) {
                        datesToProcess.add(tempCal.time)
                    }
                    tempCal.add(Calendar.DAY_OF_MONTH, 1)
                }

                // 2. Prepare WOD (Optional)
                var wodId: String? = null
                if (createWod) {
                    val newWodRef = db.collection("wods").document()
                    val newWod = hashMapOf(
                        // "id" REMOVED: Managed by @DocumentId in Wod model
                        "title" to className, // Using class name as WOD title
                        "type" to "Daily",
                        "description" to description,
                        "date" to Timestamp(cleanStartDate), // Map to start date
                        "scoreType" to wodScoreType,
                        "notes" to "Created via Batch Planner",
                        "createdAt" to Timestamp.now(),
                        "gym_id" to gid
                    )
                    // We can't batch set() and add() easily mixed, but we can put it in batch.
                    // Doing separate write for WOD to allow ID usage, or put in batch.
                    // Let's put in batch.
                    // Actually, we must create the WOD document ref first.
                    wodId = newWodRef.id
                    db.collection("wods").document(wodId).set(newWod).await()
                }

                // 3. Batch Writes
                // Split into chunks of 400 to be safe (limit 500)
                val totalOps = datesToProcess.size * selectedTimes.size
                var batch = db.batch()
                var operationCount = 0

                for (date in datesToProcess) {
                    for (slot in selectedTimes) {
                        val fullDate = combineDateAndTime(date, slot.time) ?: continue
                        
                        val newClassRef = db.collection("gymClasses").document()
                        val newClass = hashMapOf(
                            // "id" REMOVED: Managed by @DocumentId annotation in GymClass
                            "gym_id" to gid,
                            "name" to if (slot.isOpenGym) "Open Gym" else className,
                            "description" to if (slot.isOpenGym) "Espacio libre reservable" else description,
                            "coachName" to if (slot.isOpenGym) "Staff / Libre" else coachName,
                            "maxCapacity" to capacity,
                            "durationMinutes" to durationMinutes,
                            "dateTime" to Timestamp(fullDate),
                            "classType" to if (slot.isOpenGym) "OPEN_GYM" else "WOD",
                            "attendees" to emptyList<String>(),
                            "waitlist" to emptyList<String>(),
                            "wod_id" to wodId, // Nullable
                            "isCancelled" to false,
                            "isOpenGym" to slot.isOpenGym
                        )

                        batch.set(newClassRef, newClass)
                        operationCount++

                        if (operationCount >= 400) {
                            batch.commit().await()
                            batch = db.batch()
                            operationCount = 0
                        }
                    }
                }

                if (operationCount > 0) {
                    batch.commit().await()
                }

                _creationState.value = "Success: Created $totalOps classes."
                _isLoading.value = false

            } catch (e: Exception) {
                Log.e("SchedulePlannerVM", "Batch error", e)
                _creationState.value = "Error: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun clearMessage() {
        _creationState.value = null
    }

    private fun combineDateAndTime(date: Date, timeString: String): Date? {
        val parts = timeString.split(":")
        if (parts.size != 2) return null
        
        val cal = Calendar.getInstance()
        cal.time = date
        cal.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
        cal.set(Calendar.MINUTE, parts[1].toInt())
        cal.set(Calendar.SECOND, 0)
        return cal.time
    }
}
