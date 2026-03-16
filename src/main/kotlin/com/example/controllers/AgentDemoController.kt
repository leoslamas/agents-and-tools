package com.example.controllers

import com.example.agents.MathAgent
import com.example.agents.WeatherAgent
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue

@Controller("/agents")
class AgentDemoController(
    private val mathAgent: MathAgent,
    private val weatherAgent: WeatherAgent
) {

    @Get("/math")
    fun askMath(@QueryValue prompt: String): String {
        return try {
            mathAgent.ask(prompt)
        } catch (e: Exception) {
            "Error: \${e.message}"
        }
    }

    @Get("/weather")
    fun askWeather(@QueryValue prompt: String): String {
        return try {
            weatherAgent.ask(prompt)
        } catch (e: Exception) {
            "Error: \${e.message}"
        }
    }
}
