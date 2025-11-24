package com.aquiles.crosschapp.data.model

/**
 * Representa la información de un usuario inscrito que se guarda en el array 'enrolledUsers'.
 */
data class EnrolledUser(
    val userId: String = "",
    val fullName: String = "",
    val profileImageUrl: String = ""
)