package com.example.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WeatherToolTest {

    private val weatherTool = WeatherTool()

    @Test
    fun testToolNameAndDescription() {
        assertEquals("get_weather", weatherTool.name)
        assertNotNull(weatherTool.description)
    }

    @Test
    fun testExecuteReturnsWeatherForCity() {
        val result = weatherTool.execute("{\"city\": \"London\"}")

        assertTrue(result.contains("\"city\":\"London\""), "Got: " + result)
        assertTrue(result.contains("\"temperature\""), "Got: " + result)
        assertTrue(result.contains("\"conditions\""), "Got: " + result)
        assertTrue(result.contains("\"unit\":\"Celsius\""), "Got: " + result)
    }

    @Test
    fun testSameCityReturnsDeterministicResult() {
        val result1 = weatherTool.execute("{\"city\": \"Paris\"}")
        val result2 = weatherTool.execute("{\"city\": \"Paris\"}")
        assertEquals(result1, result2)
    }

    @Test
    fun testDifferentCitiesCanReturnDifferentResults() {
        val london = weatherTool.execute("{\"city\": \"London\"}")
        val tokyo = weatherTool.execute("{\"city\": \"Tokyo\"}")
        assertNotEquals(london, tokyo)
    }

    @Test
    fun testMissingCityReturnsError() {
        val result = weatherTool.execute("{}")
        assertTrue(result.contains("\"error\""))
    }

    @Test
    fun testToolDefinitionIsValid() {
        val toolDef = weatherTool.toToolDefinition()
        assertNotNull(toolDef)
    }
}
