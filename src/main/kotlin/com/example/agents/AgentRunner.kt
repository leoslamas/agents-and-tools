package com.example.agents

import com.example.tools.AgentTool
import com.openai.client.OpenAIClient
import com.openai.models.ChatModel
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.openai.models.chat.completions.ChatCompletionMessageParam
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam
import com.openai.models.chat.completions.ChatCompletionToolMessageParam
import com.openai.models.chat.completions.ChatCompletionUserMessageParam
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
class AgentRunner(private val openAIClient: OpenAIClient) {

    private val logger = LoggerFactory.getLogger(AgentRunner::class.java)

    /**
     * Executes the conversational loop using the specified system prompt and tools.
     */
    fun run(userPrompt: String, systemPrompt: String, tools: List<AgentTool>): String {
        val messages = mutableListOf<ChatCompletionMessageParam>(
            ChatCompletionMessageParam.ofSystem(
                ChatCompletionSystemMessageParam.builder()
                    .content(systemPrompt)
                    .build()
            ),
            ChatCompletionMessageParam.ofUser(
                ChatCompletionUserMessageParam.builder()
                    .content(userPrompt)
                    .build()
            )
        )

        val toolDefinitions = tools.map { it.toToolDefinition() }
        val toolsMap = tools.associateBy { it.name }

        var iteration = 0
        while (iteration < 10) { // Safety limit
            iteration++
            logger.info("Agent iteration \$iteration")

            val requestBuilder = ChatCompletionCreateParams.builder()
                .model(ChatModel.GPT_4O_MINI)
                .messages(messages)

            if (toolDefinitions.isNotEmpty()) {
                requestBuilder.tools(toolDefinitions)
            }

            val request = requestBuilder.build()
            val response = openAIClient.chat().completions().create(request)
            val responseMessage = response.choices().first().message()

            val assistantMessageBuilder = ChatCompletionAssistantMessageParam.builder()
                .content(ChatCompletionAssistantMessageParam.Content.ofText(responseMessage.content().orElse("")))

            responseMessage.toolCalls().ifPresent { toolCalls ->
                assistantMessageBuilder.toolCalls(toolCalls)
            }
            messages.add(ChatCompletionMessageParam.ofAssistant(assistantMessageBuilder.build()))

            val toolCalls = responseMessage.toolCalls()
            if (toolCalls.isEmpty || toolCalls.get().isEmpty()) {
                // No more tool calls, we have the final answer
                return responseMessage.content().orElse("No response from agent")
            }

            logger.info("Agent decided to call \${toolCalls.get().size} tool(s)")

            for (toolCall in toolCalls.get()) {
                if (toolCall.isFunction()) {
                    val functionCall = toolCall.asFunction().function()
                    val toolName = functionCall.name()
                    val arguments = functionCall.arguments()

                    logger.info("Executing tool: \$toolName with args: \$arguments")

                    val agentTool = toolsMap[toolName]
                    val result = if (agentTool != null) {
                        try {
                            agentTool.execute(arguments)
                        } catch (e: Exception) {
                            "{\"error\": \"\${e.message}\"}"
                        }
                    } else {
                        "{\"error\": \"Unknown tool \$toolName\"}"
                    }

                    logger.info("Tool \$toolName returned: \$result")

                    messages.add(
                        ChatCompletionMessageParam.ofTool(
                            ChatCompletionToolMessageParam.builder()
                                .toolCallId(toolCall.asFunction().id())
                                .content(ChatCompletionToolMessageParam.Content.ofText(result))
                                .build()
                        )
                    )
                }
            }
        }
        return "Agent reached maximum iterations without completing the task."
    }
}
