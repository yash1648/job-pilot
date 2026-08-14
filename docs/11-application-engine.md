# 11 — Application Engine

Tier: **CORE** (strategy, three modes, state machine, automation control),
**ADVANCED** (batch applications). Owning module: `application`.

## 1. Application Strategy

Before any document generation or execution, `ApplicationStrategyService`
produces an `ApplicationPlan` (doc 03) for a `JobMatch` the user (or an
automation rule) has selected:

```
JobMatch (from doc 09) + CareerStrategy context (doc 10)
   ↓ shouldApply decision (deterministic thresholds + AI judgment for
     borderline cases, always explainable via `rationale`)
   ↓ resume version selection: reuse an existing job-relevant
     ResumeVersion or trigger new tailoring (doc 12)
   ↓ emphasizedProjects / emphasizedExperience selection, from
     projectMatch/experienceMatch sub-scores (doc 09)
   ↓ coverLetterStrategy (angle: which achievements, which company
     signals to reference — doc 12 executes it)
   ↓ questionsRequiringCare: anticipated from JobRequirement +
     historical question patterns for this JobSource/platform (doc 21)
   ↓ automationFeasible: from Job.applicationMethod (doc 08 §3)
   ↓ approvalRequired: from JobPreference.automationMode (§5 below)
```

`shouldApply = false` still records the ApplicationPlan (with rationale)
so the user can see *why* JobPilot didn't propose applying — it is a
decision artifact, not a silent skip.

## 2. Three Application Modes

`ApplicationMode = AUTO | ASSISTED | MANUAL`, decided by
`Job.applicationMethod` intersected with the user's `automationMode`
setting — a job that is technically `AUTO_CAPABLE` still runs as
`ASSISTED` or triggers an approval gate if the user's automation setting
requires it. The job's technical capability is a ceiling, never a floor
that overrides user control.

- **AUTO**: `browser.BrowserAgent` (doc 14) executes the full submission
  unattended, only after any required approval (§5) has been recorded.
- **ASSISTED**: same execution path, but the Workflow Engine (doc 17)
  pauses at defined checkpoints (verification challenge, missing required
  field with no evidence, unexpected page/flow) and resumes on user action.
- **MANUAL**: no browser execution of submission at all. `application`
  builds a package (doc 15) and the state machine goes straight from
  `PREPARING`/`READY` to `WAITING_FOR_USER`.

## 3. Application State Machine

States (doc 03): `DISCOVERED, ANALYZING, MATCHED, SELECTED, PREPARING,
READY, WAITING_FOR_APPROVAL, APPLYING, WAITING_FOR_USER, SUBMITTED,
TRACKING, INTERVIEW, OFFER, REJECTED, WITHDRAWN, FAILED`.

Valid transitions:

```
DISCOVERED → ANALYZING            (Job Intelligence run, doc 09)
ANALYZING → MATCHED               (JobMatch computed)
MATCHED → SELECTED                (user or automation selects for pursuit)
SELECTED → PREPARING              (ApplicationPlan + documents generation begins)
PREPARING → READY                 (documents+answers generated, validated)
PREPARING → FAILED                (generation failure, doc 30)
READY → WAITING_FOR_APPROVAL      (approvalRequired = true)
READY → APPLYING                  (approvalRequired = false, mode=AUTO/ASSISTED)
READY → WAITING_FOR_USER          (mode=MANUAL)
WAITING_FOR_APPROVAL → APPLYING   (user approves, mode=AUTO/ASSISTED)
WAITING_FOR_APPROVAL → WAITING_FOR_USER (user approves, mode=MANUAL)
WAITING_FOR_APPROVAL → WITHDRAWN  (user declines)
APPLYING → WAITING_FOR_USER       (ASSISTED checkpoint hit, or AUTO/ASSISTED
                                    execution determines MANUAL is actually
                                    required mid-flow — see doc 15 §Fallback)
APPLYING → SUBMITTED              (execution completes successfully)
APPLYING → FAILED                 (unrecoverable execution error, doc 30)
WAITING_FOR_USER → SUBMITTED      (user completes and marks Applied)
WAITING_FOR_USER → WITHDRAWN      (user abandons)
SUBMITTED → TRACKING              (enters lifecycle tracking, doc 18)
TRACKING → INTERVIEW → OFFER      (outcome updates, doc 18)
TRACKING → REJECTED               (outcome update)
{any non-terminal state} → WITHDRAWN  (explicit user cancel)
{any non-terminal state} → FAILED     (unrecoverable system error)
```

`SUBMITTED, TRACKING, INTERVIEW, OFFER, REJECTED, WITHDRAWN, FAILED` are
terminal for the *execution* workflow (`Workflow`/`ApplicationSession`
close out); `TRACKING → INTERVIEW/OFFER/REJECTED` remain open for the
*tracking* lifecycle (doc 18), which is a distinct concern from execution
state.

## 4. Automation Control

`JobPreference.automationMode`:

- `FULLY_MANUAL` — every Application requires explicit user creation and
  execution step; API blocks `/start` with `POLICY_BLOCKED` otherwise.
- `APPROVE_EVERY_APPLICATION` — `approvalRequired = true` unconditionally.
- `APPROVE_PER_COMPANY` — `approvalRequired = true` for the first
  Application to a given Company; subsequent ones inherit approval unless
  the user revokes it.
- `APPROVE_PER_BATCH` — approval granted once per batch (§5.30/§6 below),
  not per individual Application within it.
- `AUTO_WITHIN_RULES` — `approvalRequired = false` only when the
  Application also satisfies explicit user-defined rules (allow-listed
  role categories, salary floor, company deny-list check, max applications
  per day) — evaluated by the Policy Engine (doc 47), not by
  `application` module logic directly, so the same rule engine also gates
  browser actions.

The system always renders what it is about to do before an AUTO execution
actually fires — the Application Cockpit (§7) shows the full plan, and
`AUTO_WITHIN_RULES` executions still generate a pre-execution
`ApplicationEvent` the user can review even though no blocking approval
was required.

## 5. Batch Applications (ADVANCED)

```
User selects N jobs (or automation proposes a batch under AUTO_WITHIN_RULES)
   ↓ analyze each → ApplicationPlan
   ↓ generate documents/answers for each (doc 12)
   ↓ validate (schema + evidence + duplicate check, doc 03 invariant #3)
   ↓ batch review surface: per-job summary, flags, estimated mode split
   ↓ user approves batch (or per-company/per-item exceptions)
   ↓ execute AUTO/ASSISTED-eligible items; MANUAL items go straight to
     handoff packages, all within the same batch operation
```

Duplicate prevention applies per-item exactly as it does for single
applications (doc 04 unique index) — a batch cannot create two
Applications for the same candidate/job pair, including against an
in-flight (non-terminal) Application from an earlier, still-active batch.

## 6. Application Cockpit (UI contract, rendered from this module's data)

Per-application view surfaces: Job summary, match score + reasons (doc 09),
selected resume version, cover letter, answers, mode, automation status,
current state, any error, any required user action, and the application
URL. All of this is direct read of `Application` + joined entities — the
UI does not compute anything the backend hasn't already persisted.

## 7. Failure Handling

| Failure | Behavior |
|---|---|
| Document generation fails during PREPARING | state → `FAILED`, `ApplicationEvent` records reason, user notified, retry available |
| Approval required but user never responds | Application remains `WAITING_FOR_APPROVAL` indefinitely; reminder notification scheduled (doc 18), never auto-approved by timeout |
| Automation rule evaluation errors | treated as `approvalRequired = true` (fail closed, never fail open into unattended submission) |
