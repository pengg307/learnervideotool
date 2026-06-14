package com.aigenerator.app.network

import com.aigenerator.app.model.*
import retrofit2.Response
import retrofit2.http.*

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

interface StabilityApiService {
    @POST("generation/{engine_id}/text-to-image")
    suspend fun textToImage(
        @Header("Authorization") token: String,
        @Header("Accept") accept: String = "application/json",
        @Path("engine_id") engineId: String,
        @Body request: StabilityTextToImageBody
    ): Response<StabilityImageResponse>
}

interface ReplicateApiService {
    @POST("predictions")
    suspend fun createPrediction(
        @Header("Authorization") token: String,
        @Body request: ReplicateRequest
    ): Response<ReplicateResponse>

    @GET("predictions/{id}")
    suspend fun getPrediction(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<ReplicateResponse>
}
