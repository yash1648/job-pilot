# 05 — API Specification

REST over HTTPS, JSON bodies, versioned base path `/api/v1`. Auth via
session cookie (Thymeleaf frontend) or Bearer JWT (future React frontend /
future MCP adapter) — both map to the same `SecurityContext` (doc 22).
Every endpoint below maps to exactly one service in the module owning it
(doc 02 §3); controllers never contain business logic.

Conventions used throughout this document:
- **Idempotency**: mutating endpoints that trigger side effects on external
  systems (submitting an application) require an `Idempotency-Key` header;
  the server persists the key→result mapping for 24h.
- **Errors**: `{ "error": { "code", "message", "details" } }`, HTTP status
  matches `code` category (400 validation, 401/403 auth, 404 not found, 409
  conflict/state, 422 policy-blocked, 429 rate-limited, 5xx).
- **Pagination**: `?page=&size=`, response envelope
  `{ "items": [...], "page", "size", "totalItems" }`.

## 1. Authentication (`auth` module)

| Method | Path | Purpose |
|---|---|---|
| POST | `/auth/register` | create User, 201 or 409 if email exists |
| POST | `/auth/login` | issue session/JWT |
| POST | `/auth/logout` | invalidate session |
| POST | `/auth/refresh` | refresh JWT |
| GET | `/auth/oauth/{provider}/callback` | OAuth completion |

## 2. Candidate (`candidate`, `resume`, `skill`, `experience`, ... modules)

| Method | Path | Purpose |
|---|---|---|
| GET | `/candidate/profile` | current profile aggregate |
| PATCH | `/candidate/profile` | update headline etc. |
| POST | `/candidate/resumes` | upload resume (multipart), 202, triggers async parse |
| GET | `/candidate/resumes` | list resumes |
| GET | `/candidate/resumes/{id}` | resume metadata + parse status |
| DELETE | `/candidate/resumes/{id}` | delete, 409 if `isMaster` and no replacement designated |
| POST | `/candidate/resumes/{id}/set-master` | promote to Master Resume |
| GET | `/candidate/resume-versions` | list, filterable by `jobId` |
| GET | `/candidate/resume-versions/{id}` | full content |
| GET | `/candidate/skills` | list with evidence summary |
| GET | `/candidate/skills/{id}/evidence` | full SkillEvidence list |
| GET | `/candidate/experiences` \| `/educations` \| `/projects` \| `/certifications` \| `/achievements` | list |
| POST/PATCH/DELETE | same paths + `/{id}` | manual correction by user (never AI-authored without evidence, doc 23) |
| GET | `/candidate/gaps` | strengths/weaknesses/skillGaps summary |

## 3. Preferences (`user`)

| Method | Path | Purpose |
|---|---|---|
| GET | `/preferences` | current JobPreference |
| PUT | `/preferences` | full replace, validated against enum sets in doc 03 |

## 4. Jobs (`jobs`, `company`)

| Method | Path | Purpose |
|---|---|---|
| GET | `/jobs` | search/list, filters: role, location, salary, source, status |
| GET | `/jobs/{id}` | full Job + JobRequirement list |
| GET | `/jobs/{id}/company` | Company record |
| POST | `/jobs/discover` | trigger on-demand discovery run (rate-limited, doc 33) |
| GET | `/job-sources` | list JobSource with capability/status |
| PATCH | `/job-sources/{id}` | admin: enable/disable, edit rate policy ref |

## 5. Matching & Strategy (`matching`, `career`)

| Method | Path | Purpose |
|---|---|---|
| GET | `/matches` | ranked JobMatch list for current candidate, sorted by `opportunityScore` |
| GET | `/matches/{jobId}` | JobMatch + MatchReason[] for one job |
| POST | `/matches/recompute` | force recompute (rate-limited, expensive) |
| GET | `/career/strategy` | current CareerStrategy |
| POST | `/career/strategy/regenerate` | recompute from latest evidence |
| GET | `/career/memory` | CareerMemory list |

## 6. Applications (`application`, `workflow`)

