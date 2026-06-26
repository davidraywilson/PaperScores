package com.paperapps.paperscores.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paperapps.paperscores.network.models.Team
import com.paperapps.paperscores.network.models.TeamNextMatch
import com.paperapps.paperscores.network.models.Tournament
import com.paperapps.paperscores.network.models.SearchResult
import com.paperapps.paperscores.repository.SoccerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class UserProfileViewModel : ViewModel() {
    private val repository = SoccerRepository.getInstance()
    
    val followedTeams: StateFlow<List<Team>> = repository.followedTeams
    val followedTournaments: StateFlow<List<Tournament>> = repository.followedTournaments

    private val _teamFixtures = MutableStateFlow<Map<String, TeamNextMatch>>(emptyMap())
    val teamFixtures: StateFlow<Map<String, TeamNextMatch>> = _teamFixtures.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isSearchBarVisible = MutableStateFlow(false)
    val isSearchBarVisible: StateFlow<Boolean> = _isSearchBarVisible.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            followedTeams.collect { teams ->
                val matchMap = repository.fetchNextMatchesForTeams(teams.map { it.id })
                _teamFixtures.value = matchMap
            }
        }
    }

    fun performSearch(query: String) {
        searchJob?.cancel()
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            _searchResults.value = repository.searchEntities(query)
            _isSearching.value = false
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }

    fun toggleSearchBar() {
        _isSearchBarVisible.value = !_isSearchBarVisible.value
        if (!_isSearchBarVisible.value) {
            clearSearch()
        }
    }

    fun follow(item: SearchResult) {
        when (item) {
            is SearchResult.TeamResult -> repository.followTeam(item.team)
            is SearchResult.TournamentResult -> repository.followTournament(item.tournament)
        }
    }

    fun unfollowTeam(teamId: String) {
        repository.unfollowTeam(teamId)
    }

    fun unfollowTournament(tournamentId: String) {
        repository.unfollowTournament(tournamentId)
    }
}
