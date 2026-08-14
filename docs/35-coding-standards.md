# 35 — Coding Standards

Tier: **CORE**.

## 1. Java / Spring Style

Java 21 features used where they improve clarity (records for value
objects and DTOs, sealed interfaces for closed hierarchies like
`BrowserAction`, doc 14 §2; pattern matching for `switch` over those
sealed types). Constructor injection only, no field injection. Services
are interfaces + implementation only where a module needs to expose a
narrower contract to other modules (doc 34 §3); otherwise a concrete
`@Service` class is enough — no interface-for-every-class ceremony.

## 2. DTO Conventions

Records, one DTO per API operation shape (request/response are distinct
types even when nearly identical) — never expose a JPA entity directly
through a controller. Validation annotations (`@NotNull`, `@Size`, etc.)
live on the DTO, not the entity.

## 3. Entity Conventions

UUID `@Id`, `@Version` for optimistic locking on frequently-concurrently-
updated entities (`Application`, `ApplicationSession`), `created_at`/
`updated_at` via `@CreationTimestamp`/`@UpdateTimestamp`. Entities are
package-private to their module's `domain` package where doc 34 §3's
cross-module rule requires it, exposed to other modules only via DTOs
returned from a service interface.

## 4. Service Conventions

One clear responsibility per service class (a `MatchingService` computes
matches; it does not also send notifications — that's a `NotificationService`
call from the orchestrating layer, doc 02 §4). Transactional boundaries
declared explicitly (`@Transactional`) at the service method that owns
the unit of work, not scattered across repository calls.

## 5. Repository Conventions

Spring Data JPA repositories, custom queries via `@Query` (JPQL) for
anything beyond derived-method-name queries; raw native SQL only where
pgvector operators require it (doc 04 §4), isolated in a clearly-named
method.

## 6. Controller Conventions

Thin — validate input (via DTO annotations + `@Valid`), delegate to a
service, map result/exception to the doc 05 §Errors envelope via a shared
`@ControllerAdvice`. No business logic in controllers, ever — this is
checked in review, not just stated.

## 7. Exception Handling

Doc 30's taxonomy used throughout; a shared `@ControllerAdvice` maps each
exception category to the correct HTTP status/error code (doc 05 §12)
consistently across all controllers, so error-handling behavior doesn't
drift module to module.

## 8. Validation

Bean Validation (`jakarta.validation`) on DTOs for structural validation;
domain-rule validation (doc 03 §6 invariants) lives in service methods,
not annotations, since those rules involve cross-entity/cross-table
checks annotations can't express.

## 9. Logging

Structured, via the shared logging utility (doc 29 §1) — never
`System.out.println`, never raw string-concatenated log messages with
unredacted sensitive fields.

## 10. Transactions

Explicit, method-level, sized to the smallest unit that must be atomic
(doc 03 §6 invariant enforcement, e.g., the duplicate-application check
and the `Application` insert happen in one transaction).

## 11. Testing

Doc 26 conventions: unit tests colocated by module, fakes over mocks
where a fake better represents real behavior (e.g., a fake `AiService`
returning canned structured responses rather than a Mockito mock stubbing
every method), ArchUnit tests for doc 34 §3 dependency rules.

## 12. Naming

Entities: singular noun matching doc 03 exactly (`Application`, not
`Applications` or `ApplicationEntity`). Services: `{Noun}Service`.
Repositories: `{Entity}Repository`. DTOs: `{Noun}{Request|Response}`.
Enums: exactly the values listed in doc 03 (`ApplicationState`,
`ApplicationMode`, etc.) — no silent renaming or extra values introduced
without updating doc 03 first (doc 00 §Consistency Requirement).

## 13. Documentation (in-code)

Javadoc on public service interfaces explaining the *why* (business rule,
not restating the method signature); complex domain logic (matching
weights, doc 09 §3; Policy Engine predicates, doc 16 §4) gets an inline
comment linking back to the relevant docs file/section for traceability.

## 14. Agent/Component Classification Discipline

Restated from doc 16 §1: before implementing a new capability, classify it
(deterministic service / rule engine / workflow / LLM capability / agent)
and implement at that classification's complexity level — this is a
required step in the PR description template, not just guidance.

## 15. Config Defaults (canonical values, referenced by other docs)

`CONFIDENCE_THRESHOLD = 0.75` (doc 03 invariant #2, doc 23 §4) — default,
environment-overridable, per-question-category-overridable (doc 12 §3).
Minimum sample size for Learning System signals (doc 21 §3): `20`
applications per bucket, configurable.
