package com.example.agents

import jakarta.inject.Singleton

@Singleton
class GenericAgent(
    private val agentRunner: AgentRunner
) : Agent {

    override val name = "generic_agent"
    override val description = "General-purpose conversational agent. Use for questions that don't require specialized tools like math or weather."

    private val systemPrompt = """
        You are a generic agent and can talk about anything and everything.
        Always answer concisely.
    """.trimIndent()

    override fun ask(prompt: String): String {
        return agentRunner.run(prompt, systemPrompt, emptyList())
    }
}
