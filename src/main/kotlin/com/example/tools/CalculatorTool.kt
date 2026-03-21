package com.example.tools

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import jakarta.inject.Singleton

@Singleton
class CalculatorTool : AgentTool() {
    override val name: String = "calculate"
    override val description: String = "Evaluates a basic mathematical expression (e.g. 2 + 2, 5 * 10)."
    private val mapper = jacksonObjectMapper()

    override val parameters: Map<String, Any> = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "expression" to mapOf(
                "type" to "string",
                "description" to "The mathematical expression to evaluate."
            )
        ),
        "required" to listOf("expression")
    )

    override fun execute(arguments: String): String {
        return try {
            val root = mapper.readTree(arguments)
            val expressionNode = root.get("expression")
                ?: return mapper.writeValueAsString(mapOf("error" to "Missing expression"))
            val expression = expressionNode.asText()

            val cleanExpr = expression.replace(" ", "")
            val regex = Regex("(-?\\d+\\.?\\d*)([+\\-*/])(-?\\d+\\.?\\d*)")
            val match = regex.find(cleanExpr)
                ?: throw IllegalArgumentException("Could not parse simple expression")

            val (a, op, b) = match.destructured
            val numA = a.toDouble()
            val numB = b.toDouble()
            val result = when (op) {
                "+" -> numA + numB
                "-" -> numA - numB
                "*" -> numA * numB
                "/" -> if (numB == 0.0) throw ArithmeticException("Division by zero") else numA / numB
                else -> throw IllegalArgumentException("Unsupported operator")
            }

            mapper.writeValueAsString(mapOf("result" to result))
        } catch (e: Exception) {
            mapper.writeValueAsString(mapOf("error" to e.message))
        }
    }
}
