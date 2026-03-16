package com.example.tools

import com.openai.models.chat.completions.ChatCompletionTool

interface AgentTool {
    val name: String
    val description: String

    /**
     * The tool definition to be sent to the OpenAI API
     */
    fun toToolDefinition(): ChatCompletionTool

    /**
     * Executes the tool with the given arguments (which are passed as a JSON string from the model)
     */
    fun execute(arguments: String): String
}
