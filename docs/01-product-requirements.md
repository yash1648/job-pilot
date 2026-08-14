# 01 — Product Requirements

Status: locked. All downstream documents inherit vocabulary from this file.

## 1. Product Identity

**Name:** JobPilot
**Category:** AI-powered autonomous job-hunting and application orchestration
platform — an "AI Career Operating System," not a resume analyzer, not a job
scraper, not an auto-apply bot, not an LLM wrapper, not a browser automation
script. Those are components it contains, not what it is.

**Core problem:** job hunting is fragmented, repetitive, and manual across
search, qualification assessment, tailoring, form-filling, tracking, and
follow-up. JobPilot owns the full loop end to end, continuously, on the
candidate's behalf, under the candidate's control.

**Top-level user promise** (the sentence every later document should be
traceable back to):

> Find the best software engineering opportunities for me, evaluate them
> against my actual experience, prepare personalized applications,
> automatically execute applications wherever the workflow legitimately
> supports it, assist me when human interaction is required, give me a
> complete application package and direct link whenever automation isn't
> available, track everything afterward, and continuously improve my
> job-search strategy based on actual outcomes.

## 2. Product Tiers

Every capability in this and all subsequent documents is tagged with exactly
one tier. This tagging is how the roadmap (doc 36) stays honest about what
ships when without shrinking the target architecture.

- **CORE** — required for the product to be usable and safe on day one of
  each phase it belongs to. Nothing in CORE is optional.
- **ADVANCED** — meaningfully improves outcomes but the product functions
  without it; builds on CORE foundations already in place.
- **FUTURE** — architecturally accounted for now (interfaces, extension
  points, data model headroom) but not built until a CORE/ADVANCED
  foundation exists to justify it.

## 3. Primary User

A job-seeking candidate (initially: software engineers) who provides:

```
Resume + career preferences + constraints + goals
```

and expects JobPilot to operate a continuous job-hunting workflow against
that input, escalating to the user only when a decision requires their
judgment, consent, or physical action (e.g., completing a CAPTCHA).

## 4. End-to-End Workflow (canonical, referenced by doc 02 onward)

```
USER
 → Resume / Profile
 → Candidate Intelligence
 → Career Strategy Engine
 → Job Discovery Engine
 → Job Normalization Engine
 → Job Intelligence
 → Matching Engine
 → Opportunity Ranking
 → Application Strategy Engine
     → Resume Tailoring
     → Cover Letter Generation
     → Answer Generation
 → Application Engine (AUTO | ASSISTED | MANUAL)
     → Submit | User Takeover | URL + Package handoff
 → Application Tracker
 → Outcome Analytics
 → Career Learning Loop → feeds back into Career Strategy Engine
```

## 5. Functional Requirements by Capability Area

Each area below maps 1:1 to a later architecture document; the mapping is
listed so gaps are visible immediately.

### 5.1 Candidate Intelligence — CORE (doc 07)
Resume ingestion (PDF/DOCX), parsing, versioning, quality analysis, skill
extraction and normalization with evidence linkage, experience/project/
education/certification/achievement extraction, technology relationship
graph, domain classification, seniority estimation, career trajectory
analysis, strengths/weaknesses/gaps, candidate knowledge graph and
embeddings.

### 5.2 Career Strategy Engine — ADVANCED (doc 10)
Answers "which jobs *should* this candidate apply to," not just "which jobs
can they apply to." Produces tiered target recommendations (primary /
secondary / stretch) from skills, market demand, historical response data,
and stated preferences. Adaptive: strategy changes as outcome data
accumulates (feeds from doc 21).

### 5.3 Job Discovery — CORE (doc 08)
Universal `JobSource` abstraction over job APIs, ATS systems (Greenhouse,
Lever, Workable, Ashby), company career pages, and other authorized/public
sources. Sources are heterogeneous in capability; the abstraction does not
assume uniform access.

### 5.4 Job Source Intelligence — CORE (doc 08)
Per-source metadata (platform, application method, auth requirements, rate
limits, known limitations, last sync) drives a runtime decision: can this job
be applied to via AUTO, ASSISTED, or does it require MANUAL only.

### 5.5 Job Normalization — CORE (doc 08)
Canonical job identity, duplicate detection, and normalization of URL,
company, title, location, salary, employment type, seniority, and technology
fields across sources. Exists specifically to prevent duplicate applications.

### 5.6 Job Intelligence — CORE (doc 09)
Structured extraction per job: responsibilities, required/preferred skills,
experience, education, location, salary, employment type, seniority,
technologies, domain, company info, application method, potential concerns.

### 5.7 Company Intelligence — ADVANCED (doc 09)
Company profile, industry, size, technology, products, role patterns —
collected only from legitimately available sources, never fabricated. Feeds
ranking, cover letters, and answers.

### 5.8 Job Matching — CORE (doc 09)
Hybrid matching (not embeddings-only): semantic similarity, skill matching
with importance weighting, experience/project/education/seniority/location/
salary/employment-type/technology alignment, career trajectory fit, company
preference fit, and historical response data. Produces explainable
sub-scores (Overall, Technical, Experience, Project, Education, Preference,
Career Value, Application Difficulty), each with human-readable reasons.

