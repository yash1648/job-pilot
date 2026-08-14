# Implementation Readiness Report

## Can the system now be implemented?

Yes, starting with doc 36 Phase 1 (Foundation) and doc 37's Phase 1 task
breakdown. The consistency report (`00-consistency-report.md`) found zero
unresolved critical conflicts.

## What remains ambiguous?

- Exact match-weighting coefficients (doc 09 §3) are deliberately left
  configurable rather than fixed — real tuning requires real match/outcome
  data that doesn't exist before Phase 4 begins.
- Exact eval-metric target bands (doc 27) are stated as categories
  ("hit target bands") rather than fixed numbers, pending calibration
  against the actual Ollama-served model quality (doc 27 §1, consistency
  report §12).

Neither ambiguity blocks Phase 1, which touches neither system.

## What decisions remain?

- Concrete model selection within Ollama (which specific model per
  `AiTaskType` profile, doc 06 §2) — an environment-config decision made
  during TASK-JP-0005, not an architectural one.
- Specific Redis-backed queue implementation detail (doc 32 §2's "embedded
  scheduler, upgradable to Streams") — deferred until Phase 9 scheduling
  needs are concrete.

## What assumptions exist?

- Ollama is deployable with adequate compute for the target model sizes
  (ADR-005 accepted this trade-off explicitly).
- Initial job source integrations (Greenhouse, Lever) remain available on
  terms compatible with doc 08 §8's non-goal boundary (no bypass of
  auth/rate limits) — if a source's terms change, doc 08's adapter can be
  disabled (`JobSource.isActive`) without an architecture change.

## What risks remain?

- Doc 25's threat model is only as good as its ongoing test coverage (doc
  25 §14) — this is a process risk (tests must actually be maintained as
  new attack patterns emerge, doc 28 §6), not a design gap.
- AUTO mode (doc 36 Phase 8) carries the highest-consequence failure mode
  in the product (doc 25 §12); the roadmap deliberately sequences it last
  among execution modes specifically to manage this risk, not to avoid
  addressing it.

## What should be implemented first?

Doc 36 Phase 1 exactly as scoped: auth, schema, AI provider wiring, CI
gate — no business feature ships before the foundation and its test
infrastructure (doc 26 §5 PR-gate) exist.

## What should never be implemented prematurely?

Restated from doc 36 §3: AUTO mode before ASSISTED is proven; Learning
System adjustments before doc 21 §6's cold-start sample-size gate clears;
service extraction (doc 32 §4) before a concrete scaling/deploy signal;
MCP (doc future/01) before the readiness checklist in that document is
fully met.

## Gate Status

**OPEN.** Documentation phase complete. Proceeding to code scaffold
(Phase 1, TASK-JP-0001) next.
