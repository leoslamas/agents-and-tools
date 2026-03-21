package com.example.agents

interface Agent {
    val name: String
    val description: String
    fun ask(prompt: String): String
}
