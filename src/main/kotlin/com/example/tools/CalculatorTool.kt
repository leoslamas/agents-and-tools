package com.example.tools

import jakarta.inject.Singleton

data class CalculatorParams(
    @ToolParam("The mathematical expression to evaluate (single binary operation, e.g. 2 + 2, 5 * 10).")
    val expression: String
)

@Singleton
class CalculatorTool : TypedAgentTool<CalculatorParams>(CalculatorParams::class) {
    override val name = "calculate"
    override val description = "Evaluates a single binary math expression with two operands and one operator (e.g. 2 + 2, 5 * 10)."

    override fun execute(arguments: String): String {
        return try {
            val params = parseArgs(arguments)
            val expression = params.expression

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
