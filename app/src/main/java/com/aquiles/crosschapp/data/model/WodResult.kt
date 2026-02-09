package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class WodResult(
    @DocumentId val id: String = "",
    val userId: String = "",
    val wodId: String = "",
    val score: String = "",
    val numericScore: Double = 0.0, // Critical for Ranking Sorting
    val notes: String = "",
    @PropertyName("rx")
    val isRx: Boolean = true,
    @ServerTimestamp val date: Date? = null,
    val classSessionId: String? = null, // Multi-session support
    val wodName: String? = null, // [Fix] Store class name
    val gym_id: String = "",
    
    // Desnormalized Data for Feed
    val userName: String = "",
    val userProfileImageUrl: String = "",
    val userLevel: String = "",
    val userGender: String = ""
)