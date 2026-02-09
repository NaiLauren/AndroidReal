package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class BenchmarkResult(
    @DocumentId val id: String = "", // Changed from resultId to match iOS
    val userId: String = "",
    val gym_id: String = "",
    val benchmarkId: String = "",
    val benchmarkName: String = "",
    val score: String = "",
    val isRx: Boolean = true,
    val notes: String = "",
    @ServerTimestamp val date: Date? = null,
    
    // Desnormalized Data for Feed & Ranking
    val userName: String = "",
    val userLastName: String = "",
    val userLevel: String = "",
    val userProfileImageUrl: String = "",
    val userGender: String = "", // Snapshot of user gender
    
    // Smart Ranking
    val numericScore: Double = 0.0, // For sorting
    
    // Verification
    val isVerified: Boolean = false,

    // Social
    val isPublic: Boolean = true,
    val likeCount: Int = 0 // Added for iOS compatibility (social feed likes)
)