package com.example.smarthealthreminder.features.search

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class SearchFragment : Fragment() {

    private val viewModel: SearchViewModel by viewModel()
    private lateinit var searchAdapter: SearchAdapter

    private lateinit var etSearch: EditText
    private lateinit var btnBack: ImageButton
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var layoutNoResults: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        etSearch = view.findViewById(R.id.et_search)
        btnBack = view.findViewById(R.id.btn_back)
        rvSearchResults = view.findViewById(R.id.rv_search_results)
        layoutNoResults = view.findViewById(R.id.layout_no_results)

        setupRecyclerView()
        setupSearchInput()
        observeSearchResults()

        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        searchAdapter = SearchAdapter { reminder ->
            // Navigate to edit/details screen if needed
            // val action = SearchFragmentDirections.actionSearchFragmentToEditAlarmFragment(reminder.id)
            // findNavController().navigate(action)
        }

        rvSearchResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
        }
    }

    private fun setupSearchInput() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onSearchQueryChanged(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Handle "Search" button on keyboard
        etSearch.setOnEditorActionListener { _: TextView?, actionId: Int, _: KeyEvent? ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.onSearchQueryChanged(etSearch.text.toString())
                true
            } else {
                false
            }
        }
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

        if (isQueryEmpty) {
            rvSearchResults.isVisible = false
            layoutNoResults.isVisible = false
        } else {
            rvSearchResults.isVisible = !isResultsEmpty
            layoutNoResults.isVisible = isResultsEmpty
        }
    }
}
