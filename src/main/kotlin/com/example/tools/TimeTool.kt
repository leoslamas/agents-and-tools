package com.example.tools

import jakarta.inject.Singleton
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TimeParams

@Singleton
class TimeTool : TypedAgentTool<TimeParams>(TimeParams::class) {
    override val name = "get_current_time"
    override val description = "Returns the current date and time."

    override fun execute(arguments: String): String {
        val now = ZonedDateTime.now()
        val formatted = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        return mapper.writeValueAsString(mapOf("currentTime" to formatted))
    }
}
