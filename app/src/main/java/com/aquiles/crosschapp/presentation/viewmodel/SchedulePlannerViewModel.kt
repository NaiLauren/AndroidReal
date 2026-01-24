package com.aquiles.crosschapp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.GymClass // Assuming this is the equivalent of ClassSession
import com.aquiles.crosschapp.data.model.Wod
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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

    private val _scheduleTemplate = MutableStateFlow<List<String>>(emptyList())
    val scheduleTemplate = _scheduleTemplate.asStateFlow()

    // Assuming UserSession is available as a singleton or injected
    // We'll use UserSession.currentUserGymId.value ideally
    private val gymId: String?
        get() = UserSession.currentUserGymId.value

    fun fetchScheduleTemplate() {
        val gid = gymId ?: return
        viewModelScope.launch {
            try {
                val snapshot = db.collection("gyms").document(gid)
                    .collection("settings").document("schedule_template")
                    .get().await()
                
                if (snapshot.exists()) {
                    val times = snapshot.get("available_times") as? List<String> ?: emptyList()
                    _scheduleTemplate.value = times.sorted()
                }
            } catch (e: Exception) {
                Log.e("SchedulePlannerVM", "Error fetching template", e)
            }
        }
    }

    // Main Batch Creation Logic (Ported from Swift)
    fun createClassesBatch(
        startDate: Date,
        selectedTimes: Set<String>,
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
                        "id" to newWodRef.id,
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
                    for (timeString in selectedTimes) {
                        val fullDate = combineDateAndTime(date, timeString) ?: continue
                        
                        val newClassRef = db.collection("gymClasses").document()
                        val newClass = hashMapOf(
                            "id" to newClassRef.id,
                            "gym_id" to gid,
                            "name" to className,
                            "description" to description,
                            "coachName" to coachName,
                            "maxCapacity" to capacity,
                            "durationMinutes" to durationMinutes,
                            "dateTime" to Timestamp(fullDate),
                            "classType" to "WOD",
                            "attendees" to emptyList<String>(),
                            "waitlist" to emptyList<String>(),
                            "wod_id" to wodId, // Nullable
                            "isCancelled" to false
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
