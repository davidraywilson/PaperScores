package com.paperapps.paperscores.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paperapps.paperscores.repository.SoccerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

class TodaysGamesViewModel : ViewModel() {
    private val repository = SoccerRepository.getInstance()
    
    val todaysGames = repository.todaysGames
    val isLoading = repository.isLoadingTodaysGames

    private val _selectedDate = MutableStateFlow(Date())
    val selectedDate: StateFlow<Date> = _selectedDate.asStateFlow()

    private val _isToday = MutableStateFlow(true)
    val isToday: StateFlow<Boolean> = _isToday.asStateFlow()

    init {
        refreshGames()
    }

    private var pollingJob: kotlinx.coroutines.Job? = null

    fun refreshGames(forceRefresh: Boolean = false) {
        pollingJob?.cancel()
        viewModelScope.launch {
            repository.refreshTodaysGames(_selectedDate.value, forceRefresh)
            if (_isToday.value) {
                startPolling()
            }
        }
    }

    private fun startPolling() {
        pollingJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60_000L)
                repository.refreshTodaysGames(_selectedDate.value, forceRefresh = true)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }

    private fun checkIfToday() {
        val today = Calendar.getInstance()
        val selected = Calendar.getInstance().apply { time = _selectedDate.value }
        _isToday.value = today.get(Calendar.YEAR) == selected.get(Calendar.YEAR) &&
                         today.get(Calendar.DAY_OF_YEAR) == selected.get(Calendar.DAY_OF_YEAR)
    }

    fun nextDay() {
        val calendar = Calendar.getInstance()
        calendar.time = _selectedDate.value
        calendar.add(Calendar.DATE, 1)
        _selectedDate.value = calendar.time
        checkIfToday()
        refreshGames()
    }

    fun previousDay() {
        val calendar = Calendar.getInstance()
        calendar.time = _selectedDate.value
        calendar.add(Calendar.DATE, -1)
        _selectedDate.value = calendar.time
        checkIfToday()
        refreshGames()
    }

    fun returnToToday() {
        _selectedDate.value = Date()
        _isToday.value = true
        refreshGames()
    }
}
