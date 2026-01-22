package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class WodResult(
    val userId: String = "",
    val wodId: String = "",
    val score: String = "",
    val notes: String = "",
    @PropertyName("rx")
    val isRx: Boolean = true,
    @ServerTimestamp val date: Date? = null,
    val gym_id: String = ""
)