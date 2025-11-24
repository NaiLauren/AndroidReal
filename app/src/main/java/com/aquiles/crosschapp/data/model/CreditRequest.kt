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
    val userId: String = "",
    val userName: String = "",

    // --- Información del Pedido ---
    val comboName: String = "",
    val creditsRequested: Int = 0,
    val amountPaid: Double = 0.0,
    val paymentMethod: String = "",
    val contactInfo: String? = null,
    val paymentProofUrl: String? = null,

    // --- CAMBIO CLAVE ---
    // Guardamos el token del dispositivo que hizo la solicitud.
    // Esto es para una lógica futura más robusta, aunque no se use de inmediato
    // en la Cloud Function multidispositivo, es una excelente práctica tenerlo.
    val fcmToken: String? = null,

    // --- Estado y Fechas del Proceso ---
    var status: String = CreditRequestStatus.PENDING.name,

    @ServerTimestamp
    val requestDate: Date? = null,

    // --- Información de Procesamiento (llenada por el admin) ---
    var processedByAdminId: String? = null,
    var processedByAdminName: String? = null,

    // El @ServerTimestamp aquí es la clave
    @ServerTimestamp
    var processedDate: Date? = null,

    val newCreditValidUntil: Date? = null
)