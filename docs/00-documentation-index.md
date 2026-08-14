# JobPilot Documentation Index

This index is the single source of truth for build order, document status, and
the terminology contract every other document must obey. If a later document
needs a term not defined here, it must be added here first — not invented
locally.

## Build Order (dependency chain)

```
01 Product Requirements
   ↓
02 System Architecture
   ↓
03 Domain Model
   ↓
04 Database Design
   ↓
05 API Specification
   ↓
06 AI Architecture ──┬── 07 Candidate Intelligence
                     ├── 08 Job Discovery
                     ├── 09 Job Matching
                     └── 10 Career Strategy
   ↓
11 Application Engine ──┬── 12 Application Documents
                        ├── 13 Form Intelligence
                        ├── 14 Browser Agent
                        ├── 15 Manual Handoff
                        ├── 16 Agent Orchestration
                        └── 17 Workflow Engine
   ↓
18 Tracking ──┬── 19 Analytics
              ├── 20 Career Memory
              └── 21 Learning System
   ↓
22 Security ──┬── 23 AI Security
              ├── 24 Privacy
              └── 25 Threat Model
   ↓
26 Testing ──┬── 27 AI Evaluation
             └── 28 Browser Evaluation
   ↓
29 Observability ──┬── 30 Error Handling
                   ├── 31 Performance
                   └── 32 Scalability
   ↓
33 DevOps ──┬── 34 Project Structure
            ├── 35 Coding Standards
            ├── 36 Development Roadmap
            └── 37 Implementation Tasks
   ↓
ADR set (adr/ADR-001..012, extended as decisions arise)
   ↓
00-consistency-report.md (audit pass over everything above)
   ↓
implementation-readiness.md
```

Each document is only started after everything above it in the chain is
written, because each layer consumes vocabulary defined by the layer before
it. Documents are delivered a small batch at a time, not all at once — after
each batch, terminology introduced in it gets added to the contract table
below before the next batch starts.

## Status

| # | Document | Status |
|---|----------|--------|
| 00 | documentation-index | done |
| 01 | product-requirements | done |
| 02 | system-architecture | done |
| 03 | domain-model | done |
| 04 | database-design | done |
| 05 | api-specification | done |
| 06 | ai-architecture | done |
| 07 | candidate-intelligence | done |
| 08 | job-discovery | done |
| 09 | job-matching | done |
| 10 | career-strategy | done |
| 11 | application-engine | done |
| 12 | application-documents | done |
| 13 | form-intelligence | done |
| 14 | browser-agent | done |
| 15 | manual-handoff | done |
| 16 | agent-orchestration | done |
| 17 | workflow-engine | done |
| 18 | tracking | done |
| 19 | analytics | done |
| 20 | career-memory | done |
| 21 | learning-system | done |
| 22 | security | done |
| 23 | ai-security | done |
| 24 | privacy | done |
| 25 | threat-model | done |
| 26 | testing | done |
| 27 | ai-evaluation | done |
| 28 | browser-evaluation | done |
| 29 | observability | done |
| 30 | error-handling | done |
| 31 | performance | done |
| 32 | scalability | done |
| 33 | devops | done |
| 34 | project-structure | done |
| 35 | coding-standards | done |
| 36 | development-roadmap | done |
| 37 | implementation-tasks | done |
| — | ADR-001..012 | done |
| — | consistency-report | done — zero unresolved conflicts |
| — | implementation-readiness | done — gate OPEN |

## Terminology Contract (grows as docs are written)

Locked in doc 01, do not redefine downstream:

- **Product name**: JobPilot. Category: AI-powered autonomous job-hunting and
  application orchestration platform ("AI Career Operating System").
- **Application modes**: `AUTO`, `ASSISTED`, `MANUAL` (exactly these three,
  exactly this casing, as an `ApplicationMode` enum).
- **Three product tiers**: `CORE PRODUCT`, `ADVANCED PRODUCT`,
  `FUTURE EXTENSIONS` — every capability in every doc must be tagged with
  exactly one of these three.
- **Zero-Fabrication Policy**: capitalized, referenced by this exact name in
  every doc that touches generation (resume, cover letter, answers).
- **Manual handoff is a success state**, never an error state. Referred to as
  "Manual Handoff" (capitalized) everywhere.

## Rule for contributors (human or AI coding agent)

Do not begin implementation before `00-consistency-report.md` reports zero
unresolved critical conflicts and `implementation-readiness.md` exists. These
two documents are the gate.
