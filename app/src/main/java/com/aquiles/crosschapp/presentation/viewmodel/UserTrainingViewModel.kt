package com.aquiles.crosschapp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.UserTrainingIntention
import com.aquiles.crosschapp.data.model.UserTrainingSchedule
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import kotlinx.coroutines.delay
import com.aquiles.crosschapp.data.service.GamificationService

class UserTrainingViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    data class TimeRange(val startTime: String, val endTime: String)
    data class GymOperatingHours(val dayOfWeek: Int, val ranges: List<TimeRange>)

    private val _gymOperatingHours = MutableStateFlow<Map<Int, GymOperatingHours>>(emptyMap())
    val gymOperatingHours = _gymOperatingHours.asStateFlow()

    private val _userIntentions = MutableStateFlow<List<UserTrainingIntention>>(emptyList())
    val userIntentions = _userIntentions.asStateFlow()

    // Map: DayOfWeek -> Map: Hour(0-23) -> Count
    private val _heatmapData = MutableStateFlow<Map<Int, Map<Int, Int>>>(emptyMap())
    val heatmapData = _heatmapData.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(true)
    val notificationsEnabled = _notificationsEnabled.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _xpRewardMessage = MutableStateFlow<String?>(null)
    val xpRewardMessage = _xpRewardMessage.asStateFlow()

    private val currentUserGymId: String?
        get() = UserSession.currentUser.value?.gym_id
        
    private val currentUserId: String?
        get() = UserSession.currentUser.value?.id

    init {
        val user = UserSession.currentUser.value
        if (user != null && user.gym_id.isNotBlank()) {
            fetchGymOperatingHours(user.gym_id)
            fetchUserIntentions(user.id, user.gym_id)
            observeGymHeatmap(user.gym_id)
        }
    }

    private fun fetchGymOperatingHours(gymId: String) {
        viewModelScope.launch {
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
                    _gymOperatingHours.value = result
                }
            } catch (e: Exception) {
                Log.e("UserTrainingViewModel", "Error loading gym operating hours", e)
            }
        }
    }

    private fun fetchUserIntentions(userId: String, gymId: String) {
        val docId = userId // Sincronizado con iOS (Solo ID de usuario)
        firestore.collection("user_training_schedules").document(docId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("UserTrainingViewModel", "Error realtime user intentions", e)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val schedule = snapshot.toObject(UserTrainingSchedule::class.java)
                    if (schedule != null) {
                        _userIntentions.value = schedule.intentions
                        _notificationsEnabled.value = schedule.notificationsEnabled
                    }
                } else {
                    _userIntentions.value = emptyList()
                    _notificationsEnabled.value = true
                }
            }
    }

    private fun observeGymHeatmap(gymId: String) {
        firestore.collection("user_training_schedules")
            .whereEqualTo("gymId", gymId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("UserTrainingViewModel", "Listen failed for heatmap", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val newHeatmap = mutableMapOf<Int, MutableMap<Int, Int>>()
                    
                    for (doc in snapshot.documents) {
                        val schedule = doc.toObject(UserTrainingSchedule::class.java) ?: continue
                        for (intention in schedule.intentions) {
                            val parts = intention.time.split(":")
                            if (parts.size == 2) {
                                val hour = parts[0].toIntOrNull() ?: continue
                                val min = parts[1].toIntOrNull() ?: continue
                                
                                val startMins = (hour * 60) + min
                                val endMins = startMins + 75 // 75 minutes overlap definition

                                for (h in 0..23) {
                                    val hStart = h * 60
                                    val hEnd = hStart + 59
                                    if (startMins <= hEnd && endMins > hStart) {
                                        val dayMap = newHeatmap.getOrPut(intention.dayOfWeek) { mutableMapOf() }
                                        dayMap[h] = (dayMap[h] ?: 0) + 1
                                    }
                                }
                            }
                        }
                    }
                    _heatmapData.value = newHeatmap
                } else {
                    _heatmapData.value = emptyMap()
                }
            }
    }

    fun toggleIntention(day: Int, time: String) {
        val current = _userIntentions.value.toMutableList()
        val index = current.indexOfFirst { it.dayOfWeek == day && it.time == time }
        
        if (index != -1) {
            current.removeAt(index)
            _userIntentions.value = current
            saveIntentions(current)
        } else {
            val newIntention = UserTrainingIntention(dayOfWeek = day, time = time)
            current.add(newIntention)
            _userIntentions.value = current
            saveIntentions(current)
            
            // Recompensa XP
            val userId = currentUserId
            val gymId = currentUserGymId
            if (userId != null && gymId != null) {
                viewModelScope.launch {
                    val xpGained = GamificationService.awardSchedulingXP(userId, gymId, newIntention.id)
                    if (xpGained > 0) {
                        _xpRewardMessage.value = "+ $xpGained XP"
                        delay(3000)
                        if (_xpRewardMessage.value == "+ $xpGained XP") {
                            _xpRewardMessage.value = null
                        }
                    }
                }
            }
        }
    }


    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        saveIntentions(_userIntentions.value)
    }

    private fun saveIntentions(intentions: List<UserTrainingIntention>) {
        val userId = currentUserId ?: return
        val gymId = currentUserGymId ?: return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val docId = userId // Sincronizado con iOS
                val schedule = UserTrainingSchedule(
                    id = docId,
                    userId = userId,
                    gymId = gymId,
                    intentions = intentions,
                    notificationsEnabled = _notificationsEnabled.value,
                    timestamp = Timestamp(Date())
                )
                
                firestore.collection("user_training_schedules").document(docId).set(schedule).await()
                
                // Trigger local notifications check if needed...
                
            } catch (e: Exception) {
                Log.e("UserTrainingViewModel", "Error saving user intentions", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
