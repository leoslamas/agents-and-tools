package com.example.tools

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WeatherToolTest {

    private val weatherTool = WeatherTool()
    private val mapper = jacksonObjectMapper()

    @Test
    fun `deve retornar nome e descricao corretos`() {
        assertEquals("get_weather", weatherTool.name)
        assertNotNull(weatherTool.description)
    }

    @Test
    fun `deve retornar clima para uma cidade valida`() {
        val result = weatherTool.execute("""{"city": "London"}""")
        val json = mapper.readTree(result)

        assertEquals("London", json.get("city").asText())
        assertNotNull(json.get("temperature"))
        assertTrue(json.get("temperature").isInt)
        assertNotNull(json.get("conditions"))
        assertEquals("Celsius", json.get("unit").asText())
    }

    @Test
    fun `deve retornar temperatura entre 10 e 39`() {
        val result = weatherTool.execute("""{"city": "London"}""")
        val json = mapper.readTree(result)
        val temp = json.get("temperature").asInt()

        assertTrue(temp in 10..39, "Temperature should be between 10 and 39, got: $temp")
    }

    @Test
    fun `deve retornar condicao climatica valida`() {
        val result = weatherTool.execute("""{"city": "London"}""")
        val json = mapper.readTree(result)
        val conditions = json.get("conditions").asText()
        val validConditions = listOf("Sunny", "Cloudy", "Rainy", "Windy")

        assertTrue(conditions in validConditions, "Conditions should be one of $validConditions, got: $conditions")
    }

    @Test
    fun `deve retornar resultado deterministico para mesma cidade`() {
        val result1 = weatherTool.execute("""{"city": "Paris"}""")
        val result2 = weatherTool.execute("""{"city": "Paris"}""")
        assertEquals(result1, result2)
    }

    @Test
    fun `deve retornar resultados diferentes para cidades diferentes`() {
        val london = weatherTool.execute("""{"city": "London"}""")
        val tokyo = weatherTool.execute("""{"city": "Tokyo"}""")
        assertNotEquals(london, tokyo)
    }

    @Test
    fun `deve retornar erro quando cidade esta ausente`() {
        val result = weatherTool.execute("{}")
        val json = mapper.readTree(result)
        assertNotNull(json.get("error"))
    }

    @Test
    fun `deve retornar erro quando JSON e invalido`() {
        val result = weatherTool.execute("not json")
        val json = mapper.readTree(result)
        assertNotNull(json.get("error"))
    }

    @Test
    fun `deve gerar definicao de ferramenta valida`() {
        val toolDef = weatherTool.toToolDefinition()
        assertNotNull(toolDef)
    }

    @Test
    fun `deve gerar schema automaticamente a partir da data class`() {
        val params = weatherTool.parameters
        assertEquals("object", params["type"])
        @Suppress("UNCHECKED_CAST")
        val properties = params["properties"] as Map<String, Any>
        assertNotNull(properties["city"])
        @Suppress("UNCHECKED_CAST")
        val required = params["required"] as List<String>
        assertEquals(listOf("city"), required)
    }
}
