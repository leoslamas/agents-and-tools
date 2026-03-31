package com.example.tools

import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

fun <T : Any> generateSchema(kClass: KClass<T>): Map<String, Any> {
    val properties = mutableMapOf<String, Any>()
    val required = mutableListOf<String>()

    for (prop in kClass.memberProperties) {
        val annotation = prop.findAnnotation<ToolParam>()
            ?: prop.javaField?.getAnnotation(ToolParam::class.java)
            ?: throw IllegalArgumentException(
                "Property '${prop.name}' in ${kClass.simpleName} must be annotated with @ToolParam"
            )

        val jsonType = when (prop.returnType.classifier) {
            String::class -> "string"
            Int::class, Long::class -> "integer"
            Double::class, Float::class -> "number"
            Boolean::class -> "boolean"
            else -> throw IllegalArgumentException(
                "Unsupported type '${prop.returnType}' for property '${prop.name}' in ${kClass.simpleName}"
            )
        }

        properties[prop.name] = mapOf(
            "type" to jsonType,
            "description" to annotation.description
        )

        if (!prop.returnType.isMarkedNullable) {
            required.add(prop.name)
        }
    }

    return mapOf(
        "type" to "object",
        "properties" to properties,
        "required" to required
    )
}
