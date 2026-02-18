// RUTA: data/model/CreditRequest.kt

package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class CreditRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}

data class CreditRequest(
    @DocumentId
    val id: String = "",

    // --- Información del Solicitante ---
    val gym_id: String = "",
    val packId: String = "",
    val userId: String = "",
    val userName: String = "",

    // --- Información del Pedido ---
    val comboName: String = "",
    val creditsRequested: Int = 0,
    val amountPaid: Double = 0.0,
    val paymentMethod: String = "",
    val contactInfo: String? = null,
    val paymentProofUrl: String? = null,

    // N-NUEVO: Campos de CreditPack avanzado (paridad con iOS)
    val isUnlimited: Boolean? = null,  // true = Pase Libre
    val durationDays: Int? = null,     // Duración en días (default 30 si null)

    val fcmToken: String? = null,

    // --- Estado y Fechas del Proceso ---
    var status: String = CreditRequestStatus.PENDING.name,

    @ServerTimestamp
    val requestDate: Date? = null,

    // --- Información de Procesamiento (llenada por el admin) ---
    var processedByAdminId: String? = null,
    var processedByAdminName: String? = null,

    // N-NUEVO: Razón de rechazo (iOS escribe este campo, Android ahora lo lee)
    var rejectionReason: String? = null,

    @ServerTimestamp
    var processedDate: Date? = null,

    val newCreditValidUntil: Date? = null
)