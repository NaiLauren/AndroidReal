package com.aquiles.crosschapp.data.model

data class PodiumEntry(
    val id: String = "",
    val rank: Int = 0,
    val userName: String = "",
    val profileImageUrl: String? = null,
    val score: String = "",
    val isRx: Boolean = false
)
