package com.aigenerator.app.network

import com.aigenerator.app.model.ChatRequest
import com.aigenerator.app.model.OpenAIChatResponse
import com.aigenerator.app.model.OpenAIImageRequest
import com.aigenerator.app.model.OpenAIImageResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAIApiService {

    @POST("images/generations")
    suspend fun generateImage(
        @Header("Authorization") token: String,
        @Body request: OpenAIImageRequest
    ): Response<OpenAIImageResponse>

    @POST("chat/completions")
    suspend fun chat(
        @Header("Authorization") token: String,
        @Body request: ChatRequest
    ): Response<OpenAIChatResponse>
}
