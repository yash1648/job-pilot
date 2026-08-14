# 37 — Implementation Tasks

Tier: **CORE** planning artifact. Tasks are sized for an AI coding agent or
a developer to execute individually, each traceable to the docs above.

## 1. Task Template

```
TASK-JP-{id}
Title:
Description:
Dependencies: [TASK-JP-ids]
Affected modules: [doc 02 §3 module names]
Implementation requirements:
Acceptance criteria:
Tests: (doc 26 §6 — what must exist for this task to count as done)
Potential risks:
```

## 2. Phase 1 (Foundation) — Full Breakdown

```
TASK-JP-0001
Title: Project scaffold — Maven build, package skeleton, Spring Boot app
Dependencies: none
Affected modules: common
Requirements: doc 34 package structure created with placeholder
  package-info.java per package; pom.xml with Spring Boot 3.x, Postgres
  driver, pgvector, Redis, Flyway, Playwright, Testcontainers, ArchUnit;
  application.yml per profile (doc 34 §6); Docker Compose (doc 33 §3).
Acceptance criteria: `mvn spring-boot:run` starts against Compose stack;
  actuator health returns UP.
Tests: ArchUnit test asserting doc 34 §3 dependency rules (even with
  empty modules, the rule structure is in place).

TASK-JP-0002
Title: Database migrations — identity, preferences, candidate evidence
  tables (doc 04 §2.1–2.2)
Dependencies: TASK-JP-0001
Affected modules: common, candidate
Requirements: Flyway V1__identity.sql, V2__evidence_tables.sql per doc 04.
Acceptance criteria: fresh DB migrates cleanly; all constraints/indexes
  from doc 04 §2.1–2.2 present.
Tests: migration test (doc 26 §1 database tests), constraint tests
  (ux_resumes_one_master, etc.).

TASK-JP-0003
Title: users/auth — registration, login, JWT issuance
Dependencies: TASK-JP-0002
Affected modules: auth, user, security
Requirements: doc 05 §1 endpoints, doc 22 §1 password hashing/session
  security.
Acceptance criteria: register→login→authenticated request round-trip
  works; CSRF/session tests pass (doc 22 §3).
Tests: API tests for all doc 05 §1 endpoints incl. error cases (duplicate
  email → 409).

TASK-JP-0004
Title: CandidateProfile + JobPreference CRUD skeleton
Dependencies: TASK-JP-0003
Affected modules: candidate, user
Requirements: doc 05 §2/§3 endpoints, doc 22 §2 candidate-scoping enforced
  at repository layer.
Acceptance criteria: cross-candidate access attempt returns 404 (doc 25
  §5 test).
Tests: API + security tests.

TASK-JP-0005
Title: ai module — provider abstraction + Ollama implementation
Dependencies: TASK-JP-0001
Affected modules: ai
Requirements: doc 06 §1 interfaces, OllamaAiService/EmbeddingService/
  VisionService, ModelRouter (doc 06 §2) config-driven.
Acceptance criteria: a fixture prompt round-trips through the real Ollama
  container in Compose and returns schema-validated output.
Tests: unit tests with faked HTTP layer; one integration test against a
  real local Ollama instance (marked slow, doc 26 §5 nightly tier).

TASK-JP-0006
Title: audit module — AuditEvent persistence + append-only enforcement
Dependencies: TASK-JP-0002
Affected modules: audit
Requirements: doc 04 §2.6 audit_events table, DB role grants (no UPDATE/
  DELETE), AuditService.record(...) API used by other modules.
Acceptance criteria: attempted UPDATE/DELETE via the app DB role fails at
  the DB level, not just app-level convention.
Tests: integration test asserting the DB-level grant restriction.

TASK-JP-0007
Title: storage module — resume/document persistence with encryption
Dependencies: TASK-JP-0001
Affected modules: storage, security
Requirements: doc 22 §4/§5 — MIME allow-list, content-sniffing, encrypted
  storage backend, no direct file-path exposure.
Acceptance criteria: upload of a disallowed MIME type is rejected before
  reaching storage; stored files unreadable without going through
  StorageService.
Tests: security tests for upload hardening (doc 25 §8).

TASK-JP-0008
Title: CI pipeline — PR gate (lint, unit, integration, API, DB, security)
Dependencies: TASK-JP-0001..0007
Affected modules: common (build config)
Requirements: doc 33 §8 PR-gate stage.
Acceptance criteria: a PR introducing a doc 34 §3 dependency violation
  fails CI.
Tests: n/a (this task creates the test-running infrastructure itself).
```

## 3. Coverage Statement

Phases 2–13 (doc 36) follow the identical template pattern, broken out
module-by-module as each phase begins — not pre-generated in bulk here,
since task detail for, e.g., Phase 7's Browser Agent work is best written
once Phase 1–6 interfaces are real and stable rather than speculatively
detailed now against interfaces that may shift. This is a deliberate
scope boundary for this document, not an omission: doc 36 defines *what*
each phase delivers and in what order; this document's Phase 1 section
demonstrates the *task granularity* every later phase's breakdown must
match, and the pattern is applied phase-by-phase as implementation reaches
it.

## 4. Risk Notes (cross-cutting, apply to most tasks)

Every task touching `application`/`browser`/`workflow` carries the
standing risk noted in doc 25 §12 (unauthorized submission) — any task in
that surface area requires an explicit review pass against the Policy
Engine contract (doc 16 §4) before merge, not just its own unit tests.
