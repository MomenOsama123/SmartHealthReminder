package com.example.smarthealthreminder.features.chatbot


data class ChatRequest(
    val model: String,
    val messages: List<MessageRequest>
)

data class MessageRequest(
    val role: String,
    val content: String
)

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: MessageContent
)

data class MessageContent(
    val role: String,
    val content: String
)