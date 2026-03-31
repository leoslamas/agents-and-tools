# Guia de Refatoracao - Agents & Tools

Este documento descreve todas as refatoracoes aplicadas ao projeto, na ordem correta de execucao.
Pode ser usado como roteiro para replicar as mesmas mudancas em um projeto identico.

---

## Pre-requisitos

Certifique-se de que `kotlin-reflect` esta no `build.gradle.kts`:

```kotlin
implementation("org.jetbrains.kotlin:kotlin-reflect")
```

---

## Parte 1 - Tools: Schema automatico via annotations

### Problema

Cada `AgentTool` definia `parameters: Map<String, Any>` manualmente, duplicando a descricao dos campos em mapas verbosos e propensos a erro.

### Solucao

Usar data classes anotadas + reflexao para gerar o JSON Schema automaticamente.

### Passo 1.1 - Criar annotation `@ToolParam`

Novo arquivo: `src/main/kotlin/com/example/tools/ToolParam.kt`

```kotlin
package com.example.tools

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ToolParam(val description: String)
```

### Passo 1.2 - Criar `generateSchema()`

Novo arquivo: `src/main/kotlin/com/example/tools/SchemaGenerator.kt`

```kotlin
package com.example.tools

import kotlin.reflect.KClass
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.findAnnotation
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
```

Regras:
- Toda property da data class **deve** ter `@ToolParam`, senao lanca erro
- Tipos suportados: `String`, `Int`, `Long`, `Double`, `Float`, `Boolean`
- Properties nullable (`String?`) nao entram no `required`

### Passo 1.3 - Criar `TypedAgentTool<T>`

Novo arquivo: `src/main/kotlin/com/example/tools/TypedAgentTool.kt`

```kotlin
package com.example.tools

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.reflect.KClass

abstract class TypedAgentTool<T : Any>(private val paramsClass: KClass<T>) : AgentTool() {

    protected val mapper = jacksonObjectMapper()

    override val parameters: Map<String, Any> by lazy { generateSchema(paramsClass) }

    protected fun parseArgs(json: String): T = mapper.readValue(json, paramsClass.java)
}
```

Pontos importantes:
- Estende `AgentTool` (nao-generico), mantendo compatibilidade com `AgentRunner` que usa `List<AgentTool>`
- `mapper` e `protected` para que as subclasses reutilizem (evita instancias duplicadas)
- `parameters` e gerado via `lazy` a partir da data class

### Passo 1.4 - Extrair `buildToolDefinition` no `AgentTool`

Alterar `AgentTool` para incluir um `companion object` com metodo estatico reutilizavel:

```kotlin
package com.example.tools

import com.openai.core.JsonValue
import com.openai.models.FunctionDefinition
import com.openai.models.chat.completions.ChatCompletionFunctionTool
import com.openai.models.chat.completions.ChatCompletionTool

abstract class AgentTool {
    abstract val name: String
    abstract val description: String
    abstract val parameters: Map<String, Any>

    fun toToolDefinition(): ChatCompletionTool {
        val strictParameters = parameters + ("additionalProperties" to false)
        return buildToolDefinition(name, description, strictParameters, strict = true)
    }

    companion object {
        fun buildToolDefinition(
            name: String,
            description: String,
            parameters: Map<String, Any>,
            strict: Boolean = true
        ): ChatCompletionTool {
            val finalParams = if ("additionalProperties" !in parameters) {
                parameters + ("additionalProperties" to false)
            } else {
                parameters
            }

            return ChatCompletionTool.ofFunction(
                ChatCompletionFunctionTool.builder()
                    .function(
                        FunctionDefinition.builder()
                            .name(name)
                            .description(description)
                            .parameters(JsonValue.from(finalParams))
                            .strict(strict)
                            .build()
                    )
                    .build()
            )
        }
    }

    abstract fun execute(arguments: String): String
}
```

Isso permite que `AgentOrchestrator` reutilize a construcao de tool definitions sem duplicar codigo.

### Passo 1.5 - Refatorar tools existentes

Para cada tool: criar uma `data class` com `@ToolParam`, estender `TypedAgentTool`, usar `parseArgs()` e `mapper`.

**Antes (exemplo CalculatorTool):**

