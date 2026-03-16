package com.example.tools

import com.openai.core.JsonValue
import com.openai.models.FunctionDefinition
import com.openai.models.chat.completions.ChatCompletionTool
import jakarta.inject.Singleton
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Singleton
class TimeTool : AgentTool {
    override val name: String = "get_current_time"
    override val description: String = "Returns the current date and time."

    override fun toToolDefinition(): ChatCompletionTool {
        val parameters = mapOf(
            "type" to "object",
            "properties" to emptyMap<String, Any>()
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
        val now = ZonedDateTime.now()
        val formatted = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        return "{\"currentTime\":\"$formatted\"}"
    }
}
