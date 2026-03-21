package com.example.tools

import jakarta.inject.Singleton
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Singleton
class TimeTool : AgentTool() {
    override val name: String = "get_current_time"
    override val description: String = "Returns the current date and time."

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to emptyMap<String, Any>(),
        "required" to emptyList<String>()
    )

    override fun execute(arguments: String): String {
        val now = ZonedDateTime.now()
        val formatted = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        return "{\"currentTime\":\"$formatted\"}"
    }
}
