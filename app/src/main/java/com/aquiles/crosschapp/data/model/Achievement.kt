package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Achievement(
    @DocumentId val achievementId: String = "",
    val title: String = "",
    val description: String = "",
    val iconName: String = "",
    val type: String = "automatic",
    val userId: String = "",       // <-- CAMPO AÑADIDO
    val gym_id: String = "",       // <-- CAMPO AÑADIDO
    @ServerTimestamp val unlockedAt: Date? = null
)