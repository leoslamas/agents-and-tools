package com.example.controllers

import com.openai.client.OpenAIClient
import com.openai.models.chat.completions.ChatCompletion
import com.openai.models.chat.completions.ChatCompletionMessage
import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall
import com.openai.models.chat.completions.ChatCompletionMessageToolCall
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Replaces
import io.micronaut.http.HttpRequest
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
    fun testAgentEndpoint() {
        val orchestratorResponse = mockk<ChatCompletion>()
        val orchestratorChoice = mockk<ChatCompletion.Choice>()
        val orchestratorMessage = mockk<ChatCompletionMessage>()
        val toolCall = mockk<ChatCompletionMessageToolCall>()
        val functionToolCall = mockk<ChatCompletionMessageFunctionToolCall>()
        val function = mockk<ChatCompletionMessageFunctionToolCall.Function>()

        every { function.name() } returns "generic_agent"
        every { function.arguments() } returns """{"query": "What is 2+3"}"""
        every { functionToolCall.function() } returns function
        every { functionToolCall.id() } returns "call_123"
        every { toolCall.isFunction() } returns true
        every { toolCall.asFunction() } returns functionToolCall
        every { orchestratorMessage.content() } returns Optional.of("")
        every { orchestratorMessage.toolCalls() } returns Optional.of(listOf(toolCall))
        every { orchestratorChoice.message() } returns orchestratorMessage
        every { orchestratorResponse.choices() } returns listOf(orchestratorChoice)

        val agentResponse = mockk<ChatCompletion>()
        val agentChoice = mockk<ChatCompletion.Choice>()
        val agentMessage = mockk<ChatCompletionMessage>()

        every { agentMessage.content() } returns Optional.of("The answer is 5")
        every { agentMessage.toolCalls() } returns Optional.empty()
        every { agentChoice.message() } returns agentMessage
        every { agentResponse.choices() } returns listOf(agentChoice)

        every { openAIClient.chat().completions().create(any()) } returns orchestratorResponse andThen agentResponse

        val request = HttpRequest.POST("/agent", AgentRequest("What is 2+3"))
        val response = client.toBlocking().retrieve(request)
        assertEquals("The answer is 5", response)
    }
}
