# 16 — Agent Orchestration

Tier: **CORE** (policy engine, tool registry), **ADVANCED** (multi-agent
coordination beyond the initial set). Owning module: `application`
(orchestration sub-component), `security` (policy engine), `ai` (tool
execution substrate).

## 1. Classification Discipline

Not every component is an agent. Each capability in the system is
classified as exactly one of:

| Classification | Definition | Examples |
|---|---|---|
| **Deterministic service** | fixed logic, no model call | `CanonicalIdentityService`, duplicate-check, `RateLimiterService` |
| **Rule engine** | declarative rules evaluated against data | Policy Engine automation-rule evaluation (doc 11 §4) |
| **Workflow** | fixed sequence/state machine, may call AI/agents as steps | Application state machine (doc 11 §3), Workflow Engine (doc 17) |
| **LLM capability** | single-shot model call with structured I/O, no autonomous loop | skill extraction, resume-quality analysis, answer generation |
| **Agent** | multi-step, observes-decides-acts loop, chooses among tools within a bounded goal | Browser Agent execution loop (doc 14), Research tasks |

Defaulting a component to "agent" when it's really a single LLM call or a
deterministic workflow step adds unneeded autonomy and unneeded failure
surface — this classification step is mandatory before implementing any
new capability (doc 35 coding standard).

## 2. The Agents

| Agent | Goal | Tools available (§3) | Bounded by |
|---|---|---|---|
| **Candidate Intelligence Agent** | produce a complete, evidence-linked profile from raw resume input | extraction/classification LLM calls, `searchCandidateEvidence` | schema validation, evidence requirement (doc 07) |
| **Job Intelligence Agent** | extract structured requirements from a raw posting | extraction LLM calls | schema validation (doc 09) |
| **Career Strategy Agent** | synthesize target categories from aggregate match/outcome data | `searchCandidateEvidence`, aggregate-query tools | constrained to categories pre-identified by deterministic aggregation (doc 10 §4) — cannot invent categories |
| **Application Strategy Agent** | produce an `ApplicationPlan` for a selected job | `getCandidate`, `getJob`, `analyzeJob`, `searchCandidateEvidence` | Policy Engine on any downstream action it triggers |
| **Application Agent** (= Browser Agent's decision loop, doc 14) | complete a form/submission flow | `fillField`, `inspectApplication`, `uploadDocument`, `openApplication`, `pauseWorkflow` | fixed `BrowserAction` set, Policy Engine validation per action |
| **Research Agent** (FUTURE) | company/interview research beyond current enrichment scope | web-fetch-style tool (scoped, rate-limited), `searchCandidateEvidence` | source allow-list, never used to fabricate company facts |
| **Outcome Analysis Agent** | derive `CareerMemory`/learning signals from outcome data | aggregate-query tools only, no generation-with-side-effects | read-only — cannot itself modify candidate evidence or submit anything |

Each agent exists because its task genuinely requires multi-step,
context-dependent tool selection — not because "agent" sounded more
capable than "service." The Job Intelligence and Application Strategy
"agents" are deliberately kept close to single-pass LLM-capability status
in the initial implementation (doc 36 roadmap) and only promoted to a full
observe-decide-act loop if evidence shows single-pass extraction is
insufficient for the required accuracy (doc 27).

## 3. Tool Registry

Typed internal tools, each with a schema, permission set, validation, audit
event, and defined failure handling:

| Tool | Reads/Writes | Permission scope |
|---|---|---|
| `searchJobs` | read | candidate-scoped |
| `getJob` | read | any active job |
| `analyzeJob` | triggers Job Intelligence (doc 09) | system |
| `getCandidate` | read | candidate-scoped |
| `searchCandidateEvidence` | read (RAG retrieval, doc 06 §4) | candidate-scoped |
| `generateResume` | write (`ResumeVersion`) | candidate-scoped, evidence-validated |
| `generateCoverLetter` | write (`ApplicationDocument`) | candidate-scoped, evidence-validated |
| `answerQuestion` | write (`ApplicationAnswer`) | candidate-scoped, evidence-validated |
| `inspectApplication` | read (Form Intelligence, doc 13) | session-scoped |
| `fillField` | write (browser action proposal) | session-scoped, Policy-Engine-gated |
| `uploadDocument` | write (browser action proposal) | session-scoped, Policy-Engine-gated |
| `openApplication` | write (browser navigate) | session-scoped |
| `pauseWorkflow` / `resumeWorkflow` | write (`ApplicationSession.status`) | candidate-scoped |
| `markApplied` | write (`Application.state`) | candidate-scoped, user-action-only (not agent-invocable) |
| `trackApplication` | write (`ApplicationEvent`) | system |

Every tool invocation produces an `AuditEvent` (doc 04/48) with actor type
`AI_AGENT`, the tool name, and a redacted parameter summary. A tool
returning an error yields a typed failure to the calling agent/workflow,
never a silently-empty success.

## 4. Policy Engine

Deterministic wherever possible — the Policy Engine is *not* another LLM
call for most checks, because "should this be allowed" needs to be
reproducible and auditable, not itself probabilistic:

```
canUseTool(actor, tool, context) → allow | deny(reason)
canSubmitApplication(application) → allow | deny(reason)
requiresApproval(application, automationMode) → bool
isClaimSupportedByEvidence(claim, evidenceRefs) → allow | deny(reason)
isBrowserActionValid(action, currentForm, plan) → allow | deny(reason)
```

Rules are expressed as explicit, testable predicates (doc 26) over
structured state (`ApplicationPlan`, `JobPreference.automationMode`,
`FormField` context, `EvidenceRef` set) — not natural-language prompts
interpreted at decision time. Where a check genuinely requires judgment
(e.g., "is this claim's phrasing a meaningful overstatement of its
evidence") an LLM assist is used but its output is still constrained to a
bounded decision (allow/deny/downgrade) that the deterministic engine
enforces, not a free-form verdict trusted as-is.

## 5. Why This Split Matters

Every place in the system where an AI-proposed action becomes a real
side-effect (browser action, submitted application, persisted claim about
the candidate) passes through the Policy Engine — this is the single
choke point referenced in doc 02 §5 and doc 23, and it's why the tool
registry's permission/validation columns above are not decorative: they
are what the Policy Engine actually checks against.
