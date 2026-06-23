package com.paperapps.paperscores.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paperapps.paperscores.repository.SoccerRepository
import kotlinx.coroutines.launch

class TodaysGamesViewModel : ViewModel() {
    private val repository = SoccerRepository.getInstance()
    
    val todaysGames = repository.todaysGames
    val isLoading = repository.isLoadingTodaysGames

    init {
        refreshGames()
    }

    fun refreshGames() {
        viewModelScope.launch {
            repository.refreshTodaysGames()
        }
    }
}
