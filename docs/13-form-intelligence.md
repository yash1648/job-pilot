# 13 — Form Intelligence

Tier: **CORE**. Owning module: `browser`.

## 1. Purpose

Convert an arbitrary application web page into a normalized, structured
form representation the Application Engine and Browser Agent can reason
about deterministically, instead of re-interpreting raw DOM on every
action.

## 2. Normalized Structures (mirrors doc 03-style entities, browser-session
scoped, not persisted as core domain entities — held in
`ApplicationSession.formState` JSONB)

```
Form { sections: [FormSection] }
FormSection { label, fields: [FormField] }
FormField {
  id, label, type: FieldType, required: bool,
  constraints: [FieldConstraint], currentValue, options (for
  select/radio/checkbox), conditionalOn (nullable — field id + value that
  makes this field visible/required)
}
FieldType = TEXT | TEXTAREA | DROPDOWN | RADIO | CHECKBOX | FILE_UPLOAD |
            DATE_PICKER | MULTI_SELECT
FieldConstraint = { kind: MAX_LENGTH|PATTERN|MIN|MAX|FILE_TYPE|REQUIRED,
                     value }
FieldAnswer { fieldId, value, sourceType: STRUCTURED_DATA|GENERATED_ANSWER|
              USER_PROVIDED, confidence }
```

## 3. Interpretation Pipeline

```
Page loaded (browser.BrowserAgent, doc 14)
   ↓ DOM snapshot + accessibility tree extraction (structural signal —
     preferred source of truth for field type/label/constraints since
     it's deterministic, not model-guessed)
   ↓ where DOM/a11y data is ambiguous or insufficient (custom widgets,
     JS-rendered non-semantic controls): screenshot + ai.VisionService
     (task=PAGE_UNDERSTANDING) interpretation, used to fill gaps only —
     never overrides clear DOM signal
   ↓ FormIntelligenceService assembles the normalized Form
   ↓ conditional field detection: fields whose visibility/requiredness
     changed after a prior fill are re-scanned after each significant
     action (not assumed static for the whole session)
```

## 4. Field Answer Resolution Order

For each `FormField`, resolution is attempted in this fixed order, stopping
at the first confident match:

1. **Structured candidate data** (name, email, phone, address, work
   authorization flags from `CandidateProfile`/`JobPreference`) — highest
   confidence, no generation involved.
2. **Prepared `ApplicationDocument`/`ApplicationAnswer`** (doc 12) matched
   to this field by label/semantic similarity.
3. **Live generation** via the Application Question Engine (doc 12 §3) for
   a field not anticipated during preparation (platform-specific or
   dynamically-inserted question).
4. **Unresolved** → field added to `ApplicationSession.pendingFields`,
   triggers an ASSISTED-mode pause (doc 11 §2, doc 17).

## 5. Dynamic & Multi-Step Forms

Multi-step forms are modeled as a sequence of `Form` snapshots within one
`ApplicationSession`; `formState` retains completed steps' answers so a
step revisit (browser back navigation, validation error requiring
correction) doesn't require re-generation of already-resolved fields.

## 6. Safety Boundary

Form Intelligence only *reads and structures* the page. It never itself
executes an action — structured `FieldAnswer` proposals are handed to the
Browser Agent's action-proposal/validation pipeline (doc 14), which is the
actual execution boundary. This separation is what lets the Policy Engine
validate "does this proposed fill make sense for this field" independent
of "was the page correctly understood."

## 7. Failure Handling

| Failure | Behavior |
|---|---|
| DOM structure unrecognizable (heavily obfuscated/unusual widget framework) | escalate to ASSISTED/MANUAL — Form Intelligence does not force-guess an unrecognized structure |
| Field type misclassified, causing a fill rejection by the page | Browser Agent reports `ApplicationAction.result=FAILURE`; Form Intelligence re-scans that field specifically before retrying, bounded retry count (doc 30) |
| Conditional field logic loops (field visibility flapping) | session paused, `WAITING_FOR_USER`, flagged as an unsupported dynamic pattern (doc 61 browser evaluation fixture) |
