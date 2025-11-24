// RUTA: data/model/Gym.kt
package com.aquiles.crosschapp.data.model

data class Gym(
    val id: String = "",
    val name: String = "",
    val country: String = "",
    val state: String = "",
    val city: String = "",
    val logoUrl: String? = null,
    val address: String = ""
)