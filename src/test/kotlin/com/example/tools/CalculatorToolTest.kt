package com.example.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CalculatorToolTest {

    private val calculatorTool = CalculatorTool()

    @Test
    fun testToolNameAndDescription() {
        assertEquals("calculate", calculatorTool.name)
        assertNotNull(calculatorTool.description)
    }

    @Test
    fun testValidAddition() {
        val result = calculatorTool.execute("{\"expression\": \"10 + 20\"}")
        assertTrue(result.contains("\"result\":\"30.0\""), "Result should be 30.0, got: " + result)
    }

    @Test
    fun testValidSubtraction() {
        val result = calculatorTool.execute("{\"expression\": \"50 - 15\"}")
        assertTrue(result.contains("\"result\":\"35.0\""), "Result should be 35.0, got: " + result)
    }

    @Test
    fun testValidMultiplication() {
        val result = calculatorTool.execute("{\"expression\": \"-5 * 4.5\"}")
        assertTrue(result.contains("\"result\":\"-22.5\""), "Result should be -22.5, got: " + result)
    }

    @Test
    fun testValidDivision() {
        val result = calculatorTool.execute("{\"expression\": \"100 / 4\"}")
        assertTrue(result.contains("\"result\":\"25.0\""), "Result should be 25.0, got: " + result)
    }

    @Test
    fun testDivisionByZero() {
        val result = calculatorTool.execute("{\"expression\": \"10 / 0\"}")
        assertTrue(result.contains("\"result\":\"Infinity\""), "Got: " + result)
    }

    @Test
    fun testDecimalNumbers() {
        val result = calculatorTool.execute("{\"expression\": \"3.5 + 2.5\"}")
        assertTrue(result.contains("\"result\":\"6.0\""), "Got: " + result)
    }

    @Test
    fun testMissingExpression() {
        val result = calculatorTool.execute("{}")
        assertTrue(result.contains("\"error\":\"Missing expression\""), "Got: " + result)
    }

    @Test
    fun testInvalidExpression() {
        val result = calculatorTool.execute("{\"expression\": \"10 ^ 2\"}")
        assertTrue(result.contains("\"error\""), "Got: " + result)
    }

    @Test
    fun testToolDefinitionIsValid() {
        val toolDef = calculatorTool.toToolDefinition()
        assertNotNull(toolDef)
    }
}
