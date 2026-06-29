package com.paperapps.paperscores.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paperapps.paperscores.network.models.MatchDetails
import com.paperapps.paperscores.repository.SoccerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class TournamentState {
    object Loading : TournamentState()
    data class Table(val entries: List<com.paperapps.paperscores.network.models.TableEntry>) : TournamentState()
    data class Playoff(val rounds: List<com.paperapps.paperscores.network.models.PlayoffRound>) : TournamentState()
    object Empty : TournamentState()
}

class GameDetailsViewModel : ViewModel() {
    private val repository = SoccerRepository.getInstance()
    
    private val _matchDetails = MutableStateFlow<MatchDetails?>(null)
    val matchDetails: StateFlow<MatchDetails?> = _matchDetails.asStateFlow()

    private val _tournamentData = MutableStateFlow<TournamentState>(TournamentState.Loading)
    val tournamentData: StateFlow<TournamentState> = _tournamentData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var pollingJob: kotlinx.coroutines.Job? = null

    fun loadMatchDetails(matchId: String) {
        pollingJob?.cancel()
        viewModelScope.launch {
            _isLoading.value = true
            val details = repository.getMatchDetails(matchId)
            _matchDetails.value = details
            _isLoading.value = false
            
            _tournamentData.value = TournamentState.Loading
            if (!details?.tableUrl.isNullOrBlank()) {
                val table = repository.getLeagueTable(details!!.tableUrl!!)
                if (table != null) {
                    _tournamentData.value = TournamentState.Table(table)
                } else {
                    _tournamentData.value = TournamentState.Empty
                }
            } else if (!details?.leagueId.isNullOrBlank()) {
                val playoff = repository.getPlayoffBracket(details!!.leagueId!!)
                if (playoff != null && playoff.isNotEmpty()) {
                    _tournamentData.value = TournamentState.Playoff(playoff)
                } else {
                    _tournamentData.value = TournamentState.Empty
                }
            } else {
                _tournamentData.value = TournamentState.Empty
            }
            
            if (details != null && details.status != "Finished") {
                startPolling(matchId)
            }
        }
    }

    private fun startPolling(matchId: String) {
        pollingJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000L)
                _isLoading.value = true
                val newDetails = repository.getMatchDetails(matchId, forceRefresh = true)
                _matchDetails.value = newDetails
                _isLoading.value = false
                if (newDetails?.status == "Finished" || newDetails == null) {
                    break
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
