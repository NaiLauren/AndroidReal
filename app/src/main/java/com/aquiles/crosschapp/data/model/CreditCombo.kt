package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

// N-ACTUALIZADO: Ahora compatible con CreditPack de iOS.
// Mantiene retrocompatibilidad con documentos legacy que no tienen los campos nuevos.
data class CreditCombo(
    @DocumentId val id: String = "",
    val gym_id: String = "",
    val name: String = "",           // iOS: name
    val creditsAwarded: Int = 0,     // iOS: creditsAwarded
    val price: Double = 0.0,         // iOS: price (precio base)
    val description: String = "",

    // N-NUEVO: Campos de CreditPack de iOS
    val surchargePrice: Double? = null,  // Precio con recargo (si isSurchargeActive)
    val isUnlimited: Boolean? = null,    // true = Pase Libre (sin límite de clases)
    val durationDays: Int? = null,       // Duración en días (default 30 si null)
    val order: Int = 0,                  // Orden de visualización en la lista

    // iOS usa "active" como campo en Firestore (mapeado desde isActive)
    @get:PropertyName("active") val isActive: Boolean = true
)