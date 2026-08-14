---
slug: jobpilot
status: awaiting-approval
intent: clear
pending-action: on approval → append TASK-JP batches (Phases 2–13) into .omo/plans/jobpilot.md ## Todos, fill TL;DR last
approach: One decision-complete work plan for the complete JobPilot project — every task across all 13 roadmap phases broken down to TASK-JP-XXXX granularity (doc 37 template), grounded in the 53-file docs/ design set. Phase 1 tasks carried verbatim from doc 37; Phases 2–13 broken down fresh from phase docs 07–21 + security/eval/ops docs 22–32. User executes scaffolding themselves.
---

# Draft: jobpilot

## Components (topology ledger)
<!-- Lock the SHAPE before depth. One row per top-level component that can succeed or fail independently. -->
<!-- id | outcome (one line) | status: active|deferred | evidence path -->

| id | outcome | status | evidence |
|---|---|---|---|
| P1 Foundation | scaffold, Flyway, auth, module skeletons (TASK-JP-0001..0008) | active | docs/37:6-52 |
| P2 Candidate Intelligence | resume upload/parse, extraction, skill classification, evidence | active | docs/07 |
| P3 Job Intelligence | job sources, sync, normalization, dedup, company enrichment | active | docs/08 |
| P4 Matching | hybrid semantic+skill+alignment scoring, MatchReason, /matches | active | docs/09 |
| P5 Application Preparation | ApplicationStrategy, document generation, answer generation, Zero-Fabrication | active | docs/11 §1, docs/12, docs/23 §4 |
| P6 Application Engine MANUAL | Cockpit, manual application flow, ApplicationSession, handoff | active | docs/11 §2, docs/15 |
| P7 Browser Intelligence | Playwright agent, Form Intelligence, fixed action set, isolation | active | docs/13, docs/14, docs/06 §1 |
| P8 Automation ASSISTED/AUTO | Policy Engine, Workflow Engine, modes, submission, failure paths | active | docs/16, docs/17, docs/11 §3-4 |
| P9 Tracking & Notifications | ApplicationEvent timeline, outcomes, Notification, reminders, dashboard CORE subset | active | docs/18, docs/19 §1 |
| P10 Learning & Career Strategy | CareerStrategy, CareerMemory, LearningSystem signals | active | docs/10, docs/20, docs/21 |
| P11 Analytics | AnalyticsSnapshot, extended metrics, consumers | active | docs/19 §2-4 |
| P12 Scale Hardening | concurrency, queues, rate limiting, caching, load tests | active | docs/31, docs/32 |
| P13 MCP | thin adapter over service interfaces, readiness gate | deferred-until-ready | docs/future/01 |
| XC Cross-cutting | security, privacy, audit, observability, error handling, AI eval — embedded in every phase's tasks | active | docs/22-30 |

## Open assumptions (announced defaults)
<!-- assumption | adopted default | rationale | reversible? -->
1. Scope of detail: doc 37 §3 deliberately defers Phases 2–13 task breakdown "until each phase begins"; user's brief ("each and every task and all should be planned properly") overrides this → FULL TASK-JP detail for all 13 phases in ONE plan. | yes (user can reduce to Phases 1–6 at gate)
2. Plan shape: one plan file (.omo/plans/jobpilot.md), phase-grouped todos, sequential waves following roadmap order (doc 36 dependencies are phase-sequential). | yes
3. Phase 1 tasks: carried VERBATIM from doc 37 (TASK-JP-0001..0008), not rewritten. | yes
4. Scaffolding: user does it themselves → TASK-JP-0001 (project scaffold) still listed in plan, flagged USER-EXECUTED, plan does not depend on its completion to be written. | yes
5. Task numbering: continue doc 37's TASK-JP sequence (0009+) for Phases 2–13 so IDs stay globally unique. | yes
6. Env/CI: development/testing/staging/production profiles + Compose (app, pgvector:pg16, redis:7, ollama, browser-worker) per doc 33; verification assumes `./mvnw` + Testcontainers. | yes