```kotlin
@Singleton
class CalculatorTool : AgentTool() {
    override val name = "calculate"
    override val description = "..."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "expression" to mapOf(
                "type" to "string",
                "description" to "The mathematical expression to evaluate."
            )
        ),
        "required" to listOf("expression")
    )
    private val mapper = jacksonObjectMapper()

    override fun execute(arguments: String): String {
        val json = mapper.readTree(arguments)
        val expression = json.get("expression")?.asText() ?: throw ...
        // ...
    }
}
```

**Depois:**

```kotlin
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
            // ... logica igual
            mapper.writeValueAsString(mapOf("result" to result))
        } catch (e: Exception) {
            mapper.writeValueAsString(mapOf("error" to e.message))
        }
    }
}
```

Aplicar o mesmo padrao para `WeatherTool` e `TimeTool`.

**Tool sem parametros (TimeTool):**

```kotlin
class TimeParams  // classe vazia, sem properties

@Singleton
class TimeTool : TypedAgentTool<TimeParams>(TimeParams::class) {
    override val name = "get_current_time"
    override val description = "Returns the current date and time."

    override fun execute(arguments: String): String {
        val now = ZonedDateTime.now()
        val formatted = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        return mapper.writeValueAsString(mapOf("currentTime" to formatted))
    }
}
```

### Template para novas tools

```kotlin
data class MyParams(
    @ToolParam("Descricao do campo")
    val fieldName: String,
    @ToolParam("Outro campo")
    val otherField: Int
)

@Singleton
class MyTool : TypedAgentTool<MyParams>(MyParams::class) {
    override val name = "my_tool"
    override val description = "O que essa tool faz."

    override fun execute(arguments: String): String {
        val params = parseArgs(arguments)
        // usar params.fieldName, params.otherField
        return mapper.writeValueAsString(mapOf("result" to "..."))
    }
}
```

---

## Parte 2 - Agents: BaseAgent declarativo

### Problema

Todos os agents implementavam `ask()` de forma identica — delegando para `agentRunner.run()`. A diferenca entre eles era apenas configuracao (name, description, systemPrompt, tools).

### Solucao

Criar `BaseAgent` que centraliza a implementacao de `ask()`.

### Passo 2.1 - Criar `BaseAgent`

Novo arquivo: `src/main/kotlin/com/example/agents/BaseAgent.kt`

```kotlin
package com.example.agents

import com.example.tools.AgentTool

abstract class BaseAgent(
    private val agentRunner: AgentRunner
) : Agent {
    abstract val systemPrompt: String
    open val tools: List<AgentTool> = emptyList()

    override fun ask(prompt: String): String {
        return agentRunner.run(prompt, systemPrompt, tools)
    }
}
```

### Passo 2.2 - Refatorar agents existentes

**Antes (exemplo MathAgent):**

```kotlin
@Singleton
class MathAgent(
    private val agentRunner: AgentRunner,
    private val timeTool: TimeTool,
    private val calculatorTool: CalculatorTool
) : Agent {
    override val name = "math_agent"
    override val description = "..."

    private val systemPrompt = "..."
    private val tools = listOf(timeTool, calculatorTool)

    override fun ask(prompt: String): String {
        return agentRunner.run(prompt, systemPrompt, tools)
    }
}
```

**Depois:**

```kotlin
@Singleton
class MathAgent(
    agentRunner: AgentRunner,
    private val timeTool: TimeTool,
    private val calculatorTool: CalculatorTool
) : BaseAgent(agentRunner) {

    override val name = "math_agent"
    override val description = "Specialized in mathematics and time. Can evaluate math expressions and tell the current time."

    override val systemPrompt = """
        You are MathAgent, a helpful assistant specialized in mathematics and time.
        You have tools to get the current time and evaluate basic math expressions.
        Always answer concisely.
    """.trimIndent()

    override val tools: List<AgentTool> = listOf(timeTool, calculatorTool)
}
```

Agents sem tools (como `GenericAgent`) nao precisam declarar `tools` — o default e `emptyList()`.

### Template para novos agents

