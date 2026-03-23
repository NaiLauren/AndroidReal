package com.aquiles.crosschapp.data.model

import java.util.Date

data class Trophy(
    val id: String = "",
    val competitionId: String = "",
    val competitionName: String = "",
    val rank: Int = 0,
    val score: String = "",
    val isRx: Boolean = false,
    val date: Date = Date()
)
