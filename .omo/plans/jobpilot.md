# jobpilot - Work Plan

## TL;DR (For humans)
<!-- Fill this LAST, after the detailed plan below is written, so it summarizes the REAL plan. -->
<!-- Plain English for a non-engineer: NO file paths, NO todo numbers, NO wave/agent/tool names. -->

**What you'll get:** A build plan for JobPilot — a personal job-search assistant that finds roles, prepares tailored resumes and cover letters grounded in your real career data, applies through your browser (with your approval in the careful modes), tracks every application and outcome, learns from results, and shows you analytics — delivered as 84 small, independently verifiable steps across 13 phases.

**Why this approach:** Two load-bearing decisions. (1) Nothing AI-generated about your career is ever invented: a zero-fabrication guard forces every claim in a resume, letter, or answer to trace to data you confirmed — so the output is trustworthy, not plausible-sounding. (2) Automation is safety-gated by construction: manual mode can never submit, assisted mode pauses for your confirmation, and full-auto runs only on verified sites after evidence-based verification — so the tool escalates you into automation instead of quietly acting.

**What it will NOT do:** Bypass or wait out CAPTCHAs/login walls (only detects + tells you); auto-submit in manual or assisted mode; invent career facts; touch future-tier features like email integration, hosted AI providers, or microservices in this round.

**Effort:** XL — 13 phases, 84 tasks, ~13 waves.
**Risk:** Medium — a large AI + browser-automation surface; mitigated by the policy engine, zero-fabrication guard, verification gates, and a final four-part review wave.
**Decisions to sanity-check:** (1) building the browser automation engine in-house now vs. deferring; (2) local AI models (privacy-first) vs. hosted providers later; (3) whether full-auto mode belongs in the first release.

Your next move: approve the plan, or run a high-accuracy review (dual-Momus) before handing it to the worker. Full execution detail follows below.

---

> TL;DR (machine): XL effort, Medium risk — 84 tasks in 13 dependency waves + F1–F4 review; grounded-AI job assistant (zero-fab guard), policy-gated browser automation, tracking/learning/analytics, MCP adapter; evidence per task in .omo/evidence/.

