# 26 — Testing Strategy

Tier: **CORE**, cross-cutting. Owning: every module (test ownership follows
code ownership, doc 34).

## 1. Test Pyramid

```
Unit tests           — fastest, most numerous; pure logic (normalization,
                        state-machine transitions, Policy Engine
                        predicates, scoring math)
Integration tests    — module + real Postgres (Testcontainers), no AI/
                        browser dependency; verifies repositories, DB
                        constraints (doc 04 invariants), service
                        composition within a module
API tests            — full Spring context, MockMvc/RestAssured against
                        doc 05 contract, including auth/authorization
                        (doc 22 §2) and error-code correctness (doc 05 §12)
Database tests        — migration correctness (apply all Flyway
                        migrations against a fresh DB), constraint/index
                        behavior, especially doc 04's uniqueness/
                        invariant-backing constraints
Workflow tests        — Workflow Engine (doc 17) step sequencing, pause/
                        resume/retry/recover, using a fake BrowserAgent
                        and fake AiService
AI evaluation tests   — doc 27, separate suite (non-deterministic-output
                        aware, scored not just pass/fail)
Browser tests         — doc 28, Playwright against fixture pages
End-to-end tests      — full stack, a small number of critical-path
                        scenarios (discover → match → apply AUTO,
                        discover → match → apply MANUAL handoff)
Security tests        — doc 25 threat entries, each with at least one
                        automated test (e.g., cross-candidate access
                        attempt returns 404 not data)
Performance tests      — doc 31 SLA targets, load-tested against
                        realistic data volumes
Concurrency tests      — doc 51 scenarios (duplicate-submission race,
                        concurrent batch operations)
Recovery tests         — doc 30/72 failure-scenario table, each with a
                        test exercising the documented recovery behavior
```

## 2. Realistic Fixtures

- **Resumes**: clean text-PDF, scanned/OCR PDF, DOCX with tables, no-dates
  resume, overlapping-roles resume, adversarial/injection resume (doc 07
  §10).
- **Jobs**: well-structured ATS posting, sparse/malformed posting,
  adversarial/injection posting (doc 09 §8).
- **Pages**: the full doc 28 fixture set (simple/complex/dynamic forms,
  human-verification simulation, etc.).
- **Candidates**: a small standing set of synthetic candidate profiles at
  different seniority/domain combinations, reused across matching and
  strategy tests so results are comparable run-to-run.

## 3. Determinism & AI in Tests

Unit/integration/API/workflow tests never call a real model — `AiService`/
`EmbeddingService`/`VisionService` are faked with deterministic canned
responses for those layers, so those suites stay fast and reliable in CI.
Real-model behavior is exercised only in the dedicated AI evaluation suite
(doc 27), which is scored (accuracy/precision/recall bands), not asserted
as exact-match.

## 4. Critical Invariant Coverage (traces to doc 03 §6)

Each of the five domain invariants has a direct test: duplicate-
application prevention (DB constraint test + service-layer test),
tailored-resume-job-linkage, low-confidence-answer review gate, MANUAL-mode
never-calls-submit, expired-job blocks-new-application.

## 5. CI Gating

Unit + integration + API + database + security tests run on every PR
(doc 33 CI/CD) and must pass before merge. Workflow/browser/E2E/
performance suites run on a merge-to-main and nightly cadence given their
cost, with results visible but not blocking every PR — this trade-off is
revisited if regressions start slipping through (doc 36 roadmap notes it
as a thing to monitor, not a permanent stance).

## 6. What "Done" Means for a Task (feeds doc 37 task template)

A task is not complete until: unit tests for new logic exist, an
integration test exists if it touches persistence, an API test exists if
it adds/changes an endpoint, and any new failure mode has a corresponding
entry in doc 30/72 plus a recovery test.
