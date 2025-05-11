package com.davewest.pomodorotimer.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.davewest.pomodorotimer.data.SettingsRepository
import com.davewest.pomodorotimer.model.PomodoroConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    // Convert Flow to StateFlow for UI consumption
    val settings: StateFlow<PomodoroConfig> = repository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PomodoroConfig()
        )

    // Update settings
    fun updateSettings(config: PomodoroConfig) {
        viewModelScope.launch {
            repository.updateSettings(config)
        }
    }
}