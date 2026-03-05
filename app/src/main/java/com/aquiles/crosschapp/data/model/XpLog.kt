package com.aquiles.crosschapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class XpLog(
    @DocumentId val id: String = "",
    val amount: Int = 0,
    val type: String = "", // "ATTENDANCE", "ACHIEVEMENT", "BONUS"
    val title: String = "",
    val description: String = "",
    val icon: String = "",
    val gym_id: String = "",
    val timestamp: Timestamp? = null,
    val relatedId: String? = null
)
