# Multi-stage Dockerfile for NeuroGate
# Stage 1: Build (optional, can use pre-built JAR)
# Stage 2: Runtime

FROM eclipse-temurin:21-jre AS runtime

# Metadata
LABEL maintainer="Atharva Joshi <your.email@example.com>"
LABEL org.opencontainers.image.title="NeuroGate"
LABEL org.opencontainers.image.description="Production-ready AI Gateway reducing LLM costs by 40-60%"
LABEL org.opencontainers.image.version="1.0.0"
LABEL org.opencontainers.image.source="https://github.com/yourusername/neurogate"

# Build arguments
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION

# Additional metadata
LABEL org.opencontainers.image.created="${BUILD_DATE}"
LABEL org.opencontainers.image.revision="${VCS_REF}"

# Create app directory
WORKDIR /app

# Create non-root user
RUN groupadd -r neurogate && \
    useradd -r -g neurogate neurogate && \
    chown -R neurogate:neurogate /app

# Install curl for health checks (already available in temurin base image)

# Copy JAR file
COPY --chown=neurogate:neurogate build/libs/neurogate-*.jar app.jar

# Switch to non-root user
USER neurogate

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM Options
ENV JAVA_OPTS="-XX:+UseZGC \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0 \
               -XX:+AlwaysPreTouch \
               -XX:+UseStringDeduplication \
               -Djava.security.egd=file:/dev/./urandom"

# Run application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
