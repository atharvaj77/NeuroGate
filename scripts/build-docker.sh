#!/bin/bash
# Build and push Docker image for NeuroGate

set -e

# Configuration
IMAGE_NAME="${IMAGE_NAME:-neurogate}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
REGISTRY="${REGISTRY:-}"
PLATFORM="${PLATFORM:-linux/amd64,linux/arm64}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."

    if ! command -v docker &> /dev/null; then
        log_error "Docker not found. Please install Docker first."
        exit 1
    fi

    if ! command -v ./gradlew &> /dev/null; then
        log_error "Gradle wrapper not found. Run from project root."
        exit 1
    fi

    log_info "✓ Prerequisites check passed"
}

# Build JAR
build_jar() {
    log_info "Building JAR with Gradle..."
    ./gradlew clean bootJar --no-daemon

    if [ ! -f build/libs/neurogate-*.jar ]; then
        log_error "JAR file not found after build"
        exit 1
    fi

    log_info "✓ JAR built successfully"
}

# Build Docker image
build_image() {
    log_info "Building Docker image..."

    # Construct full image name
    if [ -n "$REGISTRY" ]; then
        FULL_IMAGE_NAME="${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
    else
        FULL_IMAGE_NAME="${IMAGE_NAME}:${IMAGE_TAG}"
    fi

    log_info "Image name: $FULL_IMAGE_NAME"

    # Build multi-platform image
    docker buildx build \
        --platform "${PLATFORM}" \
        -t "${FULL_IMAGE_NAME}" \
        -t "${IMAGE_NAME}:latest" \
        --build-arg BUILD_DATE="$(date -u +'%Y-%m-%dT%H:%M:%SZ')" \
        --build-arg VCS_REF="$(git rev-parse --short HEAD)" \
        --build-arg VERSION="${IMAGE_TAG}" \
        -f Dockerfile \
        .

    log_info "✓ Docker image built successfully"
    echo "Image: $FULL_IMAGE_NAME"
}

# Push Docker image
push_image() {
    if [ -z "$REGISTRY" ]; then
        log_warn "No registry specified. Skipping push."
        return
    fi

    log_info "Pushing Docker image to registry..."

    FULL_IMAGE_NAME="${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"

    docker push "${FULL_IMAGE_NAME}"

    log_info "✓ Docker image pushed successfully"
}

# Main
main() {
    log_info "Starting Docker build process..."

    check_prerequisites
    build_jar
    build_image

    # Push only if --push flag is provided
    if [[ "$*" == *"--push"* ]]; then
        push_image
    else
        log_info "Skipping push (use --push to push to registry)"
    fi

    log_info "✓ Build process completed successfully!"
}

# Run main
main "$@"
