# 34 — Project Structure

Tier: **CORE**.

## 1. Package Structure (matches doc 02 §3 module map exactly)

```
com.jobpilot
├── auth
├── user
├── candidate
│   ├── resume
│   ├── skill
│   ├── experience
│   ├── education
│   ├── project
│   └── certification
├── company
├── jobs
├── matching
├── career
├── ai
│   ├── provider          (Ollama impl, future providers)
│   ├── routing
│   └── rag
├── application
│   ├── strategy
│   ├── documents
│   └── questions
├── browser
│   ├── agent
│   ├── forms
│   └── adapters           (Greenhouse/Lever/Workable/Ashby/Generic)
├── workflow
├── tracking
├── analytics
├── notification
├── security
│   ├── auth
│   └── policy
├── audit
├── storage
└── common
    ├── exception
    ├── valueobject
    └── config
```

Each top-level package = one module from doc 02 §3. Sub-packages exist only
where doc 06–21's internal structure (e.g., `ai.provider` vs `ai.routing`
vs `ai.rag`) justifies the split.

## 2. Per-Module Internal Layout

```
candidate/
├── api/          (controllers, DTOs — doc 05 contract)
├── domain/        (entities, doc 03)
├── repository/    (Spring Data repositories)
├── service/       (business logic)
└── config/        (module-local Spring config, if any)
```

## 3. Dependency Rules (enforced, doc 02 §3)

- Arrows point one direction; `common` and `ai` have zero dependents that
  loop back.
- No module imports another module's `repository` or `domain` package
  directly — cross-module access goes through the other module's `service`
  interface (published in a `service/api` sub-package if the module needs
  to expose a narrower contract than its full service class).
- Enforced by an ArchUnit test suite (doc 35, doc 26) run in CI — a
  forbidden import fails the build, not just a code-review convention.

## 4. Build Tool

Maven (multi-module optional at this stage — a single module with strict
package-level ArchUnit enforcement is preferred over premature Maven
multi-module split, consistent with doc 02's "modular monolith, not
microservices-shaped build" stance; revisited only if the doc 32 §4
extraction candidates actually get extracted).

## 5. Test Source Layout

Mirrors main source (`src/test/java/com/jobpilot/<module>/...`); fixture
resources (doc 26 §2, doc 27 §2, doc 28 §1) live under
`src/test/resources/fixtures/` and `eval/fixtures/` (top-level, shared
between the AI/browser eval harnesses and any manual review tooling).

## 6. Configuration

`application.yml` per profile (`dev`, `test`, `staging`, `prod`), module-
local `@ConfigurationProperties` classes rather than scattered
`@Value` injections, so each module's tunables (doc 06 §3 budgets, doc 09
§3 match weights, doc 23 §4 confidence threshold) are discoverable in one
place per module.