### 5.9 Opportunity Ranking — CORE (doc 09)
A distinct `OpportunityScore`, not just sorted match %, incorporating success
probability, career value, salary, location, company quality, and
application difficulty.

### 5.10 Application Strategy — CORE (doc 11)
Per-job decision: should apply, why, which resume version, which projects to
emphasize, cover letter strategy, which answers need care, whether
automation is possible, whether approval is required before proceeding.

### 5.11 Resume Intelligence — CORE (doc 07, doc 12)
Maintains Master Resume, Candidate Profile, Evidence Database, and
job-specific tailored Resume Versions. The original resume is never
destroyed. Every tailored claim is traceable to a reason and source evidence.

### 5.12 Zero-Fabrication Policy — CORE, cross-cutting (doc 23)
JobPilot never invents employment, experience, skills, education,
certifications, projects, achievements, titles, companies, dates,
technologies, or responsibilities. Every generated claim of consequence must
trace to candidate evidence. Missing information is asked of the user or
left unanswered — never guessed. This is a hard constraint referenced by
every generation-touching document, not a suggestion.

### 5.13 Cover Letter Engine — CORE (doc 12)
Job-specific, truthful, personalized, concise, evidence-grounded, versioned.

### 5.14 Application Question Engine — CORE (doc 12)
Pipeline: question → classification → candidate evidence retrieval → job
context retrieval → answer generation → truth validation → confidence
scoring → user review when confidence is low or evidence is thin.

### 5.15 Universal Application Engine — CORE (doc 11)
Three modes only: `AUTO`, `ASSISTED`, `MANUAL`. AUTO executes through an
authorized mechanism unattended. ASSISTED executes and pauses at points
requiring a human (verification, missing info, confirmation, unexpected
page). MANUAL prepares a full package (resume, cover letter, answers,
documents, instructions, URL) for the user to submit themselves — this is a
successful workflow outcome, never an error.

### 5.16 Manual Handoff — CORE (doc 15)
Explicit success-path workflow: automation unavailable → save state → build
package → explain why → hand off URL → user applies → user marks Applied →
enters tracking. Valid reasons include unsupported platform, CAPTCHA/human
verification, auth requirements, unknown flow, platform restriction, missing
info, unsupported question type. JobPilot never attempts to defeat
anti-automation controls — see Non-Goals.

### 5.17 Universal Form Intelligence — CORE (doc 13)
Understands text inputs, textareas, dropdowns, radios, checkboxes, file
uploads, date pickers, multi-select, conditional and dynamic fields, using
DOM + accessibility tree + structure + screenshot + AI interpretation,
normalized into `Form` / `FormSection` / `FormField` / `FieldType` /
`FieldConstraint` / `FieldAnswer`.

### 5.18 Browser Agent — CORE (doc 14)
Playwright for Java. All actions (navigate, click, fill, select, upload,
scroll, wait, inspect, screenshot, back, forward) pass through a Policy
Engine and validation layer before execution. The model never executes
arbitrary generated code directly against the browser.

### 5.19 Platform Adapters — CORE for Greenhouse/Lever/Workable/Ashby,
ADVANCED for additional ATS, CORE for generic fallback (doc 11)
Adapters exist only where a platform's structure justifies one; everything
else falls through to the generic browser provider driven by Universal Form
Intelligence.

### 5.20 Application Session Management — CORE (doc 17)
Sessions support pause, resume, retry, cancel, handoff, recover, with
persisted URL, workflow state, form state, completed/pending fields,
generated documents, browser session metadata (where appropriate), errors,
and timestamps.

### 5.21 Application State Machine — CORE (doc 11)
States: `DISCOVERED, ANALYZING, MATCHED, SELECTED, PREPARING, READY,
WAITING_FOR_APPROVAL, APPLYING, WAITING_FOR_USER, SUBMITTED, TRACKING,
INTERVIEW, OFFER, REJECTED, WITHDRAWN, FAILED`. All valid transitions are
enumerated in doc 11, not left implicit.

### 5.22 Application Tracking — CORE (doc 18)
Full lifecycle timeline from discovery through offer/rejection/withdrawal,
plus manual status updates.

### 5.23 Email / Response Tracking — FUTURE (doc 18)
Optional integration to associate recruiter responses with applications.
Never assumes unrestricted mailbox access.

### 5.24 Interview Intelligence — FUTURE (doc 18 references, dedicated future
doc)
Prep, company research, resume-based questions, STAR generation, mock
interviews, feedback, follow-up drafting.

### 5.25 Career Memory — ADVANCED (doc 20)
Structured, explainable long-term record: role categories applied to,
response-rate patterns, recurring gaps, strong-match categories.

### 5.26 Learning System — ADVANCED (doc 21)
Feedback loop from applications/responses/interviews/rejections/offers into
role/skill/resume/strategy/source performance signals. Never silently
modifies critical user information; recommendations stay explainable.

