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
    // CRÍTICO: Cambiado de @PropertyName("body") a "message" para sincronizar con iOS.
    // iOS siempre escribe y lee el campo "message". Android ahora hace lo mismo.
    val message: String = "",
    var isRead: Boolean = false,
    val type: String = NotificationType.GENERAL_ANNOUNCEMENT.name,
    @ServerTimestamp
    val timestamp: Date? = null
)