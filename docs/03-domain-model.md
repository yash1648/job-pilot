# 03 — Domain Model

This document is the canonical definition of every entity, value object, and
enum used across the rest of the documentation set. Names and fields here
are authoritative; doc 04 (database) and doc 05 (API) must match exactly.

## 1. Aggregate Map

```
User ──1:1── CandidateProfile ──1:N── Resume ──1:N── ResumeVersion
                    │
                    ├──1:N── Skill ──1:N── SkillEvidence
                    ├──1:N── Experience
                    ├──1:N── Education
                    ├──1:N── Project
                    ├──1:N── Certification
                    ├──1:N── Achievement
                    ├──1:1── CandidateEmbedding
                    └──1:N── JobPreference

Company ──1:1── CompanyEmbedding
Company ──1:N── Job

JobSource ──1:N── Job ──1:N── JobRequirement
Job ──1:1── JobEmbedding

CandidateProfile ──N:M── Job (via JobMatch) ──1:N── MatchReason

CandidateProfile ──1:N── Application ──1:1── Job
Application ──1:1── ApplicationPlan
Application ──1:N── ApplicationAction
Application ──1:N── ApplicationDocument
Application ──1:N── ApplicationAnswer
Application ──1:1── ApplicationSession (while active)
Application ──1:N── ApplicationEvent
Application ──1:N── ApplicationAttempt

Workflow ──1:N── WorkflowStep
Application ──1:1── Workflow

CandidateProfile ──1:N── CareerStrategy
CandidateProfile ──1:N── CareerMemory
Application ──1:1── ApplicationOutcome (once terminal)

CandidateProfile ──1:N── Notification
CandidateProfile ──1:N── AnalyticsSnapshot
```

## 2. Core Entities

### User
`id, email, passwordHash (nullable if OAuth-only), authProvider, createdAt,
lastLoginAt, status(ACTIVE|SUSPENDED|DELETED)`

### CandidateProfile
`id, userId, headline, seniorityEstimate, domainClassification[],
careerTrajectorySummary, strengths[], weaknesses[], skillGaps[],
transferableSkills[], knowledgeGraphRef, embeddingId, createdAt, updatedAt`
One per User. Aggregate root for everything candidate-side.

### Resume
`id, candidateProfileId, originalFilename, storageRef, mimeType,
uploadedAt, isMaster(bool), parseStatus(PENDING|PARSED|FAILED)`
The uploaded artifact. Never mutated after upload — see ResumeVersion.

### ResumeVersion
`id, resumeId (nullable if synthetic), candidateProfileId, jobId (nullable
if not job-specific), label, content(structured JSON), generatedAt,
generationReason, isMasterSnapshot(bool)`
Every tailored resume is a new ResumeVersion, never an edit in place. The
Master Resume is ResumeVersion with `jobId = null, isMasterSnapshot = true`.

### Skill
`id, candidateProfileId, name, normalizedName, category, proficiency
(ESTIMATED_ENUM), yearsExperience(nullable)`

### SkillEvidence
`id, skillId, sourceType(EXPERIENCE|PROJECT|CERTIFICATION|EDUCATION),
sourceId, excerpt, confidence`
Exists specifically to satisfy the Zero-Fabrication Policy: every Skill of
consequence should resolve to at least one SkillEvidence row before it is
used in matching or generation with high confidence.

### Experience / Education / Project / Certification / Achievement
Each: `id, candidateProfileId, title/name, organization, startDate, endDate
(nullable = current), description, extractedSkills[], rawSourceExcerpt`

### CandidateEmbedding
`id, candidateProfileId, vector(pgvector), modelVersion, updatedAt`

### JobPreference
`id, candidateProfileId, targetRoles[], excludedRoles[], locations[],
workMode(REMOTE|HYBRID|ON_SITE|ANY), minSalary, preferredSalary,
employmentType[], experienceLevel, companyAllowList[], companyDenyList[],
technologies[], industries[], relocationWilling(bool),
workAuthorization(enum), applicationFrequency, automationMode (see
ApplicationAutomationMode below)`

### Company
`id, name, normalizedName, industry, sizeRange, hqLocation, website,
technologyStack[], productSummary, sourcedFrom[], lastEnrichedAt`
Never contains fabricated facts — every populated field must trace to a
source in `sourcedFrom`.

### CompanyEmbedding
`id, companyId, vector, modelVersion, updatedAt`

### JobSource
`id, name, sourceType(API|ATS|CAREER_PAGE|FEED|PARTNER), platform
(GREENHOUSE|LEVER|WORKABLE|ASHBY|GENERIC|...), applicationMethodCapability
(AUTO_CAPABLE|ASSISTED_CAPABLE|MANUAL_ONLY), authRequirements,
knownLimitations, rateLimitPolicyRef, lastSuccessfulSyncAt, isActive`

### Job
`id, jobSourceId, companyId, canonicalIdentity(hash), title,
normalizedTitle, location, normalizedLocation, salaryMin, salaryMax,
salaryCurrency, employmentType, seniority, technologies[], domain,
responsibilities[], requiredSkills[], preferredSkills[],
experienceRequirement, educationRequirement, applicationMethod
(inherits JobSource capability, can be narrower), applicationUrl,
postedAt, expiresAt(nullable), discoveredAt, status(ACTIVE|EXPIRED|
REMOVED), potentialConcerns[]`

### JobRequirement
`id, jobId, type(SKILL|EXPERIENCE|EDUCATION|CERTIFICATION|OTHER), text,
isRequired(bool as opposed to preferred), extractedEntity(nullable)`

### JobEmbedding
`id, jobId, vector, modelVersion, updatedAt`

