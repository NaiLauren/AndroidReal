package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
import java.util.Date

data class AttendanceRecord(
    @DocumentId
    val id: String = "",

    // Intenta leer "classId" (Android), si no, lee "class_id" (iOS/Web común)
    @PropertyName("classId")
    val classId: String = "",

    // Intenta leer "classDate" o "date"
    @PropertyName("classDate")
    val classDate: Date? = null,

    val dayOfWeek: Int = 0,
    val hourOfDay: Int = 0,

    // Aseguramos que lea "userId" o "user_id"
    @PropertyName("userId")
    val userId: String = "",

    val gym_id: String = ""
)