## Scope
### Must have
- All 13 roadmap phases (docs/36) as TASK-JP-XXXX todos: Phase 1 carried verbatim from docs/37 §2 (TASK-JP-0001..0008); Phases 2–13 newly broken down (TASK-JP-0009..0084, continuing doc 37's ID sequence).
- Per-task: What to do / Must NOT do, Parallelization, exhaustive References (doc:section), agent-executable Acceptance criteria, QA scenarios with tool+invocation, commit message.
- Cross-cutting controls embedded in the owning phase's tasks: auth/CSRF/audit (P1), upload hardening + adversarial fixtures (P2), SSRF-safe fetcher (P3), injection-resistant extraction (P3), evidence-grounded generation + Zero-Fabrication (P5), Policy Engine + idempotency + unauthorized-submission tests (P8), tracking/notifications/dashboard (P9), strategy/memory/learning (P10), analytics snapshots (P11), caching/rate-limits/queues/load tests (P12), MCP adapter with readiness gate (P13).
- Verification: per-task acceptance criteria are agent-executable (command + expected result); final verification wave (F1–F4) gates completion.

### Must NOT have (guardrails, anti-slop, scope boundaries)
- No implementation in this plan; plan artifacts only under `.omo/`.
- FUTURE-tier features are NOT tasks: email/response-tracking integration (docs/18 §3), interview intelligence (docs/18 §4), hosted AI providers (docs/24 §7), microservice/service extraction (docs/32 §4/§6), dedicated vector store (docs/32 §5), Research Agent (docs/16 §2).
- No speculative schema fields (docs/24 §2 data minimization); no `dangerouslySetInnerHTML`/unescaped Thymeleaf on untrusted text (docs/22 §9); no string-concatenated SQL (docs/22 §9).
- CAPTCHA/login-wall verification is NEVER solved/bypassed/waited out — only detected + escalated (docs/14 §6, docs/28 §2). No `execute script`/arbitrary-JS BrowserAction exists (docs/14 §3).
- AUTO mode never precedes ASSISTED proof (docs/36 §3); Learning System adjustments never before cold-start gate clears (docs/21 §6); MANUAL mode never calls the submit path (docs/03 invariant #4).
- No raw outbound HTTP on arbitrary input (docs/22 §9); untrusted content only ever enters AiRequest wrapped in `UntrustedContent` (docs/23 §1/§2).
- MCP never wraps `/applications/{id}/start` as an approval-bypassing side channel (docs/future/01 §3).

## Verification strategy
> Zero human intervention - all verification is agent-executed.
- Test decision: TDD + JUnit 5/AssertJ/Testcontainers/ArchUnit/MockMvc-RestAssured/Playwright (docs/26 §1 pyramid). Faked `AiService`/`EmbeddingService`/`VisionService` in unit/integration/API/workflow tests (docs/26 §3); real Ollama only in the AI eval suite (docs/27).
- Done-definition per docs/26 §6: unit tests for new logic, integration test if persistence touched, API test if endpoint added/changed, failure-mode entry + recovery test for any new failure mode.
- Evidence: .omo/evidence/task-<N>-jobpilot.<ext> (one file per task; screenshot/JSON/markdown output of the acceptance command)

## Execution strategy
### Parallel execution waves
> Target 5-8 todos per wave. Fewer than 3 (except the final) means you under-split.

Waves follow the roadmap (a dependency chain, docs/36 §2). Within a wave, tasks independent of each other run in parallel (module tests, fixture suites); tasks sharing a module's files are sequential.

- Wave 1 (Phase 1 Foundation): TASK-JP-0001..0008 (0001 → 0002 → 0003/0006/0007 parallel after deps; 0005 parallel to 0002; 0008 last, depends on all)
- Wave 2 (Phase 2 Candidate Intelligence): TASK-JP-0009..0015 (0009 → 0010 → 0011 → 0012/0013/0014 parallel after 0011; 0015 last)
- Wave 3 (Phase 3 Job Intelligence): TASK-JP-0016..0023 (0016 → 0017/0018/0019 parallel; 0020 → 0021/0022 parallel; 0023 last)
- Wave 4 (Phase 4 Matching): TASK-JP-0024..0029 (0024/0025 → 0026/0027/0028 parallel; 0029 last)
- Wave 5 (Phase 5 Application Preparation): TASK-JP-0030..0036 (0030 → 0031/0032/0033 parallel; 0034/0035 → 0036 last)
- Wave 6 (Phase 6 Application Engine MANUAL): TASK-JP-0037..0043 (0037/0038 → 0039/0040/0041/0042 parallel; 0043 last)
- Wave 7 (Phase 7 Browser Intelligence): TASK-JP-0044..0051 (0044 → 0045/0046/0047 parallel; 0048/0049/0050 parallel after; 0051 last)
- Wave 8 (Phase 8 Automation): TASK-JP-0052..0061 (0052 → 0053/0054/0055 parallel; 0056/0057/0058/0059/0060 sequential-ish; 0061 last — security gate)
- Wave 9 (Phase 9 Tracking & Notifications): TASK-JP-0062..0066 (0062/0063 parallel → 0064/0065 → 0066 last)
- Wave 10 (Phase 10 Learning & Career Strategy): TASK-JP-0067..0071 (0067/0068 → 0069/0070 parallel → 0071 last)
- Wave 11 (Phase 11 Analytics): TASK-JP-0072..0075 (0072/0073 → 0074/0075 parallel)
- Wave 12 (Phase 12 Scale Hardening): TASK-JP-0076..0081 (0076/0077/0078 parallel → 0079/0080 → 0081 last)
- Wave 13 (Phase 13 MCP): TASK-JP-0082..0084 (0082 gate → 0083/0084 parallel)
- Final verification wave: F1–F4 in parallel after ALL todos.

### Dependency matrix
| Todo | Depends on | Blocks | Can parallelize with |
| --- | --- | --- | --- |
| 0001 scaffold | — | 0002..0008 | — |
| 0002 migrations | 0001 | 0003, 0004, 0006 | 0005, 0007 |
| 0003 auth | 0002 | 0004 | 0006, 0007 |
| 0004 profile CRUD | 0003 | — | 0006, 0007 |
| 0005 ai module | 0001 | 0011, 0012, 0021, 0031..0033 | 0002..0004, 0006, 0007 |
| 0006 audit | 0002 | 0011+ (event writers) | 0003, 0004, 0005, 0007 |
| 0007 storage | 0001 | 0009, 0031 | 0002..0006 |
| 0008 CI | 0001..0007 | all later waves | — |
| 0009 resume upload | 0007 | 0010 | — |
| 0010 parsing | 0009 | 0011 | — |
| 0011 AI extraction | 0010, 0005 | 0012, 0013, 0014 | 0006 |
| 0012 skills+evidence | 0011 | 0024, 0031 | 0013, 0014 |
| 0013 synthesis+embeddings | 0011 | 0024, 0025 | 0012, 0014 |
| 0014 versioning+master | 0011 | 0031 | 0012, 0013 |
| 0015 quality+failures+eval | 0012, 0013 | 0024 | — |
| 0016 JobSource abstraction | 0008 | 0017..0019 | — |
| 0017 Greenhouse+Lever adapters | 0016 | 0020 | 0018, 0019 |
| 0018 Workable+Ashby+generic | 0016 | 0020 | 0017, 0019 |
| 0019 canonical identity+dedup | 0016 | 0020 | 0017, 0018 |
| 0020 normalization | 0017..0019 | 0021, 0022 | — |
| 0021 Job Intelligence extraction | 0020, 0005 | 0024 | 0022 |
| 0022 Company Intelligence | 0020 | 0025 | 0021 |
| 0023 scheduling+limits+SSRF | 0020 | 0029, 0076 | — |
| 0024 MatchingService | 0012, 0013, 0021 | 0026, 0027 | 0025 |
| 0025 OpportunityScore | 0022, 0024 | 0027 | — |
| 0026 MatchReason | 0024 | 0029 | 0027, 0028 |
| 0027 /matches endpoints | 0024, 0025 | 0029 | 0026, 0028 |
| 0028 match eval harness | 0024 | 0029 | 0026, 0027 |
| 0029 search/reco UI | 0026..0028 | — | — |
| 0030 ApplicationStrategy | 0024 | 0031 | — |
| 0031 resume tailoring | 0030, 0014, 0007 | 0036 | 0032, 0033 |
| 0032 cover letter | 0030 | 0036 | 0031, 0033 |
| 0033 answer engine | 0030 | 0036 | 0031, 0032 |
| 0034 RAG retrieval | 0013, 0021 | 0031..0033 | — |
| 0035 Zero-Fab enforcement | 0031..0033 | 0036, 0052 | — |
| 0036 doc-gen eval suite | 0031..0035 | 0037 | — |
| 0037 application state machine | 0027 | 0038..0043 | — |
| 0038 application CRUD | 0037 | 0039..0042 | — |
| 0039 handoff package svc | 0038, 0036 | 0040, 0041 | 0042 |
| 0040 handoff UI | 0039 | 0043 | 0041, 0042 |
| 0041 Cockpit UI | 0038, 0029 | 0043 | 0040, 0042 |
| 0042 mark-applied+outcome | 0038 | 0062, 0063 | 0040, 0041 |
| 0043 state-machine tests | 0037..0042 | 0056 | — |
| 0044 Playwright infra+isolation | 0008 | 0045..0047 | — |
| 0045 BrowserAction set+exec | 0044 | 0048..0050 | 0046 |
| 0046 Vision integration | 0044, 0005 | 0048 | 0045 |
| 0047 Form Intelligence | 0044 | 0048..0050 | 0045, 0046 |
| 0048 field resolution+dynamic | 0045..0047 | 0051, 0056 | 0049, 0050 |
| 0049 verification detection | 0045, 0047 | 0051, 0053 | 0048, 0050 |
| 0050 session persistence | 0045 | 0051, 0057 | 0048, 0049 |
| 0051 browser eval suite | 0048..0050 | 0056 | — |
| 0052 Policy Engine | 0035 | 0053..0055 | — |
| 0053 automation modes+rules | 0052 | 0056 | 0054, 0055 |
| 0054 tool registry+audit | 0052 | 0056 | 0053, 0055 |
| 0055 Workflow Engine | 0052 | 0056..0059 | 0053, 0054 |
| 0056 ASSISTED mode | 0043, 0051, 0053..0055 | 0057 | — |
| 0057 AUTO mode | 0056, 0050 | 0058..0060 | — |
| 0058 platform adapters | 0055, 0017..0018 | 0060 | 0059 |
| 0059 locking+idempotency | 0055 | 0060 | 0058 |
| 0060 e2e tests | 0057..0059 | 0061 | — |
| 0061 threat-12 security tests | 0060 | — | — |
| 0062 timeline+events | 0042 | 0066 | 0063 |
| 0063 outcome recording | 0042 | 0066 | 0062 |
| 0064 notifications | 0062 | 0066 | 0065 |
| 0065 reminder scheduling | 0064 | 0066 | — |
| 0066 dashboard CORE subset | 0062..0065 | — | — |
| 0067 CareerStrategy | 0029, 0072 | 0071 | 0068 |
| 0068 CareerMemory | 0062, 0063 | 0071 | 0067 |
| 0069 LearningSystem signals | 0068 | 0070, 0071 | — |
| 0070 adaptivity+endpoints | 0069 | 0071 | — |
| 0071 cold-start+strategy UI | 0067..0070 | — | — |
| 0072 AnalyticsSnapshot job | 0066 | 0067, 0073 | — |
| 0073 extended metrics | 0072 | 0074, 0075 | — |
| 0074 consumer wiring | 0073 | — | 0075 |
| 0075 snapshots endpoint+trends | 0073 | — | 0074 |
| 0076 caching strategy | 0023, 0013, 0021 | 0079 | 0077, 0078 |
| 0077 rate limiting | 0023 | 0079 | 0076, 0078 |
| 0078 queues+workers | 0008 | 0079 | 0076, 0077 |
| 0079 concurrency correctness | 0076..0078 | 0080 | — |
| 0080 load+perf tests | 0079 | 0081 | — |
| 0081 observability dashboards | 0080 | — | — |
| 0082 MCP readiness gate | 0061, 0051, 0036 | 0083, 0084 | — |
| 0083 MCP read tools | 0082 | — | 0084 |
| 0084 MCP write tools | 0082 | — | 0083 |

## Todos
> Implementation + Test = ONE todo. Never separate.
<!-- APPEND TASK BATCHES BELOW THIS LINE WITH edit/apply_patch - never rewrite the headers above. -->

### Wave 1 — Phase 1: Foundation (carried verbatim from docs/37 §2)

- [ ] 1. Project scaffold — Maven build, package skeleton, Spring Boot app (TASK-JP-0001)
  What to do / Must NOT do: Create doc 34 package structure with placeholder package-info.java per package; pom.xml with Spring Boot 3.x, Postgres driver, pgvector, Redis, Flyway, Playwright, Testcontainers, ArchUnit; application.yml per profile (doc 34 §6); Docker Compose (doc 33 §3). USER-EXECUTED (user does scaffolding) — plan does not block on this task's completion. Must NOT add modules beyond doc 34 §1's package list.
  Parallelization: Wave 1 | Blocked by: — | Blocks: 2..8
  References (executor has NO interview context - be exhaustive): docs/34-project-structure.md:1-47; docs/33-devops.md:5-40; docs/02-system-architecture.md §3 (module map, see docs/34 §1)
  Acceptance criteria (agent-executable): `mvn spring-boot:run` starts against Compose stack; `curl localhost:8080/actuator/health` returns UP.
  QA scenarios (name the exact tool + invocation): happy — `./mvnw spring-boot:run` + `curl` health endpoint, Evidence .omo/evidence/task-1-jobpilot.txt; failure — health returns DOWN when Postgres container stopped; ArchUnit dependency-rule test passes with empty modules.
  Commit: Y | feat(common): scaffold Spring Boot project skeleton

- [x] 2. Database migrations — identity, preferences, candidate evidence tables (TASK-JP-0002)
  What to do / Must NOT do: Flyway V1__identity.sql, V2__evidence_tables.sql per doc 04 §2.1-2.2 (users, candidate_profiles, job_preferences, resumes, resume_versions, skills, skill_evidence, experiences/educations/projects/certifications/achievements, candidate_embeddings). Must NOT add columns beyond doc 03/04 (data minimization, docs/24 §2). Additive-first migrations (docs/04 §3).
  Parallelization: Wave 1 | Blocked by: 1 | Blocks: 3, 4, 6
  References (executor has NO interview context - be exhaustive): docs/04-database-design.md:22-160; docs/03-domain-model.md:49-89; docs/26-testing.md:19-21,45-54
  Acceptance criteria (agent-executable): fresh DB migrates cleanly (`./mvnw flyway:migrate` or app boot); constraint tests pass: ux_resumes_one_master (docs/04:94-95), skill_evidence CHECK confidence 0-1 (docs/04:130).
  QA scenarios (name the exact tool + invocation): happy — migration test against fresh Testcontainers Postgres, Evidence .omo/evidence/task-2-jobpilot.txt; failure — second master resume insert violates ux_resumes_one_master.
  Commit: Y | feat(common): identity + candidate evidence migrations

- [x] 3. users/auth — registration, login, JWT issuance (TASK-JP-0003)
  What to do / Must NOT do: doc 05 §1 endpoints (register/login/logout/refresh/oauth-callback placeholder), doc 22 §1 password hashing (Argon2id/bcrypt), JWT short-lived + refresh rotation, HTTP-only+Secure+SameSite=Lax cookies for Thymeleaf frontend, CSRF per doc 22 §3. Must NOT log password hashes or tokens (docs/29 §1); must NOT store plaintext passwords.
  Parallelization: Wave 1 | Blocked by: 2 | Blocks: 4
  References (executor has NO interview context - be exhaustive): docs/05-api-specification.md:19-27; docs/22-security.md:5-13,24-30; docs/25-threat-model.md:47-55 (credential theft), 80-87 (account takeover); docs/26-testing.md:16-18
  Acceptance criteria (agent-executable): register→login→authenticated request round-trip works; API tests for all doc 05 §1 endpoints incl. error cases (duplicate email → 409); CSRF/session tests pass.
  QA scenarios (name the exact tool + invocation): happy — API test: POST /auth/register → 201, POST /auth/login → JWT, GET /candidate/profile with Bearer → 200, Evidence .omo/evidence/task-3-jobpilot.txt; failure — duplicate email → 409; wrong password → 401.
  Commit: Y | feat(auth): registration, login, JWT issuance
  DONE 2026-08-14: commit 9cd49e1, evidence .omo/evidence/task-3-jobpilot.txt, full suite 11 tests green. JWT bearer (not cookies) for API; /me returns RegisterResponse; logout permitAll; 401 via authenticationEntryPoint. Dev: PasswordConfig split from SecurityConfig (Boot 4.1 circular-ref ban), Boot 4 test import path, test-local ObjectMapper.

- [x] 4. CandidateProfile + JobPreference CRUD skeleton (TASK-JP-0004)
  What to do / Must NOT do: doc 05 §2/§3 endpoints (GET/PATCH /candidate/profile, GET/PUT /preferences), doc 22 §2 candidate-scoping enforced at repository layer (NOT only controller). Must NOT return another candidate's data under any path (docs/25 §5); domain-rule validation in service, not annotations (docs/35 §8).
  Parallelization: Wave 1 | Blocked by: 3 | Blocks: —
  References (executor has NO interview context - be exhaustive): docs/05-api-specification.md:29-55; docs/22-security.md:14-22; docs/25-threat-model.md:41-46 (data theft); docs/03-domain-model.md:55-59,91-97; docs/04-database-design.md:36-76
  Acceptance criteria (agent-executable): cross-candidate access attempt returns 404 (security test, docs/25 §5); GET/PATCH/PUT round-trips pass API tests.
  QA scenarios (name the exact tool + invocation): happy — candidate A creates profile, candidate B GET /candidate/profile/{A-id} → 404, Evidence .omo/evidence/task-4-jobpilot.txt; failure — JobPreference PUT with invalid enum → 400 VALIDATION_ERROR.
  Commit: Y | feat(candidate): profile + preferences CRUD with candidate scoping
  DONE 2026-08-15: commit 8607b2f, evidence .omo/evidence/task-4-jobpilot.txt, full suite 15 tests green. Scoping is structural (no candidate-id path param; repository resolves owner by user_id). Live dev boot validated new entities (ddl-auto:validate) — no type mismatch. Note: dev Postgres/Redis containers had exited; restart via backend/docker-compose.yml (jobpilot-postgres-1 / jobpilot-redis-1) before live boots.

- [x] 5. ai module — provider abstraction + Ollama implementation (TASK-JP-0005)
  What to do / Must NOT do: doc 06 §1 interfaces (AiService/EmbeddingService/VisionService), OllamaAiService/OllamaEmbeddingService/OllamaVisionService, ModelRouter (doc 06 §2) config-driven, AiRequest with UntrustedContent marker + output schema + budget (docs/06 §1, docs/23 §1), output validation per docs/06 §6. Must NOT let anything outside `ai` talk to a provider directly; must NOT string-concatenate untrusted content into instructions (docs/23 §2).
  Parallelization: Wave 1 | Blocked by: 1 | Blocks: 11, 12, 21, 31..33
  References (executor has NO interview context - be exhaustive): docs/06-ai-architecture.md:1-47,108-121; docs/23-ai-security.md:5-14; docs/26-testing.md:58-63 (fake AI in unit tests); docs/33-devops.md:31-33 (ollama container)
  Acceptance criteria (agent-executable): a fixture prompt round-trips through the real Ollama container in Compose and returns schema-validated output; unit tests pass with faked HTTP layer; one slow-tagged integration test against real local Ollama.
  QA scenarios (name the exact tool + invocation): happy — integration test: SKILL_CLASSIFICATION call returns StructuredResponse with schema-valid fields, Evidence .omo/evidence/task-5-jobpilot.txt; failure — Ollama down → AiUnavailableException typed failure, not silent empty.
  Commit: Y | feat(ai): provider abstraction + Ollama implementation

- [x] 6. audit module — AuditEvent persistence + append-only enforcement (TASK-JP-0006)
  What to do / Must NOT do: doc 04 §2.6 audit_events table, DB role grants (no UPDATE/DELETE for app role), AuditService.record(...) API. Must NOT allow application DB role to UPDATE/DELETE audit_events (docs/04:481, docs/22 §10); must NOT log secrets in payload (docs/29 §1).
  Parallelization: Wave 1 | Blocked by: 2 | Blocks: 11+ (event writers)
  References (executor has NO interview context - be exhaustive): docs/04-database-design.md:470-482; docs/22-security.md:95-101; docs/03-domain-model.md:223-225; docs/29-observability.md:36-45 (audit vs logs vs metrics)
  Acceptance criteria (agent-executable): attempted UPDATE/DELETE via the app DB role fails at the DB level (integration test); AuditService.record round-trips.
  QA scenarios (name the exact tool + invocation): happy — integration test: record() then SELECT works, UPDATE via app role throws, Evidence .omo/evidence/task-6-jobpilot.txt; failure — payload containing secret-shaped key is redacted by wrapper (docs/29 §1).
  Commit: Y | feat(audit): append-only audit event store
  DONE 2026-08-15: implemented (not yet committed). Files: V4__audit.sql (table + ix_audit_entity + trg_audit_events_append_only BEFORE UPDATE/DELETE trigger), audit.domain.{ActorType,AuditEvent}, audit.repository.AuditEventRepository, audit.api.{AuditService,AuditController(AuditDtos)}, audit.service.AuditServiceImpl (secret-key scan rejects password/token/secret/etc in payload, doc 29 §1). GET /api/v1/settings/audit scoped to principal (doc 05 §9, doc 22 §10). Tests: AuditApiTest (3: own-trail scoping, unauth 401, append-only trigger blocks UPDATE/DELETE via JdbcTemplate), AuditServiceImplTest (4: secret guard), ModuleBoundaryTest still green. Full suite 29 run/0 fail/1 skip (Ollama slow test); the 1 error in the full run was a Testcontainers container-startup flake (MigrationConstraintTest passes in isolation). Dev boot (ddl-auto:validate) applied V4 + started clean on :8080. NOTE: append-only enforced via DB trigger (not role revocation) because the app connects as the schema owner in dev/test, where GRANT revocation would not stop the owner — trigger holds for any role.

- [x] 7. storage module — resume/document persistence with encryption (TASK-JP-0007)
  What to do / Must NOT do: doc 22 §4/§5 — MIME allow-list (PDF/DOCX) enforced by content-sniffing (not extension), size limits, storage outside web root, access only via StorageService, encrypted at rest. Must NOT expose direct file paths via URLs; must NOT accept disallowed MIME types.
  Parallelization: Wave 1 | Blocked by: 1 | Blocks: 9, 31
  References (executor has NO interview context - be exhaustive): docs/22-security.md:32-49; docs/25-threat-model.md:8-15 (malicious resume), 62-68 (malicious upload); docs/26-testing.md:30-34 (security tests)
  Acceptance criteria (agent-executable): upload of a disallowed MIME type is rejected before reaching storage; stored files unreadable without going through StorageService.
  QA scenarios (name the exact tool + invocation): happy — upload PDF → stored, retrieve via StorageService, Evidence .omo/evidence/task-7-jobpilot.txt; failure — renamed .exe→.pdf rejected by content-sniffing (docs/25 §8 test).
  Commit: Y | feat(storage): hardened encrypted document storage
  DONE 2026-08-15: implemented (not yet committed). Files: storage.api.{StorageService,StorageDtos}, storage.service.EncryptedFilesystemStorageService. Hardening: MIME allow-list (PDF/DOCX) by content-sniffing (PDF magic %PDF; DOCX = ZIP magic + [Content_Types].xml), size limit (default 10MB, configurable), AES/GCM encryption at rest (key from jobpilot.storage.encryption-key passphrase → SHA-256 → 32-byte key; IV per file; stored as IV||ciphertext), storage outside web root (jobpilot.storage.root, default ./jobpilot-storage, gitignored), opaque owner/uuid ref (no web path), path-traversal guard. Rejects (IllegalArgumentException) bad MIME / oversize before bytes hit disk. No own DB table — returns opaque storageRef for the future resume module (doc 04 resumes.storage_ref) to persist. Tests: EncryptedFilesystemStorageServiceTest (8: PDF/DOCX store+retrieve, on-disk encrypted-not-plaintext, disallowed MIME rejected, renamed .exe→.pdf rejected by sniffing, oversize rejected, delete, traversal ref rejected), ModuleBoundaryTest green. Full suite 37 run/0 fail/1 skip; the 1 error in full run was a Testcontainers container-startup flake (AuditApiTest passes in isolation).

- [x] 8. CI pipeline — PR gate (lint, unit, integration, API, DB, security) (TASK-JP-0008)
  What to do / Must NOT do: doc 33 §8 PR-gate stage (lint + unit + integration + API + database + security tests on PR; workflow/browser/E2E/AI-eval/browser-eval on merge-to-main + nightly), doc 33 §5 migrations as explicit deploy step in staging/prod. Must NOT gate every PR on slow AI/browser suites (docs/26 §5).
  Parallelization: Wave 1 | Blocked by: 1..7 | Blocks: all later waves
  References (executor has NO interview context - be exhaustive): docs/33-devops.md:71-99; docs/26-testing.md:72-79; docs/34-project-structure.md:83-88 (test layout, eval/fixtures)
  Acceptance criteria (agent-executable): a PR introducing a doc 34 §3 dependency violation fails CI (ArchUnit in pipeline).
  QA scenarios (name the exact tool + invocation): happy — CI green on clean PR, Evidence .omo/evidence/task-8-jobpilot.txt; failure — intentional forbidden import fails the build.
  Commit: Y | ci: PR gate pipeline (lint/unit/integration/api/db/security)

### Wave 2 — Phase 2: Candidate Intelligence (docs/07)

- [x] 9. Resume upload endpoint + storage integration (TASK-JP-0009)
  What to do / Must NOT do: POST /candidate/resumes (multipart, 202, async parse), GET /candidate/resumes, GET /candidate/resumes/{id}, DELETE /candidate/resumes/{id} (409 if isMaster and no replacement), POST /candidate/resumes/{id}/set-master (docs/05 §2). Route through StorageService (docs/22 §4): MIME allow-list PDF/DOCX by content-sniffing, size limit, parse_status PENDING on insert. Must NOT store outside StorageService; must NOT auto-promote is_master on upload (docs/07 §8).
  Parallelization: Wave 2 | Blocked by: 7 | Blocks: 10
  References (executor has NO interview context - be exhaustive): docs/05-api-specification.md:33-39; docs/22-security.md:32-49; docs/07-candidate-intelligence.md:12-38,84-94; docs/03-domain-model.md:61-64; docs/04-database-design.md:82-95
  Acceptance criteria (agent-executable): API tests: multipart upload → 202 + parse_status PENDING; list/detail/delete/set-master round-trips; disallowed MIME rejected 400.
  QA scenarios (name the exact tool + invocation): happy — curl -F resume=@fixture.pdf → 202, Evidence .omo/evidence/task-9-jobpilot.txt; failure — .txt upload → 400; DELETE master without replacement → 409.
  Commit: Y | feat(candidate): resume upload + storage integration

- [ ] 10. Resume parsing — PDF (text-layer + OCR fallback) and DOCX (structured XML) (TASK-JP-0010)
  What to do / Must NOT do: ResumeParsingService per docs/07 §2: PDF text-layer extraction first, OCR fallback for scanned PDFs; DOCX structured XML parsing; parse failures set parse_status=FAILED with user-facing reason (docs/07:36-38). Zip-bomb/XXE protections for DOCX (docs/22:39-41). Must NOT silently produce an empty profile; must NOT attempt OCR on clean text PDFs first.
  Parallelization: Wave 2 | Blocked by: 9 | Blocks: 11
  References (executor has NO interview context - be exhaustive): docs/07-candidate-intelligence.md:14-19,36-38; docs/22-security.md:39-41; docs/26-testing.md:45-47 (fixture resumes); docs/30-error-handling.md (doc 30 §1 taxonomy); docs/07 §9 failure table
  Acceptance criteria (agent-executable): fixture resumes parse: clean text-PDF, scanned/OCR PDF, DOCX with tables → structured text; corrupt file → parse_status=FAILED + reason.
  QA scenarios (name the exact tool + invocation): happy — unit test parses each doc 26 §2 resume fixture, Evidence .omo/evidence/task-10-jobpilot.txt; failure — zip-bomb DOCX rejected without OOM; unreadable file → FAILED.
  Commit: Y | feat(candidate): PDF/DOCX resume parsing with OCR fallback

- [ ] 11. AI extraction pipeline — experiences/education/projects/certifications/achievements (TASK-JP-0011)
  What to do / Must NOT do: AiService task=SIMPLE_EXTRACTION then RESUME_REASONING extracts structured candidates each tagged rawSourceExcerpt; schema validation (docs/06 §6); UntrustedContent wrapping of resume text (docs/23 §1). Parse failures set FAILED + audit event ResumeParsingFailed (docs/07:102). Must NOT fabricate entities absent from the document; must NOT pass resume text as trusted instruction.
  Parallelization: Wave 2 | Blocked by: 10, 5 | Blocks: 12, 13, 14
  References (executor has NO interview context - be exhaustive): docs/07-candidate-intelligence.md:20-23,96-103; docs/06-ai-architecture.md:20-24,108-121; docs/23-ai-security.md:15-22; docs/03-domain-model.md:84-87; docs/25-threat-model.md:8-15
  Acceptance criteria (agent-executable): extraction integration test (real Ollama, slow-tagged) on fixture resume yields entities with non-empty rawSourceExcerpt; adversarial resume (embedded instructions, docs/07:109-110) produces no injected behavior.
  QA scenarios (name the exact tool + invocation): happy — fixture resume → structured entities incl. overlapping-roles resume, Evidence .omo/evidence/task-11-jobpilot.txt; failure — schema-invalid after retry → FAILED + audit event, no partial profile.
  Commit: Y | feat(candidate): evidence extraction pipeline

- [ ] 12. Skill extraction + normalization + SkillEvidence (TASK-JP-0012)
  What to do / Must NOT do: SkillExtractionService derives Skill rows from extracted entities, each linked to ≥1 SkillEvidence (docs/07:24-26); SkillNormalizationService maps raw strings to normalized taxonomy ("ReactJS"/"React.js"/"React" → "react", docs/07:27-28); bare skills-list gets lower-confidence evidence (docs/07:52-57). Must NOT create Skills without SkillEvidence; must NOT mark "listed, unverified" skills as demonstrated (docs/07:56-57).
  Parallelization: Wave 2 | Blocked by: 11 | Blocks: 24, 31
  References (executor has NO interview context - be exhaustive): docs/07-candidate-intelligence.md:24-28,49-57; docs/03-domain-model.md:73-82; docs/04-database-design.md:111-132; docs/09-job-matching.md:46-60 (skill-based technicalMatch); docs/23-ai-security.md:51-66
  Acceptance criteria (agent-executable): normalization unit tests (ReactJS→react etc.); every extracted Skill has ≥1 SkillEvidence row; skill-precision/recall eval fixture runnable (docs/27 §1).
  QA scenarios (name the exact tool + invocation): happy — fixture resume → skills with evidence, Evidence .omo/evidence/task-12-jobpilot.txt; failure — skill with no evidence → rejected by invariant test.
  Commit: Y | feat(candidate): skill extraction + normalization + evidence

- [ ] 13. Profile synthesis + embeddings (TASK-JP-0013)
  What to do / Must NOT do: ProfileSynthesisService computes domainClassification, seniorityEstimate, careerTrajectorySummary, strengths, weaknesses, skillGaps, transferableSkills (docs/07:29-32); seniority: heuristic pre-pass then RESUME_REASONING refinement, deviations >1 band must be explained (docs/07:59-66); CandidateEmbedding via EmbeddingService (docs/07:32, docs/06 §4 chunking). Career trajectory (docs/07 §6) reads ordered Experience rows. Must NOT fill derived fields without evidence citations (docs/07:63-64).
  Parallelization: Wave 2 | Blocked by: 11 | Blocks: 24, 25, 34
  References (executor has NO interview context - be exhaustive): docs/07-candidate-intelligence.md:29-33,59-74; docs/06-ai-architecture.md:73-99 (RAG + embedding); docs/03-domain-model.md:55-59,88-90; docs/04-database-design.md:151-159; docs/10-career-strategy.md:12-17 (consumers)
  Acceptance criteria (agent-executable): synthesis produces all derived fields with EvidenceRef-backed citations; embedding row persisted for fixture profile; deviation-explanation path covered by unit test.
  QA scenarios (name the exact tool + invocation): happy — fixture profile → derived fields + embedding, Evidence .omo/evidence/task-13-jobpilot.txt; failure — model claims seniority 3 bands above heuristic without explanation → rejected.
  Commit: Y | feat(candidate): profile synthesis + candidate embeddings

- [ ] 14. Resume versioning + Master Resume snapshot (TASK-JP-0014)
  What to do / Must NOT do: ResumeVersion(isMasterSnapshot=true) persisted as Master Resume after parse (docs/07:33); versioning rules: upload never auto-overwrites is_master, set-master promotes explicitly (docs/07:86-88); re-parse creates new snapshot preserving history (docs/07:89-91); generationReason recorded (docs/07:93-94). Invariant #1: jobId≠null version must reference matching Application (docs/03:236-238). Must NOT mutate the original Resume file (docs/07:16,64-65).
  Parallelization: Wave 2 | Blocked by: 11 | Blocks: 31
  References (executor has NO interview context - be exhaustive): docs/07-candidate-intelligence.md:33-34,84-94; docs/03-domain-model.md:66-71,236-238; docs/04-database-design.md:97-109; docs/12-application-documents.md:8-21 (tailoring branches from Master)
  Acceptance criteria (agent-executable): exactly one is_master=true per candidate (constraint test); set-master promotes + demotes atomically; re-parse creates new isMasterSnapshot row.
  QA scenarios (name the exact tool + invocation): happy — upload → parse → set-master → list versions shows master snapshot, Evidence .omo/evidence/task-14-jobpilot.txt; failure — second master insert → ux_resumes_one_master violation.
  Commit: Y | feat(candidate): resume versioning + master snapshot

- [ ] 15. Resume quality analysis + failure handling + eval harness (TASK-JP-0015)
  What to do / Must NOT do: quality pass (docs/07 §3): missing dates, unquantified achievements, inconsistent formatting, missing contact info, evidence-less skill claims → user-facing suggestions, never auto-applied; failure table (docs/07 §9): unreadable → FAILED, OCR low-confidence → flag, zero-skills → incomplete not placeholder; eval harness for resume-extraction accuracy + skill precision/recall per docs/27 §1/§2/§3 with adversarial fixtures (docs/07:109-110). Must NOT auto-edit the resume; must NOT fabricate placeholders (docs/07:103).
  Parallelization: Wave 2 | Blocked by: 12, 13 | Blocks: 24
  References (executor has NO interview context - be exhaustive): docs/07-candidate-intelligence.md:40-47,96-110; docs/27-ai-evaluation.md:6-29; docs/23-ai-security.md:88-93 (adversarial fixtures); docs/30-error-handling.md (recovery tests); docs/26-testing.md:45-54
  Acceptance criteria (agent-executable): eval harness runs nightly-tagged suite: resume-extraction accuracy + skill precision/recall against hand-labeled fixtures with tolerance bands; quality-analysis unit tests pass.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=ai-eval on fixture set reports accuracy ≥ band, Evidence .omo/evidence/task-15-jobpilot.txt; failure — injection resume scores 0 injected-behavior (docs/27 prompt-injection resistance).
  Commit: Y | feat(candidate): quality analysis + eval harness

### Wave 3 — Phase 3: Job Discovery (docs/08)

- [ ] 16. JobSource abstraction + RawJobPosting (TASK-JP-0016)
  What to do / Must NOT do: `JobSource` interface per docs/08 §1 (metadata(), fetchSince(Instant), capabilityFor(RawJobPosting)); `RawJobPosting` is source-native shape; JobSourceMetadata (docs/08 §2) declares capability (`AUTO_CAPABLE`/`ASSISTED_CAPABLE`/`MANUAL_ONLY`), auth requirements, rate-limit policy. Must NOT expose RawJobPosting to any module downstream of discovery; must NOT assume another adapter's capability exists.
  Parallelization: Wave 3 | Blocked by: 8 | Blocks: 17, 18, 19
  References (executor has NO interview context - be exhaustive): docs/08-job-discovery.md:6-19,21-34; docs/03-domain-model.md (JobSource entity fields); docs/04-database-design.md:180-194 (job_sources table); docs/05-api-specification.md:63-64 (job-sources endpoints)
  Acceptance criteria (agent-executable): unit test: a stub adapter round-trips through the JobSource contract; JobSourceMetadata declares capability/auth/rate-policy; architecture test: no module outside `jobs` imports RawJobPosting.
  QA scenarios (name the exact tool + invocation): happy — stub adapter fetchSince() streams RawJobPosting, Evidence .omo/evidence/task-16-jobpilot.txt; failure — JobSourceException propagates typed, no partial state persisted.
  Commit: Y | feat(jobs): JobSource abstraction + RawJobPosting

- [ ] 17. ATS adapters — Greenhouse + Lever (TASK-JP-0017)
  What to do / Must NOT do: `GreenhouseSource`, `LeverSource` per docs/08 §2 ATS row: each implements JobSource, declares its own JobSourceMetadata (capability/auth/rate-limit policy), maps source API responses to RawJobPosting. Credentials via secrets manager (docs/22 §7), never in code or logs (docs/29 §1). Must NOT retry 429 with shortened backoff (docs/08 §6); must NOT bypass login walls or solve CAPTCHAs (docs/08 §8).
  Parallelization: Wave 3 | Blocked by: 16 | Blocks: 20
  References (executor has NO interview context - be exhaustive): docs/08-job-discovery.md:21-34,83-100; docs/22-security.md (secrets handling); docs/26-testing.md:30-34 (security tests); docs/33-devops.md (secrets config)
  Acceptance criteria (agent-executable): adapter unit tests with mocked HTTP (WireMock) map a fixture API payload → RawJobPosting; metadata capability values correct; rate-limit backoff test: 429 → next attempt waits longer, never shorter.
  QA scenarios (name the exact tool + invocation): happy — WireMock fixture fetchSince() returns N postings, Evidence .omo/evidence/task-17-jobpilot.txt; failure — 429 mid-run halts remaining requests (docs/08 §7), resumes next window.
  Commit: Y | feat(jobs): Greenhouse + Lever ATS adapters

- [ ] 18. ATS adapters — Workable + Ashby + generic page/feed (TASK-JP-0018)
  What to do / Must NOT do: `WorkableSource`, `AshbySource`, `GenericCareerPageSource` (structured-data/sitemap based), `GenericJobFeedSource` per docs/08 §2. Career-page fetcher must be SSRF-safe (docs/22 §9): only allow-listed domains/schemes, no raw outbound HTTP on arbitrary input. Must NOT build bespoke scrapers where structured data exists (docs/08 §2); must NOT violate source terms (docs/08 §8).
  Parallelization: Wave 3 | Blocked by: 16 | Blocks: 20
  References (executor has NO interview context - be exhaustive): docs/08-job-discovery.md:23-27; docs/22-security.md:32-49 (SSRF-safe fetcher); docs/25-threat-model.md:8-15 (malicious source); docs/26-testing.md:30-34
  Acceptance criteria (agent-executable): WireMock/mocked-HTTP tests for Workable/Ashby/GenericPage/GenericFeed map fixtures → RawJobPosting; SSRF test: fetch of a private-range URL is rejected.
  QA scenarios (name the exact tool + invocation): happy — each adapter parses its fixture payload, Evidence .omo/evidence/task-18-jobpilot.txt; failure — career-page fetch targeting 169.254.169.254 blocked (docs/25 §8 SSRF test).
  Commit: Y | feat(jobs): Workable + Ashby + generic adapters

- [ ] 19. Canonical identity + dedup (TASK-JP-0019)
  What to do / Must NOT do: `CanonicalIdentityService` per docs/08 §4: hash(normalizedTitle + normalizedCompany + normalizedLocation + sourcePostingId-or-URL-fingerprint); dedup check against jobs.canonical_identity unique index (docs/04:227); re-seen posting updates existing Job, never a duplicate row. Must NOT create a second Job row for the same canonical identity (docs/08 §5 belt-and-suspenders with ux_applications_active_per_job).
  Parallelization: Wave 3 | Blocked by: 16 | Blocks: 20
  References (executor has NO interview context - be exhaustive): docs/08-job-discovery.md:48-52,74-81; docs/04-database-design.md:196-230 (jobs table, ux_jobs_canonical_identity); docs/03-domain-model.md (Job entity); docs/26-testing.md:19-21
  Acceptance criteria (agent-executable): unit test: same posting via two sources → one Job row (second is update); duplicate-insert attempt violates ux_jobs_canonical_identity.
  QA scenarios (name the exact tool + invocation): happy — two fetches of same posting → 1 Job row, Evidence .omo/evidence/task-19-jobpilot.txt; failure — direct duplicate canonical_identity insert → unique violation.
  Commit: Y | feat(jobs): canonical identity + dedup

- [ ] 20. Job normalization pipeline (TASK-JP-0020)
  What to do / Must NOT do: field normalization per docs/08 §4: title → normalized_title (casing, seniority-token stripped for grouping, preserved in title), company → resolved/created Company row (normalized_name, ux_companies_normalized_name), location → normalized_location (city/region/country, remote flag), salary → currency-normalized range, employment → enum (FULL_TIME/PART_TIME/CONTRACT/INTERN), seniority → enum, technology → normalized against the doc 07 skill taxonomy. Unresolvable fields → Job persisted with `potentialConcerns` ("normalization_incomplete"), never dropped (docs/08 §4). Must NOT guess company/location when evidence is malformed.
  Parallelization: Wave 3 | Blocked by: 17, 18, 19 | Blocks: 21, 22
  References (executor has NO interview context - be exhaustive): docs/08-job-discovery.md:46-72; docs/04-database-design.md:165-178,196-230; docs/07-candidate-intelligence.md:27-28 (skill taxonomy); docs/09-job-matching.md:6-27 (consumes normalized fields); docs/26-testing.md:19-21
  Acceptance criteria (agent-executable): unit tests: seniority-token stripping, salary currency normalization, location city/region/remote flag, employment enum mapping, technology taxonomy mapping; malformed input → persisted with potentialConcerns entry.
  QA scenarios (name the exact tool + invocation): happy — fixture RawJobPosting → fully normalized Job, Evidence .omo/evidence/task-20-jobpilot.txt; failure — unknown location → Job with normalization_incomplete concern, matching proceeds at lower confidence.
  Commit: Y | feat(jobs): normalization pipeline

- [ ] 21. Job Intelligence extraction + JobEmbedding (TASK-JP-0021)
  What to do / Must NOT do: AiService task=JOB_ANALYSIS (docs/09 §1) on raw description text wrapped as UntrustedContent (docs/23 §1): extracts responsibilities[], requiredSkills[] vs preferredSkills[] (explicit "required/must-have" vs "nice to have" language), experienceRequirement, educationRequirement, potentialConcerns (informational — never auto-exclude); JobRequirement rows persisted (isRequired, extractedEntity where resolvable); JobEmbedding via EmbeddingService (docs/06 §4). Must NOT treat all listed skills as equally required (docs/09 §1); must NOT string-concatenate untrusted text into instructions (docs/23 §2).
  Parallelization: Wave 3 | Blocked by: 20, 5 | Blocks: 24
  References (executor has NO interview context - be exhaustive): docs/09-job-matching.md:6-27; docs/06-ai-architecture.md:20-24,73-99 (JOB_ANALYSIS, embedding); docs/23-ai-security.md:15-22; docs/04-database-design.md:232-249 (job_requirements, job_embeddings); docs/05-api-specification.md:59-62 (jobs endpoints)
  Acceptance criteria (agent-executable): extraction integration test (real Ollama, slow-tagged) on a fixture job description yields required/preferred separation + JobRequirement rows with isRequired set; JobEmbedding persisted; adversarial job description (embedded instructions) produces no injected behavior.
  QA scenarios (name the exact tool + invocation): happy — fixture JD → JobRequirement rows + embedding, Evidence .omo/evidence/task-21-jobpilot.txt; failure — AI extraction fails → Job still matchable on normalized fields only, potentialConcerns flagged (docs/09 §7).
  Commit: Y | feat(jobs): Job Intelligence extraction + embeddings

- [ ] 22. Company Intelligence enrichment (TASK-JP-0022)
  What to do / Must NOT do: `CompanyEnrichmentService` (docs/09 §2) populates Company fields ONLY from sources recorded in `sourced_from` (career page, company's own materials, the posting); never infers unstated facts (e.g., size) from LLM general knowledge; unsourced field stays NULL; re-enrichment on long interval + eager trigger on first live match; CompanyEmbedding via EmbeddingService (docs/04:251-257). Must NOT fill a field the source doesn't state (docs/09 §2).
  Parallelization: Wave 3 | Blocked by: 20 | Blocks: 25
  References (executor has NO interview context - be exhaustive): docs/09-job-matching.md:29-38; docs/04-database-design.md:165-178,251-257; docs/06-ai-architecture.md:20-24,73-99; docs/05-api-specification.md:61 (GET /jobs/{id}/company)
  Acceptance criteria (agent-executable): enrichment unit test: facts present in sourced material populate, absent facts stay NULL; no-fabrication test: model claims unsourced size → field stays null.
  QA scenarios (name the exact tool + invocation): happy — fixture company page → enriched Company row with sourced_from recorded, Evidence .omo/evidence/task-22-jobpilot.txt; failure — enrichment unavailable → matching proceeds without careerValue company input (docs/09 §7).
  Commit: Y | feat(company): evidence-grounded enrichment

- [ ] 23. Scheduling + rate limits + SSRF-safe fetch (TASK-JP-0023)
  What to do / Must NOT do: per-source sync schedules respecting rateLimitPolicyRef (docs/08 §6); shared Redis-backed token-bucket RateLimiterService enforcing per-source concurrency + request ceilings; 429/rate-limit backoff only increases; on-demand discovery (POST /jobs/discover, docs/05:62) rate-limited per user; failure table behavior per docs/08 §7 (source unreachable → JobSourceException + lastSuccessfulSyncAt unchanged; malformed posting skipped + audit; auth expired → inactive-pending-reauth). SSRF-safe fetch layer: allow-list schemes/domains, no raw outbound HTTP on arbitrary input (docs/22 §9). Must NOT bypass CAPTCHAs or login walls (docs/08 §8, docs/14 §6).
  Parallelization: Wave 3 | Blocked by: 20 | Blocks: 29, 76
  References (executor has NO interview context - be exhaustive): docs/08-job-discovery.md:83-100; docs/22-security.md:32-49; docs/25-threat-model.md:8-15,62-68; docs/33-devops.md (schedules); docs/05-api-specification.md:62; docs/29-observability.md (sync metrics)
  Acceptance criteria (agent-executable): rate-limit integration test: burst of requests → 429 beyond ceiling, backoff window increases; SSRF test: private-range/loopback target rejected; failure-table tests for unreachable/malformed/auth-expired sources.
  QA scenarios (name the exact tool + invocation): happy — scheduled sync on fixture source completes within limits, Evidence .omo/evidence/task-23-jobpilot.txt; failure — source returns 429 → remaining requests halted, next window resumes (docs/08 §7).
  Commit: Y | feat(jobs): scheduling + rate limits + SSRF-safe fetch

### Wave 4 — Phase 4: Matching (docs/09)

- [ ] 24. Hybrid MatchingService (TASK-JP-0024)
  What to do / Must NOT do: `MatchingService.computeMatch(candidateProfileId, jobId)` per docs/09 §3: independently computed sub-scores — technicalMatch (Skill ↔ JobRequirement SKILL overlap weighted by isRequired + SkillEvidence confidence, docs/09:48), experienceMatch, projectMatch (embedding similarity + explicit tech overlap), educationMatch, preferenceMatch (deterministic rule eval, not AI), careerValue (CareerStrategy alignment, docs/10), applicationDifficulty (inverse of applicationMethod + question-complexity signals from doc 21), overallMatch = weighted combination with configurable per-experiment weights. semanticSimilarity is one input, NOT the whole engine (docs/09:57-60). Must NOT persist a match on compute error — never a fabricated 0% or 100% (docs/09 §7).
  Parallelization: Wave 4 | Blocked by: 12, 13, 21 | Blocks: 26, 27
  References (executor has NO interview context - be exhaustive): docs/09-job-matching.md:40-60; docs/04-database-design.md:263-280 (job_matches); docs/07-candidate-intelligence.md:24-28 (SkillEvidence); docs/10-career-strategy.md:12-17; docs/03-domain-model.md (JobMatch entity)
  Acceptance criteria (agent-executable): unit tests: each sub-score computed from its documented inputs; weights configurable per experiment; compute error → no JobMatch row persisted; incremental recompute paths (docs/09 §6) don't re-run AI.
  QA scenarios (name the exact tool + invocation): happy — fixture candidate+job pair → all sub-scores + overallMatch persisted, Evidence .omo/evidence/task-24-jobpilot.txt; failure — embedding service down → matching proceeds with remaining sub-scores, no fabricated number.
  Commit: Y | feat(matching): hybrid MatchingService

- [ ] 25. OpportunityScore ranking function (TASK-JP-0025)
  What to do / Must NOT do: `OpportunityScore` computed separately from overallMatch per docs/09 §5: documented, versioned function f(overallMatch, successProbability [from doc 21 response-rate history, prior when no history], careerValue, preferenceAlignment, applicationDifficulty). Must NOT silently reorder on factors the user hasn't opted into (docs/09:89-92); must be explainable + testable (docs/26), not opaque model output.
  Parallelization: Wave 4 | Blocked by: 22, 24 | Blocks: 27
  References (executor has NO interview context - be exhaustive): docs/09-job-matching.md:71-92; docs/04-database-design.md:263-280 (opportunity_score column, ix_job_matches_opportunity); docs/10-career-strategy.md:59-67; docs/26-testing.md (versioned scoring tests)
  Acceptance criteria (agent-executable): unit tests: function is versioned (model_version recorded), prior used when no history, easier+strong match ranks above harder+equal match; ranking change from a version bump is testable.
  QA scenarios (name the exact tool + invocation): happy — fixture pair → opportunityScore persisted with model_version, Evidence .omo/evidence/task-25-jobpilot.txt; failure — no history → successProbability defaults to documented prior, score still computed.
  Commit: Y | feat(matching): OpportunityScore ranking function

- [ ] 26. MatchReason explainability (TASK-JP-0026)
  What to do / Must NOT do: MatchReason rows in three categories per docs/09 §4: STRONG (clearly met), MODERATE (partial/indirect), MISSING (required + absent); each reason references the specific Skill/JobRequirement pair — NOT a generic template string; UI renders ✓/◐/⚠ directly off stored data (docs/09:62-69). Must NOT fabricate reasons not backed by computed factors.
  Parallelization: Wave 4 | Blocked by: 24 | Blocks: 29
  References (executor has NO interview context - be exhaustive): docs/09-job-matching.md:62-69; docs/04-database-design.md:282-288 (match_reasons); docs/01-product-requirements.md §5.8 (example format)
  Acceptance criteria (agent-executable): unit test: computeMatch produces STRONG/MODERATE/MISSING rows referencing real Skill↔JobRequirement pairs; rendering test: stored reasons → ✓/◐/⚠ output.
  QA scenarios (name the exact tool + invocation): happy — fixture pair → match_reasons rows, Evidence .omo/evidence/task-26-jobpilot.txt; failure — MISSING reason has no matching JobRequirement row → invariant test fails.
  Commit: Y | feat(matching): MatchReason explainability

- [ ] 27. /matches endpoints (TASK-JP-0027)
  What to do / Must NOT do: GET /matches (ranked by opportunityScore), GET /matches/{jobId} (JobMatch + MatchReason[]), POST /matches/recompute (force recompute, rate-limited, docs/05:72) per docs/05 §5; candidate-scoping enforced at service/repository layer, not only controller (docs/22 §2, docs/05 §11). Recompute triggers per docs/09 §6 (new job, profile change, preference change, explicit request); incremental where possible. Must NOT return another candidate's matches under any path (docs/25 §5).
  Parallelization: Wave 4 | Blocked by: 24, 25 | Blocks: 29
  References (executor has NO interview context - be exhaustive): docs/05-api-specification.md:66-76; docs/22-security.md:14-22; docs/25-threat-model.md:41-46 (data theft); docs/09-job-matching.md:94-101 (recompute triggers); docs/04-database-design.md:263-288
  Acceptance criteria (agent-executable): API tests: GET /matches returns ranked list sorted by opportunityScore; GET /matches/{jobId} returns match + reasons; POST /matches/recompute honored; cross-candidate access → 404 (docs/25 §5 test).
  QA scenarios (name the exact tool + invocation): happy — candidate A sees own matches, Evidence .omo/evidence/task-27-jobpilot.txt; failure — candidate B GET /matches/{A-jobId} → 404 NOT_FOUND.
  Commit: Y | feat(matching): /matches endpoints with candidate scoping

- [ ] 28. Match eval harness (TASK-JP-0028)
  What to do / Must NOT do: benchmark job/candidate pairs with hand-labeled expected match bands per docs/09 §8; matchPrecision/matchRecall computed per docs/27; adversarial job descriptions with embedded instructions join the same prompt-injection resistance suite as docs/07's adversarial resume (docs/23). Must NOT evaluate against self-generated labels (docs/27 methodology).
  Parallelization: Wave 4 | Blocked by: 24 | Blocks: 29
  References (executor has NO interview context - be exhaustive): docs/09-job-matching.md:111-116; docs/27-ai-evaluation.md:6-29; docs/23-ai-security.md:88-93; docs/26-testing.md:45-54 (fixtures)
  Acceptance criteria (agent-executable): eval suite (nightly-tagged) runs hand-labeled fixture pairs and reports matchPrecision/matchRecall with tolerance bands; injection fixture scores 0 injected-behavior.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=ai-eval on match fixtures → bands met, Evidence .omo/evidence/task-28-jobpilot.txt; failure — injection JD → 0 injected-behavior (docs/27).
  Commit: Y | feat(matching): match eval harness

- [ ] 29. Search + ranked recommendations UI (TASK-JP-0029)
  What to do / Must NOT do: Thymeleaf UI for GET /jobs search/list (filters: role, location, salary, source, status, docs/05:59), job detail (Job + JobRequirement list, docs/05:60), ranked matches view sorted by opportunityScore rendering ✓/◐/⚠ from stored MatchReason data (docs/09 §4); recommendations surface careers page. Escape all untrusted text — no unescaped Thymeleaf on job/company content (docs/22 §9).
  Parallelization: Wave 4 | Blocked by: 26, 27, 28 | Blocks: 41, 67
  References (executor has NO interview context - be exhaustive): docs/05-api-specification.md:55-64; docs/09-job-matching.md:62-69; docs/34-project-structure.md (web module); docs/22-security.md:9-13 (output escaping); docs/10-career-strategy.md:69-74 (strategy-informed surfacing)
  Acceptance criteria (agent-executable): Playwright smoke test: search filters work, job detail renders JobRequirements, matches page renders ✓/◐/⚠ per stored reasons; XSS test: job with embedded script text renders escaped.
  QA scenarios (name the exact tool + invocation): happy — Playwright: filter + detail + matches flows, Evidence .omo/evidence/task-29-jobpilot.txt; failure — job title containing <script> renders as text, not executed.
  Commit: Y | feat(jobs): search + ranked recommendations UI

### Wave 5 — Phase 5: Application Preparation (docs/11, docs/12)

- [ ] 30. Application strategy service + plan model (TASK-JP-0030)
  What to do / Must NOT do: ApplicationStrategyService producing a per-job ApplicationPlan (company research, resume targeting brief, cover letter brief, interview questions) persisted with job + profile snapshot. Must NOT call AI without job context; must not fabricate company facts (Zero-Fab docs/23).
  Parallelization: Wave 5 | Blocked by: 24 | Blocks: 31
  References (executor has NO interview context - be exhaustive): docs/11-application-engine.md:6-30; docs/34-project-structure.md (application/strategy sub-package); docs/06-ai-architecture.md:40-69 (AiTaskType table, cost/cache); docs/23-ai-security.md:51-70; docs/03-domain-model.md (Job, Candidate)
  Acceptance criteria (agent-executable): unit test: strategy service returns complete plan for a matched job; plan persisted with jobId + profileVersion; missing job context returns typed error without AI call.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=ai-eval strategy tests, Evidence .omo/evidence/task-30-jobpilot.txt; failure — no job context → typed error, no LLM call (docs/23).
  Commit: Y | feat(strategy): application strategy service + plan model

- [ ] 31. Resume tailoring (TASK-JP-0031)
  What to do / Must NOT do: tailored resume via AiTaskType.RESUME_TAILORING from the plan's targeting brief: keyword scoring against job, JSON change-diff (section changed + why) for review. Must NOT invent/modify experience facts — only emphasis, reordering, keyword alignment (docs/12 §2, Zero-Fab docs/23).
  Parallelization: Wave 5 | Blocked by: 30, 14, 7 | Blocks: 36
  References (executor has NO interview context - be exhaustive): docs/12-application-documents.md:8-27; docs/11-application-engine.md:6-30; docs/06-ai-architecture.md:40-69; docs/23-ai-security.md:51-70; docs/07-candidate-intelligence.md (profile source)
  Acceptance criteria (agent-executable): unit test: tailoring returns content + change-diff; no new experience lines added; keyword score improves vs baseline; existing facts preserved.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=ai-eval tailoring tests, Evidence .omo/evidence/task-31-jobpilot.txt; failure — prompt injects "add 10 years at Google" → rejected, change-diff empty (docs/23).
  Commit: Y | feat(strategy): resume tailoring

- [ ] 32. Cover letter generation (TASK-JP-0032)
  What to do / Must NOT do: cover letter from plan brief (docs/12 §3): intro/body/close, job keywords, links to concrete profile evidence. Must NOT fabricate achievements; must return evidence citations per claim (Zero-Fab).
  Parallelization: Wave 5 | Blocked by: 30 | Blocks: 36
  References (executor has NO interview context - be exhaustive): docs/12-application-documents.md:29-46; docs/11-application-engine.md:6-30; docs/23-ai-security.md:51-70; docs/06-ai-architecture.md:40-69
  Acceptance criteria (agent-executable): unit test: letter contains job keywords + one evidence-cited claim per paragraph; every factual claim traces to profile data; hallucinated claim → rejected.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=ai-eval cover letter tests, Evidence .omo/evidence/task-32-jobpilot.txt; failure — claim "led team of 50" not in profile → rejected (docs/23).
  Commit: Y | feat(strategy): cover letter generation

- [ ] 33. Application question engine (TASK-JP-0033)
  What to do / Must NOT do: generate/answer application questions (docs/12 §4): question comprehension → answer with profile evidence; supports "why us" / "why role" via plan research. Must NOT answer with non-profile facts; every answer cites source.
  Parallelization: Wave 5 | Blocked by: 30 | Blocks: 36
  References (executor has NO interview context - be exhaustive): docs/12-application-documents.md:48-73; docs/11-application-engine.md:6-30; docs/23-ai-security.md:51-70; docs/06-ai-architecture.md:40-69
  Acceptance criteria (agent-executable): unit test: question classified, answer generated with evidence citations; unanswerable question → typed refusal, no invented answer.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=ai-eval question engine tests, Evidence .omo/evidence/task-33-jobpilot.txt; failure — question about facts not in profile → refusal (docs/23).
  Commit: Y | feat(strategy): application question engine

- [ ] 34. RAG context retrieval (TASK-JP-0034)
  What to do / Must NOT do: RAG pipeline (docs/06 §3) feeding strategy/doc-gen: chunk profile + career memory + job docs, embed, retrieve top-k per prompt. Must NOT pass raw retrieval without grounding guard; cache embeddings (docs/06 cost table).
  Parallelization: Wave 5 | Blocked by: 13, 21 | Blocks: 31, 32, 33
  References (executor has NO interview context - be exhaustive): docs/06-ai-architecture.md:73-99; docs/04-database-design.md:492 (vector); docs/20-career-memory.md:12-50; docs/31-performance.md:28-36
  Acceptance criteria (agent-executable): integration test: retrieval returns top-k chunks for a strategy prompt; chunks come only from indexed profile/memory/job corpus; cache hit served without re-embed.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=ai-eval RAG tests, Evidence .omo/evidence/task-34-jobpilot.txt; failure — query outside corpus → empty/refusal, no fabricated chunk.
  Commit: Y | feat(strategy): RAG context retrieval

- [ ] 35. Zero-fabrication guard (TASK-JP-0035)
  What to do / Must NOT do: cross-cutting guard (docs/23 §Zero-Fab) applied to all AI application-content outputs: claim-vs-profile verification, refusal on ungrounded claims. Must NOT gate user-typed content; must run before content is persisted or sent.
  Parallelization: Wave 5 | Blocked by: 31, 32, 33 | Blocks: 36, 52
  References (executor has NO interview context - be exhaustive): docs/23-ai-security.md:51-70; docs/27-ai-evaluation.md:6-19; docs/26-testing.md:64-71 (invariants)
  Acceptance criteria (agent-executable): guard unit tests: ungrounded claim blocked, grounded claim passes, refusal path returns typed error + reason; all strategy/doc-gen outputs pass through guard.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=ai-eval guard tests, Evidence .omo/evidence/task-35-jobpilot.txt; failure — hallucinated achievement in output → blocked before persist (docs/23).
  Commit: Y | feat(ai-security): zero-fabrication guard

- [ ] 36. Doc-gen evaluation suite (TASK-JP-0036)
  What to do / Must NOT do: eval suite over hand-labeled fixtures for resume/cover-letter/questions (docs/27): groundedness, keyword coverage, hallucination rate. Must NOT self-generate labels (docs/27 methodology); fixtures versioned.
  Parallelization: Wave 5 | Blocked by: 31, 32, 33, 34, 35 | Blocks: 37
  References (executor has NO interview context - be exhaustive): docs/27-ai-evaluation.md:6-19, 45-51; docs/28-browser-evaluation.md:22-33 (zero-tolerance pattern); docs/23-ai-security.md:51-70
  Acceptance criteria (agent-executable): eval run on fixtures reports hallucination rate within docs/27 tightest band and keyword coverage >= band; nightly-tagged, fails build on breach.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=ai-eval on doc-gen fixtures → bands met, Evidence .omo/evidence/task-36-jobpilot.txt; failure — injected fabrication in fixture → rate breaches band (docs/27).
  Commit: Y | test(strategy): document generation eval suite

### Wave 6 — Phase 6: Application Engine MANUAL (docs/16)

- [ ] 37. Application state machine (TASK-JP-0037)
  What to do / Must NOT do: MANUAL-mode application state machine (docs/16 §4): DRAFT → PREPARING → REVIEW_READY → SUBMITTING → SUBMITTED → OUTCOME_*; guard transitions. Must NOT auto-submit (MANUAL never calls submit, docs/26 invariant #1); invalid transition → STATE_CONFLICT error (docs/05:148).
  Parallelization: Wave 6 | Blocked by: 27 | Blocks: 38, 39, 40, 41, 42, 43
  References (executor has NO interview context - be exhaustive): docs/16-agent-orchestration.md:74-104; docs/05-api-specification.md:135-138, 148; docs/03-domain-model.md (Application state); docs/26-testing.md:64-71
  Acceptance criteria (agent-executable): state machine unit tests: legal transitions succeed, illegal → STATE_CONFLICT; MANUAL mode can never reach submit action; state persisted atomically.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=invariants state machine tests, Evidence .omo/evidence/task-37-jobpilot.txt; failure — MANUAL-mode submit attempt → blocked, invariant test fails loudly (docs/26).
  Commit: Y | feat(applications): MANUAL application state machine

- [ ] 38. Application CRUD + status endpoints (TASK-JP-0038)
  What to do / Must NOT do: REST endpoints for applications (docs/05 §6): create/read/list/update/cancel/start, status transitions via state machine, error envelope + Idempotency-Key on create. Must NOT bypass state machine for status changes; must scope to owning user.
  Parallelization: Wave 6 | Blocked by: 37 | Blocks: 39, 40, 41, 42
  References (executor has NO interview context - be exhaustive): docs/05-api-specification.md:77-97; docs/04-database-design.md:313 (applications tables); docs/16-agent-orchestration.md:74-104; docs/30-error-handling.md (envelope)
  Acceptance criteria (agent-executable): API integration tests: create/list/update/cancel/start return documented shapes; duplicate create with same Idempotency-Key returns first result; cross-user access → 403/404.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=api application CRUD tests, Evidence .omo/evidence/task-38-jobpilot.txt; failure — idempotent replay returns same application, no duplicate row.
  Commit: Y | feat(applications): application CRUD + status endpoints

- [ ] 39. Manual handoff package builder (TASK-JP-0039)
  What to do / Must NOT do: build the manual handoff package (docs/15): collates tailored resume, cover letter, answers, company research, application URL into a reviewable package; exposes preview endpoint + manual-package API (docs/05:123-127). Must NOT trigger any browser automation or external submit.
  Parallelization: Wave 6 | Blocked by: 38, 36 | Blocks: 40, 41
  References (executor has NO interview context - be exhaustive): docs/15-manual-handoff.md; docs/05-api-specification.md:123-127; docs/34-project-structure.md (application/documents); docs/12-application-documents.md
  Acceptance criteria (agent-executable): unit test: package contains all prepared docs with content hashes; package versioned; manual-package endpoint returns 200 with package metadata; no network/browser call occurs.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=api handoff package tests, Evidence .omo/evidence/task-39-jobpilot.txt; failure — package build with missing doc → 422 with field list.
  Commit: Y | feat(applications): manual handoff package builder

- [ ] 40. Manual handoff UI (TASK-JP-0040)
  What to do / Must NOT do: Thymeleaf page rendering the handoff package (docs/15): per-doc review, edit-in-place for resume/letter/answers, confirm button that marks package finalized. Must NOT expose a "submit application" action in MANUAL mode; escape all content (docs/22 §9).
  Parallelization: Wave 6 | Blocked by: 39 | Blocks: 43
  References (executor has NO interview context - be exhaustive): docs/15-manual-handoff.md; docs/34-project-structure.md (web module); docs/22-security.md:9-13 (output escaping); docs/16-agent-orchestration.md:74-104
  Acceptance criteria (agent-executable): Playwright test: review page shows all docs, edit saves and re-hashes, finalize marks package ready; page contains no submit button; XSS payload in doc renders escaped.
  QA scenarios (name the exact tool + invocation): happy — Playwright: review/edit/finalize flow, Evidence .omo/evidence/task-40-jobpilot.txt; failure — XSS in resume text renders as text, not executed.
  Commit: Y | feat(applications): manual handoff UI

- [ ] 41. Application cockpit UI (TASK-JP-0041)
  What to do / Must NOT do: cockpit view of all applications (dashboard, docs/02:116-127): status, stage, next action, outcome; links to handoff package when REVIEW_READY. Must NOT show other users' applications; escape all text.
  Parallelization: Wave 6 | Blocked by: 38, 29 | Blocks: 43
  References (executor has NO interview context - be exhaustive): docs/02-system-architecture.md:116-127; docs/05-api-specification.md:77-97; docs/34-project-structure.md (web module); docs/22-security.md:9-13
  Acceptance criteria (agent-executable): Playwright test: cockpit lists owned applications with correct status badges; empty state renders; other-user application not listed.
  QA scenarios (name the exact tool + invocation): happy — Playwright: cockpit list + status rendering, Evidence .omo/evidence/task-41-jobpilot.txt; failure — IDOR attempt on other user's application → 403/404.
  Commit: Y | feat(applications): application cockpit UI

- [ ] 42. mark-applied + outcome endpoints (TASK-JP-0042)
  What to do / Must NOT do: endpoints to record manual submission (mark-applied, docs/05:90) and outcome (docs/05:104): OFFER, REJECTED, WITHDRAWN etc. with notes; feed tracking (docs/18). Must NOT accept status the state machine forbids; must not fabricate outcome data.
  Parallelization: Wave 6 | Blocked by: 38 | Blocks: 62, 63
  References (executor has NO interview context - be exhaustive): docs/05-api-specification.md:90, 104; docs/18-tracking.md:6-24; docs/16-agent-orchestration.md:74-104; docs/30-error-handling.md
  Acceptance criteria (agent-executable): API tests: mark-applied transitions REVIEW_READY→SUBMITTED; outcome endpoints accept valid outcomes and reject invalid; timeline event recorded for each change.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=api mark-applied/outcome tests, Evidence .omo/evidence/task-42-jobpilot.txt; failure — outcome set on non-submitted application → STATE_CONFLICT.
  Commit: Y | feat(applications): mark-applied + outcome endpoints

- [ ] 43. State machine transition tests (TASK-JP-0043)
  What to do / Must NOT do: exhaustive transition coverage over the MANUAL state machine (docs/26): every legal path, every illegal path, invariants incl. never-calls-submit. Must NOT mock the guard — exercise real guard; parameterized over all states.
  Parallelization: Wave 6 | Blocked by: 37, 38, 39, 40, 41, 42 | Blocks: 56
  References (executor has NO interview context - be exhaustive): docs/26-testing.md:64-71; docs/16-agent-orchestration.md:74-104; docs/25-threat-model.md:87-103
  Acceptance criteria (agent-executable): full transition matrix test passes; invariant tests (MANUAL never submits, no state skip) all green; failing illegal transition reproduces as STATE_CONFLICT.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=invariants full matrix, Evidence .omo/evidence/task-43-jobpilot.txt; failure — any illegal transition silently accepted → test fails.
  Commit: Y | test(applications): state machine transition coverage

### Wave 7 — Phase 7: Browser Intelligence (docs/14)

- [ ] 44. Playwright session harness + browser isolation (TASK-JP-0044)
  What to do / Must NOT do: browser session service over Playwright (docs/14): per-application isolated browser context, profile container (docs/33 compose), headless-capable; base for all browser tasks. Must NOT share sessions across applications/users; must not store plaintext credentials.
  Parallelization: Wave 7 | Blocked by: 8 | Blocks: 45, 46, 47
  References (executor has NO interview context - be exhaustive): docs/14-browser-agent.md; docs/28-browser-evaluation.md:22-33; docs/33-devops.md:19-40 (browser in compose); docs/24-privacy.md; docs/34-project-structure.md (browser module)
  Acceptance criteria (agent-executable): integration test: browser launches in compose profile, session is isolated per application (no cookie/cache bleed), healthcheck passes; teardown cleans resources.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=browser-eval session harness test, Evidence .omo/evidence/task-44-jobpilot.txt; failure — two sessions leak cookies → test red.
  Commit: Y | feat(browser): Playwright session harness with isolation

- [ ] 45. BrowserAction loop (TASK-JP-0045)
  What to do / Must NOT do: BrowserAction execution loop (docs/14): navigate/click/fill/select/submit-candidate actions with retry + timeout policy, per-action outcome. Must NOT execute unverified actions; every action result recorded to audit trail (docs/16 tools).
  Parallelization: Wave 7 | Blocked by: 44 | Blocks: 48, 49, 50
  References (executor has NO interview context - be exhaustive): docs/14-browser-agent.md; docs/16-agent-orchestration.md:46-72; docs/30-error-handling.md; docs/28-browser-evaluation.md:22-33
  Acceptance criteria (agent-executable): unit test: action queue executes in order, retry policy fires on transient failure, timeout aborts with typed error; audit record per action.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=browser-eval action loop, Evidence .omo/evidence/task-45-jobpilot.txt; failure — action times out → typed error, no partial submit.
  Commit: Y | feat(browser): BrowserAction execution loop

- [ ] 46. Vision-based verification (TASK-JP-0046)
  What to do / Must NOT do: screenshot + vision-model page-state verification (docs/14): detect success/error/validation-returned from rendered page. Must NOT act on unverified state; low-confidence → typed UNVERIFIED, never blind proceed.
  Parallelization: Wave 7 | Blocked by: 44, 5 | Blocks: 48
  References (executor has NO interview context - be exhaustive): docs/14-browser-agent.md; docs/06-ai-architecture.md:40-42; docs/27-ai-evaluation.md:45-51; docs/25-threat-model.md:87-95
  Acceptance criteria (agent-executable): vision verifier on fixture screenshots classifies success/error/validation with confidence; adversarial screenshot (fake success text) → low confidence / UNVERIFIED.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=ai-eval vision fixtures, Evidence .omo/evidence/task-46-jobpilot.txt; failure — spoofed success page → UNVERIFIED, not success.
  Commit: Y | feat(browser): vision-based page-state verification

- [ ] 47. Form Intelligence (TASK-JP-0047)
  What to do / Must NOT do: form field discovery + label→field mapping + required detection (docs/13) over DOM; dynamic form triggers. Must NOT auto-fill fields it cannot resolve confidently; unresolved required field → typed error.
  Parallelization: Wave 7 | Blocked by: 44 | Blocks: 48, 49, 50
  References (executor has NO interview context - be exhaustive): docs/13-form-intelligence.md; docs/34-project-structure.md (browser/forms sub-package); docs/14-browser-agent.md
  Acceptance criteria (agent-executable): form model extracted from fixture HTML; mapping covers label/placeholder/aria; dynamic form re-scans on trigger; unresolved required field reported.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=browser-eval form fixtures, Evidence .omo/evidence/task-47-jobpilot.txt; failure — required field unresolved → typed error, no submit.
  Commit: Y | feat(browser): form intelligence + field mapping

- [ ] 48. Field resolution + dynamic forms (TASK-JP-0048)
  What to do / Must NOT do: resolve mapped form fields from profile/career-memory (docs/13); handle dynamic forms (conditional fields). Must NOT send user data outside encrypted channels (docs/24); unresolved → explicit prompt, never guess.
  Parallelization: Wave 7 | Blocked by: 45, 46, 47 | Blocks: 51, 56
  References (executor has NO interview context - be exhaustive): docs/13-form-intelligence.md; docs/20-career-memory.md:12-50; docs/24-privacy.md; docs/34-project-structure.md (browser/adapters)
  Acceptance criteria (agent-executable): resolver fills fields from profile/memory with source tag; dynamic field appears after trigger and is filled; unknown field → prompt, not guess.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=browser-eval field resolution, Evidence .omo/evidence/task-48-jobpilot.txt; failure — unmapped required field → explicit prompt.
  Commit: Y | feat(browser): field resolution for dynamic forms

- [ ] 49. Submission verification detection (TASK-JP-0049)
  What to do / Must NOT do: post-submit verification: classify success/error/validation-returned (docs/14 verification, threat 12 docs/25:87-95). Must NOT report success without evidence; ambiguous → UNVERIFIED with screenshot evidence.
  Parallelization: Wave 7 | Blocked by: 45, 47 | Blocks: 51, 53
  References (executor has NO interview context - be exhaustive): docs/14-browser-agent.md; docs/25-threat-model.md:87-95; docs/28-browser-evaluation.md:35-42 (graceful failure)
  Acceptance criteria (agent-executable): verifier on fixture pages returns correct classification; error page classified failed; ambiguous → UNVERIFIED + evidence artifact; no false success.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=browser-eval verification fixtures, Evidence .omo/evidence/task-49-jobpilot.txt; failure — validation-returned classified success → red.
  Commit: Y | feat(browser): submission verification detection

- [ ] 50. Browser session persistence (TASK-JP-0050)
  What to do / Must NOT do: persist browser session state (cookies/storage) encrypted per application for resume (docs/14); restore on demand. Must NOT persist plaintext credentials; expiry + invalidation policy.
  Parallelization: Wave 7 | Blocked by: 45 | Blocks: 51, 57
  References (executor has NO interview context - be exhaustive): docs/14-browser-agent.md; docs/24-privacy.md; docs/04-database-design.md (encrypted storage); docs/25-threat-model.md:97-103
  Acceptance criteria (agent-executable): session save/restore roundtrip works; stored blob is ciphertext (no plaintext tokens); expired/invalidated session → typed error on restore.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=browser-eval session persistence, Evidence .omo/evidence/task-50-jobpilot.txt; failure — restored session with stale token → typed error, no silent reuse.
  Commit: Y | feat(browser): encrypted session persistence

- [ ] 51. Browser eval suite (TASK-JP-0051)
  What to do / Must NOT do: automated eval suite over browser fixtures (docs/28, 12 fixtures): action loop, form fill, verification — zero-tolerance checks + graceful failure band. Must NOT hit live external sites; fixtures self-contained.
  Parallelization: Wave 7 | Blocked by: 48, 49, 50 | Blocks: 56
  References (executor has NO interview context - be exhaustive): docs/28-browser-evaluation.md:6-42 (fixture list, zero-tolerance 22-33, graceful failure 35-42); docs/26-testing.md:45-54; docs/27-ai-evaluation.md:6-19
  Acceptance criteria (agent-executable): suite runs all 12 fixtures green; zero-tolerance checks pass; graceful-failure band met; nightly-tagged.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=browser-eval full suite → bands met, Evidence .omo/evidence/task-51-jobpilot.txt; failure — any fixture breaches band → red.
  Commit: Y | test(browser): browser evaluation suite

### Wave 8 — Phase 8: Automation (docs/17)

- [ ] 52. Policy Engine (TASK-JP-0052)
  What to do / Must NOT do: Policy Engine (docs/16 §4) evaluating mode/site/action rules before every browser action; returns POLICY_BLOCKED with reason (docs/05:135-138). Must NOT execute actions policy denies; policy decisions audited.
  Parallelization: Wave 8 | Blocked by: 35 | Blocks: 53, 54, 55
  References (executor has NO interview context - be exhaustive): docs/16-agent-orchestration.md:74-104; docs/05-api-specification.md:135-138; docs/17-workflow-engine.md; docs/24-privacy.md
  Acceptance criteria (agent-executable): policy unit tests: allow/deny matrix per mode+site+action; denied action → POLICY_BLOCKED with reason; every decision written to audit trail.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=invariants policy engine tests, Evidence .omo/evidence/task-52-jobpilot.txt; failure — denied action executes → red.
  Commit: Y | feat(automation): policy engine with audit

- [ ] 53. Automation modes (ASSISTED/AUTO/manual) (TASK-JP-0053)
  What to do / Must NOT do: mode gating (docs/16): manual/ASSISTED/AUTO per application+site; mode resolved at workflow start. Must NOT run AUTO on sites without verified adapters (docs/17); mode changes audited.
  Parallelization: Wave 8 | Blocked by: 52 | Blocks: 56
  References (executor has NO interview context - be exhaustive): docs/16-agent-orchestration.md:74-104; docs/17-workflow-engine.md:57-75 (adapters); docs/26-testing.md:64-71 (invariants)
  Acceptance criteria (agent-executable): mode resolution unit tests: AUTO requires verified adapter + policy allow; ASSISTED requires human confirm; manual default; all transitions audited.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=invariants mode gating, Evidence .omo/evidence/task-53-jobpilot.txt; failure — AUTO on unverified site → POLICY_BLOCKED.
  Commit: Y | feat(automation): automation mode gating

- [ ] 54. Tool registry + audit trail (TASK-JP-0054)
  What to do / Must NOT do: registry of executable tools (navigate/click/fill/verify/submit-candidate) with schemas + permission mapping (docs/16 §3); every invocation recorded (action, args, result, actor, timestamp). Must NOT execute unregistered tools; audit immutable.
  Parallelization: Wave 8 | Blocked by: 52 | Blocks: 56
  References (executor has NO interview context - be exhaustive): docs/16-agent-orchestration.md:46-72; docs/24-privacy.md; docs/25-threat-model.md:87-103
  Acceptance criteria (agent-executable): registry lists all tools with schemas; invocation audit rows complete; unknown tool → typed error; audit append-only.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=invariants tool registry, Evidence .omo/evidence/task-54-jobpilot.txt; failure — unregistered tool call → typed error, no execution.
  Commit: Y | feat(automation): tool registry + immutable audit trail

- [ ] 55. Workflow Engine (TASK-JP-0055)
  What to do / Must NOT do: workflow engine executing steps (prepare → fill → verify → submit-candidate) with state resumability (docs/17 §2); step outcomes recorded. Must NOT submit without verification passing; workflow state persisted.
  Parallelization: Wave 8 | Blocked by: 52 | Blocks: 56, 57, 58, 59
  References (executor has NO interview context - be exhaustive): docs/17-workflow-engine.md:15-26; docs/16-agent-orchestration.md:74-104; docs/34-project-structure.md (workflow sub-package)
  Acceptance criteria (agent-executable): engine executes step sequence for fixture; resumable after mid-step crash; submit step requires verified state; step outcomes logged.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=invariants workflow engine, Evidence .omo/evidence/task-55-jobpilot.txt; failure — unverified submit attempted → engine refuses.
  Commit: Y | feat(automation): resumable workflow engine

- [ ] 56. ASSISTED mode (TASK-JP-0056)
  What to do / Must NOT do: ASSISTED flow (docs/17 session ops): run prepare/fill, pause before submit-candidate, surface handoff package for human review/approval (docs/15), resume on confirm. Must NOT auto-submit in ASSISTED; confirm required per submission.
  Parallelization: Wave 8 | Blocked by: 43, 51, 53, 54, 55 | Blocks: 57
  References (executor has NO interview context - be exhaustive): docs/17-workflow-engine.md:34-55; docs/15-manual-handoff.md; docs/26-testing.md:64-71 (never-calls-submit invariant); docs/05-api-specification.md:123-127
  Acceptance criteria (agent-executable): ASSISTED e2e on fixture: workflow pauses at review, confirms, submits via manual handoff path; no confirm → no submit; invariant holds.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=browser-eval ASSISTED flow, Evidence .omo/evidence/task-56-jobpilot.txt; failure — confirm skipped → submit never fires.
  Commit: Y | feat(automation): ASSISTED mode with confirm gate

- [ ] 57. AUTO mode (TASK-JP-0057)
  What to do / Must NOT do: AUTO flow (docs/17 adapters): execute full workflow with policy-approved adapter, auto-submit after verification passes (threat 12 docs/25:87-95). Must NOT auto-submit unverified; every action audited; abort on policy block.
  Parallelization: Wave 8 | Blocked by: 56, 50 | Blocks: 58, 59, 60
  References (executor has NO interview context - be exhaustive): docs/17-workflow-engine.md:57-75; docs/25-threat-model.md:87-95; docs/16-agent-orchestration.md:74-104
  Acceptance criteria (agent-executable): AUTO e2e on fixture site completes submit only after verification; policy block mid-flow aborts cleanly; audit trail complete.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=browser-eval AUTO flow, Evidence .omo/evidence/task-57-jobpilot.txt; failure — verification fails → no submit, state UNVERIFIED.
  Commit: Y | feat(automation): AUTO mode with verified submit

- [ ] 58. Platform adapters (TASK-JP-0058)
  What to do / Must NOT do: platform adapters (docs/17 §3): per-site field maps/selectors/verification rules for allowlisted platforms. Must NOT support sites without verification rules; adapter failure → typed error, no blind fallback.
  Parallelization: Wave 8 | Blocked by: 55, 17, 18 | Blocks: 60
  References (executor has NO interview context - be exhaustive): docs/17-workflow-engine.md:57-75; docs/13-form-intelligence.md; docs/05-api-specification.md:119 (settings/automation); docs/14-browser-agent.md
  Acceptance criteria (agent-executable): adapter registry covers allowlisted sites with field maps + verification; missing selector → typed error; adapter config exposed via settings endpoint.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=browser-eval adapter fixtures, Evidence .omo/evidence/task-58-jobpilot.txt; failure — unknown site AUTO attempt → POLICY_BLOCKED.
  Commit: Y | feat(automation): platform adapters

- [ ] 59. Locking + idempotency (TASK-JP-0059)
  What to do / Must NOT do: per-application workflow lock (no concurrent runs) + idempotent step execution with Idempotency-Key (docs/05 convention); concurrency-safe (docs/32 §1). Must NOT double-submit; lock released on all exits.
  Parallelization: Wave 8 | Blocked by: 55 | Blocks: 60
  References (executor has NO interview context - be exhaustive): docs/32-scalability.md:7-25; docs/05-api-specification.md (Idempotency-Key, STATE_CONFLICT 148); docs/17-workflow-engine.md; docs/26-testing.md:64-71
  Acceptance criteria (agent-executable): concurrent workflow start on same application → second rejected/waits; step replay with same key returns cached result; lock released after success and after failure.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=invariants locking/idempotency, Evidence .omo/evidence/task-59-jobpilot.txt; failure — concurrent duplicate submit → red.
  Commit: Y | feat(automation): workflow locking + idempotency

- [ ] 60. Automation e2e (TASK-JP-0060)
  What to do / Must NOT do: full end-to-end automation scenario (docs/26 pyramid top): ASSISTED and AUTO runs across fixture sites, verification, audit. Must NOT touch live external sites; fixture-only.
  Parallelization: Wave 8 | Blocked by: 57, 58, 59 | Blocks: 61
  References (executor has NO interview context - be exhaustive): docs/26-testing.md:6-41 (test pyramid); docs/17-workflow-engine.md; docs/28-browser-evaluation.md:6-42; docs/33-devops.md:19-40
  Acceptance criteria (agent-executable): e2e suite green: ASSISTED confirm path, AUTO verified-submit path, failure-path abort; audit trail assertions per scenario.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=browser-eval e2e, Evidence .omo/evidence/task-60-jobpilot.txt; failure — any scenario misbehaves → red with scenario named.
  Commit: Y | test(automation): end-to-end automation scenarios

- [ ] 61. Threat-12/13 regression suite (TASK-JP-0061)
  What to do / Must NOT do: regression suite for threat 12 (spoofed/false verification, docs/25:87-95) and threat 13 (session/credential, docs/25:97-103): adversarial fixtures. Must NOT weaken verification to pass; adversarial pages must be classified UNVERIFIED/failed.
  Parallelization: Wave 8 | Blocked by: 60 | Blocks: 82
  References (executor has NO interview context - be exhaustive): docs/25-threat-model.md:87-103; docs/28-browser-evaluation.md:22-42; docs/26-testing.md:64-71
  Acceptance criteria (agent-executable): adversarial fixtures (fake success page, tampered session) all classified UNVERIFIED or failed; suite green = guards hold; red = regression.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=browser-eval threat-12/13, Evidence .omo/evidence/task-61-jobpilot.txt; failure — spoofed success accepted → red.
  Commit: Y | test(security): threat-12/13 regression suite

### Wave 9 — Phase 9: Tracking & Notifications (docs/18)

- [ ] 62. Application timeline + events (TASK-JP-0062)
  What to do / Must NOT do: append-only timeline of application events (created, status change, note, outcome) per docs/18 §2; feeds dashboard + analytics. Must NOT allow event deletion/rewrite; events carry timestamp + actor.
  Parallelization: Wave 9 | Blocked by: 42 | Blocks: 66
  References (executor has NO interview context - be exhaustive): docs/18-tracking.md:6-24; docs/04-database-design.md (timeline tables); docs/05-api-specification.md (event endpoints); docs/24-privacy.md
  Acceptance criteria (agent-executable): timeline API returns ordered events for owned application; append-only enforced (no delete/update endpoints); every state transition produces an event.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=api timeline tests, Evidence .omo/evidence/task-62-jobpilot.txt; failure — tampered/rewritten event → rejected.
  Commit: Y | feat(tracking): application timeline + events

- [ ] 63. Outcome recording (TASK-JP-0063)
  What to do / Must NOT do: record application outcome (OFFER/REJECTED/WITHDRAWN/EXPIRED) with notes + dates (docs/18 §2, docs/05:104); outcome feeds analytics + learning. Must NOT accept invalid outcome for state; outcome change → timeline event.
  Parallelization: Wave 9 | Blocked by: 42 | Blocks: 66
  References (executor has NO interview context - be exhaustive): docs/18-tracking.md:6-24; docs/05-api-specification.md:104; docs/19-analytics.md:5-57; docs/04-database-design.md (outcome fields)
  Acceptance criteria (agent-executable): outcome endpoint validates against state machine; invalid outcome → 422/STATE_CONFLICT; each outcome change logged as timeline event.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=api outcome tests, Evidence .omo/evidence/task-63-jobpilot.txt; failure — outcome set on non-submitted application → rejected.
  Commit: Y | feat(tracking): outcome recording

- [ ] 64. Notifications (TASK-JP-0064)
  What to do / Must NOT do: notification dispatch on events (status change, action required, outcome) per docs/18 §5 and docs/05:105-106: in-app + email channels. Must NOT send sensitive application content unencrypted; per-channel opt-out honored.
  Parallelization: Wave 9 | Blocked by: 62 | Blocks: 66
  References (executor has NO interview context - be exhaustive): docs/18-tracking.md:51-61; docs/05-api-specification.md:105-106; docs/24-privacy.md; docs/33-devops.md (mail in compose)
  Acceptance criteria (agent-executable): notification created on subscribed events; in-app list endpoint works; email sent via configured transport; opt-out respected per channel.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=api notifications, Evidence .omo/evidence/task-64-jobpilot.txt; failure — opted-out channel still notified → red.
  Commit: Y | feat(notifications): event-driven notifications

- [ ] 65. Follow-up reminders (TASK-JP-0065)
  What to do / Must NOT do: scheduled follow-up reminders (docs/18 §6) via job scheduler (docs/33 scheduling lock): due-date based on application stage; reminder notification + timeline event. Must NOT double-fire (idempotent); no reminder for resolved applications.
  Parallelization: Wave 9 | Blocked by: 64 | Blocks: 66
  References (executor has NO interview context - be exhaustive): docs/18-tracking.md:63-69; docs/33-devops.md:86-92 (scheduling lock); docs/32-scalability.md:27-34 (queues)
  Acceptance criteria (agent-executable): reminder scheduled per stage rule; fires once (idempotency test); resolved application → no reminder; lock prevents duplicate scheduler runs.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=invariants reminders, Evidence .omo/evidence/task-65-jobpilot.txt; failure — double-fire on restart → red.
  Commit: Y | feat(notifications): follow-up reminders

- [ ] 66. Tracking dashboard (CORE) (TASK-JP-0066)
  What to do / Must NOT do: CORE tracking dashboard (docs/02:116-127, docs/18): application list with status/stage/next action, follow-ups due, outcomes; feeds analytics in Wave 11. Must NOT expose other users' data; escape all content.
  Parallelization: Wave 9 | Blocked by: 62, 63, 64, 65 | Blocks: 72
  References (executor has NO interview context - be exhaustive): docs/02-system-architecture.md:116-127; docs/18-tracking.md; docs/34-project-structure.md (web module); docs/22-security.md:9-13
  Acceptance criteria (agent-executable): Playwright: dashboard renders owned applications with status/next action; follow-ups section lists due reminders; cross-user data absent; XSS escaped.
  QA scenarios (name the exact tool + invocation): happy — Playwright tracking dashboard, Evidence .omo/evidence/task-66-jobpilot.txt; failure — IDOR on dashboard data → 403/404.
  Commit: Y | feat(tracking): tracking dashboard

### Wave 10 — Phase 10: Learning & Career Strategy (docs/10, docs/20, docs/21)

- [ ] 67. Career strategy service (TASK-JP-0067)
  What to do / Must NOT do: career strategy service (docs/10 §2-3): goals, gap analysis, action plan from outcome history + career memory; endpoints per docs/05:73-74. Must NOT fabricate recommendations (Zero-Fab); every recommendation cites evidence.
  Parallelization: Wave 10 | Blocked by: 29, 72 | Blocks: 71
  References (executor has NO interview context - be exhaustive): docs/10-career-strategy.md:21-57; docs/05-api-specification.md:73-74; docs/20-career-memory.md:12-50; docs/19-analytics.md:5-57
  Acceptance criteria (agent-executable): strategy service returns goals + gaps + action plan from real data; recommendation cites memory/outcome source; empty data → graceful cold-start response.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=ai-eval strategy service, Evidence .omo/evidence/task-67-jobpilot.txt; failure — recommendation without evidence source → rejected.
  Commit: Y | feat(strategy): career strategy service

- [ ] 68. Career memory store (TASK-JP-0068)
  What to do / Must NOT do: career memory store (docs/20 §2): experiences, achievements, skills, preferences with source + confidence; feeds RAG and doc-gen. Must NOT store unverified claims as facts (Zero-Fab); user confirmation required for extracted items.
  Parallelization: Wave 10 | Blocked by: 62, 63 | Blocks: 71
  References (executor has NO interview context - be exhaustive): docs/20-career-memory.md:12-50; docs/05-api-specification.md:75; docs/06-ai-architecture.md:73-99 (RAG); docs/04-database-design.md:492 (vector)
  Acceptance criteria (agent-executable): memory items CRUD with source tag; extraction flow requires user confirmation before storage; verified items retrievable via RAG; unverified excluded from doc-gen.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=ai-eval career memory, Evidence .omo/evidence/task-68-jobpilot.txt; failure — unconfirmed item used in resume → blocked.
  Commit: Y | feat(memory): career memory store

- [ ] 69. Learning signals (TASK-JP-0069)
  What to do / Must NOT do: learning system (docs/21 §2): collect signals (outcome patterns, user feedback, strategy choices) → derive preference features; persist per-user. Must NOT infer sensitive attributes (docs/24); signals pseudonymous.
  Parallelization: Wave 10 | Blocked by: 68 | Blocks: 70, 71
  References (executor has NO interview context - be exhaustive): docs/21-learning-system.md:13-39; docs/24-privacy.md; docs/18-tracking.md:6-24; docs/04-database-design.md (learning tables)
  Acceptance criteria (agent-executable): signal collection on tracked events; feature derivation per docs/21 formulas; no sensitive-attribute inference (privacy test); signals stored pseudonymized.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=ai-eval learning signals, Evidence .omo/evidence/task-69-jobpilot.txt; failure — sensitive inference attempt → dropped + logged.
  Commit: Y | feat(learning): learning signal collection

- [ ] 70. Adaptivity + learning endpoints (TASK-JP-0070)
  What to do / Must NOT do: adaptive behavior (docs/21 §4): strategy/content adapts to learned preferences; endpoints to read/update preferences (docs/05:75). Must NOT override explicit user settings; adaptivity explainable.
  Parallelization: Wave 10 | Blocked by: 69 | Blocks: 71
  References (executor has NO interview context - be exhaustive): docs/21-learning-system.md:59-65; docs/05-api-specification.md:75; docs/10-career-strategy.md:59-67
  Acceptance criteria (agent-executable): preference update endpoint persists; adapted output changes per preference; explicit user setting beats learned preference; explainability field in response.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=ai-eval adaptivity, Evidence .omo/evidence/task-70-jobpilot.txt; failure — learned preference overrides explicit setting → red.
  Commit: Y | feat(learning): adaptive behavior + endpoints

- [ ] 71. Cold-start onboarding + strategy UI (TASK-JP-0071)
  What to do / Must NOT do: cold-start wizard (docs/20 §3): collects initial career memory items with confirmation; strategy/adaptivity UI (docs/10 §4, docs/21 §4). Must NOT require AI for wizard (graceful offline); all content escaped.
  Parallelization: Wave 10 | Blocked by: 67, 68, 69, 70 | Blocks: —
  References (executor has NO interview context - be exhaustive): docs/20-career-memory.md:52-57; docs/10-career-strategy.md:76-81; docs/21-learning-system.md:59-65; docs/34-project-structure.md (web module)
  Acceptance criteria (agent-executable): Playwright: wizard collects items → confirmed → visible in memory; strategy page renders goals/gaps; preference toggle works; empty state graceful.
  QA scenarios (name the exact tool + invocation): happy — Playwright cold-start + strategy UI, Evidence .omo/evidence/task-71-jobpilot.txt; failure — wizard without network → still functional.
  Commit: Y | feat(strategy): cold-start onboarding + strategy UI

### Wave 11 — Phase 11: Analytics (docs/19)

- [ ] 72. Analytics snapshot job (TASK-JP-0072)
  What to do / Must NOT do: scheduled job (docs/33 scheduling lock) computing analytics snapshots from tracking/outcome data per docs/19 metrics; store snapshots. Must NOT recompute on read (read from snapshot); job idempotent + locked.
  Parallelization: Wave 11 | Blocked by: 66 | Blocks: 67, 73
  References (executor has NO interview context - be exhaustive): docs/19-analytics.md:5-57; docs/33-devops.md:86-92; docs/18-tracking.md:6-24; docs/04-database-design.md (analytics tables)
  Acceptance criteria (agent-executable): snapshot job computes documented metrics over fixture data; rerun with same input → same snapshot (idempotent); lock prevents concurrent runs.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=invariants snapshot job, Evidence .omo/evidence/task-72-jobpilot.txt; failure — double-run diverges → red.
  Commit: Y | feat(analytics): snapshot aggregation job

- [ ] 73. Extended metrics (TASK-JP-0073)
  What to do / Must NOT do: extended metrics (docs/19): conversion rate, response time, source performance, strategy efficacy — per docs/19 formulas. Must NOT fabricate/mislabel metrics; all metrics defined + unit-tested.
  Parallelization: Wave 11 | Blocked by: 72 | Blocks: 74, 75
  References (executor has NO interview context - be exhaustive): docs/19-analytics.md:5-57; docs/27-ai-evaluation.md:6-19 (metric rigor); docs/18-tracking.md
  Acceptance criteria (agent-executable): each extended metric computed per documented formula; fixture data → expected values; unknown/empty input → defined default, no NaN.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=api extended metrics, Evidence .omo/evidence/task-73-jobpilot.txt; failure — metric value deviates from fixture expectation → red.
  Commit: Y | feat(analytics): extended metrics computation

- [ ] 74. Analytics consumer wiring (TASK-JP-0074)
  What to do / Must NOT do: wire analytics into dashboard + strategy + learning consumers (docs/19 §consumer, docs/05:112): read snapshots, expose via API. Must NOT compute on read path; consumers read precomputed snapshots only.
  Parallelization: Wave 11 | Blocked by: 73 | Blocks: —
  References (executor has NO interview context - be exhaustive): docs/19-analytics.md:5-57; docs/05-api-specification.md:112; docs/34-project-structure.md (analytics sub-package); docs/02-system-architecture.md:116-127
  Acceptance criteria (agent-executable): dashboard/strategy consumers read snapshot data via API; response time within docs/31 SLA; no aggregation on read path.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=api analytics consumer, Evidence .omo/evidence/task-74-jobpilot.txt; failure — read path triggers heavy computation → red.
  Commit: Y | feat(analytics): analytics consumer wiring

- [ ] 75. Snapshots endpoint + trends (TASK-JP-0075)
  What to do / Must NOT do: analytics endpoints (docs/05:112): snapshot list, trends over time (weeks/months), per-user scoping. Must NOT leak cross-user analytics; escape all rendered values.
  Parallelization: Wave 11 | Blocked by: 73 | Blocks: —
  References (executor has NO interview context - be exhaustive): docs/05-api-specification.md:112; docs/19-analytics.md:5-57; docs/22-security.md:9-13; docs/30-error-handling.md
  Acceptance criteria (agent-executable): endpoints return snapshots + trend series scoped to authenticated user; empty history → defined empty response; cross-user request → 403/404.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=api analytics endpoints, Evidence .omo/evidence/task-75-jobpilot.txt; failure — IDOR analytics request → 403/404.
  Commit: Y | feat(analytics): snapshots endpoint + trends

### Wave 12 — Phase 12: Scale Hardening (docs/31, docs/32, docs/33)

- [ ] 76. Caching layer (TASK-JP-0076)
  What to do / Must NOT do: caching for AI responses/embeddings/read-heavy endpoints per docs/31 §caching; cache keys scoped per user. Must NOT cache cross-user or sensitive data (docs/24); invalidation on write; TTL per docs/31.
  Parallelization: Wave 12 | Blocked by: 23, 13, 21 | Blocks: 79
  References (executor has NO interview context - be exhaustive): docs/31-performance.md:28-36; docs/06-ai-architecture.md:49-69 (AI cost/cache); docs/24-privacy.md; docs/32-scalability.md:7-25
  Acceptance criteria (agent-executable): cache hit returns stored result; write invalidates affected keys; per-user cache isolation verified; TTL expiry respected.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=perf caching tests, Evidence .omo/evidence/task-76-jobpilot.txt; failure — cross-user cache leak → red.
  Commit: Y | perf(cache): scoped caching with invalidation

- [ ] 77. Rate limiting (TASK-JP-0077)
  What to do / Must NOT do: per-user + per-IP rate limits on API and AI endpoints (docs/32 §rate limiting); 429 with Retry-After. Must NOT rate-limit health checks; limits configurable, not hardcoded.
  Parallelization: Wave 12 | Blocked by: 23 | Blocks: 79
  References (executor has NO interview context - be exhaustive): docs/32-scalability.md:36-42; docs/05-api-specification.md (error envelope); docs/22-security.md; docs/31-performance.md:6-17
  Acceptance criteria (agent-executable): limit exceeded → 429 + Retry-After header; health endpoint exempt; per-user limits independent; config change applies without restart.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=perf rate limiting, Evidence .omo/evidence/task-77-jobpilot.txt; failure — limit bypass via header spoof → red.
  Commit: Y | feat(api): rate limiting

- [ ] 78. Queues + workers (TASK-JP-0078)
  What to do / Must NOT do: message queues + worker pool for async work (notifications, reminders, snapshot job, workflow steps) per docs/32 §queues; at-least-once delivery with idempotent handlers. Must NOT lose jobs on crash; dead-letter queue for poison messages.
  Parallelization: Wave 12 | Blocked by: 8 | Blocks: 79
  References (executor has NO interview context - be exhaustive): docs/32-scalability.md:27-34; docs/33-devops.md:19-40 (compose services); docs/17-workflow-engine.md; docs/18-tracking.md:51-69
  Acceptance criteria (agent-executable): job enqueued → processed exactly-once-effect (idempotent retry); worker crash → job redelivered; poison job → DLQ; no job loss on restart.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=invariants queues, Evidence .omo/evidence/task-78-jobpilot.txt; failure — duplicate processing after redelivery → red.
  Commit: Y | feat(async): queues + worker pool

- [ ] 79. Concurrency hardening (TASK-JP-0079)
  What to do / Must NOT do: concurrency-safe state transitions (docs/32 §1): optimistic locking on application/outcome writes; concurrent workflow runs guarded. Must NOT double-apply transitions; conflict → STATE_CONFLICT (docs/05:148).
  Parallelization: Wave 12 | Blocked by: 76, 77, 78 | Blocks: 80
  References (executor has NO interview context - be exhaustive): docs/32-scalability.md:7-25; docs/05-api-specification.md:148; docs/26-testing.md:64-71 (invariants under concurrency); docs/17-workflow-engine.md
  Acceptance criteria (agent-executable): concurrent writes → one wins, other gets STATE_CONFLICT; invariant tests pass under parallel load; no lost updates (test with threads).
  QA scenarios (name the exact tool + invocation): happy — ./mvnw test -Dgroups=invariants concurrency, Evidence .omo/evidence/task-79-jobpilot.txt; failure — lost update under race → red.
  Commit: Y | feat(async): concurrency-safe state transitions

- [ ] 80. Load + performance tests (TASK-JP-0080)
  What to do / Must NOT do: load/performance suite (docs/31 §load tests) against SLA table (docs/31:6-17): latency, throughput, p95 targets; CI-benchmarked. Must NOT run heavy load against prod DB; isolated benchmark profile.
  Parallelization: Wave 12 | Blocked by: 79 | Blocks: 81
  References (executor has NO interview context - be exhaustive): docs/31-performance.md:6-17, 55-61; docs/33-devops.md:19-40; docs/27-ai-evaluation.md (no flaky thresholds)
  Acceptance criteria (agent-executable): benchmark run against fixtures meets SLA table (p95 latency, throughput); results recorded in evidence; thresholds from docs/31 exactly.
  QA scenarios (name the exact tool + invocation): happy — ./mvnw verify -Dgroups=perf load tests → SLA met, Evidence .omo/evidence/task-80-jobpilot.txt; failure — p95 exceeds SLA → red.
  Commit: Y | test(perf): load + performance suite

- [ ] 81. Observability dashboards + alerting (TASK-JP-0081)
  What to do / Must NOT do: metrics export (docs/29 metrics table 17-26), dashboards + alerts (docs/29:47-59) for latency/errors/AI spend/queue depth; structured logging. Must NOT log PII/credentials (docs/24); alert thresholds from docs/31 SLA.
  Parallelization: Wave 12 | Blocked by: 80 | Blocks: —
  References (executor has NO interview context - be exhaustive): docs/29-observability.md:17-26, 47-59; docs/31-performance.md:6-17; docs/24-privacy.md; docs/33-devops.md (monitoring services)
  Acceptance criteria (agent-executable): metrics endpoint exposes documented metrics; dashboard renders them (compose profile); alert rule fires on threshold breach (tested with injected value); log scrubber test: no PII in logs.
  QA scenarios (name the exact tool + invocation): happy — metrics scrape + dashboard render, Evidence .omo/evidence/task-81-jobpilot.txt; failure — PII appears in logs → red.
  Commit: Y | ops(observability): dashboards + alerting

### Wave 13 — Phase 13: MCP (docs/future/01)

- [ ] 82. MCP gateway + approval gate (TASK-JP-0082)
  What to do / Must NOT do: MCP server exposing JobPilot tools per docs/future/01 §2, with approval gate for destructive/write tools; /start excluded per §3. Must NOT expose raw DB or bypass policy engine; all MCP actions audited.
  Parallelization: Wave 13 | Blocked by: 61, 51, 36 | Blocks: 83, 84
  References (executor has NO interview context - be exhaustive): docs/future/01-mcp-readiness.md:6-33, 41-48; docs/16-agent-orchestration.md:74-104 (policy engine); docs/25-threat-model.md:87-103; docs/24-privacy.md
  Acceptance criteria (agent-executable): MCP server starts in compose profile; tool list matches readiness checklist §5; /start tool absent; write tools require approval; policy engine enforced on every call.
  QA scenarios (name the exact tool + invocation): happy — MCP client handshake + tool listing, Evidence .omo/evidence/task-82-jobpilot.txt; failure — unapproved write call → rejected + audited.
  Commit: Y | feat(mcp): MCP gateway with approval gate

- [ ] 83. MCP read tools (TASK-JP-0083)
  What to do / Must NOT do: read-only MCP tools (jobs, applications, timeline, analytics, memory) per docs/future/01 §2; user-scoped. Must NOT mutate state; read tools return the same data contracts as REST.
  Parallelization: Wave 13 | Blocked by: 82 | Blocks: —
  References (executor has NO interview context - be exhaustive): docs/future/01-mcp-readiness.md:13-25; docs/05-api-specification.md (REST contracts to mirror); docs/30-error-handling.md
  Acceptance criteria (agent-executable): each read tool returns documented shape; tools are read-only (no state change verified); cross-user scoping enforced.
  QA scenarios (name the exact tool + invocation): happy — invoke each read tool via MCP client, Evidence .omo/evidence/task-83-jobpilot.txt; failure — read tool mutates state → red.
  Commit: Y | feat(mcp): read-only MCP tools

- [ ] 84. MCP write tools (TASK-JP-0084)
  What to do / Must NOT do: write tools (create application, record outcome, update notes, trigger workflow) per docs/future/01 §2 with approval gate + audit. Must NOT bypass state machine or policy; every write audited with actor.
  Parallelization: Wave 13 | Blocked by: 82 | Blocks: —
  References (executor has NO interview context - be exhaustive): docs/future/01-mcp-readiness.md:13-25, 41-48; docs/16-agent-orchestration.md:74-104; docs/05-api-specification.md:77-97, 104
  Acceptance criteria (agent-executable): each write tool requires approval; write goes through same service layer as REST (state machine honored); audit row per write; denial path returns typed error.
  QA scenarios (name the exact tool + invocation): happy — approved write creates application via MCP, Evidence .omo/evidence/task-84-jobpilot.txt; failure — write bypassing state machine → rejected.
  Commit: Y | feat(mcp): write MCP tools with approval gate

## Final verification wave
> Runs in parallel after ALL todos. ALL must APPROVE. Surface results and wait for the user's explicit okay before declaring complete.
- [ ] F1. Plan compliance audit
- [ ] F2. Code quality review
- [ ] F3. Real manual QA
- [ ] F4. Scope fidelity

## Commit strategy
- One commit per todo, exactly when its `Commit: Y` line appears; commit message format `<type>(<scope>): <summary>` matching the per-todo Commit line verbatim.
- Scopes follow the roadmap phases (scaffold, auth, security, matching, jobs, strategy, applications, browser, automation, tracking, learning, analytics, perf, ops, mcp); type ∈ feat/fix/test/perf/ops/docs.
- Never combine two todos' changes in one commit; never commit half a todo. If a todo's changes straddle modules, split the diff by module but keep the todo's single Commit line as the record.
- F-tasks (F1–F4) have no Commit line — they are verification gates, not code changes.
- Evidence files (`Evidence .omo/evidence/task-N-jobpilot.txt`) are written during QA and are not part of the feature commit; commit them separately under `test` scope if the repo tracks them.

## Success criteria
- All 84 todos checked and their `Commit: Y` commits landed; dependency matrix satisfied (no todo completed before its Blocked-by set).
- Every todo's Acceptance criteria demonstrably met (the agent-executable check passes and is recorded in `Evidence .omo/evidence/task-N-jobpilot.txt`).
- QA scenarios green: per-task happy-path test passes; each listed failure scenario produces the named typed error (no silent success).
- F1 Plan compliance audit: plan scope, guardrails, and Must NOT boundaries all honored in code; F2 Code quality review: build + test suite green, no injected slop; F3 Real manual QA: the app demonstrably works end-to-end for a human; F4 Scope fidelity: nothing outside scope added. ALL four APPROVE and the user gives explicit okay before the plan is declared complete.
