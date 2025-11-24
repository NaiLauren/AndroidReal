package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.DocumentId

data class CreditCombo(
    @DocumentId
    val gym_id: String = "",
    val id: String = "", // ID único del combo
    val name: String = "", // Ej: "Paquete Básico", "Combo Mensual"
    val creditsAwarded: Int = 0, // Cantidad de créditos que otorga este combo
    val price: Double = 0.0, // Precio del combo (ej. 1500.0)
    val description: String = "", // Descripción opcional del combo
    val isActive: Boolean = true // Para poder habilitar/deshabilitar combos

)