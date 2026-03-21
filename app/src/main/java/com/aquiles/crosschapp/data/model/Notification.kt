package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

// MARK: - Tipos sincronizados con iOS (Notification.swift)
// Los nombres coinciden con los rawValues de iOS para compatibilidad en Firestore.
enum class NotificationType {
    CREDIT_APPROVED,
    CREDIT_REJECTED,
    CREDIT_PENDING,
    EXPIRATION_WARNING,
    CLASS_CANCELLATION,
    SYSTEM_ALERT,
    GENERAL_ANNOUNCEMENT;

    companion object {
        fun fromString(value: String?): NotificationType {
            return entries.find { it.name == value } ?: GENERAL_ANNOUNCEMENT
        }
    }
}

data class Notification(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val gym_id: String = "",
    val title: String = "",
    // "message" es el campo principal. Se usa PropertyName para forzar el nombre exacto.
    @get:PropertyName("message") @set:PropertyName("message")
    var message: String = "",
    // CRÍTICO: Firestore CustomClassMapper quita el prefijo "is" de los booleanos en Kotlin
    // (busca setRead() en vez de setIsRead()). @PropertyName fuerza el nombre exacto "isRead".
    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false,
    val type: String = NotificationType.GENERAL_ANNOUNCEMENT.name,
    @ServerTimestamp
    val timestamp: Date? = null
)