package com.davewest.pomodorotimer.di

import com.davewest.pomodorotimer.data.SettingsRepository
import com.davewest.pomodorotimer.viewmodel.PomodoroViewModel
import com.davewest.pomodorotimer.viewmodel.SettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

// Define all your app dependencies in a module
val appModule = module {
    // Single instance of SettingsRepository
    single { SettingsRepository(get()) }

    // ViewModels
    viewModel { PomodoroViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
}