package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.Date

data class GymClass(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val gym_id: String = "",
    val description: String = "",
    val dateTime: Date? = null,
    val durationMinutes: Int = 0,
    val maxCapacity: Int = 0,
    val coachName: String = "",
    val wodId: String? = null,

    val enrolledUserIds: List<String> = emptyList(),

    val isCancelled: Boolean = false,
    val attendanceTaken: Boolean = false,
    val attendedUserIds: List<String> = emptyList(), // Lista manual del admin

    // --- AGREGAR ESTA LÍNEA (Es para el QR) ---
    val checkedInUserIds: List<String> = emptyList(),
    // ------------------------------------------

    val classType: String = "WOD",

    @PropertyName("hexColor")
    val hexColor: String = "#FC5200"
)