### JobMatch
`id, candidateProfileId, jobId, overallMatch, technicalMatch,
experienceMatch, projectMatch, educationMatch, preferenceMatch,
careerValue, applicationDifficulty, opportunityScore, computedAt,
modelVersion`

### MatchReason
`id, jobMatchId, category(STRONG|MODERATE|MISSING), skillOrFactor,
explanation`

### CareerStrategy
`id, candidateProfileId, primaryTargets[], secondaryTargets[],
stretchTargets[], allocationPlan(JSON: role → percentage), rationale,
generatedAt, supersededBy(nullable)`

### CareerMemory
`id, candidateProfileId, category(RESPONSE_PATTERN|SKILL_GAP|
STRONG_MATCH_CATEGORY|LOW_RESPONSE_CATEGORY), statement, evidenceRefs[],
confidence, createdAt`

## 3. Application-Side Entities

### ApplicationMode (enum)
`AUTO, ASSISTED, MANUAL`

### ApplicationAutomationMode (enum, user setting)
`FULLY_MANUAL, APPROVE_EVERY_APPLICATION, APPROVE_PER_COMPANY,
APPROVE_PER_BATCH, AUTO_WITHIN_RULES`

### ApplicationState (enum) — see doc 11 for full transition table
`DISCOVERED, ANALYZING, MATCHED, SELECTED, PREPARING, READY,
WAITING_FOR_APPROVAL, APPLYING, WAITING_FOR_USER, SUBMITTED, TRACKING,
INTERVIEW, OFFER, REJECTED, WITHDRAWN, FAILED`

### Application
`id, candidateProfileId, jobId, state(ApplicationState), mode
(ApplicationMode), resumeVersionId, coverLetterDocumentId,
createdAt, updatedAt, decidedBy(SYSTEM|USER), approvalRequiredReason
(nullable)`

### ApplicationPlan
`id, applicationId, shouldApply(bool), rationale, emphasizedProjects[],
emphasizedExperience[], coverLetterStrategy, questionsRequiringCare[],
automationFeasible(bool), approvalRequired(bool)`

### ApplicationDocument
`id, applicationId, type(RESUME|COVER_LETTER|OTHER), resumeVersionId
(nullable), storageRef, generatedAt`

### ApplicationAnswer
`id, applicationId, question, classification, answerText,
evidenceRefs[], confidence, requiresUserReview(bool), reviewedByUser(bool)`

### ApplicationSession
`id, applicationId, status(ACTIVE|PAUSED|COMPLETED|ABANDONED),
currentUrl, browserSessionRef(nullable), formState(JSON),
completedFields[], pendingFields[], lastErrorRef(nullable),
createdAt, updatedAt`

### ApplicationAction
`id, applicationSessionId, actionType(NAVIGATE|CLICK|FILL|SELECT|
UPLOAD|SCROLL|WAIT|INSPECT|SCREENSHOT|BACK|FORWARD), targetSelector,
proposedBy(AI), validatedBy(PolicyEngine), result(SUCCESS|FAILURE),
executedAt`

### ApplicationEvent
`id, applicationId, eventType (see doc 48 audit event list), payload
(JSON), occurredAt`

### ApplicationAttempt
`id, applicationId, attemptNumber, outcome(SUCCESS|FAILED|
MANUAL_HANDOFF), failureReason(nullable), startedAt, endedAt`

### ApplicationOutcome
`id, applicationId, outcomeType(INTERVIEW|OFFER|REJECTED|WITHDRAWN|
NO_RESPONSE), occurredAt, notes, source(USER_REPORTED|EMAIL_INTEGRATION)`

### Workflow / WorkflowStep
`Workflow: id, applicationId, currentStepId, status`
`WorkflowStep: id, workflowId, stepType, status(PENDING|IN_PROGRESS|
DONE|SKIPPED|FAILED), startedAt, completedAt, resumeToken(nullable)`

## 4. Cross-Cutting Entities

### Notification
`id, candidateProfileId, type, payload, channel(IN_APP|EMAIL|DESKTOP),
readAt(nullable), createdAt`

### AnalyticsSnapshot
`id, candidateProfileId, periodStart, periodEnd, metrics(JSON matching
doc 19's metric set), computedAt`

### AuditEvent (owned by `audit` module, referenced everywhere)
`id, actorType(USER|SYSTEM|AI_AGENT), actorId, eventType, entityType,
entityId, payload(JSON, no secrets), occurredAt`

## 5. Shared Value Objects

- `Money(amount, currency)`
- `DateRange(start, end nullable)`
- `EvidenceRef(sourceType, sourceId, excerpt)`
- `ConfidenceScore(0.0–1.0, method)`

## 6. Invariants (enforced at service layer, not just DB constraints)

1. A `ResumeVersion` with `jobId != null` must reference an `Application`
   that references the same `jobId` — tailored resumes cannot float free of
   an application context.
2. Every `ApplicationAnswer` with `confidence < CONFIDENCE_THRESHOLD` (doc
   23 defines the value) must have `requiresUserReview = true`.
3. No two `Application` rows may exist for the same
   `(candidateProfileId, job.canonicalIdentity)` pair except where the prior
   one is `WITHDRAWN` or `FAILED` and the user explicitly re-initiates —
   this is the technical enforcement of duplicate-application prevention
   (doc 07/08).
4. `Application.state = MANUAL` handoff transitions (`WAITING_FOR_USER` →
   `SUBMITTED` via user action) never pass through `APPLYING` performed by
   the system — MANUAL mode never calls the Browser Agent's submit action.
5. `Job.status = EXPIRED` blocks new `Application` creation but does not
   retroactively alter existing `Application` rows.

These invariants are restated in doc 04 as DB constraints/triggers where
enforceable there, and in doc 11 as state-machine guards where they are not.
