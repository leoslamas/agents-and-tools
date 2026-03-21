package com.example.tools

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.inject.Singleton

@Singleton
class WeatherTool : AgentTool() {
    override val name: String = "get_weather"
    override val description: String = "Returns the current weather for a given city."
    private val mapper = jacksonObjectMapper()

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "city" to mapOf(
                "type" to "string",
                "description" to "The city to get the weather for."
            )
        ),
        "required" to listOf("city")
    )

    override fun execute(arguments: String): String {
        return try {
            val root = mapper.readTree(arguments)
            val cityNode = root.get("city")
            if (cityNode == null) return mapper.writeValueAsString(mapOf("error" to "Missing city"))
            val city = cityNode.asText()

            val temperature = kotlin.math.abs(city.hashCode()) % 30 + 10
            val conditions = listOf("Sunny", "Cloudy", "Rainy", "Windy")[kotlin.math.abs(city.hashCode()) % 4]

            mapper.writeValueAsString(mapOf("city" to city, "temperature" to temperature, "conditions" to conditions, "unit" to "Celsius"))
        } catch (e: Exception) {
            mapper.writeValueAsString(mapOf("error" to e.message))
        }
    }
}
