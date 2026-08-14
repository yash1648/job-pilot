# 20 — Career Memory

Tier: **ADVANCED**. Owning module: `career`.

## 1. Purpose

A structured, explainable long-term record about the candidate's
job-search behavior and outcomes — distinct from `CandidateProfile`
(what the candidate *is*) and `CareerStrategy` (what to *do next*).
`CareerMemory` is what's been *observed* over time.

## 2. Categories (`CareerMemory.category`, doc 03)

`RESPONSE_PATTERN` ("Applied to 12 Java Backend roles, 40% response
rate"), `SKILL_GAP` ("AWS missing from 60% of high-match rejected
applications"), `STRONG_MATCH_CATEGORY` ("Spring Boot roles consistently
score >85% match"), `LOW_RESPONSE_CATEGORY` ("Full Stack roles: strong
match, low response — possible market saturation or resume framing
issue").

## 3. Generation

`CareerMemoryService` runs on the same schedule as Analytics (doc 19) and
Learning System updates (doc 21), deriving statements from aggregated
`Application`/`JobMatch`/`ApplicationOutcome` data with a bounded LLM pass
to phrase the statement — the underlying numbers are always deterministic
aggregates, the LLM only phrases the observation and every statement
carries `evidenceRefs` pointing at the specific Applications/JobMatches
that support it (same evidence-linkage discipline as candidate evidence,
doc 07 §4). A statement with no supporting aggregate signal is not
generated — this is not a creative-writing pass.

## 4. Consumers

- Doc 10 Career Strategy reads `SKILL_GAP` and `LOW_RESPONSE_CATEGORY`
  memories directly into its `rationale` generation.
- Doc 09 Matching can surface relevant memory in `MatchReason` context
  (e.g., a `MISSING: AWS` reason can note "flagged in 60% of similar
  rejected applications" when a supporting memory exists) — additive
  context, not a change to the underlying score.
- Application Cockpit (doc 11 §6) can surface relevant memory alongside a
  specific job (e.g., a `LOW_RESPONSE_CATEGORY` memory shown when viewing
  a job in that category), purely informational.

## 5. Non-Mutation Guarantee

`CareerMemory` never modifies `CandidateProfile`, `Skill`, or any evidence
entity — it is a read-derived, additive record. This keeps it safely
regenerable (bad memory statements can be discarded and recomputed without
any risk of having corrupted underlying candidate data).

## 6. Failure Handling

Insufficient application volume for a category yields no memory statement
for it rather than a low-confidence guess — `CareerMemory` rows always
meet a minimum evidence-count threshold (configurable, doc 21 shares the
same threshold concept for its own signal generation).
