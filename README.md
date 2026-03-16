# Micronaut Kotlin Agents and Tools Demo

A Kotlin project using the Micronaut framework that demonstrates how to cleanly build and use AI Agents and Tools via the official OpenAI Java SDK.

## Overview

This project showcases a straightforward architecture to build conversational agents capable of calling deterministic functions ("tools").

The application features:
* **Micronaut Framework**: Fast startup, dependency injection, and HTTP server capabilities.
* **Coroutines**: Fully non-blocking concurrency for handling API calls.
* **OpenAI Java SDK v4**: Uses the official `com.openai:openai-java` SDK to leverage models with strict tool definitions.
* **Agents**: Custom encapsulated entities that share tools or have exclusive tools.
* **Tools**: Simple, interface-driven objects (`AgentTool`) mapped seamlessly to OpenAI's tool JSON schemas.

### Implemented Agents
1. **Math Agent**: Designed to answer questions about mathematics.
   * **Tools**: `CalculatorTool` (Math expression evaluation) and `TimeTool` (Current date/time).
2. **Weather Agent**: Designed to answer questions about the weather.
   * **Tools**: `WeatherTool` (Mock weather conditions) and `TimeTool` (Current date/time).

## Prerequisites
* **Java**: JDK 21+
* **Kotlin**: 1.9+
* **API Key**: You need an active OpenAI API Key.

## Getting Started

1. **Set your API Key**
   Export your OpenAI API key in your terminal session:
   ```bash
   export OPENAI_API_KEY="YOUR_API_KEY"
   ```

2. **Build the Application**
   Compile and build the project using the Gradle wrapper:
   ```bash
   ./gradlew build
   ```

3. **Run the Application**
   Start the Micronaut server locally (runs on port `8080` by default):
   ```bash
   ./gradlew run
   ```

4. **Test the Endpoints**

   **Math Agent**:
   ```bash
   curl "http://localhost:8080/agents/math?prompt=What+is+25+plus+45"
   ```

   **Weather Agent**:
   ```bash
   curl "http://localhost:8080/agents/weather?prompt=What+is+the+weather+in+London"
   ```

## Code Formatting

This project uses [ktlint](https://pinterest.github.io/ktlint/) to enforce Kotlin coding standards. You can run the following custom Gradle task to format your code:

* **Format Code**: Automatically corrects any formatting deviations.
  ```bash
  ./gradlew format
  ```

## Architecture Details

* `com.example.agents.AgentRunner`: A core component that manages the iterative lifecycle of an agent exchanging messages with the OpenAI API. It handles parsing the model's `tool_calls` request, finding the correct implementation locally, executing it, and sending the results back.
* `com.example.tools.AgentTool`: An interface implemented by all tools. It dictates the schema mapping expected by the OpenAI SDK and provides the standard `execute(arguments: String): String` function signature.
* `com.example.config.OpenAIConfig`: Injects the configured SDK client as a `@Singleton` bean.
