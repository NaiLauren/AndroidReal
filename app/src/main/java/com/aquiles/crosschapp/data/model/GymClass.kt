package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.util.Date

@IgnoreExtraProperties
data class GymClass(
    @DocumentId
    val documentId: String = "",
    val name: String = "",
    val gym_id: String = "",
    val description: String = "",
    val dateTime: Date? = null,
    val durationMinutes: Int = 0,
    val maxCapacity: Int = 0,
    val coachName: String = "",
    
    @PropertyName("wod_id")
    val wodId: String? = null,
    
    val wodScoreType: String? = null,

    val enrolledUserIds: List<String> = emptyList(),

    // CRÍTICO CORREGIDO: Antes era @PropertyName("cancelled") lo que causaba que
    // una clase cancelada desde iOS (que escribe "isCancelled") apareciera activa en Android.
    // Ahora ambas plataformas leen y escriben el campo "isCancelled" en Firestore.
    val isCancelled: Boolean = false,
    val attendanceTaken: Boolean = false,
    val attendedUserIds: List<String> = emptyList(), // Lista manual del admin

    // --- AGREGAR ESTA LÍNEA (Es para el QR) ---
    val checkedInUserIds: List<String> = emptyList(),
    // ------------------------------------------

    val classType: String = "WOD",

    @PropertyName("hexColor")
    val hexColor: String = "#FC5200"
) {
    // Backward compatibility for reads
    val id: String
        get() = documentId
}