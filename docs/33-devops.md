# 33 — DevOps

Tier: **CORE**.

## 1. Stack

Java 21+, Spring Boot 3.x, PostgreSQL 16 + pgvector, Redis, Ollama,
Playwright for Java, Docker, Docker Compose.

## 2. Environments

`development` (Docker Compose, local Ollama, seeded fixture data),
`testing` (CI, Testcontainers for Postgres/Redis, faked AI per doc 26 §3),
`staging` (production-shaped, real Ollama instance, synthetic/anonymized
data only per doc 24 §2), `production`.

## 3. Docker Compose (dev/staging topology)

```yaml
services:
  app:
    build: .
    depends_on: [postgres, redis, ollama]
    environment:
      - SPRING_PROFILES_ACTIVE=dev
  postgres:
    image: pgvector/pgvector:pg16
    volumes: [pgdata:/var/lib/postgresql/data]
  redis:
    image: redis:7
  ollama:
    image: ollama/ollama
    volumes: [ollamadata:/root/.ollama]
  browser-worker:
    build: ./browser-worker
    # isolated, resource-capped per doc 14 §4 / doc 02 §8
volumes:
  pgdata:
  ollamadata:
```

## 4. Health Checks

Spring Actuator `/actuator/health` composed of: DB connectivity, Redis
connectivity, Ollama reachability (degraded, not down, if Ollama is
unreachable but circuit breaker doc 30 §3 is handling it), browser worker
pool availability. Readiness vs. liveness distinguished — liveness failures
restart the pod/container, readiness failures drain traffic without
restart.

## 5. Migrations

Flyway runs on startup in dev/test, as a separate, explicit deploy step
(not on every instance boot) in staging/production to avoid concurrent
migration races across multiple app replicas (doc 04 §3).

## 6. Backups

Postgres: continuous WAL archiving + daily full snapshot, retention per
doc 24 §4 policy; restore drills scheduled quarterly (not just backups
taken and never tested). Object storage (resumes/documents, doc 22 §5):
provider-level versioning/replication.

## 7. Secrets

Managed via a secrets manager (e.g., cloud provider's native service or
Vault) injected as environment variables at container start — never baked
into images, never in the Compose file's committed version (a
`.env.example` documents required keys without values).

## 8. CI/CD

```
PR opened → lint + unit + integration + API + database + security tests
            (doc 26 §5) → build image
Merge to main → above + workflow/browser/E2E suites (doc 26 §5) +
                AI eval suite (doc 27 §3) + browser eval suite (doc 28 §4)
                → deploy to staging → smoke tests → manual promote
                to production
```

Rollback: previous image tag redeployed; Flyway migrations are written
additive-first (doc 04 §3) specifically so a code rollback doesn't require
an immediate matching DB rollback.

## 9. Scheduling Infrastructure (cross-ref doc 32 §2)

Scheduled jobs (discovery sync, analytics aggregation, reminder scans,
career strategy regeneration) run via Spring's scheduler in the initial
single-instance-per-job-type setup, with a distributed lock (Redis) to
prevent double-execution if multiple app instances are running — the same
lock pattern used in doc 17 §5.

## 10. Rollback Strategy

Application: standard blue/green or rolling deploy with the previous image
kept warm. Database: forward-only migrations by default (§5/§8); a
genuinely broken migration is fixed forward with a corrective migration,
not rolled back destructively against a live database with real user data.
