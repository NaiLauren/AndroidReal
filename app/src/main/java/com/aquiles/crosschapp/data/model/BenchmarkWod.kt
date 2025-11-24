package com.aquiles.crosschapp.data.model

import com.google.firebase.firestore.DocumentId

data class BenchmarkWod(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val scoreType: String = "",
    val strategy: String = "",
    val gym_id: String = ""
)