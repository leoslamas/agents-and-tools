package com.example.agents

import com.example.tools.AgentTool
import com.openai.client.OpenAIClient
import com.openai.models.chat.completions.ChatCompletion
import com.openai.models.chat.completions.ChatCompletionMessage
import com.openai.models.chat.completions.ChatCompletionMessageFunctionToolCall
import com.openai.models.chat.completions.ChatCompletionMessageToolCall
import com.openai.models.chat.completions.ChatCompletionTool
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Optional

class AgentRunnerTest {

    private val openAIClient = mockk<OpenAIClient>()
    private val runner = AgentRunner(openAIClient, "test-model")

    private fun mockChatResponse(message: ChatCompletionMessage): ChatCompletion {
        val choice = mockk<ChatCompletion.Choice>()
        val response = mockk<ChatCompletion>()
        every { choice.message() } returns message
        every { response.choices() } returns listOf(choice)
        return response
    }

    private fun mockTextMessage(content: String): ChatCompletionMessage {
        val message = mockk<ChatCompletionMessage>()
        every { message.content() } returns Optional.of(content)
        every { message.toolCalls() } returns Optional.empty()
        return message
    }

    private fun mockToolCallMessage(
        toolName: String,
        arguments: String,
        toolCallId: String = "call_123"
    ): ChatCompletionMessage {
        val function = mockk<ChatCompletionMessageFunctionToolCall.Function>()
        every { function.name() } returns toolName
        every { function.arguments() } returns arguments

        val functionToolCall = mockk<ChatCompletionMessageFunctionToolCall>()
        every { functionToolCall.function() } returns function
        every { functionToolCall.id() } returns toolCallId

        val toolCall = mockk<ChatCompletionMessageToolCall>()
        every { toolCall.isFunction() } returns true
        every { toolCall.asFunction() } returns functionToolCall

        val message = mockk<ChatCompletionMessage>()
        every { message.content() } returns Optional.of("")
        every { message.toolCalls() } returns Optional.of(listOf(toolCall))
        return message
    }

    private fun createMockTool(name: String, executeResult: String): AgentTool {
        return object : AgentTool {
            override val name: String = name
            override val description: String = "Test tool"
            override fun toToolDefinition(): ChatCompletionTool = mockk()
            override fun execute(arguments: String): String = executeResult
        }
    }

    @Test
    fun `deve completar sem ferramentas quando LLM retorna texto`() {
        val textResponse = mockChatResponse(mockTextMessage("Hello, I'm an agent."))
        every { openAIClient.chat().completions().create(any()) } returns textResponse

        val result = runner.run("Say hello", "You are a helpful assistant.", emptyList())

        assertEquals("Hello, I'm an agent.", result)
    }

    @Test
    fun `deve executar ferramenta e retornar resposta final`() {
        val toolCallResponse = mockChatResponse(
            mockToolCallMessage("get_weather", """{"city": "London"}""")
        )
        val finalResponse = mockChatResponse(
            mockTextMessage("The weather in London is sunny and 25C.")
        )

        every { openAIClient.chat().completions().create(any()) } returns toolCallResponse andThen finalResponse

        val weatherTool = createMockTool("get_weather", """{"city":"London","temperature":25,"conditions":"Sunny"}""")
        val result = runner.run("What's the weather in London?", "You are a weather agent.", listOf(weatherTool))

        assertEquals("The weather in London is sunny and 25C.", result)
    }

    @Test
    fun `deve retornar erro para ferramenta desconhecida`() {
        val toolCallResponse = mockChatResponse(
            mockToolCallMessage("unknown_tool", """{"param": "value"}""")
        )
        val finalResponse = mockChatResponse(
            mockTextMessage("I encountered an error.")
        )

        every { openAIClient.chat().completions().create(any()) } returns toolCallResponse andThen finalResponse

        val result = runner.run("Do something", "System prompt", emptyList())

        assertEquals("I encountered an error.", result)
    }

    @Test
    fun `deve retornar mensagem de max iteracoes quando limite e atingido`() {
        val toolCallMessage = mockToolCallMessage("my_tool", """{"x": 1}""")
        val loopingResponse = mockChatResponse(toolCallMessage)

        every { openAIClient.chat().completions().create(any()) } returns loopingResponse

        val tool = createMockTool("my_tool", """{"result": "ok"}""")
        val result = runner.run("Loop forever", "System prompt", listOf(tool))

        assertEquals("Agent reached maximum iterations without completing the task.", result)
    }

    @Test
    fun `deve retornar fallback quando choices esta vazio`() {
        val response = mockk<ChatCompletion>()
        every { response.choices() } returns emptyList()

        every { openAIClient.chat().completions().create(any()) } returns response

        val result = runner.run("Hello", "System prompt", emptyList())

        assertEquals("No response from agent", result)
    }

    @Test
    fun `deve executar multiplas chamadas de ferramenta em sequencia`() {
        val firstToolCall = mockChatResponse(
            mockToolCallMessage("calculator", """{"expression": "2+2"}""", "call_1")
        )
        val secondToolCall = mockChatResponse(
            mockToolCallMessage("calculator", """{"expression": "4*3"}""", "call_2")
        )
        val finalResponse = mockChatResponse(
            mockTextMessage("2+2=4 and 4*3=12")
        )

        every { openAIClient.chat().completions().create(any()) } returns firstToolCall andThen secondToolCall andThen finalResponse

        val calcTool = createMockTool("calculator", """{"result": "4.0"}""")
        val result = runner.run("Calculate 2+2 then 4*3", "System prompt", listOf(calcTool))

        assertEquals("2+2=4 and 4*3=12", result)
    }

    @Test
    fun `deve tratar excecao na execucao da ferramenta`() {
        val toolCallResponse = mockChatResponse(
            mockToolCallMessage("failing_tool", """{"x": 1}""")
        )
        val finalResponse = mockChatResponse(
            mockTextMessage("Something went wrong with the tool.")
        )

        every { openAIClient.chat().completions().create(any()) } returns toolCallResponse andThen finalResponse

        val failingTool = object : AgentTool {
            override val name = "failing_tool"
            override val description = "A tool that fails"
            override fun toToolDefinition(): ChatCompletionTool = mockk()
            override fun execute(arguments: String): String {
                throw RuntimeException("Tool execution failed")
            }
        }

        val result = runner.run("Use the tool", "System prompt", listOf(failingTool))

        assertEquals("Something went wrong with the tool.", result)
    }

    @Test
    fun `deve nao enviar definicoes de ferramentas quando lista esta vazia`() {
        val textResponse = mockChatResponse(mockTextMessage("No tools available."))
        every { openAIClient.chat().completions().create(any()) } returns textResponse

        val result = runner.run("Hello", "System prompt", emptyList())

        assertEquals("No tools available.", result)
        verify(exactly = 1) { openAIClient.chat().completions().create(any()) }
    }

    @Test
    fun `deve retornar fallback quando conteudo da mensagem esta vazio e sem tool calls`() {
        val message = mockk<ChatCompletionMessage>()
        every { message.content() } returns Optional.empty()
        every { message.toolCalls() } returns Optional.empty()

        val response = mockChatResponse(message)
        every { openAIClient.chat().completions().create(any()) } returns response

        val result = runner.run("Hello", "System prompt", emptyList())

        assertEquals("No response from agent", result)
    }
}
