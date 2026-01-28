package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class BenchmarkResult(
    @DocumentId val resultId: String = "",
    val userId: String = "",
    val gym_id: String = "",
    val benchmarkId: String = "",
    val benchmarkName: String = "",
    val score: String = "",
    val isRx: Boolean = true,
    val notes: String = "",
    @ServerTimestamp val date: Date? = null,
    
    // Desnormalized Data for Feed
    val userName: String = "",
    val userLastName: String = "",
    val userLevel: String = "",
    val userProfileImageUrl: String = "", // Opcional
    
    // Verification
    val isVerified: Boolean = false
)