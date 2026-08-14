# 21 — Learning System

Tier: **ADVANCED**. Owning module: `career` (learning sub-component),
consuming `tracking`, `analytics`.

## 1. Purpose

Closes the feedback loop: outcomes observed downstream (doc 18) feed back
into upstream decisions (matching priors, strategy allocation, resume
approach) — without ever silently rewriting the candidate's actual
evidence or making unexplained decisions.

## 2. Inputs → Signals

| Input | Derived signal | Consumer |
|---|---|---|
| Applications + JobMatch scores | correlation between match sub-scores and response rate, by role category | doc 09 `successProbability` prior (feeds `opportunityScore`) |
| ApplicationOutcome by resumeVersionId | resume-version performance | doc 12 (informs which tailoring emphasis patterns to prefer) |
| ApplicationOutcome by jobSourceId | source performance | doc 08 (informs discovery scheduling priority, doc 33) |
| ApplicationOutcome by role category | strategy allocation adjustment | doc 10 `allocationPlan` |
| ApplicationAttempt failure patterns | automation reliability by platform/adapter | doc 17 (flags adapters needing attention) |

## 3. Signal Computation — Deterministic First

All signals above are computed as deterministic statistical aggregates
(response rate = outcomes / applications within a bucket, with a minimum
sample-size threshold before a signal is considered reliable enough to
act on) — not learned via an opaque model retrain. This keeps every
downstream adjustment traceable to "N applications, X% response, therefore
Y" rather than an unexplainable weight shift.

```
ApplicationOutcome events (scheduled aggregation, same cadence as doc 19)
   ↓ bucket by category (role/source/resumeVersion/platform)
   ↓ compute rate + sample size
   ↓ apply minimum-sample-size gate (signal not emitted below threshold)
   ↓ compare against prior period (trend direction, not just point value)
   ↓ signal persisted (feeds doc 09/10/20/08 consumers via read, not push)
```

## 4. Bounded LLM Use

Where a signal needs qualitative synthesis (e.g., a `CareerMemory`
statement's phrasing, doc 20 §3), that pass is bounded to phrasing already-
computed numbers — never used to invent a signal that the deterministic
aggregation didn't already produce. Recommendations that consumers act on
(doc 10 §5 allocation shift, doc 09 prior adjustment) always carry the
underlying evidence (sample size, rate, comparison basis) so they remain
explainable in UI (doc 01 §28's explicit requirement).

## 5. Non-Mutation Guarantee (restated from doc 20 §5)

Learning System outputs never modify `CandidateProfile`, `Skill`,
`SkillEvidence`, or any resume/document content directly. They adjust
*decision inputs* (priors, weights, allocation percentages) used by other
services — those services remain responsible for their own
evidence-validation and explainability requirements (docs 09, 10, 12).

## 6. Cold Start

New candidates or new role categories with no outcome history yet use
documented default priors (configurable, doc 26 test fixtures cover the
cold-start case explicitly) rather than a signal computed from zero/near-
zero sample size — this prevents a single early rejection or single early
success from producing an outsized, statistically meaningless adjustment.

## 7. Failure Handling

| Failure | Behavior |
|---|---|
| Aggregation job fails | prior signals remain in effect (stale but not wrong-direction), retried next scheduled window |
| Sample size never reaches threshold for a category | that category's consumers keep using cold-start defaults indefinitely — never forced into an unreliable signal |
