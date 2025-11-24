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
    // val enrolledUsers: List<EnrolledUser> = emptyList(), // Comentado si no se usa para evitar errores de parseo

    val isCancelled: Boolean = false,
    val attendanceTaken: Boolean = false,
    val attendedUserIds: List<String> = emptyList(),

    val classType: String = "WOD",

    // --- CAMBIO CLAVE: Renombramos a hexColor para coincidir con iOS ---
    // @PropertyName asegura que Firestore busque el campo "hexColor" aunque la variable se llame igual
    @PropertyName("hexColor")
    val hexColor: String = "#FC5200" // Default Naranja
)