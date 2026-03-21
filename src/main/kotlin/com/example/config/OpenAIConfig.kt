package com.example.config

import com.openai.client.OpenAIClient
import com.openai.client.okhttp.OpenAIOkHttpClient
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton

@Factory
class OpenAIConfig {

    @Singleton
    fun openAIClient(
        @Value("\${llm.api.key}") apiKey: String,
        @Value("\${llm.url}") baseUrl: String
    ): OpenAIClient {
        return OpenAIOkHttpClient.builder()
            .apiKey(apiKey)
            .baseUrl(baseUrl)
            .build()
    }
}
