package com.example.agents

import com.example.tools.CalculatorTool
import com.example.tools.TimeTool
import jakarta.inject.Singleton

@Singleton
class MathAgent(
    private val agentRunner: AgentRunner,
    private val timeTool: TimeTool,
    private val calculatorTool: CalculatorTool
) : Agent {

    override val name = "math_agent"
    override val description = "Specialized in mathematics and time. Can evaluate math expressions and tell the current time."

    private val systemPrompt = """
        You are MathAgent, a helpful assistant specialized in mathematics and time.
        You have tools to get the current time and evaluate basic math expressions.
        Always answer concisely.
    """.trimIndent()

    private val tools = listOf(timeTool, calculatorTool)

    override fun ask(prompt: String): String {
        return agentRunner.run(prompt, systemPrompt, tools)
    }
}
