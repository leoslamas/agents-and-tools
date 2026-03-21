# Agents and Tools

A Kotlin project using Micronaut that demonstrates a two-layer agent architecture powered by any OpenAI-compatible LLM API.

## Overview

A single HTTP endpoint receives a user prompt. An orchestrator layer uses the LLM to decide which specialized agent should handle the request. Each agent runs its own tool-calling loop with domain-specific tools.

## Prerequisites

- JDK 21+

## Getting Started

```bash
./gradlew run
```

The server starts on port `8080` by default.

## Usage

```bash
curl -X POST http://localhost:8080/agent \
  -H "Content-Type: application/json" \
  -d '{"prompt": "What is 25 * 4?"}'
```

```bash
curl -X POST http://localhost:8080/agent \
  -H "Content-Type: application/json" \
  -d '{"prompt": "What is the weather in London?"}'
```

## Agents

| Agent | Description | Tools |
|-------|-------------|-------|
| **MathAgent** | Answers math-related questions | CalculatorTool, TimeTool |
| **WeatherAgent** | Answers weather-related questions | WeatherTool, TimeTool |
| **GenericAgent** | General conversation | None |

## Architecture

```
POST /agent
  -> AgentDemoController
    -> AgentOrchestrator (LLM picks the right agent)
      -> Agent.ask(prompt)
        -> AgentRunner (tool-calling loop, up to 10 iterations)
          -> AgentTool.execute(arguments)
```

### Key Components

- **`AgentOrchestrator`**: Presents all registered agents as function tools to the LLM. The LLM decides which agent to invoke based on the user prompt.
- **`Agent`**: Interface with `name`, `description`, and `ask(prompt)`. Each agent has its own system prompt and set of tools.
- **`AgentRunner`**: Executes the iterative tool-calling loop for a given agent -- sends messages, processes tool calls, feeds results back, up to 10 rounds.
- **`AgentTool`**: Interface for tools. Defines the JSON schema for the function and provides `execute(arguments): String`.
- **`OpenAIConfig`**: Singleton factory that creates the SDK client configured via `llm.*` properties.

## Development

```bash
./gradlew format    # ktlint formatting
./gradlew test      # run tests
./gradlew build     # compile and build
```
