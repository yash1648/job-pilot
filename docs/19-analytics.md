# 19 — Analytics

Tier: **ADVANCED**. Owning module: `analytics`.

## 1. Dashboard Metrics (CORE subset, surfaced via `GET
/analytics/dashboard`)

Jobs discovered, strong matches (opportunityScore above threshold),
applications prepared, applications submitted (split AUTO/ASSISTED/
MANUAL), interviews, offers, rejections, response rate, application
success rate — plus the "top opportunities" (ranked `JobMatch` list) and
"needs attention" queue (Applications in blocking states, doc 18 §6) that
the Dashboard screen (doc 02 §7) renders directly.

## 2. Extended Analytics (ADVANCED)

Match distribution (histogram of `overallMatch` across discovered jobs),
applications by role/company/source, response rate, interview rate, offer
rate, time-to-response (median days from `SUBMITTED` to first
`ApplicationOutcome`), resume-version performance (response rate grouped
by `resumeVersionId`, revealing which tailoring approach performs better),
job-source performance (response rate grouped by `jobSourceId`, feeding
back into doc 08 source prioritization and doc 21 learning).

## 3. Computation Model

`AnalyticsSnapshot` (doc 03/04) is computed on a schedule (default daily)
per candidate over a rolling window, not recomputed synchronously on every
dashboard load — the dashboard reads the latest snapshot plus a thin
real-time overlay for counts that must be current-second accurate (e.g.,
unread notification count). This keeps dashboard load cheap and keeps
historical snapshots available for trend display without re-deriving them.

```
Scheduled job (per candidate, or batched across candidates)
   ↓ aggregate Application/JobMatch/ApplicationOutcome rows for the period
   ↓ compute metrics per doc 03 `metrics` JSONB shape (versioned schema —
     `metrics.schemaVersion` so historical snapshots remain interpretable
     across metric-definition changes)
   ↓ AnalyticsSnapshot persisted
```

## 4. Consumers

Doc 10 (Career Strategy allocation adjustment), doc 21 (Learning System
input), doc 09 (`applicationDifficulty`/success-probability priors) all
read `AnalyticsSnapshot`/underlying aggregates rather than recomputing
their own — single source of truth for "what actually happened," avoiding
drift between, e.g., the Dashboard's response rate and Career Strategy's
response-rate-driven reallocation.

## 5. Failure Handling

| Failure | Behavior |
|---|---|
| Scheduled snapshot computation fails for a candidate | prior snapshot remains the latest available; failure logged, retried next window, dashboard shows "as of <last successful>" rather than stale-but-unlabeled data |
| Insufficient data (new candidate) | metrics computed with explicit zero/null rather than divide-by-zero errors; UI renders "not enough data yet" rather than misleading 0% |
