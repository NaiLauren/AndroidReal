package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class NotificationType {
    CREDIT_APPROVED,
    CREDIT_REJECTED,
    CREDIT_PENDING,
    EXPIRATION_WARNING,
    GENERAL_ANNOUNCEMENT
}

data class Notification(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val gym_id: String = "", // <-- ¡CRÍTICO! Añadido el campo para el multi-gimnasio
    val title: String = "",
    @PropertyName("body")
    val message: String = "",
    var isRead: Boolean = false,
    val type: String = NotificationType.GENERAL_ANNOUNCEMENT.name,
    @ServerTimestamp
    val timestamp: Date? = null
)