| Method | Path | Purpose |
|---|---|---|
| GET | `/applications` | list, filter by `state`, `mode` |
| GET | `/applications/{id}` | full Application + Plan + Documents + Answers |
| POST | `/applications` | create from `jobId`, state→`ANALYZING`, returns 202 |
| POST | `/applications/{id}/approve` | user approval, required when `state=WAITING_FOR_APPROVAL` |
| POST | `/applications/{id}/reject` | user declines, state→`WITHDRAWN` |
| POST | `/applications/{id}/start` | begin execution (state must be `READY`), requires `Idempotency-Key` |
| POST | `/applications/{id}/pause` | pause active session |
| POST | `/applications/{id}/resume` | resume paused session |
| POST | `/applications/{id}/cancel` | cancel, state→`WITHDRAWN` |
| POST | `/applications/{id}/mark-applied` | MANUAL handoff completion by user, state→`SUBMITTED` |
| GET | `/applications/{id}/documents` | ApplicationDocument list with download links |
| GET | `/applications/{id}/answers` | ApplicationAnswer list |
| PATCH | `/applications/{id}/answers/{answerId}` | user edits/approves a low-confidence answer |
| GET | `/applications/{id}/session` | ApplicationSession state (for Cockpit UI, doc 11) |
| GET | `/applications/{id}/events` | ApplicationEvent timeline |
| POST | `/applications/batch` | batch creation from job id list (doc 11 §Batch) |
| POST | `/applications/batch/{batchId}/approve` | approve reviewed batch |

## 7. Tracking (`tracking`)

| Method | Path | Purpose |
|---|---|---|
| GET | `/tracking/timeline` | cross-application timeline |
| POST | `/applications/{id}/outcome` | manual status update (interview/offer/rejected) |
| GET | `/notifications` | list, `?unreadOnly=` |
| POST | `/notifications/{id}/read` | mark read |

## 8. Analytics (`analytics`)

| Method | Path | Purpose |
|---|---|---|
| GET | `/analytics/dashboard` | Dashboard metric bundle (doc 19) |
| GET | `/analytics/snapshots` | historical AnalyticsSnapshot list |

## 9. Settings

| Method | Path | Purpose |
|---|---|---|
| GET/PUT | `/settings/automation` | ApplicationAutomationMode + related rules |
| GET/PUT | `/settings/notifications` | channel preferences |
| GET | `/settings/audit` | user-visible audit trail for their own data |

## 10. Manual Handoff Detail Endpoint

| Method | Path | Purpose |
|---|---|---|
| GET | `/applications/{id}/manual-package` | reason, prepared documents, instructions, application URL — feeds the Manual Handoff UI (doc 15) |

## 11. Validation & Authorization Summary

- All `candidate/*`, `applications/*`, `matches/*` endpoints scope to
  `SecurityContext.candidateProfileId` — no candidate can address another's
  resources; enforced at the service layer with a repository-level filter,
  not just a controller check (defense in depth, doc 22).
- `POST /applications/{id}/start` is blocked by the Policy Engine (422) if
  `automationMode = FULLY_MANUAL` or if the required approval hasn't been
  recorded — this is the API-level enforcement of automation control
  (doc 11 §Automation Control).
- `job-sources` admin endpoints require an elevated role, not available to
  standard candidate accounts.

## 12. Error Codes (non-exhaustive, extended per-module in later docs)

| Code | Meaning |
|---|---|
| `VALIDATION_ERROR` | request body fails schema |
| `NOT_FOUND` | entity doesn't exist or isn't owned by caller |
| `STATE_CONFLICT` | action invalid for current `ApplicationState` |
| `POLICY_BLOCKED` | Policy Engine denied the action (doc 47) |
| `DUPLICATE_APPLICATION` | violates invariant #3, doc 03 |
| `RATE_LIMITED` | source or internal rate limit hit |
| `SOURCE_UNAVAILABLE` | upstream JobSource unreachable |

## 13. Future MCP Surface

Doc 66/future/01-mcp-readiness.md maps a subset of these endpoints
(read-mostly plus a narrow, approval-gated write set) to MCP tools. No MCP
tool bypasses the Policy Engine or the approval requirements defined here —
MCP is an adapter over this API's service layer, not a parallel path.
