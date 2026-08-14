# 36 — Development Roadmap

Tier: **CORE** planning artifact.

## 1. Principle

Every phase below builds toward the complete architecture in docs 01–35;
no phase introduces a design that a later phase has to rip out. Where a
phase ships a narrower version of a capability (e.g., single-pass LLM call
instead of a full agent loop, doc 16 §2), the narrower version already
satisfies the same interface/contract the fuller version will — swapping
implementation, not redesigning callers.

## 2. Phases

### Phase 1 — Foundation
Auth, User, CandidateProfile skeleton, database schema (doc 04) fully
migrated, `ai` module with Ollama provider wired (doc 06), basic API
scaffold (doc 05), CI pipeline (doc 33 §8) with unit/integration/API
suites (doc 26). **Exit criterion**: a user can register, log in, and the
system is deployable end-to-end with no business features yet.

### Phase 2 — Candidate Intelligence
Doc 07 full pipeline: upload → parse → extract → skills/evidence →
embeddings. Resume Manager UI. **Exit criterion**: doc 27's resume
extraction accuracy/skill precision-recall metrics hit their target bands
on the benchmark set.

### Phase 3 — Job Intelligence
Doc 08 discovery (initial adapters: Greenhouse, Lever, one generic
career-page pattern) + doc 09 Job Intelligence extraction, Company
Intelligence baseline. **Exit criterion**: jobs flow from discovery through
normalization into a queryable `Job` table with dedup verified under load
(doc 26 concurrency tests).

### Phase 4 — Matching
Doc 09 hybrid matching + opportunity ranking, doc 05 `/matches` endpoints,
Job Search / Recommendations / Match Analysis UI. **Exit criterion**: doc
27 match precision/recall targets met; explainability (MatchReason)
verified against the doc 01 §5.8 UI example format.

### Phase 5 — Application Preparation
Doc 11 §1 Application Strategy, doc 12 (resume tailoring, cover letters,
answer engine) — generation only, no execution yet. **Exit criterion**: doc
27 hallucination rate at target (tightest tolerance, doc 27 §4) before
proceeding to any execution phase — generation quality gate before
automation is layered on top.

### Phase 6 — Application Engine (MANUAL mode first)
Doc 11 full state machine, doc 15 Manual Handoff, Application Cockpit UI,
Manual Handoff UI. **Exit criterion**: a full DISCOVERED → ... →
WAITING_FOR_USER → SUBMITTED → TRACKING flow works end-to-end for MANUAL
mode — deliberately the safest mode first, since it has no browser
execution risk.

### Phase 7 — Browser Intelligence
Doc 13 Form Intelligence, doc 14 Browser Agent, doc 28 browser evaluation
suite passing on the full fixture set including verification-detection
fixtures. **Exit criterion**: doc 28 §5 regression policy in force, zero
tolerance fixtures passing consistently.

### Phase 8 — Automation (ASSISTED then AUTO)
Doc 11 §2/§4 ASSISTED mode first (human-in-the-loop by construction, lower
risk), then AUTO mode gated behind `AUTO_WITHIN_RULES` with the Policy
Engine (doc 16 §4) fully tested. **Exit criterion**: doc 25 §12
(unauthorized-submission threat) mitigations verified by dedicated
security tests before AUTO ships to any real user.

### Phase 9 — Tracking & Notifications
Doc 18 full lifecycle tracking, notifications, dashboard. **Exit
criterion**: Dashboard (doc 19 §1 CORE subset) live with real data.

### Phase 10 — Learning & Career Strategy
Doc 10 Career Strategy, doc 20 Career Memory, doc 21 Learning System. Cold
start (doc 21 §6) verified before enabling for real candidates with
limited history.

### Phase 11 — Analytics
Doc 19 extended analytics, source/resume-version performance views.

### Phase 12 — Scale Hardening
Doc 31/32: load testing at target multiples, extraction-candidate signals
monitored (not acted on unless triggered).

### Phase 13 — MCP Integration (FUTURE)
Doc 66/future/01-mcp-readiness.md's tool surface implemented as a thin
adapter over the existing API/service layer — no earlier phase's internal
design changes to accommodate this.

## 3. What Should Never Be Implemented Prematurely

AUTO mode before ASSISTED mode is proven (Phase 8 ordering is
deliberate); Learning System adjustments before enough outcome data exists
to clear doc 21 §6's cold-start gate; microservice extraction (doc 32 §4)
before a concrete scaling/deploy-cadence/team signal is observed; MCP
surface before the core API/Policy Engine contract is stable (doc 66).
