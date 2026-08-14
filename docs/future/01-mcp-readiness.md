# future/01 — MCP Readiness

Tier: **FUTURE**. Not implemented now; this document ensures doc 02–17's
architecture doesn't need rework when it is.

## 1. Principle

MCP is an adapter over existing `application`/`jobs`/`matching`/`career`
service interfaces (doc 05's underlying services) — never a place where
new business logic lives. Every MCP tool below is a thin wrapper calling
the same service method an API controller would call.

## 2. Candidate Tool Surface

`search_jobs, get_job, analyze_job, get_match, get_candidate_profile,
prepare_application, get_application, open_manual_application,
get_application_answers, mark_applied, get_application_status,
get_career_strategy, get_application_analytics`

Read tools map directly to doc 05 GET endpoints. Write tools
(`prepare_application`, `mark_applied`) map to the same doc 05 POST
endpoints and inherit their exact authorization/approval requirements —
`prepare_application` cannot skip the doc 11 §4 automation-mode/approval
gate just because it arrived via MCP instead of the REST API; the Policy
Engine (doc 16 §4) does not distinguish caller transport.

## 3. Explicit Exclusion

No MCP tool wraps `/applications/{id}/start` directly for AUTO execution
without the same approval flow a human user would go through in the
Cockpit — an external AI client cannot use MCP as a side-channel around
doc 11's automation controls.

## 4. Future AI Client Integrations

OpenCode, Hermes, other agents/desktop assistants integrate through this
MCP surface (or the stable REST API directly) — never direct database
access, consistent with doc 02's module-boundary discipline extended to
external consumers.

## 5. Readiness Checklist (must all be true before implementation starts)

- [ ] doc 05 API contract stable across at least one full roadmap phase
  (doc 36) without breaking changes.
- [ ] Policy Engine (doc 16 §4) proven correct via doc 25 §12's dedicated
  security tests.
- [ ] Doc 27/28 eval suites green at target bands for the underlying
  capabilities MCP would expose.
