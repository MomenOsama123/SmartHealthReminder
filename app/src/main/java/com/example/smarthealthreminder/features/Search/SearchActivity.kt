package com.example.smarthealthreminder.features.search

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.smarthealthreminder.R

class SearchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.search_container, SearchFragment())
                .commit()
        }
    }
}
