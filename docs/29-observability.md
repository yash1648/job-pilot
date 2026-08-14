# 29 — Observability

Tier: **CORE**, cross-cutting. Owning module: all (instrumentation),
`common` (shared logging/metrics infrastructure).

## 1. Structured Logs

JSON-structured logs (not free-text), every log line carrying:
`timestamp, level, module, traceId, candidateProfileId (when applicable,
never for anonymous/system-level logs), event, details`. **Never logged**:
password hashes, JWT/session tokens, OAuth secrets, raw resume/document
content (log a `documentId` reference instead), browser session
credentials (doc 22 §6/§8) — this list is enforced by a logging-utility
wrapper that redacts known-sensitive field names by default rather than
relying on every call site remembering.

## 2. Metrics

| Category | Examples |
|---|---|
| AI request metrics | latency, token count, model/profile used, success/failure/timeout rate — per `AiTaskType` (doc 06 §2) |
| Workflow metrics | step duration, pause rate by reason, resume success rate |
| Browser metrics | action success rate by `BrowserAction` type, session duration, escalation rate by `ManualHandoffReason` |
| Application metrics | applications by state (doc 11 §3) over time, mode split (AUTO/ASSISTED/MANUAL), duplicate-prevention trigger count |
| Discovery metrics | jobs discovered/normalized per source, sync success rate, dedup rate |
| System metrics | standard JVM/DB/Redis/HTTP metrics (Micrometer → Prometheus) |

## 3. Tracing

Distributed tracing (OpenTelemetry) with a `traceId` propagated from the
initiating API request through async workflow steps, AI calls, and browser
actions — a single Application's full execution can be reconstructed
end-to-end from trace spans, which matters specifically because failures
here (doc 30/72) often span multiple async steps.

## 4. Audit Events vs. Logs vs. Metrics

Distinct and complementary, not redundant: `AuditEvent` (doc 04/22 §10) is
the permanent, queryable, user-visible-for-their-own-data record of
consequential actions; logs are operational/debugging detail with a
retention window (30–90 days, doc 33); metrics are aggregated numeric
signal with no per-event detail. A consequential action (application
submitted) produces all three, each serving its own purpose — logs are
never treated as an audit substitute since logs aren't append-only-
guaranteed at the DB level the way `audit_events` is.

## 5. Dashboards & Alerting

Operational dashboards (Grafana) built on the metrics above; alerting on:
AI provider unavailability sustained beyond a threshold, browser
action-success-rate drop, discovery sync failure streak, error-rate
spikes on `/applications/*/start`, queue backlog growth (doc 33/doc 51).
Alerting thresholds are defined in doc 31's SLA targets, not arbitrary.

## 6. Correlation with Doc 30

Every failure category in doc 30's exception taxonomy emits a metric
(count by exception type) and a structured log at the point of handling —
this is what makes doc 30's recovery-behavior table something that can be
verified in production, not just asserted in documentation.
