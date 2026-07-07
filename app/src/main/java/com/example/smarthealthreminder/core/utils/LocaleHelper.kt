package com.example.smarthealthreminder.core.utils

import android.content.Context
import android.content.res.Configuration
import com.example.smarthealthreminder.features.settings.SettingsPrefs
import java.util.Locale

object LocaleHelper {

    fun wrapContext(context: Context): Context {
        val prefs = context.getSharedPreferences(SettingsPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        val lang = prefs.getString(SettingsPrefs.KEY_LANGUAGE, SettingsPrefs.LANG_EN) ?: SettingsPrefs.LANG_EN
        
        val locale = Locale(lang)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }

    fun applyLocale(context: Context) {
        val prefs = context.getSharedPreferences(SettingsPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        val lang = prefs.getString(SettingsPrefs.KEY_LANGUAGE, SettingsPrefs.LANG_EN) ?: SettingsPrefs.LANG_EN
        
        val locale = Locale(lang)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}
