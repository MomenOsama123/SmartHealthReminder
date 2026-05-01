package com.example.testchatbot

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smarthealthreminder.databinding.ActivityChatbotBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatBotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatbotBinding
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<Message>()

    private lateinit var api: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityChatbotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle Keyboard (IME) and System Bars insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(systemBars.bottom, ime.bottom)
            )
            insets
        }

        api = RetrofitClient.api

        // RecyclerView setup: default top-to-bottom layout
        val layoutManager = LinearLayoutManager(this)
        binding.chatRecyclerView.layoutManager = layoutManager

        adapter = ChatAdapter(messages)
        binding.chatRecyclerView.adapter = adapter

        binding.sendButton.setOnClickListener {
            val userMessage = binding.messageEditText.text.toString().trim()
            if (userMessage.isEmpty()) return@setOnClickListener

            // 1. Add user message
            addMessage(Message(userMessage, true))
            binding.messageEditText.text.clear()
            
            // Hide keyboard
            hideKeyboard()

            // 2. Show thinking indicator and call AI
            showTyping(true)
            sendToAI(userMessage)
        }
    }

    private fun addMessage(message: Message) {
        messages.add(message)
        adapter.notifyItemInserted(messages.size - 1)
        binding.chatRecyclerView.smoothScrollToPosition(messages.size - 1)
    }

    private fun hideKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun showTyping(show: Boolean) {
        if (show) {
            binding.typingIndicator.visibility = View.VISIBLE
            binding.typingIndicator.text = "SerenePulse is thinking."
            binding.typingIndicator.alpha = 0f
            binding.typingIndicator.animate().alpha(1f).setDuration(300).start()
            
            // Loop for "Thinking..." text animation
            val handler = Handler(Looper.getMainLooper())
            val runnable = object : Runnable {
                var dots = 0
                override fun run() {
                    if (binding.typingIndicator.visibility == View.VISIBLE) {
                        dots = (dots + 1) % 4
                        binding.typingIndicator.text = "SerenePulse is thinking" + ".".repeat(dots)
                        handler.postDelayed(this, 500)
                    }
                }
            }
            handler.post(runnable)
        } else {
            binding.typingIndicator.visibility = View.GONE
        }
    }

    private fun sendToAI(userMessage: String) {
        val request = ChatRequest(
            model = "llama-3.1-8b-instant",
            messages = listOf(
                MessageRequest("system", "You are a helpful medical assistant. Do not diagnose."),
                MessageRequest("user", userMessage)
            )
        )

        RetrofitClient.api.sendMessage(
            "Bearer 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",
            "application/json",
            request
        ).enqueue(object : Callback<ChatResponse> {
            override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                showTyping(false)
                if (response.isSuccessful) {
                    val reply = response.body()?.choices?.firstOrNull()?.message?.content ?: "No response"
                    addMessage(Message(reply, false))
                } else {
                    addMessage(Message("Error: ${response.errorBody()?.string()}", false))
                }
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                showTyping(false)
                addMessage(Message("Failure: ${t.message}", false))
            }
        })
    }
}