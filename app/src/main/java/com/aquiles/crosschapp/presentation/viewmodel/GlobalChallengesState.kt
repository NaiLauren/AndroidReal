package com.aquiles.crosschapp.presentation.viewmodel

import com.aquiles.crosschapp.data.model.GlobalChallenge

sealed class GlobalChallengesState {
    object Idle : GlobalChallengesState()
    object Loading : GlobalChallengesState()
    data class Success(val challenges: List<GlobalChallenge>) : GlobalChallengesState()
    data class Error(val message: String) : GlobalChallengesState()
}
