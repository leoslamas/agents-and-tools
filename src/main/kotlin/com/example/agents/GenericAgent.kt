package com.example.agents

import jakarta.inject.Singleton

@Singleton
class GenericAgent(
    agentRunner: AgentRunner
) : BaseAgent(agentRunner) {

    override val name = "generic_agent"
    override val description = "General-purpose conversational agent. Use for questions that don't require specialized tools like math or weather."

    override val systemPrompt = """
        You are a generic agent and can talk about anything and everything.
        Always answer concisely.
    """.trimIndent()
}
