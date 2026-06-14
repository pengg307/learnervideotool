package com.aigenerator.app.network

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY })
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()

    @Provides @Singleton @Named("openai")
    fun openAIRetrofit(c: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.openai.com/v1/")
        .client(c).addConverterFactory(GsonConverterFactory.create()).build()

    @Provides @Singleton @Named("stability")
    fun stabilityRetrofit(c: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.stability.ai/v1/")
        .client(c).addConverterFactory(GsonConverterFactory.create()).build()

    @Provides @Singleton @Named("replicate")
    fun replicateRetrofit(c: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.replicate.com/v1/")
        .client(c).addConverterFactory(GsonConverterFactory.create()).build()

    @Provides @Singleton
    fun openAIService(@Named("openai") r: Retrofit): OpenAIApiService =
        r.create(OpenAIApiService::class.java)

    @Provides @Singleton
    fun stabilityService(@Named("stability") r: Retrofit): StabilityApiService =
        r.create(StabilityApiService::class.java)

    @Provides @Singleton
    fun replicateService(@Named("replicate") r: Retrofit): ReplicateApiService =
        r.create(ReplicateApiService::class.java)
}
