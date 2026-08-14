# 10 — Career Strategy Engine

Tier: **ADVANCED**. Owning module: `career`.

## 1. Purpose

Answers "which jobs *should* this candidate pursue," distinct from
per-job matching which answers "how well does this candidate fit *this*
job." Strategy operates at the role-category level, not the individual
posting level.

## 2. Inputs

Candidate skills/experience/trajectory (doc 07), aggregate market signal
(distribution of `requiredSkills` across recently discovered Jobs in
relevant categories), historical `JobMatch`/`Application`/
`ApplicationOutcome` data (doc 21), `JobPreference`, and prior
`CareerStrategy` (for continuity — strategy shouldn't oscillate wildly run
to run without new evidence).

## 3. Output Shape

```
CareerStrategy {
  primaryTargets:   ["Java Backend Engineer", "Spring Boot Developer"]
  secondaryTargets: ["Software Engineer", "Full Stack Developer"]
  stretchTargets:   ["AI Engineer", "Platform Engineer"]
  allocationPlan:   { "Java Backend": 0.40, "Software Engineer": 0.25,
                       "Spring Boot": 0.20, "Full Stack": 0.10,
                       "AI Engineer": 0.05 }
  rationale: "<evidence-grounded explanation>"
}
```

`primaryTargets` = strong current fit + healthy market demand.
`secondaryTargets` = solid fit, broader net. `stretchTargets` = partial fit
today, worth pursuing selectively for growth or where career trajectory
(doc 07 §6) suggests a deliberate pivot. Every target category entry must
be traceable to specific matching evidence — this list is not
free-generated; it is derived from aggregating existing `JobMatch` data
across discovered postings in that category plus skill-gap analysis.

## 4. Generation Pipeline

```
Aggregate JobMatch scores by normalized role category (last N days)
   ↓ identify categories with high average overallMatch + sufficient
     posting volume → candidate primary/secondary targets
   ↓ identify adjacent categories with moderate match + explicit
     transferableSkills overlap (doc 07) → candidate stretch targets
   ↓ ai.AiService (task=RESUME_REASONING profile) synthesizes rationale
     text and proposes the allocation split, constrained to only the
     categories identified above (the model ranks/writes rationale, it
     does not invent new categories from nothing)
   ↓ CareerStrategy persisted; prior strategy row gets supersededBy set,
     never deleted — full strategy history retained
```

## 5. Adaptivity (feeds from doc 21)

Regeneration is triggered on schedule (weekly default, configurable) and
on-demand. The Learning System (doc 21) supplies response-rate-by-category
signals that shift `allocationPlan` over time — e.g., a category with
strong match scores but consistently low response rate gets its allocation
reduced even though raw match quality hasn't changed, and `rationale`
states this explicitly ("reduced allocation: low historical response
despite strong technical match") rather than silently reweighting.

## 6. Relationship to Automation (doc 11)

`CareerStrategy.allocationPlan` informs which jobs get proactively
surfaced/prioritized for application preparation under `AUTO_WITHIN_RULES`
automation mode, but it never itself triggers an application — strategy is
advisory input to Application Strategy (doc 11), not an execution trigger.

## 7. Failure Handling

Insufficient data (new candidate, few discovered jobs yet) yields a
strategy with wider, lower-confidence target sets and a rationale that
says so explicitly, rather than blocking strategy generation or fabricating
confident-sounding targets from thin evidence.
