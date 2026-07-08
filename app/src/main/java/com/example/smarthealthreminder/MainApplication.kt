package com.example.smarthealthreminder

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.smarthealthreminder.core.utils.LocaleHelper
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data_dashboard.DatabaseHelper
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import com.example.smarthealthreminder.features.data.repository.ReportRepository
import com.example.smarthealthreminder.features.reports.ReportViewModel
import com.example.smarthealthreminder.features.search.SearchHistoryManager
import com.example.smarthealthreminder.features.search.SearchViewModel
import com.example.smarthealthreminder.features.settings.SettingsPrefs
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Apply saved locale and theme before any activity is created.
        LocaleHelper.applyLocale(this)

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
    single { DatabaseHelper(get()) }
    
    // Repositories
    single { HealthRepository(get()) }
    single { ReportRepository(get<AppDatabase>().reportDao()) }

    // Search History
    single { SearchHistoryManager(get()) }
    
    // ViewModels
    viewModel { SearchViewModel(get(), get()) }
    viewModel { ReportViewModel(get(), get(), get<AppDatabase>().reminderDao()) }
    viewModel { HealthViewModel(get()) }
}
