package com.neurogate.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.tomcat.TomcatProtocolHandlerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Configuration for Java 21 Virtual Threads (Project Loom).
 * Gracefully degrades to regular thread pool on Java 17.
 */
@Slf4j
@Configuration
public class VirtualThreadConfig {

    /**
     * Configures Tomcat to use Virtual Threads for request handling (Java 21+).
     * Falls back to default thread pool on Java 17.
     *
     * @return TomcatProtocolHandlerCustomizer that sets up virtual thread executor
     */
    @Bean
    public TomcatProtocolHandlerCustomizer<?> protocolHandlerVirtualThreadExecutorCustomizer() {
        return protocolHandler -> {
            try {
                // Try to create virtual thread executor using reflection (Java 21+)
                Method newVirtualThreadPerTaskExecutor = Executors.class.getMethod("newVirtualThreadPerTaskExecutor");
                Executor executor = (Executor) newVirtualThreadPerTaskExecutor.invoke(null);
                protocolHandler.setExecutor(executor);
                log.info("✅ Virtual Threads (Java 21+) enabled for Tomcat");
            } catch (NoSuchMethodException e) {
                // Virtual threads not available (Java < 21)
                log.warn("⚠️  Virtual Threads not available (requires Java 21+). Using default thread pool.");
                log.warn("⚠️  For production deployment, upgrade to Java 21 for 10x better concurrency");
            } catch (Exception e) {
                log.error("Failed to initialize Virtual Threads", e);
            }
        };
    }
}
