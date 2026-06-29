package com.paperapps.paperscores.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    private val _isMatchRemindersEnabled = MutableStateFlow(prefs.getBoolean("match_reminders_enabled", true))
    val isMatchRemindersEnabledFlow: StateFlow<Boolean> = _isMatchRemindersEnabled.asStateFlow()

    var isMatchRemindersEnabled: Boolean
        get() = prefs.getBoolean("match_reminders_enabled", true)
        set(value) {
            prefs.edit().putBoolean("match_reminders_enabled", value).apply()
            _isMatchRemindersEnabled.value = value
        }

    companion object {
        @Volatile
        private var INSTANCE: UserPreferences? = null

        fun getInstance(context: Context): UserPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
