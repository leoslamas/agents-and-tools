package com.example.agents

import com.openai.client.OpenAIClient
import com.openai.models.chat.completions.ChatCompletion
import com.openai.models.chat.completions.ChatCompletionMessage
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Optional

class AgentRunnerTest {

    @Test
    fun testRunnerCompletesWithoutTools() {
        val mockResponse = mockk<ChatCompletion>()
        val mockChoice = mockk<ChatCompletion.Choice>()
        val mockMessage = mockk<ChatCompletionMessage>()

        // Chained mock for the fluent API
        val openAIClient = mockk<OpenAIClient> {
            every { chat().completions().create(any()) } returns mockResponse
        }

        every { mockResponse.choices() } returns listOf(mockChoice)
        every { mockChoice.message() } returns mockMessage
        every { mockMessage.content() } returns Optional.of("Hello, I'm an agent.")
        every { mockMessage.toolCalls() } returns Optional.empty()

        val runner = AgentRunner(openAIClient)
        val result = runner.run("Say hello", "You are a helpful assistant", emptyList())

        assertEquals("Hello, I'm an agent.", result)
    }
}
