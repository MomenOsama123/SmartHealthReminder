package com.example.smarthealthreminder.features.search

import android.content.Context
import android.content.SharedPreferences

class SearchHistoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("search_history", Context.MODE_PRIVATE)

    fun saveSearch(query: String) {
        if (query.isBlank()) return
        val history = getHistory().toMutableList()
        history.remove(query)
        history.add(0, query)
        val limitedHistory = if (history.size > 5) history.take(5) else history
        prefs.edit().putStringSet("history", limitedHistory.toSet()).apply()
        // SharedPreferences doesn't guarantee order for StringSet, let's use a string with separator
        prefs.edit().putString("history_ordered", limitedHistory.joinToString("|")).apply()
    }

    fun getHistory(): List<String> {
        val historyString = prefs.getString("history_ordered", "") ?: ""
        return if (historyString.isBlank()) emptyList() else historyString.split("|")
    }

    fun clearHistory() {
        prefs.edit().clear().apply()
    }
}
