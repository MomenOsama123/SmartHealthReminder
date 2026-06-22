package com.example.smarthealthreminder.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarthealthreminder.data.repository.HealthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(
    private val repository: HealthRepository,
    private val historyManager: SearchHistoryManager
) : ViewModel() {

    enum class SearchFilter { ALL, REMINDERS, ALARMS }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow(SearchFilter.ALL)
    val activeFilter: StateFlow<SearchFilter> = _activeFilter.asStateFlow()

    private val _searchHistory = MutableStateFlow(historyManager.getHistory())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    val searchResults: StateFlow<List<SearchResult>> = combine(_searchQuery, _activeFilter) { query, filter ->
        query to filter
    }
        .debounce(300L.milliseconds)
        .flatMapLatest { (query, filter) ->
            if (query.isBlank()) {
                flowOf(emptyList<SearchResult>())
            } else {
                val reminderFlow = if (filter == SearchFilter.ALL || filter == SearchFilter.REMINDERS) {
                    repository.searchReminders(query.trim())
                        .map { list -> list.map { SearchResult.Reminder(it) } }
                } else {
                    flowOf(emptyList<SearchResult>())
                }
                
                val alarmFlow = if (filter == SearchFilter.ALL || filter == SearchFilter.ALARMS) {
                    repository.searchAlarms(query.trim())
                        .map { list -> list.map { SearchResult.Alarm(it) } }
                } else {
                    flowOf(emptyList<SearchResult>())
                }

                combine(reminderFlow, alarmFlow) { reminders, alarms ->
                    reminders + alarms
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChanged(filter: SearchFilter) {
        _activeFilter.value = filter
    }

    fun saveSearch(query: String) {
        if (query.isNotBlank()) {
            historyManager.saveSearch(query.trim())
            _searchHistory.value = historyManager.getHistory()
        }
    }

    fun clearHistory() {
        historyManager.clearHistory()
        _searchHistory.value = emptyList()
    }
}
