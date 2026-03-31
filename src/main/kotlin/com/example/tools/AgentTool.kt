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
        return buildToolDefinition(name, description, strictParameters, strict = true)
    }

    companion object {
        fun buildToolDefinition(
            name: String,
            description: String,
            parameters: Map<String, Any>,
            strict: Boolean = true
        ): ChatCompletionTool {
            val finalParams = if ("additionalProperties" !in parameters) {
                parameters + ("additionalProperties" to false)
            } else {
                parameters
            }

            return ChatCompletionTool.ofFunction(
                ChatCompletionFunctionTool.builder()
                    .function(
                        FunctionDefinition.builder()
                            .name(name)
                            .description(description)
                            .parameters(JsonValue.from(finalParams))
                            .strict(strict)
                            .build()
                    )
                    .build()
            )
        }
    }

    abstract fun execute(arguments: String): String
}
