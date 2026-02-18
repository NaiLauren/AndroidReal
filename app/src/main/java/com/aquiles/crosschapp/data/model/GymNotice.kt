package com.aquiles.crosschapp.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class GymNotice(
    @DocumentId val id: String = "",
    @PropertyName("gym_id") val gymId: String = "",
    val title: String? = null,
    val message: String = "",
    @PropertyName("image_url") val imageUrl: String = "",
    @PropertyName("imageUrl") val imageUrlLegacy: String = "",
    val type: NoticeType? = null,
    val priority: NoticePriority? = null,
    @PropertyName("author_id") val authorId: String = "",
    @PropertyName("author_name") val authorName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    // Use direct nullable names to match Firestore keys exactly
    @PropertyName("isActive") val isActiveField: Boolean? = null, 
    val active: Boolean? = null, // Legacy field
    val viewCount: Int = 0
) {
    // Helper to get the actual image URL regardless of field name
    val actualImageUrl: String
        get() = if (imageUrl.isNotBlank()) imageUrl else imageUrlLegacy

    // Logic to determine if notice is effectively active
    // Prioritize 'isActive' if present. Fallback to 'active'. Default to true.
    fun isEffectivelyActive(): Boolean {
        return isActiveField ?: active ?: true
    }
}

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
