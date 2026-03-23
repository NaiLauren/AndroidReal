package com.aquiles.crosschapp.data.model

import java.util.Date

data class GlobalRecord(
    val id: String = "",
    val userId: String = "",
    var userName: String = "",
    var profileImageUrl: String? = null,
    val wodName: String = "",
    val score: String = "",
    val isRx: Boolean = false,
    val date: Date = Date(),
    val isChallenge: Boolean = false,
    var reactions: Map<String, String> = emptyMap()
)
