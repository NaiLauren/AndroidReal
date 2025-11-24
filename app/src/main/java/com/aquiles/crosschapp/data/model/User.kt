package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    @DocumentId
    val id: String = "",
    val email: String = "",
    val name: String = "",
    var lastName: String = "",
    var phoneNumber: String? = null,
    var profileImageUrl: String? = null,
    val gym_id: String = "",
    val role: String = "member",

    @get:Exclude @set:Exclude
    var isAdmin: Boolean = false,

    var credits: Int = 0,
    var creditValidUntil: Date? = null,
    var totalClassesAttended: Int = 0,
    var currentClassesReserved: Int = 0,
    @ServerTimestamp
    val registrationDate: Date? = null,

    val emergencyContact: String? = null,
    val birthDate: Date? = null,

    val boxIdentifier: String? = null,
    val fcmTokens: List<String> = emptyList()
) {
    @get:Exclude
    val hasValidCredits: Boolean
        get() {
            if (this.role == "owner" || this.role == "coach") return true
            val today = Date()
            return credits > 0 && (creditValidUntil?.after(today) ?: false)
        }

    @get:Exclude
    val fullName: String
        get() = "$name $lastName".trim()

    @get:Exclude
    val isOwner: Boolean
        get() = this.role == "owner"

    @get:Exclude
    val isCoach: Boolean
        get() = this.role == "coach"

    @get:Exclude
    val isMember: Boolean
        get() = this.role == "member"

    // --- ESTA ES LA PROPIEDAD QUE FALTABA ---
    // Permite entrar al Horario si es Admin, tiene créditos O tiene reservas pendientes.
    @get:Exclude
    val canAccessSchedule: Boolean
        get() = isAdmin || hasValidCredits || currentClassesReserved > 0
}