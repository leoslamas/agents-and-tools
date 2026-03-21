package com.example.controllers

import com.example.agents.AgentOrchestrator
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class AgentRequest(val prompt: String)

@Controller("/agent")
class AgentDemoController(private val orchestrator: AgentOrchestrator) {

    @Post
    @ExecuteOn(TaskExecutors.BLOCKING)
    fun ask(@Body request: AgentRequest): String {
        return try {
            orchestrator.route(request.prompt)
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
