# NeuroGate - Quick Start Guide

**Get NeuroGate running in 5 minutes.**

---

## Prerequisites

```bash
# Required
✓ Java 17+ (Java 21 recommended)
✓ Docker & Docker Compose
✓ OpenAI API Key

# Optional (for production)
✓ Kubernetes 1.28+
✓ Helm 3.10+
```

---

## Option 1: Local Development (Fastest)

### Step 1: Start Infrastructure
```bash
docker-compose up -d
```

### Step 2: Set API Key
```bash
export OPENAI_API_KEY="sk-your-openai-api-key-here"
```

### Step 3: Run Application
```bash
./gradlew bootRun
```

### Step 4: Test
```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-3.5-turbo",
    "messages": [{"role": "user", "content": "What is Java?"}]
  }'
```

**Expected Response**: JSON with AI response + cache metadata

**Access Points**:
- API: http://localhost:8080
- Health: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/prometheus
- Grafana: http://localhost:3000 (admin/neurogate2024)

---

## Option 2: Docker (Isolated)

### Step 1: Build Image
```bash
./scripts/build-docker.sh
```

### Step 2: Run Container
```bash
docker run -d \
  --name neurogate \
  -p 8080:8080 \
  -e OPENAI_API_KEY="sk-your-key" \
  -e SPRING_PROFILES_ACTIVE=prod \
  neurogate:latest
```

### Step 3: Check Logs
```bash
docker logs -f neurogate
```

### Step 4: Test API
```bash
curl http://localhost:8080/actuator/health
```

---

## Option 3: Kubernetes (Production)

### Prerequisites
```bash
# Verify cluster access
kubectl cluster-info

# Verify Helm
helm version
```

### Step 1: Deploy with Helm
```bash
helm install neurogate helm/neurogate \
  --namespace neurogate \
  --create-namespace \
  --set secrets.openaiApiKey="sk-your-key-here" \
  --set image.tag=latest
```

### Step 2: Verify Deployment
```bash
# Check pods
kubectl get pods -n neurogate

# Check services
kubectl get svc -n neurogate

# Check ingress
kubectl get ingress -n neurogate
```

### Step 3: Access API
```bash
# Port forward (for testing)
kubectl port-forward -n neurogate svc/neurogate 8080:80

# Test
curl http://localhost:8080/actuator/health
```

### Step 4: Production Access
```bash
# Via Ingress (configure DNS first)
curl https://api.neurogate.yourdomain.com/actuator/health
```

---

## Option 4: Automated Deployment Script

### Full Automation
```bash
# Set environment variables
export ENVIRONMENT=prod
export NAMESPACE=neurogate
export OPENAI_API_KEY="sk-your-key-here"
export IMAGE_TAG=1.0.0

# Deploy everything
./scripts/deploy-k8s.sh
```

**What it does**:
1. Validates prerequisites (kubectl, helm)
2. Creates namespace
3. Creates secrets from environment variables
4. Deploys via Helm
5. Waits for rollout
6. Verifies deployment health
7. Shows access information

---

## Common Commands

### Local Development
```bash
# Start infrastructure
docker-compose up -d

# Stop infrastructure
docker-compose down

# View logs
./gradlew bootRun

# Run tests
./gradlew test

# Build JAR
./gradlew build
```

### Docker
```bash
# Build image
./scripts/build-docker.sh

# Run container
docker run -p 8080:8080 -e OPENAI_API_KEY=sk-xxx neurogate:latest

# View logs
docker logs -f neurogate

# Stop container
docker stop neurogate

# Remove container
docker rm neurogate
```

### Kubernetes
```bash
# Deploy
helm install neurogate helm/neurogate --set secrets.openaiApiKey=sk-xxx

# Upgrade
helm upgrade neurogate helm/neurogate --reuse-values

# Rollback
helm rollback neurogate

# Uninstall
helm uninstall neurogate -n neurogate

# View logs
kubectl logs -n neurogate -l app=neurogate --tail=100 -f

# Check health
kubectl exec -n neurogate -it deploy/neurogate -- curl localhost:8080/actuator/health
```

---

