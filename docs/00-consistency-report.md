# 00 — Consistency Report

Audit pass over docs 01–37, adr/001–012, future/01, performed against doc
78's checklist. This report is the gate: implementation does not start
until every item below is either clean or has a logged, accepted
resolution.

## 1. Domain Consistency

Entity/enum names cross-checked doc 03 → doc 04 → doc 05 → doc 11 → all
module docs. `ApplicationState`, `ApplicationMode`,
`ApplicationAutomationMode`, `ManualHandoffReason` used identically
everywhere they appear. **Result: consistent.**

One naming note resolved during authoring, not left ambiguous:
`Job.applicationMethod` (per-job, doc 03) vs.
`JobSource.applicationMethodCapability` (per-source, doc 03) — deliberately
distinct fields (doc 08 §3 explains a job's method can be narrower than its
source's general capability). Documented explicitly so it doesn't read as
a duplicate/typo. **Result: intentional, documented.**

## 2. Database Consistency

Doc 04 schema checked against every doc 03 entity — all entities have a
corresponding table; all doc 03 §6 invariants have a corresponding doc 04
enforcement mechanism (constraint, unique index, or explicit note that
enforcement is service-layer, doc 04 §2.5 comment on `application_answers`
confidence gate). **Result: consistent.**

## 3. API Consistency

Doc 05 endpoints checked against the services/modules they claim to call
(doc 02 §3, doc 11, doc 12, doc 18, doc 19). Every endpoint maps to a real
module. `POST /applications/{id}/start` correctly cross-references doc 11
§4's automation-gate enforcement. **Result: consistent.**

## 4. AI Consistency

Every AI-touching doc (06, 07, 09, 10, 12, 16, 23) uses the same
`AiTaskType` routing concept (doc 06 §2) and the same `UntrustedContent`/
evidence-validation discipline (doc 23). No document introduces a
parallel/competing AI-access pattern. **Result: consistent.**

## 5. Workflow Consistency

Doc 11 §3 state transitions checked against doc 15 (Manual Handoff), doc
17 (Workflow Engine), doc 18 (Tracking) — all three correctly reference
the same state names and the same transition boundaries (e.g., doc 15's
mid-flow fallback correctly targets `APPLYING → WAITING_FOR_USER`, an
actual listed transition). **Result: consistent.**

## 6. Browser Consistency

Doc 14's fixed `BrowserAction` set matches doc 13's field-resolution
handoff and doc 28's fixture/evaluation set — no document assumes an
action type doc 14 §3 doesn't define. **Result: consistent.**

## 7. Manual Handoff Consistency

Doc 15's `ManualHandoffReason` enum is referenced identically in doc 11
§1 (`ApplicationPlan`), doc 14 §6 (CAPTCHA trigger), doc 17 §6 (adapter
fallback). **Result: consistent.**

## 8. Security Consistency

Doc 22 (general security), doc 23 (AI-specific), doc 24 (privacy), doc 25
(threat model) cross-reference each other without contradiction; doc 25's
threat entries each point to a real mitigating section in 22/23/24 rather
than a dangling reference. **Result: consistent.**

## 9. Privacy Consistency

Doc 24's data inventory (§1) matches doc 04's actual table list; retention
rules (§4) don't contradict doc 04 §5's cascade-delete behavior — audit
event non-cascade is stated identically in both documents. **Result:
consistent.**

## 10. Testing Coverage

Doc 26 §1 pyramid references doc 27 (AI eval) and doc 28 (browser eval) as
distinct, correctly-scoped suites rather than folding them into the
general pyramid — avoids double-counting or a coverage gap. Every doc 03
§6 invariant has an explicit test reference (doc 26 §4). Every doc 30 §6
failure scenario has a corresponding "Tests" expectation via doc 26 §6's
done-definition. **Result: no gaps identified.**

## 11. Scalability Risks

Doc 32 §4's extraction candidates are explicitly gated on signals, not
scheduled — cross-checked against doc 36's roadmap (Phase 12) to confirm
no earlier phase silently assumes an extracted-service topology. **Result:
consistent, no premature-extraction risk found in the roadmap.**

## 12. Implementation Risks

- Doc 06's Ollama-only initial provider (ADR-005) is a real constraint on
  achievable model quality; doc 27's eval-band targets must be set with
  this in mind rather than assuming hosted-frontier-model performance.
  **Logged as an accepted risk, not a blocking conflict** — doc 36 Phase
  2's exit criterion already frames targets as "hit their target bands,"
  leaving room to calibrate bands to the actual provider once real eval
  data exists, rather than the docs prescribing a fixed number now that
  might be wrong.
- Doc 37 §3 intentionally defers granular task breakdown for Phases 2–13
  until each phase begins. **Logged as an accepted scope boundary**, not a
  missing-requirement gap — doc 36 already specifies phase-level exit
  criteria sufficient to start Phase 1 implementation now.

## 13. Missing Requirements Check

Every master-prompt section (§1–81 of the original brief) traced to at
least one doc 01–37/adr/future file. No section identified without a home.
Frontend screens (master §55–58) intentionally folded into doc 02 §7
(overview) and doc 11 §6 / doc 15 §4 (detailed UI contracts) rather than
given a standalone numbered doc, since the original file list (master
§74) has no dedicated frontend document number — **noted as a deliberate
interpretation, not an omission**.

## 14. Recommended Corrections

None outstanding. The two items in §12 are risks to monitor during
implementation (doc 36/27), not documentation conflicts requiring a
rewrite.

## 15. Verdict

**Zero unresolved critical conflicts.** Documentation set is internally
consistent per doc 76's requirement. Proceed to
`implementation-readiness.md`.
