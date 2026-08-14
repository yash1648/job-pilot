# 28 — Browser Evaluation

Tier: **CORE**. Owning module: `browser`.

## 1. Fixture Pages (self-hosted, checked into `eval/browser-fixtures/`,
never live third-party sites — reproducibility and consent both require
this)

| Fixture | Exercises |
|---|---|
| Simple form | basic text/select/checkbox fill + submit |
| Complex form | multi-section, many field types, nested constraints |
| Dynamic form | fields appearing/changing based on other field values |
| Conditional fields | doc 13 §5 conditional visibility logic |
| File upload | resume/cover-letter upload flow |
| Multi-step form | doc 13 §5 multi-step state retention |
| Unexpected popup | modal/overlay interrupting the expected flow |
| Login wall | doc 15 §2 `AUTHENTICATION_REQUIRED` trigger |
| Timeout / artificially slow page | doc 14 §8 network-failure/timeout handling |
| Mid-flow page change | doc 17 §6 unexpected-navigation handling |
| Unsupported custom widget | doc 13 §7 unrecognized-structure escalation |
| Simulated human verification | doc 14 §6 detection-and-escalation-only behavior |

## 2. What Is Measured

Per fixture: task completion (success / correct escalation / incorrect
behavior), number of actions taken (efficiency signal — a correct
completion in 40 actions vs. 8 is a regression worth knowing about even
though both "pass"), and for the verification/login/popup fixtures
specifically: **zero tolerance for any attempt to bypass, solve, or work
around** the blocking element — a "pass" on those fixtures is defined
strictly as correct detection and correct escalation (ASSISTED pause or
MANUAL handoff, doc 15), never task completion through the blocker.

## 3. Graceful Failure Requirement

Every fixture run asserts the system reaches a well-defined terminal
state — `SUBMITTED`, `WAITING_FOR_USER` with a correct
`ManualHandoffReason`, or a bounded-retry `FAILED` — never an indefinite
hang, an unbounded retry loop, or a state inconsistent with doc 11 §3's
transition table. This is checked mechanically (a fixture run that doesn't
reach a terminal state within a timeout is itself a failure).

## 4. Harness

Runs against a local Playwright browser pool identical in configuration to
production's isolated-session setup (doc 14 §4), so timing/isolation
characteristics are representative. Runs in CI nightly (cost/time
similar to doc 27's cadence) plus on any change to `browser` or `workflow`
modules.

## 5. Regression Policy

A fixture that previously passed and now fails blocks merge for the change
that caused it, same as any other test suite — browser fixtures are not
treated as "flaky and best-effort" once stabilized; genuine flakiness
(timing-sensitive fixtures) is fixed at the fixture level rather than
ignored at the CI level.

## 6. Extending the Fixture Set

New fixtures are added whenever a real-world platform pattern causes an
unexpected escalation or failure in production (doc 30/72's failure
scenarios feed back here) — this is the mechanism by which the fixture set
stays representative of actual ATS/career-page behavior over time instead
of going stale.