## Findings (cited - path:lines)
- Product trust proposition: "candidate owns data, Zero-Fabrication, user-in-control" — unauthorized submission is the single worst failure (docs/25:86-95; docs/01).
- Automation consent model: mode + approve-* rules, fail-closed, Policy Engine is the ONLY path to real submission (docs/11 §4, docs/16 §4, docs/25:92-95).
- Zero-Fabrication enforced at 3 layers: schema EvidenceRef → Policy Engine isClaimSupportedByEvidence → confidence-gated review (docs/23:51-66).
- State machine: 16 states; AUTO/ASSISTED/MANUAL; ManualHandoffReason 9 values (docs/11 §3, docs/15).
- Browser agent: fixed typed action set (no code-gen), per-session isolation, vision via PAGE_UNDERSTANDING (docs/13, docs/14, docs/06 §2).
- Matching: hybrid semantic + importance-weighted skill + alignment; OpportunityScore; configurable weights, tuned with real data (docs/09 §3).
- Test pyramid + done-definition: docs/26 §1, §6; AI eval metrics incl. hallucination rate tightest band, blocks release (docs/27); browser fixtures self-hosted, zero-tolerance bypass (docs/28).
- Threat model: 13 threats; each maps to a concrete mitigation + test (docs/25). Privacy: retention/deletion/export (docs/24). Observability: logs/metrics/tracing + audit distinct (docs/29 §4).
- SLA targets p95 (docs/31 §1); concurrency rules (docs/32); queues Redis-backed, stateless workers (docs/32 §2).

## Decisions (with rationale)
- D1: Full task detail for all 13 phases now, in one plan — user's explicit override of doc 37's deferral; cheaper to review once than 13 times, and user asked for "complete project first."
- D2: Phases planned in doc 36 roadmap order; todos sequential (roadmap is a dependency chain), parallelized where docs allow (independent module tests, parallel fixture batches).
- D3: Security/privacy/eval requirements folded into the tasks of the phase that owns each surface (e.g., TASK-JP-0003 auth hardening in Phase 1; resume upload hardening in Phase 2) rather than a separate "security phase" — matches docs/25's control-to-owner mapping.
- D4: MCP (P13) planned as a readiness-checked adapter (docs/future/01 §5) — deferred, gated on Policy Engine tests + API stability; plan lists it last with explicit gate.

## Scope IN
- 13 roadmap phases (docs/36), each as TASK-JP-grouped todos with acceptance criteria, QA scenarios, evidence paths, commit strategy.
- Cross-cutting controls embedded per phase: auth/CSRF/audit (P1), upload hardening + adversarial fixtures (P2), SSRF-safe fetcher (P3), injection-resistant extraction (P3), evidence-grounded generation (P5), Policy Engine + idempotency (P8), observability/logging/metrics (each), eval harness + fixture suites (P2/P4/P5/P7), error taxonomy/retry/circuit breakers (each external dep), privacy (deletion/export/retention).

## Scope OUT (Must NOT have)
- No implementation — plan artifacts only under .omo/.
- FUTURE-scoped features are NOT tasks: email/response tracking integration (docs/18 §3), interview intelligence (docs/18 §4), hosted AI providers (docs/24 §7), service extraction/microservices (docs/32 §4/§6), dedicated vector store (docs/32 §5), MCP implementation before its §5 readiness checklist is true.
- No speculative DB schema fields (docs/24 §2 data minimization).
- No AI-application-platform bypass: CAPTCHA/login verification is never "solved" (docs/14 §6, docs/28 §2) — detection + escalation only.

## Open questions
- None blocking. (Two doc-internal ambiguities noted, non-blocking: "skill gaps" undefined in docs/19:13; reminder-interval defaults unspecified in docs/18:55 — plan uses configurable values with documented defaults.)

## Approval gate
status: awaiting-approval
<!-- When exploration is exhausted and unknowns are answered, set status: awaiting-approval. -->
<!-- That durable record is the loop guard: on a later turn read it and resume at the gate instead of re-running exploration. -->

Gate items for user (approve or veto):
1. **Full detail now for Phases 2–13** (vs. only Phases 1–6 now, deferring 7–13) — recommended: full, per your brief.
2. **One plan file** with phase-grouped todos, roadmap-sequential execution.
3. **Phase 1 verbatim from doc 37** (TASK-JP-0001..0008); new IDs continue at TASK-JP-0009.
4. **Scaffold task stays in plan**, flagged USER-EXECUTED.
5. Approve → I append todos and fill TL;DR; your next move is `/start-work` (scaffold yourself, then execute).
