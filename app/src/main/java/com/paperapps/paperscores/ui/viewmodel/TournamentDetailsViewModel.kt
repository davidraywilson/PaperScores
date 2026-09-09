package com.paperapps.paperscores.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paperapps.paperscores.db.SoccerDatabase
import com.paperapps.paperscores.db.FollowedTournamentEntity
import com.paperapps.paperscores.network.models.TournamentDetails
import com.paperapps.paperscores.repository.SoccerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class TournamentDetailsState {
    object Loading : TournamentDetailsState()
    data class Success(val details: TournamentDetails) : TournamentDetailsState()
    data class Error(val message: String) : TournamentDetailsState()
}

class TournamentDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SoccerRepository.getInstance()
    private val dao = SoccerDatabase.getDatabase(application).soccerDao()

    private val _uiState = MutableStateFlow<TournamentDetailsState>(TournamentDetailsState.Loading)
    val uiState: StateFlow<TournamentDetailsState> = _uiState.asStateFlow()

    private val _isFollowed = MutableStateFlow(false)
    val isFollowed: StateFlow<Boolean> = _isFollowed.asStateFlow()

    fun loadTournamentDetails(leagueId: String) {
        viewModelScope.launch {
            _uiState.value = TournamentDetailsState.Loading
            
            // Check follow state
            launch {
                dao.getFollowedTournamentsFlow().collect { tournaments ->
                    _isFollowed.value = tournaments.any { it.id == leagueId }
                }
            }

            val details = repository.getTournamentDetails(leagueId)
            if (details != null) {
                _uiState.value = TournamentDetailsState.Success(details)
            } else {
                _uiState.value = TournamentDetailsState.Error("Failed to load tournament details")
            }
        }
    }

    fun toggleFollow(tournamentId: String, name: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (_isFollowed.value) {
                dao.deleteTournament(tournamentId)
            } else {
                dao.insertTournament(FollowedTournamentEntity(id = tournamentId, name = name, imageUrl = ""))
            }
        }
    }
}
