package com.aquiles.crosschapp.presentation.viewmodel

import com.aquiles.crosschapp.data.model.ChallengeResult

sealed class ChallengeResultsState {
    object Idle : ChallengeResultsState()
    object Loading : ChallengeResultsState()
    data class Success(val results: List<ChallengeResult>) : ChallengeResultsState()
    data class Error(val message: String) : ChallengeResultsState()
}
