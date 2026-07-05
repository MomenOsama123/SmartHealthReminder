package com.example.smarthealthreminder

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import com.example.smarthealthreminder.features.search.SearchHistoryManager
import com.example.smarthealthreminder.features.search.SearchViewModel
import com.example.smarthealthreminder.features.settings.SettingsPrefs
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Apply saved theme before any activity is created.
        // Defaults to light mode if the user has never changed it.
        AppCompatDelegate.setDefaultNightMode(
            SettingsPrefs.getSavedNightMode(this)
        )

        startKoin {
            androidContext(this@MainApplication)
            modules(appModule)
        }
    }
}

val appModule = module {
    // Single instance of database
    single { AppDatabase.getDatabase(get()) }
    
    // Repository
    single { HealthRepository(get()) }

    // Search History
    single { SearchHistoryManager(get()) }
    
    // ViewModels
    viewModel { SearchViewModel(get(), get()) }
}
