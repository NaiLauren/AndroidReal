package com.aquiles.crosschapp.data.model

import java.util.Date

data class CompetitionPodium(
    val id: String = "",
    val competitionName: String = "",
    val endDate: Date = Date(),
    val prizeDescription: String? = null,
    val winners: List<PodiumEntry> = emptyList()
)
