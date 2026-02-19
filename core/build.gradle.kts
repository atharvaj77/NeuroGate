plugins {
    id("java-library")
    id("org.openapi.generator") version "7.4.0"
}

dependencies {
    // Spring Boot Starters
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.boot:spring-boot-starter-webflux")
    api("org.springframework.boot:spring-boot-starter-actuator")
    api("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-quartz")

    // Spring AI
    implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter")

    // Core Libs
    implementation("io.qdrant:client:1.9.1")
    implementation("redis.clients:jedis:5.1.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.apache.commons:commons-text:1.11.0")
    implementation("io.github.java-diff-utils:java-diff-utils:4.12")
    implementation("com.h2database:h2")

    // Cloud SDKs (Bedrock, Azure, Google)
    implementation("software.amazon.awssdk:bedrockruntime:2.23.0")
    implementation("software.amazon.awssdk:bedrock:2.23.0")
    implementation("software.amazon.awssdk:s3:2.23.0")
    implementation("com.azure:azure-ai-openai:1.0.0-beta.6")
    implementation("com.google.cloud:google-cloud-aiplatform:3.38.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")
    
    // JSON & Observability
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")

    // OpenTelemetry OTLP Exporter
    implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.36.0")
    implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:1.36.0")

    // SpringDoc OpenAPI (Swagger UI)
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    
    // JSON Schema Validation
    implementation("com.networknt:json-schema-validator:1.0.87")

    // Apache Commons Math for statistics (A/B testing)
    implementation("org.apache.commons:commons-math3:3.6.1")

    // Resilience4j
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("io.github.resilience4j:resilience4j-circuitbreaker:2.2.0")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:2.2.0")
    implementation("io.github.resilience4j:resilience4j-retry:2.2.0")
    implementation("io.github.resilience4j:resilience4j-reactor:2.2.0")
    implementation("io.github.resilience4j:resilience4j-bulkhead:2.2.0")
    implementation("io.github.resilience4j:resilience4j-timelimiter:2.2.0")
    implementation("io.github.resilience4j:resilience4j-micrometer:2.2.0")
    implementation("org.springframework.kafka:spring-kafka")

    // Test
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")

    runtimeOnly("io.netty:netty-resolver-dns-native-macos:4.1.115.Final:osx-aarch_64")
}

// Generate Python SDK for Data Scientists
openApiGenerate {
    generatorName.set("python")
    inputSpec.set("$projectDir/src/main/resources/openapi.yml") // We need to generate this first or use URL
    outputDir.set("$projectDir/clients/python")
    apiPackage.set("neurogate.api")
    modelPackage.set("neurogate.models")
    configOptions.set(mapOf(
        "packageName" to "neurogate",
        "projectName" to "neurogate-sdk",
        "packageVersion" to "0.1.0"
    ))
}

tasks.register("generateOpenApiSpec") {
    // In a real scenario, we might hit the actuator endpoint to get the spec
    // or use springdoc-openapi-gradle-plugin.
    // For now, we assume the spec exists or will be generated.
}
