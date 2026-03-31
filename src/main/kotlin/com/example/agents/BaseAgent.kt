package com.example.agents

import com.example.tools.AgentTool

abstract class BaseAgent(
    private val agentRunner: AgentRunner
) : Agent {
    abstract val systemPrompt: String
    open val tools: List<AgentTool> = emptyList()

    override fun ask(prompt: String): String {
        return agentRunner.run(prompt, systemPrompt, tools)
    }
}
