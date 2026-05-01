package com.example.smarthealthreminder.features.Search

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smarthealthreminder.R

class SearchActivity : AppCompatActivity() {

    lateinit var etSearch: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_search)

        etSearch = findViewById(R.id.etSearch)

        setupListeners()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.searchActivity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupListeners() {
        etSearch.setOnEditorActionListener { _, _, _ ->

            val query = etSearch.text.toString().trim()

            if (query.isEmpty()) {
                goToNoResult()
            } else {
                handleSearch(query)
            }

            true
        }
    }

    private fun goToNoResult() {
        val intent = Intent(this, NoResultsActivity::class.java)
        startActivity(intent)
    }

    private fun handleSearch(query: String) {
        val fakeData = listOf(
            "Paracetamol",
            "Ibuprofen",
            "Vitamin D",
            "Diabetes"
        )

        val result = fakeData.filter {
            it.contains(query, ignoreCase = true)
        }

        if (result.isEmpty()) {
            goToNoResult()
        } else {
            Toast.makeText(this, "Found ${result.size} results", Toast.LENGTH_SHORT).show()
        }
    }
}