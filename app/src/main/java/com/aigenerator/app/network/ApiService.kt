package com.aigenerator.app.network

import com.aigenerator.app.model.ChatRequest
import com.aigenerator.app.model.OpenAIChatResponse
import com.aigenerator.app.model.OpenAIImageRequest
import com.aigenerator.app.model.OpenAIImageResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

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
}/text-to-image")
    suspend fun textToImage(
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/json",
        @Path("engine_id") engineId: String,
        @Body request: StabilityTextToImageBody
    ): Response<StabilityImageResponse>
}")
    suspend fun getPrediction(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<ReplicateResponse>
}
