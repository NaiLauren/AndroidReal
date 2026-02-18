// RUTA: data/model/Gym.kt
package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class Gym(
    val id: String = "",
    val name: String = "",
    val country: String = "",
    val state: String = "",   // Sincronizado con iOS (Gym.swift)
    val city: String = "",
    val address: String = "",
    val logoUrl: String? = null,
    
    @get:PropertyName("primary_color")
    @set:PropertyName("primary_color")
    @PropertyName("primary_color")
    var primaryColor: String? = null,
    
    @PropertyName("last_daily_closing")
    val lastDailyClosing: com.google.firebase.Timestamp? = null,
    
    // Sincronizados con iOS (Gym.swift)
    @PropertyName("admin_email")
    val adminEmail: String? = null,
    val splashVideoUrl: String? = null
)