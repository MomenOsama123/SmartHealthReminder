//Message.kt file
package com.example.smarthealthreminder.features.chatbot

import com.google.firebase.firestore.PropertyName

data class Message(
    val text: String = "",
    @get:PropertyName("isUser")
    @set:PropertyName("isUser")
    var isUser: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

