package com.example.smarthealthreminder.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarthealthreminder.features.data.repository.HealthRepository
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

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchHistory = MutableStateFlow(historyManager.getHistory())
    val searchHistory: StateFlow<List<String>> = _searchHistory.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<SearchResult>> = combine(_searchQuery, _activeFilter, _selectedCategory) { query, filter, category ->
        _isLoading.value = true
        Triple(query, filter, category)
    }
        .debounce(300L.milliseconds)
        .flatMapLatest { (query, filter, category) ->
            if (query.isBlank() && filter == SearchFilter.ALL && category == null) {
                _isLoading.value = false
                // When empty, we could potentially show "Suggested" or "Recent" items here 
                // but for now we follow the logic of showing empty list which triggers Suggestions UI in fragment
                flowOf(emptyList())
            } else {
                val effectiveQuery = query.trim()
                
                val reminderFlow = if (filter == SearchFilter.ALL || filter == SearchFilter.REMINDERS) {
                    repository.searchReminders(effectiveQuery)
                        .map { list -> 
                            list.map { SearchResult.Reminder(it) }
                                .filter { item -> category == null || item.entity.category?.equals(category, ignoreCase = true) == true }
                        }
                } else {
                    flowOf(emptyList())
                }

                val alarmFlow = if (filter == SearchFilter.ALL || filter == SearchFilter.ALARMS) {
                    repository.searchAlarms(effectiveQuery)
                        .map { list -> 
                            list.map { SearchResult.Alarm(it) }
                                .filter { item -> category == null || item.entity.category?.equals(category, ignoreCase = true) == true }
                        }
                } else {
                    flowOf(emptyList())
                }

                combine(reminderFlow, alarmFlow) { reminders, alarms ->
                    (reminders + alarms).sortedBy { it.title.lowercase() }
                }.onEach { _isLoading.value = false }
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

    fun onCategoryChanged(category: String?) {
        _selectedCategory.value = category
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
