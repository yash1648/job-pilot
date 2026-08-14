# 17 — Workflow Engine

Tier: **CORE**. Owning module: `workflow`, `browser` (platform adapters).

## 1. Purpose

Owns the *execution* lifecycle of a single `Application` — distinct from
the `ApplicationState` machine (doc 11 §3), which tracks the
business-meaning state. `Workflow`/`WorkflowStep` (doc 03/04) track the
concrete step sequence within `PREPARING`/`APPLYING`, so a crash or pause
mid-step doesn't require replaying already-completed work.

## 2. Workflow Steps (typical AUTO/ASSISTED sequence)

```
RESOLVE_PLATFORM_ADAPTER
GENERATE_DOCUMENTS        (delegates to doc 12)
OPEN_APPLICATION          (delegates to doc 14 navigate)
INSPECT_FORM              (delegates to doc 13)
RESOLVE_FIELD_ANSWERS     (doc 13 §4 resolution order)
FILL_FORM                 (doc 14 action loop)
REVIEW_BEFORE_SUBMIT      (final validation pass — required fields
                            present, no evidence-validation flags open)
SUBMIT
CONFIRM_SUBMISSION        (verify success signal, not just "no error")
```

Each `WorkflowStep` persists `status` and a `resumeToken` capturing exactly
what's needed to continue from that point (e.g., for `FILL_FORM`, the
token references the current `formState` snapshot). A step in `FAILED`
halts the workflow and surfaces to `application.state` handling (doc 11
§7) rather than the engine silently skipping ahead.

## 3. Session Management

`ApplicationSession` (doc 03/04) is the workflow's persisted execution
context. Supported operations:

- **pause**: explicit (user/ASSISTED checkpoint) or implicit (process
  shutdown/timeout) — `status → PAUSED`, current step's `resumeToken`
  persisted.
- **resume**: re-attach or re-derive `BrowserSession` (doc 14 §7), resume
  from the paused `WorkflowStep`, not from `RESOLVE_PLATFORM_ADAPTER`.
- **retry**: re-attempt the current step after a transient failure
  (bounded count, doc 30), creating a new `ApplicationAttempt` row if the
  whole workflow is being retried rather than a single step.
- **cancel**: `status → ABANDONED`, `Application.state → WITHDRAWN`.
- **handoff**: transition to Manual Handoff (doc 15), preserving
  `formState` for partial-progress display.
- **recover**: on application-server restart, any session left `ACTIVE`
  (not cleanly `PAUSED`) is reconciled to `PAUSED` with
  `lastErrorRef = "process_restart"` rather than resumed blind — recovery
  always re-validates current page state via a fresh `INSPECT_FORM` step
  before continuing, since the real browser/page may have changed while
  the process was down.

## 4. Platform Adapter System

```
ApplicationProvider (interface: prepare(), execute(WorkflowStep))
├── GreenhouseProvider
├── LeverProvider
├── WorkableProvider
├── AshbyProvider
├── CompanyCareerProvider   (for structured, known company-page flows)
└── GenericBrowserProvider  (Form Intelligence + Browser Agent, no
                             platform-specific shortcuts)
```

`RESOLVE_PLATFORM_ADAPTER` selects a provider from `Job.jobSourceId` /
`Job.applicationUrl` pattern matching; unmatched platforms fall through to
`GenericBrowserProvider` rather than failing — adapters are an
optimization (fewer, more reliable steps for well-known ATS UIs), not a
requirement for a platform to be supported at all. This mirrors doc 08 §2's
principle that adapters exist only where justified.

## 5. Concurrency & Locking

One active `ApplicationSession` per `Application` (enforced by the unique
constraint on `application_sessions.application_id`, doc 04). A
distributed lock (Redis) guards the transition into `APPLYING` so a
duplicate concurrent trigger (e.g., a retried API call and a scheduled
retry firing close together) cannot start two browser executions for the
same application — ties into doc 51 concurrency design.

## 6. Failure Handling

| Failure | Behavior |
|---|---|
| Step fails, retries exhausted | `WorkflowStep.status=FAILED`, workflow halts, `Application.state` handling per doc 11 §7/doc 30 |
| Adapter-specific execution error (platform UI changed) | falls back to `GenericBrowserProvider` mid-workflow only if the failure is isolated to a step that `GenericBrowserProvider` can also perform (e.g., a fill step); a fundamentally adapter-dependent failure (e.g., adapter's auth flow) escalates to Manual Handoff instead of a risky provider-swap mid-session |
| Resume attempted on an expired/stale session (TTL exceeded, doc 14 §7) | fresh `INSPECT_FORM` forced before any fill action, per §3 recover behavior |
