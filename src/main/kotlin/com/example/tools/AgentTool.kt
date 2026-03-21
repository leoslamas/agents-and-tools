package com.example.tools

import com.openai.core.JsonValue
import com.openai.models.FunctionDefinition
import com.openai.models.chat.completions.ChatCompletionFunctionTool
import com.openai.models.chat.completions.ChatCompletionTool

abstract class AgentTool {
    abstract val name: String
    abstract val description: String
    abstract val parameters: Map<String, Any>

    fun toToolDefinition(): ChatCompletionTool {
        val strictParameters = parameters + ("additionalProperties" to false)

        val functionDef = FunctionDefinition.builder()
            .name(name)
            .description(description)
            .parameters(JsonValue.from(strictParameters))
            .strict(true)
            .build()

        return ChatCompletionTool.ofFunction(
            ChatCompletionFunctionTool.builder()
                .function(functionDef)
                .build()
        )
    }

    abstract fun execute(arguments: String): String
}
