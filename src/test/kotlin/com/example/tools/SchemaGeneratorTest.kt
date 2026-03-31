package com.example.tools

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

data class AllTypesParams(
    @ToolParam("A string field")
    val name: String,
    @ToolParam("An integer field")
    val count: Int,
    @ToolParam("A long field")
    val bigCount: Long,
    @ToolParam("A double field")
    val ratio: Double,
    @ToolParam("A float field")
    val score: Float,
    @ToolParam("A boolean field")
    val active: Boolean
)

data class NullableParams(
    @ToolParam("Required field")
    val required: String,
    @ToolParam("Optional field")
    val optional: String?
)

data class MissingAnnotationParams(
    val noAnnotation: String
)

class SchemaGeneratorTest {

    @Test
    fun `deve gerar schema para todos os tipos suportados`() {
        val schema = generateSchema(AllTypesParams::class)

        assertEquals("object", schema["type"])
        @Suppress("UNCHECKED_CAST")
        val properties = schema["properties"] as Map<String, Map<String, String>>

        assertEquals("string", properties["name"]!!["type"])
        assertEquals("integer", properties["count"]!!["type"])
        assertEquals("integer", properties["bigCount"]!!["type"])
        assertEquals("number", properties["ratio"]!!["type"])
        assertEquals("number", properties["score"]!!["type"])
        assertEquals("boolean", properties["active"]!!["type"])
    }

    @Test
    fun `deve incluir descricoes do ToolParam`() {
        val schema = generateSchema(AllTypesParams::class)

        @Suppress("UNCHECKED_CAST")
        val properties = schema["properties"] as Map<String, Map<String, String>>
        assertEquals("A string field", properties["name"]!!["description"])
    }

    @Test
    fun `deve marcar campos nao-nullable como required`() {
        val schema = generateSchema(NullableParams::class)

        @Suppress("UNCHECKED_CAST")
        val required = schema["required"] as List<String>
        assertTrue(required.contains("required"))
        assertTrue(!required.contains("optional"))
    }

    @Test
    fun `deve lancar erro quando property nao tem ToolParam`() {
        assertThrows(IllegalArgumentException::class.java) {
            generateSchema(MissingAnnotationParams::class)
        }
    }

    @Test
    fun `deve gerar schema vazio para classe sem properties`() {
        val schema = generateSchema(TimeParams::class)

        assertEquals("object", schema["type"])
        @Suppress("UNCHECKED_CAST")
        val properties = schema["properties"] as Map<String, Any>
        assertTrue(properties.isEmpty())
        @Suppress("UNCHECKED_CAST")
        val required = schema["required"] as List<String>
        assertTrue(required.isEmpty())
    }
}
