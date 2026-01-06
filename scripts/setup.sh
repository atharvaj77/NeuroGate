#!/bin/bash

# NeuroGate Setup Script
# Downloads the Gradle wrapper JAR and starts infrastructure services

set -e

echo "🚀 NeuroGate Setup Script"
echo "=========================="

# Check if Java 21 is installed
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 21 (Temurin JDK recommended)"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "❌ Java 21 or higher is required. Found Java $JAVA_VERSION"
    exit 1
fi

echo "✅ Java $JAVA_VERSION detected"

# Check if Docker is running
if ! docker info &> /dev/null; then
    echo "❌ Docker is not running. Please start Docker Desktop"
    exit 1
fi

echo "✅ Docker is running"

# Download Gradle wrapper JAR if not exists
if [ ! -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    echo "📦 Downloading Gradle wrapper JAR..."
    mkdir -p gradle/wrapper
    curl -L -o gradle/wrapper/gradle-wrapper.jar \
        https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar
    echo "✅ Gradle wrapper JAR downloaded"
else
    echo "✅ Gradle wrapper JAR exists"
fi

# Start Docker Compose services
echo "🐳 Starting infrastructure services (Qdrant, Redis, Ollama, Prometheus, Grafana)..."
docker-compose up -d

echo ""
echo "✅ Infrastructure services started!"
echo ""
echo "📊 Service URLs:"
echo "   - Qdrant:     http://localhost:6333"
echo "   - Redis:      localhost:6379"
echo "   - Ollama:     http://localhost:11434"
echo "   - Prometheus: http://localhost:9090"
echo "   - Grafana:    http://localhost:3000 (admin/neurogate2024)"
echo ""
echo "⚙️  Next steps:"
echo "   1. Set your OpenAI API key:"
echo "      export OPENAI_API_KEY='sk-your-key-here'"
echo ""
echo "   2. Build and run the application:"
echo "      ./gradlew bootRun"
echo ""
echo "   3. Test the API:"
echo "      curl -X POST http://localhost:8080/v1/chat/completions \\"
echo "        -H 'Content-Type: application/json' \\"
echo "        -d '{\"model\": \"gpt-3.5-turbo\", \"messages\": [{\"role\": \"user\", \"content\": \"What is Java?\"}]}'"
echo ""
echo "🎉 Setup complete! Happy coding!"
