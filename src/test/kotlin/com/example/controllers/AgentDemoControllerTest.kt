package com.example.controllers

import com.openai.client.OpenAIClient
import com.openai.models.chat.completions.ChatCompletion
import com.openai.models.chat.completions.ChatCompletionMessage
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Replaces
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import io.mockk.every
import io.mockk.mockk
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional

@Factory
class TestOpenAIConfig {
    @Singleton
    @Replaces(OpenAIClient::class)
    fun mockOpenAIClient(): OpenAIClient {
        return mockk<OpenAIClient>(relaxed = true)
    }
}

@MicronautTest
class AgentDemoControllerTest {

    @Inject
    @field:Client("/")
    lateinit var client: HttpClient

    @Inject
    lateinit var openAIClient: OpenAIClient

    @BeforeEach
    fun setup() {
        io.mockk.clearMocks(openAIClient)
    }

    @Test
    fun testMathEndpoint() {
        val mockResponse = mockk<ChatCompletion>()
        val mockChoice = mockk<ChatCompletion.Choice>()
        val mockMessage = mockk<ChatCompletionMessage>()

        every { openAIClient.chat().completions().create(any()) } returns mockResponse
        every { mockResponse.choices() } returns listOf(mockChoice)
        every { mockChoice.message() } returns mockMessage
        every { mockMessage.content() } returns Optional.of("Calculated result is 5")
        every { mockMessage.toolCalls() } returns Optional.empty()

        val response = client.toBlocking().retrieve("/agents/math?prompt=What%20is%202%2B3")
        assertEquals("Calculated result is 5", response)
    }
}
