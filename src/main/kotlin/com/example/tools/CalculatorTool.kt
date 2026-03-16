package com.example.tools

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.openai.core.JsonValue
import com.openai.models.FunctionDefinition
import com.openai.models.chat.completions.ChatCompletionTool
import jakarta.inject.Singleton

@Singleton
class CalculatorTool : AgentTool {
    override val name: String = "calculate"
    override val description: String = "Evaluates a basic mathematical expression (e.g. 2 + 2, 5 * 10)."
    private val mapper = jacksonObjectMapper()

    override fun toToolDefinition(): ChatCompletionTool {
        val parameters = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "expression" to mapOf(
                    "type" to "string",
                    "description" to "The mathematical expression to evaluate."
                )
            ),
            "required" to listOf("expression")
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
        return try {
            val root = mapper.readTree(arguments)
            val expression = root.get("expression").asText()

            val cleanExpr = expression.replace(" ", "")
            val regex = Regex("(-?\\d+\\.?\\d*)([+\\-*/])(-?\\d+\\.?\\d*)")
            val match = regex.find(cleanExpr)

            val result = if (match != null) {
                val (a, op, b) = match.destructured
                val numA = a.toDouble()
                val numB = b.toDouble()
                when (op) {
                    "+" -> numA + numB
                    "-" -> numA - numB
                    "*" -> numA * numB
                    "/" -> numA / numB
                    else -> throw IllegalArgumentException("Unsupported operator")
                }
            } else {
                throw IllegalArgumentException("Could not parse simple expression")
            }

            "{\"result\":\"\$result\"}"
        } catch (e: Exception) {
            "{\"error\":\"\${e.message}\"}"
        }
    }
}
