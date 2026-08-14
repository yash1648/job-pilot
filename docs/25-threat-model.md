# 25 — Threat Model

Tier: **CORE**, cross-cutting. Owning module: `security`.

STRIDE-style enumeration; each threat references the mitigating control
documented elsewhere rather than restating it.

## 1. Malicious Resume

**Threat**: crafted PDF/DOCX exploiting a parser vulnerability, or
containing embedded prompt-injection text targeting the extraction
pipeline. **Mitigation**: doc 22 §4 (upload hardening, sandboxed parsing),
doc 23 §2 (structural prompt separation), doc 07 §10 (adversarial fixture
testing).

## 2. Malicious Job Description / Company Page

**Threat**: posting or page text designed to manipulate Job Intelligence
extraction or company enrichment into false conclusions, or to inject
instructions targeting downstream generation (cover letters, answers).
**Mitigation**: doc 23 §2/§3 (UntrustedContent wrapping, evidence
validation on generated output), doc 09 §2 (Company Intelligence never
fills unsourced fields).

## 3. Malicious Application Page (Browser Agent target)

**Threat**: a page attempting to manipulate the Browser Agent's action
loop via deceptive DOM content, or attempting drive-by exploitation of the
browser engine itself. **Mitigation**: doc 14 §4 (process/container
isolation), doc 14 §5 (untrusted content boundary applies to page content
too), doc 14 §3 (fixed action set, no code execution path).

## 4. Prompt Injection (general)

Covered comprehensively in doc 23 §2; threat-model entry exists here for
completeness of the STRIDE pass — Tampering + Elevation of Privilege
category (an injected instruction attempting to make the system act beyond
its intended scope).

## 5. Data Theft (Information Disclosure)

**Threat**: unauthorized read of another candidate's profile/applications;
bulk exfiltration via a compromised account or API abuse. **Mitigation**:
doc 22 §2 (repository-layer scoping), rate limiting on read endpoints (doc
52), audit logging (doc 22 §10) enabling detection of anomalous bulk-read
patterns.

## 6. Credential Theft

**Threat**: theft of JobPilot account credentials, OAuth tokens, or
application-platform credentials (doc 22 §8 credential vault) via
phishing, XSS, or a compromised dependency. **Mitigation**: doc 22 §1
(hashing, rotation), doc 22 §9 (XSS controls), doc 22 §8 (vault
encryption, minimal retention), dependency scanning (doc 33 CI/CD).

## 7. Browser Session Theft

**Threat**: an active `BrowserSession`'s cookies/storage exposed to
another process or candidate. **Mitigation**: doc 14 §4 (per-session
isolated context, container boundary), doc 22 §8.

## 8. Malicious Uploaded File (beyond resume — future document uploads)

**Threat**: any future file-upload surface (e.g., additional certification
documents) reintroducing the doc 22 §4 risk class. **Mitigation**: any new
upload surface must route through the same `storage.StorageService`
hardening — documented here as a standing requirement for future
contributors, not just a one-time fix on the resume path.

## 9. SSRF

Covered in doc 22 §9; threat-model entry: Tampering/Information Disclosure
via server-side requests to internal infrastructure disguised as
company-page enrichment or career-page discovery targets.

## 10. XSS / CSRF

Covered in doc 22 §3/§9.

## 11. Account Takeover

**Threat**: credential stuffing, session hijacking, OAuth callback
manipulation. **Mitigation**: rate-limited login attempts, doc 22 §1
session security, doc 22 §7 OAuth state validation, anomalous-login
notification (doc 18 extension point).

## 12. Unauthorized Application Submission

**Threat**: a bug or manipulated automation rule causing an application to
be submitted without valid consent (violates doc 01's core trust
proposition, arguably the single worst possible failure for this specific
product). **Mitigation**: doc 11 §4 (fail-closed automation rule
evaluation), doc 16 §4 (Policy Engine as the sole path to a real
submission), doc 04 unique constraint (can't duplicate-submit even if
triggered twice), idempotency keys on `/applications/{id}/start` (doc 05).

## 13. False AI-Generated Claims

**Threat**: fabricated experience/skill/claim reaching a real application,
damaging candidate credibility or constituting misrepresentation.
**Mitigation**: doc 23 §4 (three-layer Zero-Fabrication enforcement) — this
is treated as a security/trust threat, not merely a quality issue, given
its real-world consequence for the candidate.

## 14. Residual Risk Statement

No system eliminates all risk. The mitigations above reduce likelihood and
blast radius; doc 27's hallucination-rate and injection-resistance metrics,
and doc 26's security test suite, are the ongoing measurement mechanism
that keeps this threat model from becoming a stale, one-time document.
