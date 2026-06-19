package com.example.smarthealthreminder.features.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smarthealthreminder.databinding.ActivityMainWelcomeBinding
import com.example.smarthealthreminder.features.Profileinfo.reports.ProfileActivity
import com.example.smarthealthreminder.features.chatbot.ChatBotActivity
import com.example.smarthealthreminder.features.search.SearchActivity

class MainWelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainWelcomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = ActivityMainWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        setupWindowInsets()
    }

    private fun setupClickListeners() {
        binding.searchIcon.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        binding.fabChatbot.setOnClickListener {
            startActivity(Intent(this, ChatBotActivity::class.java))
        }
        binding.profileInfo.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}