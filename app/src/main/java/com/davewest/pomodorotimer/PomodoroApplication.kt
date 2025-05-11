package com.davewest.pomodorotimer

import android.app.Application
import com.davewest.pomodorotimer.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class PomodoroApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Start Koin
        startKoin {
            // Use Android logger for debugging
            androidLogger()
            // Provide Android context
            androidContext(this@PomodoroApplication)
            // Load modules
            modules(appModule)
        }
    }
}