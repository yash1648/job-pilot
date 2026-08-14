# 02 — System Architecture

## 1. Architectural Style

JobPilot is built as a **modular monolith** (ADR-001): a single Spring Boot
deployable, internally partitioned into strongly-bounded modules with
enforced dependency rules (doc 34), sharing one PostgreSQL database (doc 04)
but never sharing internal state across module boundaries except through
published interfaces. This is chosen over microservices-from-day-one because
the domain is still coupled (candidate → job → match → application is one
continuous decision chain) and premature service boundaries would force
distributed transactions across that chain before the domain is even stable.
Doc 32 documents the extraction candidates and the conditions under which
each becomes a standalone service.

## 2. Layered View

```
┌─────────────────────────────────────────────────────────────┐
│  Frontend (Thymeleaf now → React+Vite later, same REST API)  │
├─────────────────────────────────────────────────────────────┤
│  API Layer (Spring MVC controllers, doc 05)                  │
├─────────────────────────────────────────────────────────────┤
│  Application/Orchestration Layer                              │
│    Workflow Engine · Agent Orchestration · Policy Engine      │
├───────────────┬───────────────┬───────────────┬──────────────┤
│  Candidate     │  Job          │  Matching &   │  Application │
│  Intelligence  │  Discovery/   │  Career       │  Engine +    │
│                │  Intelligence │  Strategy     │  Browser     │
├───────────────┴───────────────┴───────────────┴──────────────┤
│  Cross-Cutting Services: AI Provider Abstraction, RAG,        │
│  Tracking, Analytics, Notification, Security, Audit           │
├─────────────────────────────────────────────────────────────┤
│  Persistence: PostgreSQL + pgvector · Redis (cache/queue) ·   │
│  Object storage (resumes, generated documents)                │
├─────────────────────────────────────────────────────────────┤
│  External: Ollama (AI) · Playwright browsers · Job sources /  │
│  ATS adapters                                                 │
└─────────────────────────────────────────────────────────────┘
```

## 3. Module Map (mirrors package structure, doc 34)

| Module | Responsibility | Depends on |
|---|---|---|
| `auth` | authentication, sessions, JWT | `security`, `user` |
| `user` | account, preferences | `common` |
| `candidate` | profile aggregate root | `resume`, `skill`, `experience`, `education`, `project`, `certification`, `ai` |
| `resume` | ingestion, parsing, versioning | `storage`, `ai` |
| `skill` / `experience` / `education` / `project` / `certification` | evidence sub-entities | `candidate` |
| `company` | company intelligence | `ai`, `jobs` |
| `jobs` | discovery, normalization, job intelligence | `ai`, `company` |
| `matching` | hybrid matching, opportunity ranking | `candidate`, `jobs`, `ai` |
| `career` | strategy engine, career memory | `matching`, `tracking` |
| `ai` | provider abstraction, model routing, RAG | none (foundation) |
| `application` | strategy, documents, question engine, state machine | `candidate`, `jobs`, `matching`, `ai` |
| `browser` | browser agent, form intelligence, platform adapters | `application`, `security` |
| `workflow` | session/state persistence, pause/resume | `application`, `browser` |
| `tracking` | lifecycle timeline, notifications | `application` |
| `analytics` | aggregation, dashboards | `tracking`, `career` |
| `security` | authz, policy engine, threat mitigation | `common` |
| `audit` | append-only event log | `common` |
| `storage` | file/document persistence | `common` |
| `common` | shared kernel: value objects, exceptions, base entities | none |

Dependency rule: arrows point one direction only — `common` and `ai` have no
dependents that loop back into them, and no module reaches into another
module's repository/entity classes directly; cross-module access goes
through a published service interface. Doc 34 enforces this with build-level
checks (module boundary tests).

## 4. Request Flow — Illustrative (Match → Apply)

```
User views job → API → matching.MatchingService (cached JobMatch)
   ↓
User approves application → API → application.ApplicationStrategyService
   ↓ decides mode (AUTO/ASSISTED/MANUAL) using jobs.JobSourceIntelligence
   ↓
application.DocumentGenerationService (resume/cover letter/answers via ai.RagService)
   ↓
workflow.WorkflowEngine creates ApplicationSession, state → PREPARING → READY
   ↓ (if AUTO/ASSISTED) browser.BrowserAgent executes validated actions
   ↓ (if MANUAL) application prepares package, state → WAITING_FOR_USER handoff
   ↓
tracking.ApplicationTracker records lifecycle event, audit.AuditService logs
   ↓
notification.NotificationService informs user
```

## 5. AI Integration Points (detailed in doc 06)

AI is never called directly from controllers or from `browser`. All AI
access goes through the `ai` module's `AiService` / `EmbeddingService` /
`VisionService` interfaces, and any AI output that will drive an action
(browser action, generated claim, application decision) passes through the
Policy Engine (`security`) before it is allowed to take effect. This is the
single choke point that makes the Zero-Fabrication Policy and prompt-
injection defenses enforceable rather than aspirational (doc 23).

## 6. Data Flow Boundaries

Three trust boundaries recur throughout the system and are referenced by
name in later documents:

- **Trusted boundary**: user-authored input (preferences, approvals,
  resume upload as a *file*, not its content) and JobPilot's own generated
  system prompts.
- **Untrusted content boundary**: resume *content*, job description text,
  company page text, application page DOM/text. Always treated as data,
  never as instructions, when passed to an AI model (doc 23).
- **Execution boundary**: the Policy Engine, which is the only path by which
  an AI-proposed action becomes a real browser action, a real submitted
  application, or a persisted claim about the candidate.

## 7. Frontend Architecture

Initial frontend: server-rendered Thymeleaf views consuming the same REST
API defined in doc 05 (no server-side-only endpoints — this constraint is
what makes the React+Vite migration additive rather than a rewrite). Core
screens: Dashboard, Candidate Profile, Resume Manager, Job Search &
Recommendations, Job Details / Match Analysis, Application Center /
Cockpit, Manual Handoff view, Application Timeline, Analytics, Career
Strategy, Settings / Automation Settings, Notifications. The Application
Cockpit and Manual Handoff / Assisted Mode views are specified in detail in
docs 11 and 15, since their content is generated by those modules.

## 8. Deployment Topology (detailed in doc 33)

Single Spring Boot application + PostgreSQL + Redis + Ollama + headless
Playwright browser pool, orchestrated via Docker Compose in dev/staging and
containerized with the same images in production. Browser execution runs in
isolated, resource-capped containers/processes so a hung or malicious page
cannot affect the main application process.

## 9. Why Not Microservices Yet (ADR-001 summary)

The core decision chain (candidate → match → apply) needs strong
consistency and low-latency internal calls during a single workflow
execution; splitting it into services now would mean distributed
transactions or eventual consistency across a chain where a
duplicate-application bug is a real user-facing failure, not a
resolved-on-retry inconvenience. Doc 32 defines the specific signals
(independent scaling need, independent deploy cadence, team boundary) that
justify extracting `browser`, `ai`, `notification`, or `analytics` later.
