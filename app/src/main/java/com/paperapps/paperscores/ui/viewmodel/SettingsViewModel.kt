package com.paperapps.paperscores.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paperapps.paperscores.repository.SoccerRepository
import com.paperapps.paperscores.repository.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val userPrefs = UserPreferences.getInstance(application)
    
    val isMatchRemindersEnabled: StateFlow<Boolean> = userPrefs.isMatchRemindersEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = userPrefs.isMatchRemindersEnabled
        )

    fun setMatchRemindersEnabled(enabled: Boolean) {
        userPrefs.isMatchRemindersEnabled = enabled
        
        viewModelScope.launch {
            if (!enabled) {
                SoccerRepository.getInstance().cancelAllNotifications()
            }
        }
    }
}
