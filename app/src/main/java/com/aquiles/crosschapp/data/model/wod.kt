// RUTA: data/model/Wod.kt
// VERSIÓN CORREGIDA

package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Wod(
    // --- CAMBIO 1: '@DocumentId' VUELVE AL CAMPO 'id' ---
    // El ID del documento WOD debe mapearse al campo 'id' de nuestro objeto.
    @DocumentId
    val id: String = "",

    // --- CAMBIO 2: 'gym_id' ES UN CAMPO NORMAL ---
    // Este es solo otro campo de texto dentro del documento WOD, como el título.
    val gym_id: String = "",

    val title: String = "",
    val type: String = "",
    val description: String = "",
    val scoreType: String? = null,
    val date: Date? = null,
    val notes: String? = null,
    val imageUrl: String? = null,
    @ServerTimestamp
    val createdAt: Date? = null

    // NOTA: He eliminado el @get:Exclude del 'id' porque ya no es necesario.
    // @DocumentId se encarga de todo correctamente.
)