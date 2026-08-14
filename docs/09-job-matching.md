# 09 — Job Matching

Tier: **CORE** (job intelligence, matching, ranking), **ADVANCED** (company
intelligence). Owning module: `jobs` (intelligence), `company`, `matching`.

## 1. Job Intelligence

Runs once per newly-normalized `Job` (and on significant content change):

```
Job (normalized fields already present from doc 08)
   ↓ ai.AiService (task=JOB_ANALYSIS) extracts, from the raw description
     text (treated as UntrustedContent, doc 23):
        responsibilities[], requiredSkills[] vs preferredSkills[]
        (explicit "required/must-have" vs "nice to have" language),
        experienceRequirement, educationRequirement, potentialConcerns
        (e.g., vague comp, red-flag language — informational only, never
        used to auto-exclude)
   ↓ JobRequirement rows persisted per extracted item, each tagged
     isRequired and, where resolvable, extractedEntity (normalized skill/
     credential id) so matching can join against candidate Skill rows
   ↓ ai.EmbeddingService produces JobEmbedding
```

Extraction schema forces required-vs-preferred separation because treating
all listed skills as equally required is the single most common cause of
inflated or deflated match scores in practice.

## 2. Company Intelligence (ADVANCED)

`CompanyEnrichmentService` populates `Company` fields only from sources
recorded in `sourcedFrom` — company career page, company's own public
materials, the job posting itself. It never infers unstated facts (e.g.,
company size) from an LLM's general knowledge; if a fact isn't sourced, the
field stays null rather than filled with a guess. Re-enrichment is
scheduled per company on a long interval (companies change slowly) and
triggered eagerly the first time a candidate has a live match against that
company.

## 3. Hybrid Matching Engine

`MatchingService.computeMatch(candidateProfileId, jobId)` combines multiple
independently-computed sub-scores rather than one embedding similarity
number:

| Sub-score | Computed from |
|---|---|
| `technicalMatch` | Skill ↔ JobRequirement(SKILL) overlap, weighted by `isRequired` and by SkillEvidence confidence (doc 07 §4) |
| `experienceMatch` | Experience rows vs. `experienceRequirement`, seniority alignment |
| `projectMatch` | Project relevance via embedding similarity between Project descriptions and Job responsibilities, plus explicit technology overlap |
| `educationMatch` | Education vs. `educationRequirement` |
| `preferenceMatch` | JobPreference fields (location, salary, work mode, employment type, company allow/deny) — deterministic rule evaluation, not AI |
| `careerValue` | alignment with CareerStrategy targets (doc 10) and career trajectory direction |
| `applicationDifficulty` | inverse function of `Job.applicationMethod`, question-count/complexity signals from prior similar applications (doc 21 feedback) |
| `overallMatch` | weighted combination of the above; weights are configurable per candidate-set experiment, not hard-coded magic numbers buried in code |

`semanticSimilarity` (CandidateEmbedding ↔ JobEmbedding cosine) is one
input into `technicalMatch`/`projectMatch`, not the whole engine — this is
the explicit rejection of embeddings-only matching from the product
requirement (doc 01 §5.8).

## 4. Explainability

Every computed `JobMatch` produces `MatchReason` rows in three categories:
`STRONG` (skills/factors clearly met), `MODERATE` (partial/indirect
evidence), `MISSING` (required and absent). Reasons reference the specific
Skill/JobRequirement pair, not a generic template string — this is what
lets the UI render the doc 01 §5.8 example format (✓ Java, ◐ Docker, ⚠
AWS) directly off stored data rather than re-deriving it at render time.

## 5. Opportunity Ranking

`OpportunityScore` is computed separately from `overallMatch`:

```
opportunityScore = f(
  overallMatch,
  successProbability,      // historical response rate for similar
                            // matches (doc 21), defaults to a prior when
                            // no history exists yet
  careerValue,
  preferenceAlignment,     // salary/location/company-quality signals
  applicationDifficulty    // inverse-weighted: easier + strong match
                            // ranks above harder + equally-strong match
)
```

`f` is a documented, versioned scoring function (not opaque model output)
so ranking changes are explainable and testable (doc 26). Ranking never
silently reorders based on factors the user hasn't opted into (e.g.,
company-quality weighting is itself a preference, doc 03 `JobPreference`
extension point for FUTURE).

## 6. Recompute Triggers

Match recompute is triggered by: new Job discovered, candidate profile
material change (new resume parsed, skills changed), JobPreference change,
or explicit user request (`POST /matches/recompute`, rate-limited since
it's compute-expensive). Recompute is incremental where possible — a
JobPreference change re-runs only `preferenceMatch`/`overallMatch`/
`opportunityScore`, not the full AI extraction pipeline.

## 7. Failure Handling

| Failure | Behavior |
|---|---|
| Job Intelligence AI extraction fails | Job still creatable/matchable using normalized fields only (doc 08 output); `potentialConcerns` flagged, matching proceeds at reduced confidence rather than blocking |
| Company enrichment fails/unavailable | Company fields stay sparse; matching proceeds without `careerValue` company-quality input |
| Matching computation error | JobMatch not persisted for that pair; job simply doesn't appear ranked until a subsequent successful compute — never a fabricated 0% or 100% |

## 8. Testing Hooks (doc 26/27)

Benchmark job/candidate pairs with hand-labeled expected match bands, used
for `matchPrecision`/`matchRecall` in doc 27; adversarial job descriptions
containing embedded instructions, used for the same prompt-injection
resistance suite as doc 07's adversarial resume fixture.
