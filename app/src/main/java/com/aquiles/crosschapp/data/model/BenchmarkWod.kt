package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.DocumentId

data class BenchmarkWod(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val scoreType: String = "", // TIME, REPS, WEIGHT, etc. (Legacy compatibility)
    val measurementUnit: String = "TIME", // TIME, WEIGHT, REPS, DISTANCE, PERCENTAGE
    val sortOrder: String = "ASC", // ASC (Time), DESC (Weight, Reps)
    val strategy: String = "",
    val gym_id: String = ""
)