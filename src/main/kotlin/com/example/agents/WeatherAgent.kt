package com.example.agents

import com.example.tools.AgentTool
import com.example.tools.TimeTool
import com.example.tools.WeatherTool
import jakarta.inject.Singleton

@Singleton
class WeatherAgent(
    agentRunner: AgentRunner,
    private val timeTool: TimeTool,
    private val weatherTool: WeatherTool
) : BaseAgent(agentRunner) {

    override val name = "weather_agent"
    override val description = "Specialized in weather forecasts and time. Can check weather for cities and tell the current time."

    override val systemPrompt = """
        You are WeatherAgent, a helpful assistant specialized in weather forecasts and time.
        You have tools to get the current time and check the weather for cities.
        Always answer concisely.
    """.trimIndent()

    override val tools: List<AgentTool> = listOf(timeTool, weatherTool)
}
