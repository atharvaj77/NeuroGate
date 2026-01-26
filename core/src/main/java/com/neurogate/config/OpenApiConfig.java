package com.neurogate.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for NeuroGate API documentation.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "NeuroGate API",
        version = "1.0.0",
        description = """
            Enterprise AI Gateway API providing intelligent LLM routing, PII protection,
            semantic caching, and multi-provider orchestration.

            ## Features
            - **Multi-Provider Routing**: Intelligent routing across OpenAI, Anthropic, Google, AWS Bedrock, and Azure
            - **PII Protection**: Automatic detection and redaction of sensitive data
            - **Semantic Caching**: 4-tier caching with semantic similarity matching
            - **Cost Tracking**: Real-time usage and cost analytics
            - **Agent Operations**: Trace and monitor AI agent workflows
            """,
        contact = @Contact(
            name = "NeuroGate Team",
            url = "https://neurogate.dev"
        ),
        license = @License(
            name = "Apache 2.0",
            url = "https://www.apache.org/licenses/LICENSE-2.0"
        )
    ),
    servers = {
        @Server(url = "http://localhost:8080", description = "Local Development"),
        @Server(url = "https://api.neurogate.dev", description = "Production")
    },
    tags = {
        @Tag(name = "Chat", description = "OpenAI-compatible chat completions API"),
        @Tag(name = "Models", description = "Model information and pricing"),
        @Tag(name = "Analytics", description = "Usage analytics and cost tracking"),
        @Tag(name = "NeuroGuard", description = "PII detection and protection"),
        @Tag(name = "RAG", description = "Retrieval-Augmented Generation"),
        @Tag(name = "Cortex", description = "Response evaluation engine"),
        @Tag(name = "Forge", description = "Fine-tuning and model distillation"),
        @Tag(name = "Reinforce", description = "Human-in-the-loop feedback"),
        @Tag(name = "Synapse", description = "Prompt optimization"),
        @Tag(name = "Consensus", description = "Multi-model consensus (Hive Mind)"),
        @Tag(name = "AgentOps", description = "Agent trace and monitoring"),
        @Tag(name = "Memory", description = "Agent memory management"),
        @Tag(name = "Flywheel", description = "Continuous improvement pipeline"),
        @Tag(name = "Debugger", description = "Session replay and debugging"),
        @Tag(name = "Prompts", description = "Prompt management and versioning")
    }
)
public class OpenApiConfig {
}