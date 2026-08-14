# 30 — Error Handling

Tier: **CORE**, cross-cutting.

## 1. Exception Taxonomy

```
JobPilotException (base, unchecked)
├── RetryableException          (transient — network blip, timeout)
├── NonRetryableException       (permanent — bad input, logic error)
├── UserActionRequiredException (ASSISTED pause, approval needed)
├── ManualHandoffRequiredException (doc 15 triggers)
└── SecurityBlockedException    (Policy Engine denial, doc 16 §4)
```

Module-specific exceptions extend these (e.g., `AiUnavailableException
extends RetryableException`, doc 06 §7; `JobSourceException` may be
Retryable or NonRetryable depending on cause).

## 2. Retry Policy

Exponential backoff with jitter, bounded attempt count, applied uniformly
via a shared `RetryTemplate` (Spring Retry) — attempt counts and base
delay are configured per exception type, not per call site, so retry
behavior is consistent and centrally tunable.

## 3. Circuit Breakers

Applied to external dependencies with a failure-rate threshold (Ollama,
each `JobSource`, Redis): sustained failure trips the breaker, subsequent
calls fail fast (typed exception) rather than piling up latency, breaker
half-opens on a timer to probe recovery. Circuit state changes are logged
and alerted (doc 29 §5).

## 4. Idempotency

State-changing external-effect endpoints (`/applications/{id}/start`, doc
05) require `Idempotency-Key`; the workflow-level lock (doc 17 §5) is the
second layer preventing duplicate execution even if the idempotency cache
were somehow bypassed.

## 5. Dead-Letter Handling

Async jobs (discovery sync, analytics aggregation, scheduled reminders)
that exhaust retries move to a dead-letter queue rather than being dropped
silently — a scheduled sweep alerts on non-empty dead-letter queues so
persistent failures get human attention (doc 29 §5).

## 6. Failure Scenario Table (doc 72 detail, summarized here as the
canonical cross-reference)

| Scenario | Category | Recovery |
|---|---|---|
| Ollama unavailable | Retryable → circuit breaker | fallback per doc 06 §7, caller decides (e.g., defer to MANUAL if generation blocked) |
| Database unavailable | Retryable, app-level health check fails | readiness probe fails, traffic drained (doc 33), no partial writes (transactional boundaries) |
| Job source unavailable | Retryable | doc 08 §7 |
| Job expired mid-workflow | NonRetryable | `Application` blocked from further progression, user notified, existing terminal states unaffected (doc 03 invariant #5) |
| Browser crash | Retryable (session-level) | doc 14 §8/§7 |
| Page changes mid-flow | UserActionRequired/ManualHandoff | doc 17 §6 |
| CAPTCHA appears | ManualHandoffRequired | doc 14 §6, doc 15 §2 — never retried around |
| Login expires mid-session | ManualHandoffRequired (`AUTHENTICATION_REQUIRED`) | doc 15 §2 |
| User closes browser (if using Take-Control) | treated as a pause | doc 17 §3 pause, resumable |
| Network failure | Retryable | backoff, doc 30 §2 |
| Generated answer fails validation | NonRetryable at content level | doc 12 §6 bounded retry then `requiresUserReview` |
| Resume parsing fails | NonRetryable | doc 07 §9 |
| Duplicate job detected | not a failure — expected dedup | doc 08 §5 |
| Application submission fails (platform-side error) | Retryable then ManualHandoff | doc 17 §6, capped attempts (`ApplicationAttempt`) |

## 7. User-Facing Error Communication

Every user-facing error message states what happened and what (if
anything) the user needs to do — never a bare stack trace or generic "an
error occurred" where a more specific, actionable message is available
(doc 05 §Errors' `details` field carries this).
