package com.example.smarthealthreminder.features.chatbot


import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    @POST("v1/chat/completions")
    fun sendMessage(
        @Header("Authorization") token: String,
        @Header("Content-Type") contentType: String,
        @Body body: ChatRequest
    ): Call<ChatResponse>
}