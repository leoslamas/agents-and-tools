package com.example.agents

import com.example.tools.CalculatorTool
import com.example.tools.TimeTool
import jakarta.inject.Singleton

@Singleton
class MathAgent(
    private val agentRunner: AgentRunner,
    private val timeTool: TimeTool,
    private val calculatorTool: CalculatorTool
) {
    private val systemPrompt = """
        You are MathAgent, a helpful assistant specialized in mathematics and time.
        You have tools to get the current time and evaluate basic math expressions.
        Always answer concisely.
    """.trimIndent()

    private val tools = listOf(timeTool, calculatorTool)

    fun ask(prompt: String): String {
        return agentRunner.run(prompt, systemPrompt, tools)
    }
}
