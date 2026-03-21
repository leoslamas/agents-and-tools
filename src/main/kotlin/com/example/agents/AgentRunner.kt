package com.example.agents

import com.example.tools.AgentTool
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.openai.client.OpenAIClient
import com.openai.models.chat.completions.ChatCompletionAssistantMessageParam
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.openai.models.chat.completions.ChatCompletionMessageParam
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam
import com.openai.models.chat.completions.ChatCompletionToolMessageParam
import com.openai.models.chat.completions.ChatCompletionUserMessageParam
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

@Singleton
class AgentRunner(
    private val openAIClient: OpenAIClient,
    @Value("\${llm.model}") private val model: String
) {

    private val logger = LoggerFactory.getLogger(AgentRunner::class.java)
    private val mapper = jacksonObjectMapper()

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
        while (iteration < 10) {
            iteration++
            logger.info("Agent iteration $iteration")

            val requestBuilder = ChatCompletionCreateParams.builder()
                .model(model)
                .messages(messages)

            if (toolDefinitions.isNotEmpty()) {
                requestBuilder.tools(toolDefinitions)
            }

            val request = requestBuilder.build()
            val response = openAIClient.chat().completions().create(request)
            val responseMessage = response.choices().firstOrNull()?.message() ?: return "No response from agent"

            val assistantMessageBuilder = ChatCompletionAssistantMessageParam.builder()
                .content(ChatCompletionAssistantMessageParam.Content.ofText(responseMessage.content().orElse("")))

            responseMessage.toolCalls().ifPresent { toolCalls ->
                assistantMessageBuilder.toolCalls(toolCalls)
            }
            messages.add(ChatCompletionMessageParam.ofAssistant(assistantMessageBuilder.build()))

            val toolCalls = responseMessage.toolCalls()
            if (toolCalls.isEmpty || toolCalls.get().isEmpty()) {
                return responseMessage.content().orElse("No response from agent")
            }

            logger.info("Agent decided to call ${toolCalls.get().size} tool(s)")

            for (toolCall in toolCalls.get()) {
                if (toolCall.isFunction()) {
                    val functionCall = toolCall.asFunction().function()
                    val toolName = functionCall.name()
                    val arguments = functionCall.arguments()

                    logger.debug("Executing tool: $toolName with args: $arguments")

                    val agentTool = toolsMap[toolName]
                    val result = if (agentTool != null) {
                        try {
                            agentTool.execute(arguments)
                        } catch (e: Exception) {
                            mapper.writeValueAsString(mapOf("error" to e.message))
                        }
                    } else {
                        mapper.writeValueAsString(mapOf("error" to "Unknown tool $toolName"))
                    }

                    logger.debug("Tool $toolName returned: $result")

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
