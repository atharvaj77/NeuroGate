#!/bin/bash
# Deploy NeuroGate to Kubernetes

set -e

# Configuration
NAMESPACE="${NAMESPACE:-neurogate}"
ENVIRONMENT="${ENVIRONMENT:-prod}"
KUBECONFIG="${KUBECONFIG:-$HOME/.kube/config}"

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

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

    if ! command -v kubectl &> /dev/null; then
        log_error "kubectl not found. Please install kubectl first."
        exit 1
    fi

    if ! command -v helm &> /dev/null; then
        log_error "helm not found. Please install Helm 3 first."
        exit 1
    fi

    # Test cluster connectivity
    if ! kubectl cluster-info &> /dev/null; then
        log_error "Cannot connect to Kubernetes cluster"
        exit 1
    fi

    log_info "✓ Prerequisites check passed"
}

# Check for required secrets
check_secrets() {
    log_info "Checking required environment variables..."

    if [ -z "$OPENAI_API_KEY" ]; then
        log_error "OPENAI_API_KEY environment variable not set"
        log_error "Export it: export OPENAI_API_KEY=sk-your-key"
        exit 1
    fi

    log_info "✓ Required secrets available"
}

# Create namespace
create_namespace() {
    log_info "Creating namespace: $NAMESPACE"

    kubectl create namespace "$NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -

    log_info "✓ Namespace created/updated"
}

# Create secrets
create_secrets() {
    log_info "Creating secrets..."

    kubectl create secret generic neurogate-secrets \
        --from-literal=openai-api-key="$OPENAI_API_KEY" \
        ${ANTHROPIC_API_KEY:+--from-literal=anthropic-api-key="$ANTHROPIC_API_KEY"} \
        ${REDIS_PASSWORD:+--from-literal=redis-password="$REDIS_PASSWORD"} \
        --namespace="$NAMESPACE" \
        --dry-run=client -o yaml | kubectl apply -f -

    log_info "✓ Secrets created/updated"
}

# Deploy using Helm
deploy_helm() {
    log_info "Deploying NeuroGate using Helm..."

    local VALUES_FILE="helm/neurogate/values-${ENVIRONMENT}.yaml"

    if [ ! -f "$VALUES_FILE" ]; then
        log_warn "Values file $VALUES_FILE not found, using default values"
        VALUES_FILE="helm/neurogate/values.yaml"
    fi

    helm upgrade neurogate helm/neurogate \
        --install \
        --namespace "$NAMESPACE" \
        --values "$VALUES_FILE" \
        --set secrets.create=false \
        --set secrets.existingSecret=neurogate-secrets \
        --wait \
        --timeout 10m \
        --debug

    log_info "✓ Helm deployment completed"
}

# Deploy using Kustomize (alternative)
deploy_kustomize() {
    log_info "Deploying NeuroGate using Kustomize..."

    kubectl apply -k "k8s/overlays/${ENVIRONMENT}" --namespace="$NAMESPACE"

    log_info "✓ Kustomize deployment completed"
}

# Wait for deployment
wait_for_deployment() {
    log_info "Waiting for deployment to be ready..."

    kubectl wait --for=condition=available \
        --timeout=300s \
        deployment/neurogate \
        --namespace="$NAMESPACE"

    log_info "✓ Deployment is ready"
}

# Verify deployment
verify_deployment() {
    log_info "Verifying deployment..."

    # Check pods
    log_info "Pods status:"
    kubectl get pods -n "$NAMESPACE" -l app=neurogate

    # Check service
    log_info "Service status:"
    kubectl get svc -n "$NAMESPACE" -l app=neurogate

    # Check ingress
    log_info "Ingress status:"
    kubectl get ingress -n "$NAMESPACE"

    # Test health endpoint
    log_info "Testing health endpoint..."
    POD=$(kubectl get pods -n "$NAMESPACE" -l app=neurogate -o jsonpath='{.items[0].metadata.name}')
    kubectl exec -n "$NAMESPACE" "$POD" -- wget -qO- http://localhost:8080/actuator/health || true

    log_info "✓ Deployment verification completed"
}

# Show access info
show_access_info() {
    log_info "=== Deployment Complete ==="

    echo ""
    echo "Access Information:"
    echo "===================="

    # Get ingress URL
    INGRESS_HOST=$(kubectl get ingress -n "$NAMESPACE" neurogate -o jsonpath='{.spec.rules[0].host}' 2>/dev/null || echo "Not configured")
    echo "API URL: https://$INGRESS_HOST/v1"

    # Get service IP (for LoadBalancer)
    SERVICE_IP=$(kubectl get svc -n "$NAMESPACE" neurogate -o jsonpath='{.status.loadBalancer.ingress[0].ip}' 2>/dev/null || echo "")
    if [ -n "$SERVICE_IP" ]; then
        echo "LoadBalancer IP: $SERVICE_IP"
    fi

    echo ""
    echo "Useful commands:"
    echo "================"
    echo "View logs:       kubectl logs -f -l app=neurogate -n $NAMESPACE"
    echo "View pods:       kubectl get pods -n $NAMESPACE"
    echo "View events:     kubectl get events -n $NAMESPACE --sort-by='.lastTimestamp'"
    echo "Port forward:    kubectl port-forward -n $NAMESPACE svc/neurogate 8080:80"
    echo "Execute shell:   kubectl exec -it -n $NAMESPACE \$(kubectl get pod -n $NAMESPACE -l app=neurogate -o jsonpath='{.items[0].metadata.name}') -- /bin/sh"
    echo ""
}

# Rollback deployment
rollback() {
    log_warn "Rolling back deployment..."

    helm rollback neurogate --namespace="$NAMESPACE"

    log_info "✓ Rollback completed"
}

# Main
main() {
    log_info "Starting NeuroGate deployment to Kubernetes..."
    log_info "Environment: $ENVIRONMENT"
    log_info "Namespace: $NAMESPACE"

    check_prerequisites
    check_secrets
    create_namespace
    create_secrets

    # Deploy using Helm (default) or Kustomize
    if [[ "$*" == *"--kustomize"* ]]; then
        deploy_kustomize
    else
        deploy_helm
    fi

    wait_for_deployment
    verify_deployment
    show_access_info

    log_info "✓ Deployment completed successfully!"
}

# Handle command line arguments
case "${1:-}" in
    rollback)
        rollback
        ;;
    *)
        main "$@"
        ;;
esac
