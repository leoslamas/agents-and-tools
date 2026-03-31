package com.example.tools

import jakarta.inject.Singleton

data class WeatherParams(
    @ToolParam("The city to get the weather for.")
    val city: String
)

@Singleton
class WeatherTool : TypedAgentTool<WeatherParams>(WeatherParams::class) {
    override val name = "get_weather"
    override val description = "Returns the current weather for a given city."

    override fun execute(arguments: String): String {
        return try {
            val params = parseArgs(arguments)
            val city = params.city

            val temperature = kotlin.math.abs(city.hashCode()) % 30 + 10
            val conditions = listOf("Sunny", "Cloudy", "Rainy", "Windy")[kotlin.math.abs(city.hashCode()) % 4]

            mapper.writeValueAsString(
                mapOf("city" to city, "temperature" to temperature, "conditions" to conditions, "unit" to "Celsius")
            )
        } catch (e: Exception) {
            mapper.writeValueAsString(mapOf("error" to e.message))
        }
    }
}
