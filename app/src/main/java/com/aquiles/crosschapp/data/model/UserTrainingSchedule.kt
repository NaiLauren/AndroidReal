package com.aquiles.crosschapp.data.model

import com.google.firebase.Timestamp
import java.util.Date
import java.util.UUID

data class UserTrainingIntention(
    val id: String = UUID.randomUUID().toString(),
    val dayOfWeek: Int = 1, // 1=Sunday, 2=Monday, ..., 7=Saturday
    val time: String = "00:00"
)

data class UserTrainingSchedule(
    val id: String = "",
    val userId: String = "",
    val gymId: String = "",
    val intentions: List<UserTrainingIntention> = emptyList(),
    val notificationsEnabled: Boolean = true,
    val timestamp: Timestamp? = Timestamp(Date())
)