## Testing the API

### Basic Chat Request
```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-3.5-turbo",
    "messages": [
      {"role": "user", "content": "What is Java Virtual Threads?"}
    ]
  }'
```

### Test PII Protection
```bash
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "gpt-3.5-turbo",
    "messages": [{
      "role": "user",
      "content": "Send email to john.doe@example.com about account 378282246310005"
    }]
  }'
```

**What happens**:
1. PII detected: `john.doe@example.com` → `<EMAIL_1>`, `378282246310005` → `<CC_1>`
2. Sanitized prompt sent to OpenAI
3. Response restored with original PII
4. You receive response with real email/CC

### Test Cache
```bash
# First request (cache miss)
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"model": "gpt-3.5-turbo", "messages": [{"role": "user", "content": "What is 2+2?"}]}'

# Second request (cache hit)
curl -X POST http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{"model": "gpt-3.5-turbo", "messages": [{"role": "user", "content": "What is 2+2?"}]}'
```

**Expected**: Second request returns in ~2ms with `x_neurogate_cache_hit: true`

---

## Monitoring

### Health Check
```bash
curl http://localhost:8080/actuator/health
```

### Prometheus Metrics
```bash
curl http://localhost:8080/actuator/metrics
```

### Grafana Dashboard
1. Open http://localhost:3000
2. Login: admin / neurogate2024
3. Navigate to "NeuroGate Overview" dashboard

**Key Metrics**:
- Cache hit ratio
- Request latency (P50, P95, P99)
- PII detections by type
- Circuit breaker states
- Cost savings

---

## Troubleshooting

### Problem: Port 8080 already in use
```bash
# Find process
lsof -i :8080

# Kill process
kill -9 <PID>

# Or use different port
./gradlew bootRun --args='--server.port=8081'
```

### Problem: OpenAI API key not set
```bash
# Set in environment
export OPENAI_API_KEY="sk-your-key-here"

# Or in application.yml
echo "spring.ai.openai.api-key: sk-your-key" >> src/main/resources/application.yml
```

### Problem: Docker build fails
```bash
# Build JAR first
./gradlew clean build

# Then build Docker image
docker build -t neurogate:latest .
```

### Problem: Kubernetes pods not starting
```bash
# Check pod status
kubectl get pods -n neurogate

# View logs
kubectl logs -n neurogate -l app=neurogate

# Describe pod
kubectl describe pod -n neurogate <pod-name>

# Common fixes:
# - Check secret exists: kubectl get secrets -n neurogate
# - Check resources: kubectl top pods -n neurogate
# - Check events: kubectl get events -n neurogate
```

---

## Next Steps

### For Developers
1. Read [LOCAL_DEPLOYMENT.md](docs/LOCAL_DEPLOYMENT.md) for detailed development guide
2. Explore [API_DOCUMENTATION.md](docs/api/API_DOCUMENTATION.md) for complete API reference
3. Check [ARCHITECTURE_DETAILED.md](docs/technical/ARCHITECTURE_DETAILED.md) for system design

### For Operations
1. Read [DEPLOYMENT_GUIDE.md](docs/technical/DEPLOYMENT_GUIDE.md) for production deployment
2. Review [PHASE3_DEPLOYMENT.md](docs/technical/PHASE3_DEPLOYMENT.md) for resilience patterns
3. Check [PROJECT_STATUS.md](PROJECT_STATUS.md) for complete system overview

### For Business
1. Review [BUSINESS_PLAN.md](docs/business/BUSINESS_PLAN.md) for market analysis
2. Check [FUTURE_ROADMAP.md](docs/technical/FUTURE_ROADMAP.md) for upcoming features

---

## Support

### Documentation
- **Main Index**: [docs/INDEX.md](docs/INDEX.md)
- **Complete Status**: [PROJECT_STATUS.md](PROJECT_STATUS.md)

### Community
- GitHub Issues: Report bugs and request features
- Discussions: Ask questions and share ideas

---

**Ready to deploy? Choose your deployment option above and get started!**

---

Version: 1.0.0
Last Updated: December 30, 2024
Status: Production Ready ✅