```kotlin
@Singleton
class MyAgent(
    agentRunner: AgentRunner,
    private val myTool: MyTool
) : BaseAgent(agentRunner) {

    override val name = "my_agent"
    override val description = "O que esse agent faz."

    override val systemPrompt = """
        You are MyAgent. ...
    """.trimIndent()

    override val tools: List<AgentTool> = listOf(myTool)
}
```

---

## Parte 3 - AgentOrchestrator: reutilizar `buildToolDefinition`

### Problema

O `AgentOrchestrator` construia `ChatCompletionTool` manualmente para representar agents como tools, duplicando a logica de `AgentTool.toToolDefinition()`.

### Solucao

Usar `AgentTool.buildToolDefinition()` (metodo estatico adicionado no Passo 1.4):

```kotlin
private val agentToolDefinitions by lazy {
    agents.map { agent ->
        AgentTool.buildToolDefinition(
            name = agent.name,
            description = agent.description,
            parameters = mapOf(
                "type" to "object",
                "properties" to mapOf(
                    "query" to mapOf(
                        "type" to "string",
                        "description" to "The user's question or request to forward to this agent."
                    )
                ),
                "required" to listOf("query")
            )
        )
    }
}
```

---

## Parte 4 - Melhorias gerais

### 4.1 - Constante para max iterations no AgentRunner

**Antes:** `while (iteration < 10)`

**Depois:**

```kotlin
companion object {
    private const val MAX_ITERATIONS = 10
}
// ...
while (iteration < MAX_ITERATIONS)
```

### 4.2 - Controller com respostas tipadas e validacao

**Antes:** retornava `String` raw com HTTP 200 em todos os casos.

**Depois:** usa `HttpResponse`, DTOs tipados e valida input:

```kotlin
@Serdeable
data class AgentRequest(val prompt: String)

@Serdeable
data class AgentResponse(val response: String)

@Serdeable
data class ErrorResponse(val error: String)

@Controller("/agent")
class AgentDemoController(private val orchestrator: AgentOrchestrator) {

    @Post
    @ExecuteOn(TaskExecutors.BLOCKING)
    fun ask(@Body request: AgentRequest): HttpResponse<*> {
        if (request.prompt.isBlank()) {
            return HttpResponse.badRequest(ErrorResponse("Prompt must not be blank"))
        }

        return try {
            HttpResponse.ok(AgentResponse(orchestrator.route(request.prompt)))
        } catch (e: Exception) {
            HttpResponse.serverError(ErrorResponse(e.message ?: "Internal error"))
        }
    }
}
```

A resposta agora e JSON tipado:
- Sucesso: `{"response": "..."}` com HTTP 200
- Input invalido: `{"error": "Prompt must not be blank"}` com HTTP 400
- Erro interno: `{"error": "..."}` com HTTP 500

---

## Hierarquia final

```
AgentTool (abstract)
  +-- buildToolDefinition() [companion/static]
  +-- toToolDefinition()
  |
  +-- TypedAgentTool<T> (abstract, generic)
        +-- generateSchema() via @ToolParam
        +-- parseArgs() / mapper
        |
        +-- CalculatorTool (CalculatorParams)
        +-- WeatherTool (WeatherParams)
        +-- TimeTool (TimeParams)

Agent (interface)
  |
  +-- BaseAgent (abstract)
        +-- ask() -> agentRunner.run()
        |
        +-- MathAgent (tools: calculator, time)
        +-- WeatherAgent (tools: weather, time)
        +-- GenericAgent (sem tools)
        +-- SpecialAgent (sem tools)

AgentOrchestrator
  +-- usa AgentTool.buildToolDefinition() para expor agents como tools
  +-- roteia para o agent correto via LLM
```

---

## Checklist de validacao

Apos aplicar todas as mudancas, verifique:

- [ ] `./gradlew test` passa com todos os testes
- [ ] Nenhuma tool define `parameters` como `Map<String, Any>` manualmente
- [ ] Nenhum agent implementa `ask()` diretamente
- [ ] Nao ha instancias duplicadas de `jacksonObjectMapper()` nas tools
- [ ] `AgentOrchestrator` usa `AgentTool.buildToolDefinition()` ao inves de construir `ChatCompletionTool` na mao
- [ ] Controller retorna `HttpResponse` com status codes corretos
- [ ] `AgentRunner` usa constante `MAX_ITERATIONS` ao inves de magic number
