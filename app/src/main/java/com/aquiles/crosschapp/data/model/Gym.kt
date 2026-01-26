// RUTA: data/model/Gym.kt
package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class Gym(
    val id: String = "",
    val name: String = "",
    val country: String = "",
    val state: String = "",
    val city: String = "",
    val logoUrl: String? = null,
    val address: String = "",
    
    @get:PropertyName("primary_color")
    @set:PropertyName("primary_color")
    @PropertyName("primary_color")
    var primaryColor: String? = null // Hex Color for Dynamic Theming
)