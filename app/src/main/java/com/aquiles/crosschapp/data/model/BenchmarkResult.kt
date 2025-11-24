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
    @ServerTimestamp val date: Date? = null
)