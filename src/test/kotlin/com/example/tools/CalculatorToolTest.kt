package com.example.tools

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class CalculatorToolTest {

    private val calculatorTool = CalculatorTool()
    private val mapper = jacksonObjectMapper()

    @Test
    fun `deve retornar nome e descricao corretos`() {
        assertEquals("calculate", calculatorTool.name)
        assertNotNull(calculatorTool.description)
    }

    @Test
    fun `deve calcular soma corretamente`() {
        val result = calculatorTool.execute("""{"expression": "10 + 20"}""")
        val json = mapper.readTree(result)
        assertEquals("30.0", json.get("result").asText())
    }

    @Test
    fun `deve calcular subtracao corretamente`() {
        val result = calculatorTool.execute("""{"expression": "50 - 15"}""")
        val json = mapper.readTree(result)
        assertEquals("35.0", json.get("result").asText())
    }

    @Test
    fun `deve calcular multiplicacao corretamente`() {
        val result = calculatorTool.execute("""{"expression": "-5 * 4.5"}""")
        val json = mapper.readTree(result)
        assertEquals("-22.5", json.get("result").asText())
    }

    @Test
    fun `deve calcular divisao corretamente`() {
        val result = calculatorTool.execute("""{"expression": "100 / 4"}""")
        val json = mapper.readTree(result)
        assertEquals("25.0", json.get("result").asText())
    }

    @Test
    fun `deve retornar Infinity quando dividir por zero`() {
        val result = calculatorTool.execute("""{"expression": "10 / 0"}""")
        val json = mapper.readTree(result)
        assertEquals("Infinity", json.get("result").asText())
    }

    @Test
    fun `deve calcular numeros decimais corretamente`() {
        val result = calculatorTool.execute("""{"expression": "3.5 + 2.5"}""")
        val json = mapper.readTree(result)
        assertEquals("6.0", json.get("result").asText())
    }

    @Test
    fun `deve retornar erro quando expressao esta ausente`() {
        val result = calculatorTool.execute("{}")
        val json = mapper.readTree(result)
        assertEquals("Missing expression", json.get("error").asText())
    }

    @Test
    fun `deve retornar erro quando expressao e invalida`() {
        val result = calculatorTool.execute("""{"expression": "10 ^ 2"}""")
        val json = mapper.readTree(result)
        assertNotNull(json.get("error"))
    }

    @Test
    fun `deve gerar definicao de ferramenta valida`() {
        val toolDef = calculatorTool.toToolDefinition()
        assertNotNull(toolDef)
    }
}
