# 31 — Performance

Tier: **CORE** targets for CORE-tier flows, **ADVANCED** tuning beyond
baseline.

## 1. SLA Targets (initial, revisited with real load data)

| Operation | Target (p95) |
|---|---|
| API read endpoints (`/jobs`, `/matches`, `/applications`) | < 300ms |
| Resume upload → parse completion | < 30s (async, user sees progress) |
| Job Intelligence extraction per job | < 15s |
| Match computation per candidate/job pair | < 5s |
| Match recompute for a full candidate (all active jobs) | < 5min, background job |
| Document generation (resume tailor + cover letter + answers) for one application | < 60s |
| Browser form-fill action | < 3s per action (excluding page load) |
| Dashboard load | < 500ms (reads latest `AnalyticsSnapshot`, doc 19 §3 — not recomputed live) |

## 2. Where Cost Comes From

AI calls dominate latency for generation-heavy flows (doc 06 §3's
budgeting exists specifically to keep this bounded); browser actions
dominate for execution flows (network + page render, not JobPilot's own
compute); vector search is cheap at current expected scale with the
`ivfflat` indexing (doc 04 §4) but is the first thing to revisit if
candidate/job counts grow an order of magnitude (doc 32).

## 3. Caching Strategy

Redis caches: AI response cache (doc 06 §3), computed `JobMatch` results
(invalidated on candidate-profile or job change, not time-based alone),
`Company` enrichment data (long TTL, companies change slowly),
rate-limiter token buckets. Cache invalidation is event-driven (profile
updated → matches for that candidate marked stale) rather than relying
solely on TTL expiry, to avoid serving visibly-wrong stale matches after an
obvious change like a resume update.

## 4. Database Performance

Indexes defined in doc 04 are chosen against the actual query patterns in
doc 05 (list/filter endpoints); the `opportunityScore DESC` index
specifically supports the dominant "ranked matches" query. `EXPLAIN
ANALYZE` review is part of the review checklist (doc 35) for any new
query touching `jobs`, `job_matches`, or `applications` given their
expected row-count growth.

## 5. AI Cost Controls (cross-ref doc 06 §3)

Concurrency limits per model profile prevent a burst of user activity from
degrading latency for everyone; batching for bulk operations (re-embed
after resume update touches many downstream `JobMatch` rows — batched
recompute, not N synchronous calls) is the primary lever for controlling
tail latency under load.

## 6. Load Testing

Doc 26 §1's performance test suite runs against synthetic data at
realistic multiples of expected initial scale (e.g., 10x expected
candidate count, 100x expected job count) to catch scaling cliffs before
they're hit in production — particularly around the vector index and the
ranked-matches query.

## 7. Non-Goals

Not optimizing for scale the product doesn't have yet at the expense of
correctness or the Zero-Fabrication/security controls — doc 32 defines
when architectural change (not just tuning) is warranted; this document is
about tuning within the current modular-monolith architecture.
