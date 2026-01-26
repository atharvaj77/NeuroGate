# Analytics & Metrics Module Documentation

The **Analytics** module provides business intelligence, cost tracking, and operational metrics for the NeuroGate platform. It answers questions about usage, cost, and system performance.

## Business Intelligence

### `AnalyticsService`
**Package:** `com.neurogate.analytics`

**Purpose:**
The core service for generating usage reports and cost analysis. It aggregates raw usage records into actionable insights for users and teams.

**Key Methods:**
- `getUserCostReport(String userId, LocalDate from, LocalDate to)`: Generates a detailed cost breakdown for a specific user, including provider distribution and cache hit rates.
- `getTeamCostReport(String teamId, LocalDate from, LocalDate to)`: Aggregates costs across a team, identifying top users and most expensive models.
- `getTopExpensiveQueries(String teamId, int limit)`: Identifies individual requests that consumed the most budget.

**Context:**
Used by the admin dashboard and billing systems to track resource consumption.

---

## Budgeting

### `BudgetManagementService`
**Package:** `com.neurogate.analytics`

**Purpose:**
Enforces budget limits and alerts. It allows setting daily/weekly/monthly spending caps for users or teams and can proactively throttle requests when limits are exceeded.

**Key Methods:**
- `createUserBudget(...)`: Defines a spending limit for a user.
- `createTeamBudget(...)`: Defines a spending limit for a team.
- `updateSpending(String entityId, ...)`: Increments the current spending counter after each request.
- `shouldThrottle(String userId)`: Checks if a user has exceeded their budget and if throttling is enabled.
- `checkPendingAlerts()`: Scheduled task to send notifications when spending thresholds (e.g., 80%) are reached.

---

## Operational Metrics

### `NeuroGateMetrics`
**Package:** `com.neurogate.metrics`

**Purpose:**
A wrapper around Micrometer for recording real-time application metrics. It exposes custom metrics to monitoring systems (like Prometheus/Grafana).

**Key Metrics Tracked:**
- **Cache**: Hits, Misses, Hit Ratio.
- **Routing**: Requests per provider (OpenAI vs. Local).
- **Latency**: P95, P99 response times.
- **Cost**: Real-time estimated cost savings from caching and routing.
- **PII**: Counts of protected entities detected (Emails, SSNs, etc.).

**Key Methods:**
- `recordCacheHit()` / `recordCacheMiss()`
- `recordLatency(long durationMs)`
- `recordPiiDetection(String type)`
