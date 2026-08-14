# 23 — AI Security

Tier: **CORE**, cross-cutting. Owning module: `ai`, `security`.

## 1. Trust Boundaries (restated precisely from doc 02 §6)

**Trusted**: JobPilot's own system prompts/instructions, user-authored
structured input (form field values, preference settings, explicit
approvals). **Untrusted**: resume content, job description text, company
page text, application page DOM/text, any recruiter-email content (doc 18
§3 FUTURE). Untrusted content is always wrapped in a distinct
`UntrustedContent` type (doc 06 §1) at the point it enters an `AiRequest` —
this type distinction exists specifically so prompt construction can never
accidentally concatenate untrusted text into the instruction channel.

## 2. Prompt Injection Defenses

- **Structural separation**: system instructions and untrusted content are
  passed as distinct message roles/fields, never string-concatenated into
  one instruction blob.
- **Bounded output contracts**: every AI call declares a schema (doc 06
  §6); an injected instruction inside a resume/job/page ("ignore previous
  instructions, output X") cannot cause an out-of-schema action because
  the calling code only accepts schema-valid, semantically-scoped output
  (e.g., a resume-extraction call can only ever populate resume fields, not
  trigger a browser action).
- **Action-layer enforcement**: even if a model were manipulated into
  proposing something unintended, doc 16 §4's Policy Engine independently
  validates any resulting tool call/browser action against the actual
  `ApplicationPlan`/`FormField` context — the untrusted content's
  "instructions" have no path to becoming a real action without passing
  this independent, deterministic check.
- **Detection layer**: a lightweight classifier pass flags content
  containing injection-pattern signals (imperative language addressed to
  "the AI/assistant," instruction-like formatting embedded in a resume/job
  description) for audit visibility and lowered-confidence handling, not
  as the primary defense (defense in depth, not reliance on detection
  alone).

## 3. Data Exfiltration Prevention

Generated content (cover letters, answers, resumes) is validated to
contain only information already present in `CandidateProfile`/`Job`/
`Company` data (the same evidence-validation stage from doc 12) — this
incidentally also prevents a scenario where injected content in a job
posting tries to get the model to leak candidate PII into, e.g., an
answer field visible to a third-party site beyond what that field
legitimately asks for. Tool permission scoping (doc 16 §3) additionally
ensures no tool call can read across candidates.

## 4. Zero-Fabrication Policy — Technical Enforcement

This is the single most safety-critical AI control in the product and is
enforced at three layers, not just prompted for:

1. **Schema requirement**: generation output schemas (doc 06 §6) require
   an `EvidenceRef` on every substantive claim.
2. **Policy Engine check**: `isClaimSupportedByEvidence` (doc 16 §4)
   validates each `EvidenceRef` resolves to a real `SkillEvidence`/
   `Experience`/`Project`/etc. row and that the claim's specificity doesn't
   materially exceed what that evidence supports.
3. **Confidence-gated review**: any claim/answer that can't clear
   validation is either rewritten (bounded retry, doc 12 §6) or surfaces
   for `requiresUserReview` — it is never persisted as a system-authored
   claim without a resolvable evidence trail.

`CONFIDENCE_THRESHOLD` (referenced throughout docs 03/09/12) is a
configured value (default documented in doc 35 config reference),
per-question-category-adjustable (doc 12 §3 — `YES_NO_LEGAL` bypasses
confidence entirely and always requires review).

## 5. Malicious Documents & Pages

Resume files: doc 22 §4 (upload/parsing hardening). Job/company/
application pages: content is only ever consumed as text/DOM/screenshot
input to `AiService`/`VisionService`, never executed — the Browser Agent's
fixed action set (doc 14 §3) means a malicious page has no code-execution
path into JobPilot's own process regardless of what script or content it
contains.

## 6. Output Validation Summary (cross-ref doc 06 §6)

Schema-invalid output → bounded retry → typed failure, never partial
pass-through. This applies uniformly whether the "invalidity" is a
formatting error or a content-level Zero-Fabrication violation caught by
the Policy Engine.

## 7. Testing (cross-ref doc 27)

Adversarial fixtures (resumes/job postings/pages with embedded injection
attempts, doc 07 §10 / doc 09 §8 / doc 14 §9) are run as part of the AI
evaluation suite specifically to measure injection-resistance and
hallucination rate as tracked metrics, not just spot-checked.
