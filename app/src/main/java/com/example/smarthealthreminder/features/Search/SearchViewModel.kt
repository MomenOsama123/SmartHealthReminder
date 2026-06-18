package com.example.smarthealthreminder.features.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smarthealthreminder.data.repository.HealthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class SearchViewModel(private val repository: HealthRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<SearchResult>> = _searchQuery
        .debounce(300L.milliseconds)
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                val reminderFlow = repository.searchReminders(query.trim())
                    .map { list -> list.map { SearchResult.Reminder(it) } }
                
                val alarmFlow = repository.searchAlarms(query.trim())
                    .map { list -> list.map { SearchResult.Alarm(it) } }

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
}
