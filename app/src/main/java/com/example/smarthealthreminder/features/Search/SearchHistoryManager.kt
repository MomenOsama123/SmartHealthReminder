package com.example.smarthealthreminder.features.search

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

class SearchHistoryManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("search_history", Context.MODE_PRIVATE)

    companion object {
        private const val KEY = "history_json"
        private const val MAX_ITEMS = 5
    }

    fun saveSearch(query: String) {
        if (query.isBlank()) return
        val history = getHistory().toMutableList()
        history.remove(query)          // remove duplicate
        history.add(0, query)          // most-recent first
        val limited = history.take(MAX_ITEMS)
        // Single atomic write — no pipe-character corruption
        val json = JSONArray().apply { limited.forEach { put(it) } }.toString()
        prefs.edit().putString(KEY, json).apply()
    }

    fun getHistory(): List<String> {
        val json = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearHistory() {
        prefs.edit().remove(KEY).apply()
    }
}
