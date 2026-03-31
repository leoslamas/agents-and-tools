package com.example.controllers

import com.example.agents.AgentOrchestrator
import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Body
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Post
import io.micronaut.scheduling.TaskExecutors
import io.micronaut.scheduling.annotation.ExecuteOn
import io.micronaut.serde.annotation.Serdeable

@Serdeable
data class AgentRequest(val prompt: String)

@Serdeable
data class AgentResponse(val response: String)

@Serdeable
data class ErrorResponse(val error: String)

@Controller("/agent")
class AgentDemoController(private val orchestrator: AgentOrchestrator) {

    @Post
    @ExecuteOn(TaskExecutors.BLOCKING)
    fun ask(@Body request: AgentRequest): HttpResponse<*> {
        if (request.prompt.isBlank()) {
            return HttpResponse.badRequest(ErrorResponse("Prompt must not be blank"))
        }

        return try {
            HttpResponse.ok(AgentResponse(orchestrator.route(request.prompt)))
        } catch (e: Exception) {
            HttpResponse.serverError(ErrorResponse(e.message ?: "Internal error"))
        }
    }
}
