# 24 — Privacy

Tier: **CORE**, cross-cutting. Owning module: `security`, `storage`,
`audit`.

## 1. Personal Data Inventory

| Category | Tables (doc 04) | Sensitivity |
|---|---|---|
| Identity | `users` | high (email, auth) |
| Resume content | `resumes`, `resume_versions` | high (full career history, may include age-adjacent signals like graduation year) |
| Structured career data | `skills`, `experiences`, `educations`, `projects`, `certifications`, `achievements` | high |
| Contact/eligibility | `candidate_profiles`, `job_preferences` (work authorization) | high |
| Application content | `application_documents`, `application_answers` | high (may include salary expectations, legal attestations) |
| Browser session data | `application_sessions.browser_session_ref`, credential-vault entries (doc 22 §8) | high |
| Behavioral | `application_events`, `analytics_snapshots` | medium |

## 2. Principles

- **Data minimization**: JobPilot collects only what the documented
  pipelines (docs 07–21) actually consume; no speculative field collection
  "in case it's useful later."
- **Purpose limitation**: candidate data is used only for the candidate's
  own job search — never aggregated/sold/used to train a shared model
  across candidates without separate, explicit consent (FUTURE, not
  assumed).
- **Access control**: doc 22 §2's candidate-scoping applies identically
  here — privacy and security controls are the same mechanism, not
  parallel systems.

## 3. Encryption

At rest: doc 22 §5 (documents), plus column-level encryption for the
highest-sensitivity fields (work authorization status, salary fields) in
production (doc 33 secrets/KMS integration). In transit: TLS everywhere,
including internal calls to Ollama/Redis in production deployments (doc
33).

## 4. Retention

| Data | Default retention | Notes |
|---|---|---|
| Active account data | retained while account active | user-controlled via profile |
| `resumes`/`resume_versions` for `WITHDRAWN`/`FAILED` applications | retained (part of candidate's own history, still useful to them) | not deleted automatically |
| `browser_session_ref` / credential-vault entries | short TTL, purged after session completion or timeout (doc 14 §7) | never retained beyond operational need |
| `audit_events` | retained per compliance policy (default 2 years), survives account deletion in anonymized form (doc 04 §5) | actor_id anonymized on account deletion, event record kept for security/compliance history |

## 5. Deletion

Account deletion (`users.status = DELETED` then hard-delete after a grace
period, doc 33) cascades per doc 04 `ON DELETE CASCADE` through
`candidate_profiles` and everything beneath it — resumes, versions,
applications, documents, answers, embeddings. `audit_events` referencing
that user are retained but `actor_id` is anonymized (§4). Deletion is
available self-service (`DELETE` on the account, doc 05 extension point)
and processed within a documented SLA (doc 33).

## 6. Export

Full data export (JSON bundle: profile, resumes, resume versions,
applications, documents, answers) available self-service — this is both a
privacy right and a practical need (the user should be able to take their
Master Resume and application history out of JobPilot at any time,
consistent with doc 01's "the candidate owns their data" spirit).

## 7. AI-Specific Privacy Notes

Candidate/job/company content sent to the AI provider (Ollama, self-hosted
per doc 33) never leaves JobPilot's own infrastructure boundary in the
initial deployment — this is a material privacy property of choosing a
self-hosted provider first (ADR-005) and is stated explicitly to the user
in-product. Any FUTURE hosted-provider integration (doc 41/ADR-007's
extensibility) must be opt-in per-provider with clear disclosure of what
leaves the infrastructure boundary.

## 8. Third-Party Data Sharing

None by default. Job source integrations (doc 08) are outbound-read only
(JobPilot reads public/authorized postings; it does not send candidate
data to job sources except as part of an explicit, user-approved
application submission, which is the whole point of the product and is
always visible to the user via the Cockpit, doc 11 §6).

## 9. Failure Handling

| Failure | Behavior |
|---|---|
| Deletion cascade fails partway | transactional — either fully completes or fully rolls back, never a partially-deleted account state; retried, alerted if retry also fails |
| Export generation fails | user notified, retried; export never silently omits categories without flagging the omission |