### 5.27 Job Search Strategy Optimization — ADVANCED (doc 10, doc 21)
Recommends application-effort allocation across role categories, adapting
from evidence over time.

### 5.28 User Preferences — CORE (doc 03, doc 05)
Target/excluded roles, locations, work mode, salary bounds, employment type,
experience level, company allow/deny lists, technologies, industries,
relocation, work authorization, application frequency, approval mode.

### 5.29 Automation Control — CORE (doc 11)
User-selectable: fully manual, approve-every-application, approve-per-
company, approve-per-batch, or auto-apply within explicit user-defined
rules. The system must always show what it is about to do before AUTO
execution.

### 5.30 Batch Applications — ADVANCED (doc 11)
Select N jobs → analyze → generate → validate → batch review → user
approval → execute eligible → manual handoff for the rest. Duplicate
prevention is mandatory (ties to 5.5).

### 5.31 Scheduling — ADVANCED (doc 33)
Periodic discovery, prep, follow-up, deadline reminders, status checks —
bounded by each source's stated rate limits, never by scraping around them.

### 5.32 Notifications — CORE (in-app), ADVANCED (email/desktop), FUTURE
(other channels) (doc 18)
High-match jobs, needs-attention applications, manual-required, submitted,
failed, interview, deadline, status change.

### 5.33 Dashboard — CORE (doc 05, frontend)
Discovered/matched/prepared/submitted/manual/interviews/offers/rejections
counts, response and success rates, top opportunities, needs-attention
queue, pipeline view, skill gaps.

### 5.34 Analytics — ADVANCED (doc 19)
Match distribution, applications by role/company/source, response/interview/
offer rates, time-to-response, resume-version performance, source
performance.

## 6. Non-Functional Requirements

- **Security & privacy are CORE from day one** (docs 22–25), not retrofitted.
- **Zero-Fabrication Policy is enforced technically**, not just documented —
  via schema/evidence validation in the generation pipeline (doc 23).
- **Every AI-touching component treats external content (resumes, job
  postings, company pages, application pages) as untrusted input**, subject
  to prompt-injection defenses (doc 23).
- **Every automation action is auditable** (doc 48 concept, formalized in
  doc 22/29): who/what decided, what evidence supported it, what executed.
- **Rate limits and platform terms are respected**, never bypassed. Anti-
  automation controls (CAPTCHA, human verification) are a MANUAL trigger,
  never a target for defeat.
- **Modular monolith initially** (ADR-001), with documented extraction
  candidates, not microservices-by-default.
- **Java 21+, Spring Boot 3.x, PostgreSQL + pgvector, Redis, Ollama,
  Playwright for Java** as the initial stack (ratified in doc 02 and the ADR
  set).

## 7. Explicit Non-Goals

- Not a general-purpose browser automation tool.
- Not a CAPTCHA/anti-bot defeat mechanism, under any framing.
- Not a system that fabricates or embellishes candidate history.
- Not a system that submits an AUTO application the user hasn't configured
  automation rules to cover.
- Not an MVP architecture with tacked-on scope later — see doc 36 for how
  the full target architecture is built incrementally without debt.

## 8. Success Definition

JobPilot is successful when a candidate can state a goal in natural language
("find me strong backend roles and handle the busywork"), and the system
autonomously executes as much of the discovery → match → apply → track →
learn loop as the user's automation settings permit, escalating precisely
and only at genuine decision points, with every generated claim traceable to
real candidate evidence.

## 9. Traceability to Later Documents

| Requirement area | Owning document |
|---|---|
| System shape, module boundaries | 02-system-architecture |
| Entities, enums, relationships | 03-domain-model |
| Schema, indexes, migrations | 04-database-design |
| Endpoints | 05-api-specification |
| AI provider/model routing | 06-ai-architecture |
| §5.1 | 07-candidate-intelligence |
| §5.3–5.5 | 08-job-discovery |
| §5.6–5.9 | 09-job-matching |
| §5.2, 5.27 | 10-career-strategy |
| §5.10, 5.15, 5.19, 5.21, 5.29, 5.30 | 11-application-engine |
| §5.11, 5.13, 5.14 | 12-application-documents |
| §5.17 | 13-form-intelligence |
| §5.18 | 14-browser-agent |
| §5.16 | 15-manual-handoff |
| Agent boundaries | 16-agent-orchestration |
| §5.20 | 17-workflow-engine |
| §5.22, 5.23, 5.32 | 18-tracking |
| §5.34 | 19-analytics |
| §5.25 | 20-career-memory |
| §5.26 | 21-learning-system |
| NFR security | 22-security, 23-ai-security, 24-privacy, 25-threat-model |
| NFR testing | 26-testing, 27-ai-evaluation, 28-browser-evaluation |
| NFR ops | 29-observability, 30-error-handling, 31-performance, 32-scalability |
| NFR delivery | 33-devops, 34-project-structure, 35-coding-standards,
36-development-roadmap, 37-implementation-tasks |
