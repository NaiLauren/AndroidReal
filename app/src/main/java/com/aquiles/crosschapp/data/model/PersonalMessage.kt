// RUTA: data/model/PersonalMessage.kt
// VERSIÓN FINAL CON LA CORRECCIÓN DE @PropertyName

package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName // <-- ¡ASEGÚRATE DE AÑADIR ESTA IMPORTACIÓN!
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class PersonalMessage(
    @DocumentId val id: String = "",
    val gym_id: String = "",
    val userId: String = "",
    val sender_id: String = "",
    val sender_name: String = "",
    val content: String = "",

    // --- LOS CAMPOS NUEVOS DEBEN ESTAR AQUÍ ---
    val attachmentUrl: String? = null,
    val attachmentType: String? = null,
    @get:PropertyName("isRead")
    val isRead: Boolean = false,

    @ServerTimestamp val timestamp: Date? = null
)