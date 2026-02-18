package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class WodResult(
    @DocumentId val id: String = "",
    val userId: String = "",
    val wodId: String = "",
    val score: String = "",
    val numericScore: Double = 0.0, // Critical for Ranking Sorting
    val notes: String = "",
    // CRÍTICO: Renombrado de `rx` a `isRx` para sincronizar con iOS (WodResult.swift)
    val isRx: Boolean = true,
    @ServerTimestamp val date: Date? = null,
    val classSessionId: String? = null,
    val wodName: String? = null,
    val gym_id: String = "",
    
    // Desnormalized Data for Feed
    val userName: String = "",
    val userProfileImageUrl: String = "",
    val userLevel: String = "",
    val userGender: String = "",
    
    // Competencias (sincronizado con iOS)
    val isPublic: Boolean = true,
    val validationStatus: String? = null, // "pending", "approved", "rejected"
    val competitionId: String? = null
)