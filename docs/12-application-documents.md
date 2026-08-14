# 12 — Application Documents

Tier: **CORE**. Owning module: `application` (document generation
sub-component), consuming `resume`, `ai`.

## 1. Resume Tailoring

```
ApplicationPlan (emphasizedProjects/Experience, doc 11)
   ↓ base = current Master Resume snapshot (doc 07 §8)
   ↓ ai.AiService (task=RESUME_REASONING), input = base content
     (trusted, candidate-owned) + Job requirements (UntrustedContent) +
     emphasis instructions (trusted)
   ↓ output schema requires every bullet/claim in the tailored resume to
     carry an EvidenceRef back to a source Experience/Project/Skill row
   ↓ Policy Engine evidence-validation stage (doc 23): reject any claim
     without a valid EvidenceRef, or any claim whose text materially
     exceeds what the EvidenceRef supports (e.g., evidence says
     "contributed to," output says "led" — flagged for downgrade)
   ↓ ResumeVersion(jobId=..., generationReason="tailored for <job>")
     persisted — Master Resume untouched
```

Tailoring can reorder, re-emphasize, and rephrase; it cannot add a skill,
technology, employer, title, or date that doesn't already exist in the
candidate's evidence base. This is the Zero-Fabrication Policy applied
concretely (doc 01 §5.12, enforced per doc 23).

## 2. Cover Letter Engine

```
Inputs: CandidateProfile evidence (RAG-retrieved relevant chunks, doc 06
§4), Job, Company (where enriched), coverLetterStrategy (doc 11),
career goals (from JobPreference/CareerStrategy)
   ↓ ai.AiService (task=RESUME_REASONING profile) generates letter
   ↓ same evidence-validation stage as resume tailoring
   ↓ constraints: truthful, personalized (references specific job/company
     detail actually present in Job/Company data — never invented company
     facts, doc 09 §2), concise (target length configurable, default
     ~250-350 words), non-generic (must reference ≥1 specific
     candidate achievement and ≥1 specific job/company detail, enforced
     by a post-generation checklist pass, not just prompted-for)
   ↓ ApplicationDocument(type=COVER_LETTER) persisted, versioned per
     application (regenerating for the same application creates a new
     version, prior kept for comparison/audit)
```

## 3. Application Question Engine

```
Question (from Form Intelligence, doc 13, or a known-question cache for
the platform)
   ↓ QuestionClassificationService: category (FACTUAL_SHORT,
     BEHAVIORAL_STAR, YES_NO_LEGAL, SALARY_EXPECTATION, FREE_TEXT_OTHER)
   ↓ candidate evidence retrieval (RAG, scoped by question category — a
     BEHAVIORAL_STAR question retrieves Project/Experience narrative
     chunks; a FACTUAL_SHORT "years of Java experience" question retrieves
     Skill.yearsExperience + supporting SkillEvidence directly, no LLM
     narrative synthesis needed for that subtype)
   ↓ job context retrieval (relevant JobRequirement rows)
   ↓ answer generation (ai.AiService, evidence-validation stage applies
     identically to resume/cover-letter generation)
   ↓ truth validation: FACTUAL_SHORT answers are cross-checked against
     the underlying structured data (e.g., a generated "5 years" must
     match Skill.yearsExperience within tolerance, not just sound
     plausible) — mismatches are corrected to the structured value, not
     left to the generative pass
   ↓ confidence scoring (doc 03 ApplicationAnswer.confidence)
   ↓ requiresUserReview = true when confidence < CONFIDENCE_THRESHOLD
     (doc 23) OR question category = YES_NO_LEGAL (work authorization,
     legal attestations always require explicit user confirmation
     regardless of confidence — these are never auto-answered)
```

## 4. User Review Flow

Low-confidence or legally-sensitive `ApplicationAnswer` rows block
`Application` progression past `PREPARING`/`READY` until
`reviewedByUser = true` (via `PATCH /applications/{id}/answers/{answerId}`,
doc 05). This is enforced by the state machine guard on the
`PREPARING → READY` transition, not left to UI convention.

## 5. Versioning & Traceability

Every `ApplicationDocument` and `ApplicationAnswer` set is tied to exactly
one `Application`, which is tied to exactly one `Job` and one
`ResumeVersion` — full traceability from any generated artifact back to
the resume snapshot and job posting that produced it (audit requirement,
doc 22/48).

## 6. Failure Handling

| Failure | Behavior |
|---|---|
| Evidence-validation rejects a claim | generation service strips/rewrites the claim (bounded retry) rather than surfacing an unvalidated document; if it cannot produce a fully validated document after retry, `PREPARING → FAILED` with a specific reason |
| RAG retrieval returns no relevant evidence for a question | answer generation is skipped, `requiresUserReview = true`, `answerText` left null — never a fabricated generic answer |
| Cover letter generation fails checklist (missing specific reference) | one bounded regeneration attempt with stronger constraints, then falls back to `requiresUserReview` equivalent (user prompted to write/edit manually) |
