package com.paperapps.paperscores.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paperapps.paperscores.network.models.TeamDetails
import com.paperapps.paperscores.repository.SoccerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class TeamDetailsState {
    object Loading : TeamDetailsState()
    data class Success(val details: TeamDetails) : TeamDetailsState()
    data class Error(val message: String) : TeamDetailsState()
}

class TeamDetailsViewModel : ViewModel() {
    private val repository = SoccerRepository.getInstance()

    private val _uiState = MutableStateFlow<TeamDetailsState>(TeamDetailsState.Loading)
    val uiState: StateFlow<TeamDetailsState> = _uiState.asStateFlow()

    fun loadTeamDetails(teamId: String) {
        _uiState.value = TeamDetailsState.Loading
        viewModelScope.launch {
            try {
                val details = repository.getTeamDetails(teamId)
                if (details != null) {
                    _uiState.value = TeamDetailsState.Success(details)
                } else {
                    _uiState.value = TeamDetailsState.Error("Failed to load team details.")
                }
            } catch (e: Exception) {
                _uiState.value = TeamDetailsState.Error(e.localizedMessage ?: "Unknown error occurred.")
            }
        }
    }
}
