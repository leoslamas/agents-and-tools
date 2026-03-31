package com.example.agents

import com.example.tools.AgentTool
import com.example.tools.ToolParam
import com.example.tools.generateSchema
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.openai.client.OpenAIClient
import com.openai.models.chat.completions.ChatCompletionCreateParams
import com.openai.models.chat.completions.ChatCompletionMessageParam
import com.openai.models.chat.completions.ChatCompletionSystemMessageParam
import com.openai.models.chat.completions.ChatCompletionUserMessageParam
import io.micronaut.context.annotation.Value
import jakarta.inject.Singleton
import org.slf4j.LoggerFactory

data class AgentRouteParams(
    @ToolParam("The user's question or request to forward to this agent.")
    val query: String
)

@Singleton
class AgentOrchestrator(
    private val openAIClient: OpenAIClient,
    private val agents: List<Agent>,
    @Value("\${llm.model}") private val model: String
) {

    private val logger = LoggerFactory.getLogger(AgentOrchestrator::class.java)
    private val mapper = jacksonObjectMapper()
    private val agentsMap by lazy { agents.associateBy { it.name } }

    private val systemPrompt = """
        You are a routing assistant. Your job is to analyze the user's question and delegate it to the most appropriate specialized agent.
        You MUST call exactly one agent to handle the user's request. Do not try to answer directly.
        Available agents and their capabilities are provided as tools.
    """.trimIndent()

    private val routeSchema = generateSchema(AgentRouteParams::class)

    private val agentToolDefinitions by lazy {
        agents.map { agent ->
            AgentTool.buildToolDefinition(
                name = agent.name,
                description = agent.description,
                parameters = routeSchema
            )
        }
    }

    fun route(userPrompt: String): String {
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

        val request = ChatCompletionCreateParams.builder()
            .model(model)
            .messages(messages)
            .tools(agentToolDefinitions)
            .build()

        val response = openAIClient.chat().completions().create(request)
        val responseMessage = response.choices().firstOrNull()?.message() ?: return "No response from orchestrator"

        val toolCalls = responseMessage.toolCalls()
        if (toolCalls.isEmpty || toolCalls.get().isEmpty()) {
            return responseMessage.content().orElse("No agent was selected")
        }

        val toolCall = toolCalls.get().first()
        if (!toolCall.isFunction()) return "Invalid agent selection"

        val functionCall = toolCall.asFunction().function()
        val agentName = functionCall.name()
        val arguments = functionCall.arguments()

        logger.info("Orchestrator selected agent: $agentName")

        val query = try {
            mapper.readTree(arguments).get("query")?.asText() ?: userPrompt
        } catch (e: Exception) {
            userPrompt
        }

        val agent = agentsMap[agentName] ?: return mapper.writeValueAsString(mapOf("error" to "Unknown agent $agentName"))

        return agent.ask(query)
    }
}
