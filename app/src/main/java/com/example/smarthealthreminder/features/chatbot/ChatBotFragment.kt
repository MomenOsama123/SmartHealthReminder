package com.example.smarthealthreminder.features.chatbot

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smarthealthreminder.BuildConfig
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.databinding.FragmentChatbotBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatBotFragment : Fragment() {

    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<Message>()
    private lateinit var api: ApiService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        api = RetrofitClient.api
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupRecyclerView()
        setupInput()
        setupListeners()
        
        if (auth.currentUser != null) {
            loadMessages()
        }
    }

    private fun setupRecyclerView() {
        adapter = ChatAdapter(messages)
        val layoutManager = LinearLayoutManager(requireContext())
        layoutManager.stackFromEnd = false // Show messages from top
        binding.chatRecyclerView.layoutManager = layoutManager
        binding.chatRecyclerView.adapter = adapter
    }

    private fun setupInput() {
        updateSendButtonState("")
        
        // Handle keyboard insets to keep EditText above the keyboard
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomInset = imeInsets.bottom
            
            // Adjust inputLayout bottom margin to account for keyboard
            val params = binding.inputLayout.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = bottomInset
            binding.inputLayout.layoutParams = params
            
            // Scroll to bottom if keyboard opened to keep latest message visible
            if (bottomInset > 0 && messages.isNotEmpty()) {
                binding.chatRecyclerView.post {
                    binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                }
            }
            insets
        }

        binding.messageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSendButtonState(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupListeners() {
        binding.infoIcon.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext(), R.style.AppAlertDialogTheme)
                .setTitle(getString(R.string.medical_disclaimer_title))
                .setMessage(getString(R.string.medical_disclaimer_message))
                .setPositiveButton(getString(R.string.i_understand), null)
                .show()
        }

        binding.sendButton.setOnClickListener {
            val userMessageText = binding.messageEditText.text.toString().trim()
            if (userMessageText.isEmpty()) return@setOnClickListener

            val userMessage = Message(userMessageText, true)
            addMessage(userMessage)
            saveMessageToFirestore(userMessage)
            binding.messageEditText.text.clear()
            hideKeyboard()
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
            binding.sendButton.background.setTint(ContextCompat.getColor(requireContext(), R.color.text_tertiary))
        }
    }

    private fun loadMessages() {
        val uid = auth.currentUser?.uid ?: return
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
    }

    private fun saveMessageToFirestore(message: Message) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("messages").add(message)
    }

    private fun addMessage(message: Message) {
        messages.add(message)
        adapter.notifyItemInserted(messages.size - 1)
        binding.chatRecyclerView.smoothScrollToPosition(messages.size - 1)
    }

    private fun hideKeyboard() {
        val view = activity?.currentFocus
        if (view != null) {
            val imm = requireContext().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun showTyping(show: Boolean) {
        if (show) {
            val loadingMessage = Message(isLoading = true)
            messages.add(loadingMessage)
            adapter.notifyItemInserted(messages.size - 1)
            binding.chatRecyclerView.smoothScrollToPosition(messages.size - 1)
        } else {
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

        api.sendMessage(
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
                }
            }
            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                showTyping(false)
            }
        })
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && isAdded && auth.currentUser != null) {
            loadMessages()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}