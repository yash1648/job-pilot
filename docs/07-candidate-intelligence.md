# 07 — Candidate Intelligence

Tier: **CORE**. Owning module: `candidate`, `resume`, `skill`,
`experience`, `education`, `project`, `certification`.

## 1. Purpose

Turn an uploaded resume into a structured, evidence-linked
`CandidateProfile` that every downstream module (matching, strategy,
generation) reads instead of re-parsing raw text.

## 2. Pipeline

```
Resume upload (PDF/DOCX)
   ↓ storage.StorageService persists original (never mutated)
   ↓ resume.ResumeParsingService: text/layout extraction
      (PDF: text-layer extraction first, OCR fallback for scanned PDFs;
       DOCX: structured XML parsing — see doc 26 fixtures for both paths)
   ↓ ai.AiService (task=SIMPLE_EXTRACTION, then RESUME_REASONING) extracts:
        experiences, education, projects, certifications, achievements
        as structured candidates, each tagged with rawSourceExcerpt
   ↓ schema validation (doc 06 §6)
   ↓ skill.SkillExtractionService derives Skill rows from the extracted
      entities, each Skill linked to ≥1 SkillEvidence pointing at the
      specific experience/project/certification/education it came from
   ↓ skill.SkillNormalizationService maps raw skill strings to a
      normalized taxonomy (e.g., "ReactJS"/"React.js"/"React" → "react")
   ↓ candidate.ProfileSynthesisService computes derived fields:
        domainClassification, seniorityEstimate, careerTrajectorySummary,
        strengths, weaknesses, skillGaps, transferableSkills
   ↓ ai.EmbeddingService produces CandidateEmbedding
   ↓ ResumeVersion(isMasterSnapshot=true) persisted as the Master Resume
```

Parse failures set `Resume.parseStatus = FAILED` with a user-facing reason
(unreadable file, unsupported format, empty extraction) — never silently
produce an empty profile.

## 3. Resume Quality Analysis (CORE)

A separate, deterministic-where-possible pass flags: missing dates,
unquantified achievements, inconsistent formatting, missing contact info,
skill claims with no evidence elsewhere in the document. Output is
user-facing suggestions, not auto-applied edits — the user's original
resume is never rewritten without an explicit tailoring action tied to a
specific job (doc 12).

## 4. Skill Evidence Requirement

A Skill is only usable in high-confidence matching/generation contexts if
it has at least one `SkillEvidence` row above the confidence threshold (doc
23). Skills extracted from a bare skills-list section with no supporting
experience/project text still get a `SkillEvidence` row (source = the
skills-list itself), but at lower confidence — this distinction feeds match
explanations (doc 09) so "listed, unverified" and "demonstrated" skills are
never presented identically.

## 5. Domain Classification & Seniority Estimation

`ProfileSynthesisService` uses `AiTaskType.RESUME_REASONING` with a schema
that forces the model to cite which experiences/projects justify the
seniority and domain conclusions (EvidenceRef-backed, per doc 06 §4's
citation requirement). Pure heuristic pre-pass (years-of-experience sum,
title-keyword matching) generates a candidate estimate first; the AI pass
refines it and must explain any deviation greater than one seniority band.

## 6. Career Trajectory Analysis (ADVANCED)

Looks at the ordered sequence of Experience rows (title, org, dates,
technologies) to characterize trajectory (e.g., "IC progressing toward
technical leadership," "lateral moves across domains, deepening in backend
systems"). Feeds Career Strategy (doc 10) and Career Memory (doc 20).

## 7. Candidate Knowledge Graph (ADVANCED/FUTURE)

CORE ships the relational representation (doc 03/04 tables) as the graph's
practical substrate: Skill ↔ SkillEvidence ↔ Experience/Project edges are
already a graph, queryable via SQL joins. A dedicated graph-store
representation is FUTURE, justified only if query patterns (e.g.,
multi-hop "candidates who used X to achieve Y") outgrow relational joins —
tracked as an extraction candidate in doc 32, not built speculatively now.

## 8. Resume Versioning Rules

- Uploading a new resume file never overwrites `resumes.is_master`
  automatically; the user explicitly promotes it (`set-master` endpoint,
  doc 05).
- Re-parsing an existing Master Resume (e.g., after a parser bug fix)
  creates a new `ResumeVersion(isMasterSnapshot=true)`, preserving history
  rather than mutating the prior snapshot.
- Job-specific tailoring (doc 12) always branches from the current Master
  Resume snapshot at the time of tailoring, recorded via
  `generationReason`.

## 9. Failure Handling (cross-ref doc 30, doc 72)

| Failure | Behavior |
|---|---|
| Unreadable/corrupt file | `parseStatus=FAILED`, user-facing message, no partial profile written |
| Scanned PDF, OCR low-confidence | proceed but flag low-confidence extraction; quality analysis surfaces it |
| AI extraction schema-invalid after retry (doc 06 §6) | `parseStatus=FAILED`, audit event `ResumeParsingFailed`, user can retry |
| Extraction succeeds but yields zero skills/experience | do not synthesize placeholders; profile marked incomplete, user prompted to review |

## 10. Testing Hooks (doc 26/27)

Fixture resumes: clean text-PDF, scanned/OCR PDF, DOCX with tables,
resume with no dates, resume with overlapping/concurrent roles, adversarial
resume containing embedded instructions (prompt-injection fixture, doc 23/
doc 27 hallucination-rate benchmark).
