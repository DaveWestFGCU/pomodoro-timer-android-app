package com.davewest.pomodorotimer.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.davewest.pomodorotimer.model.PomodoroConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

class SettingsRepository(private val context: Context) {
    // Preference keys
    object PreferenceKeys {
        val WORK_DURATION = intPreferencesKey("work_duration_minutes")
        val SHORT_BREAK = intPreferencesKey("short_break_minutes")
        val LONG_BREAK = intPreferencesKey("long_break_minutes")
        val CYCLES = intPreferencesKey("cycles_before_long_break")
    }

    // Get settings as a Flow
    val settingsFlow: Flow<PomodoroConfig> = context.dataStore.data.map { preferences ->
        PomodoroConfig(
            workDurationSec = (preferences[PreferenceKeys.WORK_DURATION] ?: 25) * 60L,
            shortBreakDurationSec = (preferences[PreferenceKeys.SHORT_BREAK] ?: 5) * 60L,
            longBreakDurationSec = (preferences[PreferenceKeys.LONG_BREAK] ?: 30) * 60L,
            cyclesBeforeLongBreak = preferences[PreferenceKeys.CYCLES] ?: 4
        )
    }

    // Save settings
    suspend fun updateSettings(config: PomodoroConfig) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.WORK_DURATION] = (config.workDurationSec / 60).toInt()
            preferences[PreferenceKeys.SHORT_BREAK] = (config.shortBreakDurationSec / 60).toInt()
            preferences[PreferenceKeys.LONG_BREAK] = (config.longBreakDurationSec / 60).toInt()
            preferences[PreferenceKeys.CYCLES] = config.cyclesBeforeLongBreak
        }
    }
}