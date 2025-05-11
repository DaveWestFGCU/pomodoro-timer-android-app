package com.davewest.pomodorotimer.ui

import androidx.compose.runtime.Composable
import com.davewest.pomodorotimer.viewmodel.SettingsViewModel
import org.koin.androidx.compose.koinViewModel

class SettingsScreen {

    @Composable
    fun SettingsScreen(
        onNavigateBack: () -> Unit = {},
        // Get ViewModel from Koin
        viewModel: SettingsViewModel = koinViewModel()
    ) {
        // Your implementation...
    }
}