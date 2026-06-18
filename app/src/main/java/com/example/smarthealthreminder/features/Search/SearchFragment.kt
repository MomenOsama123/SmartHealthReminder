package com.example.smarthealthreminder.features.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.activity.AddReminderActivity
import com.example.smarthealthreminder.features.activity.EditAlarmActivity
import com.google.android.material.chip.Chip
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SearchFragment : Fragment() {

    private val viewModel: SearchViewModel by viewModel()
    private lateinit var searchAdapter: SearchAdapter

    private lateinit var etSearch: EditText
    private lateinit var btnBack: ImageButton
    private lateinit var ivClearSearch: ImageView
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var layoutNoResults: View
    private lateinit var layoutSuggestions: View
    private lateinit var cgFilters: com.google.android.material.chip.ChipGroup
    private lateinit var layoutRecentSearches: View
    private lateinit var cgRecentSearches: com.google.android.material.chip.ChipGroup
    private lateinit var btnClearHistory: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupRecyclerView()
        setupSearchInput()
        setupSuggestions(view)
        observeSearchResults()

        btnBack.setOnClickListener {
            hideKeyboard()
            requireActivity().finish()
        }

        ivClearSearch.setOnClickListener {
            etSearch.text.clear()
        }

        // Autofocus and show keyboard
        etSearch.requestFocus()
        showKeyboard()
    }

    private fun initViews(view: View) {
        etSearch = view.findViewById(R.id.et_search)
        btnBack = view.findViewById(R.id.btn_back)
        ivClearSearch = view.findViewById(R.id.iv_clear_search)
        rvSearchResults = view.findViewById(R.id.rv_search_results)
        layoutNoResults = view.findViewById(R.id.layout_no_results)
        layoutSuggestions = view.findViewById(R.id.layout_suggestions)
        cgFilters = view.findViewById(R.id.cg_filters)
        layoutRecentSearches = view.findViewById(R.id.layout_recent_searches)
        cgRecentSearches = view.findViewById(R.id.cg_recent_searches)
        btnClearHistory = view.findViewById(R.id.btn_clear_history)

        setupFilters()
        setupHistory()
    }

    private fun setupHistory() {
        btnClearHistory.setOnClickListener {
            viewModel.clearHistory()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchHistory.collect { history ->
                    updateHistoryUi(history)
                }
            }
        }
    }

    private fun updateHistoryUi(history: List<String>) {
        layoutRecentSearches.isVisible = history.isNotEmpty()
        cgRecentSearches.removeAllViews()
        history.forEach { query ->
            val chip = Chip(requireContext()).apply {
                text = query
                setOnClickListener {
                    etSearch.setText(query)
                    etSearch.setSelection(query.length)
                    viewModel.saveSearch(query)
                }
            }
            cgRecentSearches.addView(chip)
        }
    }

    private fun setupFilters() {
        cgFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chip_filter_reminders -> SearchViewModel.SearchFilter.REMINDERS
                R.id.chip_filter_alarms -> SearchViewModel.SearchFilter.ALARMS
                else -> SearchViewModel.SearchFilter.ALL
            }
            viewModel.onFilterChanged(filter)
        }
    }

    private fun setupRecyclerView() {
        searchAdapter = SearchAdapter { result ->
            hideKeyboard()
            when (result) {
                is SearchResult.Alarm -> {
                    val intent = Intent(requireContext(), EditAlarmActivity::class.java).apply {
                        putExtra("alarm_id", result.entity.id)
                        putExtra("alarm_label", result.entity.label)
                        putExtra("alarm_time", result.entity.time)
                        putExtra("alarm_am_pm", result.entity.amPm)
                        putExtra("alarm_category", result.entity.category)
                        putExtra("alarm_repeat_days", result.entity.repeatDays)
                    }
                    startActivity(intent)
                }
                is SearchResult.Reminder -> {
                    val intent = Intent(requireContext(), AddReminderActivity::class.java).apply {
                        putExtra("reminder_id", result.entity.id)
                        putExtra("reminder_title", result.entity.title)
                        putExtra("reminder_desc", result.entity.description)
                        putExtra("reminder_time", result.entity.time)
                        putExtra("reminder_date", result.entity.date)
                        putExtra("reminder_category", result.entity.category)
                    }
                    startActivity(intent)
                }
            }
        }

        rvSearchResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
            
            // Hide keyboard when scrolling
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                        hideKeyboard()
                    }
                }
            })
        }
    }

    private fun setupSearchInput() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                viewModel.onSearchQueryChanged(query)
                ivClearSearch.isVisible = query.isNotEmpty()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etSearch.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = etSearch.text.toString().trim()
                viewModel.saveSearch(query)
                hideKeyboard()
                true
            } else {
                false
            }
        }
    }

    private fun setupSuggestions(view: View) {
        val suggestionsContainer = view.findViewById<ViewGroup>(R.id.layout_suggestions)
        findAllChips(suggestionsContainer).forEach { chip ->
            chip.setOnClickListener {
                val query = chip.text.toString()
                etSearch.setText(query)
                etSearch.setSelection(query.length)
                viewModel.saveSearch(query)
            }
        }
    }

    private fun findAllChips(view: View): List<Chip> {
        val chips = mutableListOf<Chip>()
        if (view is Chip) {
            chips.add(view)
        } else if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                chips.addAll(findAllChips(view.getChildAt(i)))
            }
        }
        return chips
    }

    private fun observeSearchResults() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchResults.collectLatest { results ->
                    searchAdapter.submitList(results)
                    updateUiState(results.isEmpty())
                }
            }
        }
    }

    private fun updateUiState(isResultsEmpty: Boolean) {
        val query = etSearch.text.toString().trim()
        val isQueryEmpty = query.isEmpty()

        layoutSuggestions.isVisible = isQueryEmpty
        cgFilters.isVisible = !isQueryEmpty
        
        if (isQueryEmpty) {
            rvSearchResults.isVisible = false
            layoutNoResults.isVisible = false
        } else {
            rvSearchResults.isVisible = !isResultsEmpty
            layoutNoResults.isVisible = isResultsEmpty
        }
    }

    private fun showKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(etSearch.windowToken, 0)
    }
}
