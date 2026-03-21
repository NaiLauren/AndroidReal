package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class ChallengeResult(
    @DocumentId val id: String = "",
    val challengeId: String = "",
    val challengeName: String = "",
    val userId: String = "",
    val userName: String = "",
    val userLastName: String = "",
    val userProfileImage: String = "",
    val userLevel: String = "",
    val score: String = "",
    val numericScore: Double = 0.0,
    val isRx: Boolean = false,
    val notes: String = "",
    @ServerTimestamp val date: Date? = null,
    val gym_id: String = "",
    val validationStatus: String = "pending", // "pending", "validated", "rejected"
    val videoUrl: String? = null,
    val facilityName: String = "",
    val reactions: Map<String, String> = emptyMap()
)
