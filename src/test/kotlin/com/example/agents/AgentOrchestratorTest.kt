package com.example.agents

import com.openai.client.OpenAIClient
import com.openai.models.chat.completions.ChatCompletion
import com.openai.models.chat.completions.ChatCompletionMessage
import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall
import com.openai.models.chat.completions.ChatCompletionMessageToolCall
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Optional

class AgentOrchestratorTest {

    private val openAIClient = mockk<OpenAIClient>()

    private fun createAgent(name: String, response: String): Agent {
        return object : Agent {
            override val name = name
            override val description = "Test agent: $name"
            override fun ask(prompt: String): String = response
        }
    }

    private fun mockChatResponse(message: ChatCompletionMessage): ChatCompletion {
        val choice = mockk<ChatCompletion.Choice>()
        val response = mockk<ChatCompletion>()
        every { choice.message() } returns message
        every { response.choices() } returns listOf(choice)
        return response
    }

    private fun mockToolCallMessage(agentName: String, query: String): ChatCompletionMessage {
        val function = mockk<ChatCompletionMessageFunctionToolCall.Function>()
        every { function.name() } returns agentName
        every { function.arguments() } returns """{"query": "$query"}"""

        val functionToolCall = mockk<ChatCompletionMessageFunctionToolCall>()
        every { functionToolCall.function() } returns function
        every { functionToolCall.id() } returns "call_456"

        val toolCall = mockk<ChatCompletionMessageToolCall>()
        every { toolCall.isFunction() } returns true
        every { toolCall.asFunction() } returns functionToolCall

        val message = mockk<ChatCompletionMessage>()
        every { message.content() } returns Optional.of("")
        every { message.toolCalls() } returns Optional.of(listOf(toolCall))
        return message
    }

    private fun mockTextMessage(content: String): ChatCompletionMessage {
        val message = mockk<ChatCompletionMessage>()
        every { message.content() } returns Optional.of(content)
        every { message.toolCalls() } returns Optional.empty()
        return message
    }

    @Test
    fun `deve rotear para agente correto quando LLM seleciona via tool call`() {
        val mathAgent = createAgent("math_agent", "The answer is 42")
        val weatherAgent = createAgent("weather_agent", "It's sunny")
        val orchestrator = AgentOrchestrator(openAIClient, listOf(mathAgent, weatherAgent), "test-model")

        val toolCallResponse = mockChatResponse(mockToolCallMessage("math_agent", "What is 6*7?"))
        every { openAIClient.chat().completions().create(any()) } returns toolCallResponse

        val result = orchestrator.route("What is 6*7?")

        assertEquals("The answer is 42", result)
    }

    @Test
    fun `deve retornar texto quando LLM nao seleciona agente`() {
        val agent = createAgent("generic_agent", "unused")
        val orchestrator = AgentOrchestrator(openAIClient, listOf(agent), "test-model")

        val textResponse = mockChatResponse(mockTextMessage("I can help you with that directly."))
        every { openAIClient.chat().completions().create(any()) } returns textResponse

        val result = orchestrator.route("Hello")

        assertEquals("I can help you with that directly.", result)
    }

    @Test
    fun `deve retornar fallback quando LLM retorna texto vazio e sem tool calls`() {
        val agent = createAgent("generic_agent", "unused")
        val orchestrator = AgentOrchestrator(openAIClient, listOf(agent), "test-model")

        val message = mockk<ChatCompletionMessage>()
        every { message.content() } returns Optional.empty()
        every { message.toolCalls() } returns Optional.empty()

        val response = mockChatResponse(message)
        every { openAIClient.chat().completions().create(any()) } returns response

        val result = orchestrator.route("Hello")

        assertEquals("No agent was selected", result)
    }

    @Test
    fun `deve retornar erro quando agente desconhecido e selecionado`() {
        val agent = createAgent("math_agent", "unused")
        val orchestrator = AgentOrchestrator(openAIClient, listOf(agent), "test-model")

        val toolCallResponse = mockChatResponse(mockToolCallMessage("nonexistent_agent", "query"))
        every { openAIClient.chat().completions().create(any()) } returns toolCallResponse

        val result = orchestrator.route("Do something")

        assertTrue(result.contains("Unknown agent nonexistent_agent"))
    }

    @Test
    fun `deve retornar fallback quando choices esta vazio`() {
        val agent = createAgent("generic_agent", "unused")
        val orchestrator = AgentOrchestrator(openAIClient, listOf(agent), "test-model")

        val response = mockk<ChatCompletion>()
        every { response.choices() } returns emptyList()
        every { openAIClient.chat().completions().create(any()) } returns response

        val result = orchestrator.route("Hello")

        assertEquals("No response from orchestrator", result)
    }

    @Test
    fun `deve usar prompt original quando argumentos JSON sao invalidos`() {
        val agent = object : Agent {
            override val name = "math_agent"
            override val description = "Math agent"
            var receivedPrompt: String = ""
            override fun ask(prompt: String): String {
                receivedPrompt = prompt
                return "result"
            }
        }
        val orchestrator = AgentOrchestrator(openAIClient, listOf(agent), "test-model")

        val function = mockk<ChatCompletionMessageFunctionToolCall.Function>()
        every { function.name() } returns "math_agent"
        every { function.arguments() } returns "invalid json {"

        val functionToolCall = mockk<ChatCompletionMessageFunctionToolCall>()
        every { functionToolCall.function() } returns function
        every { functionToolCall.id() } returns "call_789"

        val toolCall = mockk<ChatCompletionMessageToolCall>()
        every { toolCall.isFunction() } returns true
        every { toolCall.asFunction() } returns functionToolCall

        val message = mockk<ChatCompletionMessage>()
        every { message.content() } returns Optional.of("")
        every { message.toolCalls() } returns Optional.of(listOf(toolCall))

        val response = mockChatResponse(message)
        every { openAIClient.chat().completions().create(any()) } returns response

        orchestrator.route("What is 2+2?")

        assertEquals("What is 2+2?", agent.receivedPrompt)
    }

    @Test
    fun `deve retornar invalid selection quando tool call nao e function`() {
        val agent = createAgent("math_agent", "unused")
        val orchestrator = AgentOrchestrator(openAIClient, listOf(agent), "test-model")

        val toolCall = mockk<ChatCompletionMessageToolCall>()
        every { toolCall.isFunction() } returns false

        val message = mockk<ChatCompletionMessage>()
        every { message.content() } returns Optional.of("")
        every { message.toolCalls() } returns Optional.of(listOf(toolCall))

        val response = mockChatResponse(message)
        every { openAIClient.chat().completions().create(any()) } returns response

        val result = orchestrator.route("Hello")

        assertEquals("Invalid agent selection", result)
    }

    @Test
    fun `deve encaminhar query extraida dos argumentos para o agente`() {
        val agent = object : Agent {
            override val name = "weather_agent"
            override val description = "Weather agent"
            var receivedPrompt: String = ""
            override fun ask(prompt: String): String {
                receivedPrompt = prompt
                return "Sunny in London"
            }
        }
        val orchestrator = AgentOrchestrator(openAIClient, listOf(agent), "test-model")

        val toolCallResponse = mockChatResponse(mockToolCallMessage("weather_agent", "Weather in London"))
        every { openAIClient.chat().completions().create(any()) } returns toolCallResponse

        orchestrator.route("What's the weather in London?")

        assertEquals("Weather in London", agent.receivedPrompt)
    }
}
