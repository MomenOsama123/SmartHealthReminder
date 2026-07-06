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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
    private lateinit var containerNoResults: View
    private lateinit var progressSearch: com.google.android.material.progressindicator.LinearProgressIndicator
    private lateinit var layoutSuggestions: View
    private lateinit var cgFilters: com.google.android.material.chip.ChipGroup
    private lateinit var cgCategories: com.google.android.material.chip.ChipGroup
    private lateinit var containerFilters: View
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

        // Handle keyboard insets
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(0, 0, 0, imeInsets.bottom)
            insets
        }

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
        containerNoResults = view.findViewById(R.id.container_no_results)
        progressSearch = view.findViewById(R.id.progress_search)
        layoutSuggestions = view.findViewById(R.id.layout_suggestions)
        cgFilters = view.findViewById(R.id.cg_filters)
        cgCategories = view.findViewById(R.id.cg_categories)
        containerFilters = view.findViewById(R.id.container_filters)
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
                    hideKeyboard()
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

        cgCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            val category = when (checkedIds.firstOrNull()) {
                R.id.chip_cat_medicine -> "Medicine"
                R.id.chip_cat_appointment -> "Appointment"
                R.id.chip_cat_task -> "Task"
                R.id.chip_cat_custom -> "Custom"
                else -> null
            }
            viewModel.onCategoryChanged(category)
        }
    }

    private fun setupRecyclerView() {
        searchAdapter = SearchAdapter { result ->
            hideKeyboard()
            when (result) {
                is SearchResult.Alarm -> {
                    val intent = Intent(requireContext(), EditAlarmActivity::class.java).apply {
                        putExtra(EditAlarmActivity.EXTRA_ALARM_ID, result.entity.id)
                    }
                    startActivity(intent)
                }
                is SearchResult.Reminder -> {
                    val intent = Intent(requireContext(), AddReminderActivity::class.java).apply {
                        putExtra(AddReminderActivity.EXTRA_REMINDER_ID, result.entity.id)
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
                launch {
                    viewModel.searchResults.collectLatest { results ->
                        // Always update highlighting query before submitting new list
                        searchAdapter.updateQuery(etSearch.text.toString().trim())
                        searchAdapter.submitList(results)
                        updateUiState(results.isEmpty())
                    }
                }
                
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        progressSearch.isVisible = isLoading
                    }
                }
            }
        }
    }

    private fun updateUiState(isResultsEmpty: Boolean) {
        val query = etSearch.text.toString().trim()
        val isQueryEmpty = query.isEmpty()
        val activeFilter = viewModel.activeFilter.value
        val selectedCategory = viewModel.selectedCategory.value

        // Suggestions are shown ONLY when search is empty AND no category is selected AND "All" filter is selected
        val showSuggestions = isQueryEmpty && activeFilter == SearchViewModel.SearchFilter.ALL && selectedCategory == null
        
        layoutSuggestions.isVisible = showSuggestions
        containerFilters.isVisible = true
        
        when {
            showSuggestions -> {
                rvSearchResults.isVisible = false
                containerNoResults.isVisible = false
                hideNoResultsFragment()
            }
            isResultsEmpty -> {
                rvSearchResults.isVisible = false
                containerNoResults.isVisible = true
                showNoResultsFragment()
            }
            else -> {
                rvSearchResults.isVisible = true
                containerNoResults.isVisible = false
                hideNoResultsFragment()
            }
        }
    }

    private fun showNoResultsFragment() {
        val fragment = childFragmentManager.findFragmentByTag("no_results")
        if (fragment == null) {
            val noResultFragment = NoResultFragment.newInstance().apply {
                setOnClearFiltersClickListener {
                    // Reset everything
                    etSearch.text.clear()
                    cgFilters.check(R.id.chip_filter_all)
                    cgCategories.clearCheck()
                }
            }
            childFragmentManager.beginTransaction()
                .replace(R.id.container_no_results, noResultFragment, "no_results")
                .commit()
        }
    }

    private fun hideNoResultsFragment() {
        val fragment = childFragmentManager.findFragmentByTag("no_results")
        if (fragment != null) {
            childFragmentManager.beginTransaction()
                .remove(fragment)
                .commit()
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
