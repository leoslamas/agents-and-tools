package com.example.tools

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass

abstract class TypedAgentTool<T : Any>(private val paramsClass: KClass<T>) : AgentTool() {

    protected val mapper = jacksonObjectMapper()

    override val parameters: Map<String, Any> by lazy { generateSchema(paramsClass) }

    protected fun parseArgs(json: String): T = mapper.readValue(json, paramsClass.java)
}
