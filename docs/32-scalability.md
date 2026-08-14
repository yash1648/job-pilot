# 32 — Scalability

Tier: **CORE** (concurrency correctness), **FUTURE** (service extraction).

## 1. Concurrency (multi-search, multi-application, multi-browser,
multi-user)

- Job discovery syncs run as independent scheduled/queued jobs per
  `JobSource`, safely parallel across sources (no shared mutable state
  beyond the DB, which handles concurrent writes via normal transaction
  isolation plus the `canonical_identity` unique constraint, doc 04 §2.3).
- Application preparation (doc 11/12) for different `Application` rows is
  independently parallelizable; the workflow-level lock (doc 17 §5)
  scopes concurrency control to *one application at a time*, not the whole
  system.
- Browser sessions run in a worker pool with a configurable max
  concurrent-session cap, sized to the host resources (doc 14 §4
  isolation makes this a resource-sizing decision, not a correctness
  one).
- Multi-user: every service call is scoped by `candidateProfileId`
  (doc 22 §2); no shared mutable in-memory state exists across users —
  all cross-request state lives in Postgres/Redis, which is what makes
  horizontal scaling of the application tier possible without sticky
  sessions (beyond normal session-cookie affinity, which a shared Redis
  session store also removes if needed).

## 2. Queues & Workers

Redis-backed queues (or a lightweight embedded scheduler for the initial
scale, upgradable to a dedicated queue like a Redis Streams consumer group
without an architecture change) drive: discovery sync jobs, document
generation jobs, analytics aggregation, notification delivery, reminder
scans. Workers are stateless and horizontally scalable — adding a worker
instance requires no code change, only deployment configuration (doc 33).

## 3. Rate Limiting (cross-ref doc 52, doc 08 §6)

Token-bucket limiters in Redis, scoped per `JobSource` and per internal
operation (e.g., per-user `/matches/recompute` calls) — shared across all
application instances since the bucket lives in Redis, not per-instance
memory, which is what makes rate limiting correct under horizontal
scaling.

## 4. Extraction Candidates (FUTURE, from the modular monolith)

| Candidate module | Extraction trigger |
|---|---|
| `browser` | independent scaling need (many more concurrent sessions than the rest of the app requires), or a dedicated infra/security boundary requirement (isolating untrusted-page-handling infra further from the core app) |
| `ai` | if a second, differently-scaled AI provider (e.g., GPU-bound self-hosted model vs. hosted API) needs independent deployment/scaling from the rest of the app |
| `notification` | high fan-out volume outgrowing in-process delivery |
| `analytics` | heavy aggregation workload contending with OLTP workload on the primary DB — likely paired with a read-replica or dedicated analytics store before a full service split is justified |

Each extraction is justified by a concrete signal (doc 02 §9's criteria:
independent scaling need, independent deploy cadence, team boundary) — not
performed speculatively. Until a signal is observed, doc 02 §1's modular
monolith stands.

## 5. Data Volume Scaling

`jobs`/`job_embeddings` grow fastest (continuous discovery across many
candidates' overlapping searches, though jobs are deduplicated globally
via `canonical_identity`, so growth is sublinear in candidate count). Doc
31 §6's load testing specifically targets this table's query patterns
first. `pgvector` index tuning (`ivfflat` list count, doc 04 §4) is
revisited as row counts pass documented thresholds; a move to a dedicated
vector store is a FUTURE option if pgvector's performance profile stops
meeting doc 31 targets at scale, not a default assumption.

## 6. No Premature Microservices

Restated from ADR-001/doc 02 §9: the architecture explicitly defers
service extraction until a concrete signal exists, specifically to avoid
distributed-transaction complexity across the candidate→match→apply chain
before the domain itself has stabilized.
