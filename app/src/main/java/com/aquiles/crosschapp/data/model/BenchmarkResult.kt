package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class BenchmarkResult(
    @DocumentId val id: String = "",
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
    val userGender: String = "",
    
    // Smart Ranking
    val numericScore: Double = 0.0,
    
    // Verification
    val isVerified: Boolean = false,

    // Social
    val isPublic: Boolean = true,
    val likeCount: Int = 0,
    
    // Competencias (sincronizado con iOS — BenchmarkResult.swift)
    val validationStatus: String? = null, // "pending", "approved", "rejected"
    val competitionId: String? = null,
    val facilityName: String? = null      // Para competencias inter-gym
)