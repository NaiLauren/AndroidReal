package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class CreditTransaction(
    // CORRECCIÓN: Se eliminó @DocumentId para evitar conflicto con el campo 'id' que ya existe en la BD.
    val id: String = "",

    val userId: String = "",
    val gym_id: String = "",

    @ServerTimestamp
    val date: Date? = null,

    val amount: Int = 0, // Ej: +4, -1
    val type: String = "", // Ej: "Compra", "Reserva de Clase", etc.
    val description: String = ""
)