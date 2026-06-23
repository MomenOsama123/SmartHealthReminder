package com.example.smarthealthreminder.features.chatbot


import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smarthealthreminder.BuildConfig
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.databinding.ActivityChatbotBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class ChatBotActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
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

            // Check if keyboard is visible
            val isKeyboardVisible = ime.bottom > systemBars.bottom

            // Hide bottom navigation when keyboard is open to prevent it from being pushed on top
            binding.bottomNavigation.visibility = if (isKeyboardVisible) View.GONE else View.VISIBLE

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                maxOf(systemBars.bottom, ime.bottom)
            )

            // Scroll to the latest message when keyboard opens
            if (isKeyboardVisible && messages.isNotEmpty()) {
                binding.chatRecyclerView.post {
                    binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
            insets
        }

        api = RetrofitClient.api
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        binding.infoIcon.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(getString(R.string.medical_disclaimer_title))
                .setMessage(getString(R.string.medical_disclaimer_message))
                .setPositiveButton(getString(R.string.i_understand), null)
                .show()
        }

        // RecyclerView setup: default top-to-bottom layout
        val layoutManager = LinearLayoutManager(this)
        binding.chatRecyclerView.layoutManager = layoutManager

        adapter = ChatAdapter(messages)
        binding.chatRecyclerView.adapter = adapter

        // Initial send button state
        updateSendButtonState("")

        binding.messageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSendButtonState(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Load previous messages
        if (auth.currentUser != null) {
            loadMessages()
        } else {
            // If no user is logged in, you might want to redirect to Login
            // or show a message. For now, we'll just skip loading.
            finish() // Optional: close activity if not logged in
        }

        binding.sendButton.setOnClickListener {
            val userMessageText = binding.messageEditText.text.toString().trim()
            if (userMessageText.isEmpty()) return@setOnClickListener

            val userMessage = Message(userMessageText, true)

            // 1. Add user message locally and to Firestore
            addMessage(userMessage)
            saveMessageToFirestore(userMessage)

            binding.messageEditText.text.clear()

            // Hide keyboard
            hideKeyboard()

            // 2. Show thinking indicator and call AI
            showTyping(true)
            sendToAI(userMessageText)
        }
    }

    private fun updateSendButtonState(text: String) {
        val isNotEmpty = text.trim().isNotEmpty()
        binding.sendButton.isEnabled = isNotEmpty
        if (isNotEmpty) {
            binding.sendButton.background.setTintList(null)
        } else {
            binding.sendButton.background.setTint(ContextCompat.getColor(this, R.color.text_tertiary))
        }
    }

    private fun loadMessages() {
        val uid = auth.currentUser?.uid ?: return

        // Show shimmer effect while loading
        messages.clear()
        repeat(3) { messages.add(Message(isLoading = true)) }
        adapter.notifyDataSetChanged()

        db.collection("users").document(uid).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { documents ->
                messages.clear()
                for (document in documents) {
                    val message = document.toObject(Message::class.java)
                    messages.add(message)
                }
                adapter.notifyDataSetChanged()
                if (messages.isNotEmpty()) {
                    binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
            .addOnFailureListener { e ->
                messages.clear()
                adapter.notifyDataSetChanged()
                android.util.Log.e("FirestoreChat", "Error loading messages", e)
            }
    }

    private fun saveMessageToFirestore(message: Message) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("messages")
            .add(message)
            .addOnSuccessListener {
                android.util.Log.d("FirestoreChat", "Message saved: ${message.text}")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("FirestoreChat", "Failed to save message", e)
                android.widget.Toast.makeText(this, "Sync failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
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
            // Add a shimmer loading message
            val loadingMessage = Message(isLoading = true)
            messages.add(loadingMessage)
            adapter.notifyItemInserted(messages.size - 1)
            binding.chatRecyclerView.smoothScrollToPosition(messages.size - 1)
        } else {
            // Remove the shimmer loading message
            val index = messages.indexOfFirst { it.isLoading }
            if (index != -1) {
                messages.removeAt(index)
                adapter.notifyItemRemoved(index)
            }
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
            "Bearer ${BuildConfig.GROQ_API_KEY}",
            "application/json",
            request
        ).enqueue(object : Callback<ChatResponse> {
            override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                showTyping(false)
                if (response.isSuccessful) {
                    val replyText = response.body()?.choices?.firstOrNull()?.message?.content ?: "No response"
                    val botMessage = Message(replyText, false)
                    addMessage(botMessage)
                    saveMessageToFirestore(botMessage)
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