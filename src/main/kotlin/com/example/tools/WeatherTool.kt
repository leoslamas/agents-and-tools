package com.example.tools

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.openai.core.JsonValue
import com.openai.models.FunctionDefinition
import com.openai.models.chat.completions.ChatCompletionTool
import jakarta.inject.Singleton

@Singleton
class WeatherTool : AgentTool {
    override val name: String = "get_weather"
    override val description: String = "Returns the current weather for a given city."
    private val mapper = jacksonObjectMapper()

    override fun toToolDefinition(): ChatCompletionTool {
        val parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "city" to mapOf(
                    "type" to "string",
                    "description" to "The city to get the weather for."
                )
            ),
            "required" to listOf("city")
        )

        val functionDef = FunctionDefinition.builder()
            .name(name)
            .description(description)
            .parameters(JsonValue.from(parameters))
            .build()

        return ChatCompletionTool.ofFunction(
            com.openai.models.chat.completions.ChatCompletionFunctionTool.builder()
                .function(functionDef)
                .build()
        )
    }

    override fun execute(arguments: String): String {
        return try {
            val root = mapper.readTree(arguments)
            val city = root.get("city").asText()

            val temperature = kotlin.math.abs(city.hashCode()) % 30 + 10 // Random temp 10-40C
            val conditions = listOf("Sunny", "Cloudy", "Rainy", "Windy")[kotlin.math.abs(city.hashCode()) % 4]

            "{\"city\":\"\$city\", \"temperature\":\$temperature, \"conditions\":\"\$conditions\", \"unit\":\"Celsius\"}"
        } catch (e: Exception) {
            "{\"error\":\"\${e.message}\"}"
        }
    }
}
