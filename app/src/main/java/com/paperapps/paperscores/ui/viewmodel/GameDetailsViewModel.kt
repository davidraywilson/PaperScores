package com.paperapps.paperscores.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paperapps.paperscores.network.models.MatchDetails
import com.paperapps.paperscores.repository.SoccerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameDetailsViewModel : ViewModel() {
    private val repository = SoccerRepository.getInstance()
    
    private val _matchDetails = MutableStateFlow<MatchDetails?>(null)
    val matchDetails: StateFlow<MatchDetails?> = _matchDetails.asStateFlow()

    fun loadMatchDetails(matchId: String) {
        viewModelScope.launch {
            _matchDetails.value = repository.getMatchDetails(matchId)
        }
    }
}
