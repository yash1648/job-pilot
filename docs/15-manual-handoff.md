# 15 — Manual Handoff

Tier: **CORE**. Owning module: `application` (handoff sub-component),
`browser` (fallback trigger), `tracking` (post-handoff entry).

## 1. Framing

Manual Handoff is a **successful workflow outcome**, not an error path.
Every document referencing it uses this framing consistently — an
application that ends in a well-prepared Manual Handoff has still received
the full value of Candidate/Job/Matching/Document-generation work; only the
final click is left to the user.

## 2. Trigger Reasons (exhaustive enum, `ManualHandoffReason`)

`UNSUPPORTED_PLATFORM, CAPTCHA_OR_VERIFICATION, AUTHENTICATION_REQUIRED,
UNKNOWN_APPLICATION_FLOW, PLATFORM_RESTRICTION, MISSING_USER_INFORMATION,
UNSUPPORTED_QUESTION_TYPE, USER_PREFERENCE_MANUAL_ONLY,
AUTOMATION_LIMIT_OTHER`

Any of these can occur either as the initial mode decision (doc 11 §2, from
`Job.applicationMethod`) or as a **mid-flow fallback**: an AUTO/ASSISTED
execution that hits CAPTCHA (doc 14 §6) or an unrecoverable Form
Intelligence gap (doc 13 §7) transitions `APPLYING → WAITING_FOR_USER` with
the specific reason recorded, and the already-completed partial progress
(filled fields, uploaded documents) is preserved in `formState`, not
discarded.

## 3. Pipeline

```
Automation unavailable (initial or mid-flow)
   ↓ ApplicationSession.status → PAUSED (if mid-flow) or created fresh
   ↓ current workflow state persisted (formState, completed/pending
     fields, currentUrl)
   ↓ ManualHandoffPackageService assembles:
        - tailored Resume (ResumeVersion, downloadable)
        - Cover Letter (ApplicationDocument)
        - Application answers (ApplicationAnswer list, human-readable)
        - any other required documents already generated
        - plain-language explanation of the specific reason
        - application URL (Job.applicationUrl, or currentUrl if mid-flow
          progress exists so the user can resume where automation stopped)
   ↓ Application.state → WAITING_FOR_USER
   ↓ notification.NotificationService: "manual application required"
   ↓ user opens URL, applies (manually completing any already-filled
     progress if resuming mid-flow), returns and calls
     POST /applications/{id}/mark-applied
   ↓ Application.state → SUBMITTED → TRACKING (doc 18)
```

## 4. Manual Handoff UI Contract

```
Application cannot be automated

Reason: <plain-language ManualHandoffReason explanation>

Prepared:
✓ Tailored Resume
✓ Cover Letter
✓ N Application Answers

Application URL: [ Open Application ]

After completing: [ Mark as Applied ]
```

For mid-flow fallback, the UI additionally shows what was already
completed ("JobPilot filled: personal information, resume upload, work
history") before the "your action required" explanation — this is the
Assisted-Mode UI pattern (doc 01 §58) reused here since mid-flow MANUAL
fallback and ASSISTED pauses share the same underlying partial-progress
data model.

## 5. Relationship to Assisted Mode

ASSISTED and mid-flow MANUAL fallback share `ApplicationSession.status =
PAUSED` and the same `formState`; the difference is whether the pause is
expected to be resumed by the Browser Agent after user action (ASSISTED —
`[Take Control]` → `[Resume JobPilot]`) or is a terminal handoff to full
manual completion (MANUAL fallback — `[Open Application]` → `[Mark as
Applied]`, no further automated execution). This distinction is carried as
`ApplicationSession.status` plus the presence/absence of a
`ManualHandoffReason` that is inherently non-resumable (e.g.,
`CAPTCHA_OR_VERIFICATION` is typically resumable via ASSISTED; a platform
whose entire flow is `UNSUPPORTED_PLATFORM` is not).

## 6. Explicit Non-Goal Restated

Manual Handoff exists specifically so JobPilot never attempts to defeat a
CAPTCHA, verification challenge, or anti-automation control. It is the
designed alternative to that temptation, not a fallback of last resort to
be minimized at the cost of the boundary in doc 01 §7/§18.

## 7. Failure Handling

| Failure | Behavior |
|---|---|
| Package assembly fails (e.g., document generation incomplete) | Application stays in `PREPARING`/`FAILED` rather than presenting an incomplete handoff package |
| User never marks Applied | Application remains `WAITING_FOR_USER`; reminder notification scheduled (doc 18), no auto-transition to `SUBMITTED` |
| User marks Applied but never actually applied (self-reported inaccuracy) | out of system's control by design — MANUAL mode's trust boundary is the user's own action; downstream analytics (doc 19) label MANUAL-sourced `SUBMITTED` status as self-reported |
