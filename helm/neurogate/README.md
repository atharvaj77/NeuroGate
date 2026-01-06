# NeuroGate Helm Chart

Production-ready Helm chart for deploying NeuroGate AI Gateway to Kubernetes.

## Prerequisites

- Kubernetes 1.24+
- Helm 3.10+
- PV provisioner support in the underlying infrastructure (for Qdrant/Redis persistence)
- cert-manager (for TLS certificates)
- NGINX Ingress Controller

## Installing the Chart

### Quick Start

```bash
# Add Helm repository (if published)
helm repo add neurogate https://charts.neurogate.com
helm repo update

# Install with default values
helm install neurogate neurogate/neurogate \
  --namespace neurogate \
  --create-namespace \
  --set secrets.openaiApiKey=sk-your-key-here

# Or install from local chart
helm install neurogate ./helm/neurogate \
  --namespace neurogate \
  --create-namespace \
  --set secrets.openaiApiKey=sk-your-key-here
```

### Production Installation

```bash
# Create values file for production
cat > values-prod.yaml <<EOF
replicaCount: 5

image:
  repository: your-registry.io/neurogate
  tag: "1.0.0"

ingress:
  hosts:
    - host: api.neurogate.yourcompany.com
      paths:
        - path: /v1
          pathType: Prefix
  tls:
    - secretName: neurogate-tls
      hosts:
        - api.neurogate.yourcompany.com

resources:
  limits:
    cpu: 4000m
    memory: 8Gi
  requests:
    cpu: 2000m
    memory: 4Gi

autoscaling:
  enabled: true
  minReplicas: 5
  maxReplicas: 50

secrets:
  openaiApiKey: "sk-your-openai-key"
  anthropicApiKey: "sk-ant-your-anthropic-key"

dependencies:
  qdrant:
    enabled: true
    persistence:
      enabled: true
      size: 50Gi
  redis:
    enabled: true
    persistence:
      enabled: true
      size: 20Gi

monitoring:
  enabled: true
  serviceMonitor:
    enabled: true
EOF

# Install with production values
helm install neurogate ./helm/neurogate \
  --namespace neurogate \
  --create-namespace \
  --values values-prod.yaml
```

## Configuration

The following table lists the configurable parameters and their default values.

### Image Configuration

| Parameter | Description | Default |
|-----------|-------------|---------|
| `image.repository` | Image repository | `neurogate` |
| `image.tag` | Image tag | `1.0.0` |
| `image.pullPolicy` | Image pull policy | `IfNotPresent` |

### Deployment Configuration

| Parameter | Description | Default |
|-----------|-------------|---------|
| `replicaCount` | Number of replicas | `3` |
| `resources.limits.cpu` | CPU limit | `2000m` |
| `resources.limits.memory` | Memory limit | `4Gi` |
| `resources.requests.cpu` | CPU request | `1000m` |
| `resources.requests.memory` | Memory request | `2Gi` |

### Autoscaling

| Parameter | Description | Default |
|-----------|-------------|---------|
| `autoscaling.enabled` | Enable HPA | `true` |
| `autoscaling.minReplicas` | Minimum replicas | `3` |
| `autoscaling.maxReplicas` | Maximum replicas | `20` |
| `autoscaling.targetCPUUtilizationPercentage` | Target CPU % | `70` |

### Ingress Configuration

| Parameter | Description | Default |
|-----------|-------------|---------|
| `ingress.enabled` | Enable ingress | `true` |
| `ingress.className` | Ingress class | `nginx` |
| `ingress.hosts[0].host` | Hostname | `api.neurogate.yourdomain.com` |

### Secrets

| Parameter | Description | Default |
|-----------|-------------|---------|
| `secrets.openaiApiKey` | OpenAI API key (required) | `""` |
| `secrets.anthropicApiKey` | Anthropic API key | `""` |
| `secrets.redisPassword` | Redis password | `""` |

### Dependencies

| Parameter | Description | Default |
|-----------|-------------|---------|
| `dependencies.qdrant.enabled` | Enable Qdrant | `true` |
| `dependencies.qdrant.persistence.enabled` | Enable persistence | `true` |
| `dependencies.qdrant.persistence.size` | Storage size | `10Gi` |
| `dependencies.redis.enabled` | Enable Redis | `true` |
| `dependencies.redis.persistence.enabled` | Enable persistence | `true` |
| `dependencies.redis.persistence.size` | Storage size | `5Gi` |

## Upgrading

```bash
# Upgrade release
helm upgrade neurogate ./helm/neurogate \
  --namespace neurogate \
  --values values-prod.yaml

# Upgrade with specific version
helm upgrade neurogate ./helm/neurogate \
  --namespace neurogate \
  --version 1.1.0 \
  --values values-prod.yaml
```

## Uninstalling

```bash
# Uninstall release
helm uninstall neurogate --namespace neurogate

# Delete namespace
kubectl delete namespace neurogate
```

## Monitoring

The chart includes ServiceMonitor and PrometheusRule for monitoring with Prometheus Operator.

```bash
# Enable monitoring
helm install neurogate ./helm/neurogate \
  --set monitoring.enabled=true \
  --set monitoring.serviceMonitor.enabled=true \
  --set monitoring.prometheusRule.enabled=true
```

## Examples

### High Availability Setup

```yaml
replicaCount: 5

autoscaling:
  enabled: true
  minReplicas: 5
  maxReplicas: 100

podDisruptionBudget:
  enabled: true
  minAvailable: 3

affinity:
  podAntiAffinity:
    requiredDuringSchedulingIgnoredDuringExecution:
    - labelSelector:
        matchLabels:
          app: neurogate
      topologyKey: kubernetes.io/hostname
```

### Multi-Region Deployment

```yaml
# us-east values
ingress:
  hosts:
    - host: us-east.api.neurogate.com

config:
  qdrant:
    host: qdrant-us-east
  redis:
    host: redis-us-east

# eu-west values
ingress:
  hosts:
    - host: eu-west.api.neurogate.com

config:
  qdrant:
    host: qdrant-eu-west
  redis:
    host: redis-eu-west
```

## Troubleshooting

### Pods not starting

```bash
# Check pod status
kubectl get pods -n neurogate

# View pod logs
kubectl logs -f deployment/neurogate -n neurogate

# Describe pod
kubectl describe pod -l app=neurogate -n neurogate
```

### Ingress not working

```bash
# Check ingress
kubectl get ingress -n neurogate
kubectl describe ingress neurogate -n neurogate

# Check certificate
kubectl get certificate -n neurogate
```

### High memory usage

```bash
# Increase memory limits
helm upgrade neurogate ./helm/neurogate \
  --set resources.limits.memory=8Gi \
  --reuse-values
```

## Support

- Documentation: https://docs.neurogate.com
- Issues: https://github.com/yourusername/neurogate/issues
- Discord: https://discord.gg/neurogate
