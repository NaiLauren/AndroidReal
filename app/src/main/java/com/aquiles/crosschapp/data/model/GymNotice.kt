package com.aquiles.crosschapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class GymNotice(
    @DocumentId val id: String = "",
    @get:PropertyName("gym_id") val gymId: String = "",
    val title: String? = null, // iOS compatible - can be null
    val message: String = "", // Default empty for iOS docs (iOS doesn't have this field)
    val imageUrl: String = "",
    val type: NoticeType? = null, // Nullable - iOS doesn't have this
    val priority: NoticePriority? = null, // Nullable - iOS doesn't have this
    val authorId: String = "", // Default empty - iOS doesn't have this
    val authorName: String = "", // Default empty - iOS doesn't have this
    @ServerTimestamp val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    @get:PropertyName("isActive") val isActive: Boolean = true,
    val viewCount: Int = 0 // Default 0 - iOS doesn't have this
)

enum class NoticeType {
    GENERAL,
    CLASS_CANCELLED,
    EVENT,
    PRICE_CHANGE,
    ACHIEVEMENT,
    ANNOUNCEMENT;

    fun displayName(): String {
        return when(this) {
            GENERAL -> "General"
            CLASS_CANCELLED -> "Clase Cancelada"
            EVENT -> "Evento"
            PRICE_CHANGE -> "Precios"
            ACHIEVEMENT -> "Logro"
            ANNOUNCEMENT -> "Anuncio"
        }
    }
}

enum class NoticePriority {
    LOW,
    NORMAL,
    HIGH,
    PINNED;

    fun displayName(): String {
        return when(this) {
            LOW -> "Baja"
            NORMAL -> "Normal"
            HIGH -> "Alta (Urgente)"
            PINNED -> "Fijado"
        }
    }
}
