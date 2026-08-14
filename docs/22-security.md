# 22 — Security

Tier: **CORE**, cross-cutting. Owning module: `security`, `audit`.

## 1. Authentication

Password auth (bcrypt/Argon2id hashing, never reversible encryption) plus
OAuth providers (doc 05 `/auth/oauth/{provider}/callback`). Sessions: JWT
for API/future-frontend use, HTTP-only+Secure+SameSite=Lax cookies for the
Thymeleaf frontend. JWT short-lived (access) + refresh token rotation;
refresh tokens stored hashed, revocable per-session (supports "log out
everywhere").

## 2. Authorization

Every `candidate`-scoped resource access is filtered by
`SecurityContext.candidateProfileId` at the repository/query layer, not
only checked in the controller — this defense-in-depth means a missed
controller-level check doesn't leak cross-candidate data (doc 05 §11).
Admin-only endpoints (`job-sources` management) require a distinct role
claim, checked via a method-security annotation, not a manual `if` in each
controller.

## 3. Session Security

CSRF protection (Spring Security CSRF tokens) on all state-changing
Thymeleaf-form-originated requests; API (JWT-bearer) requests are exempted
from CSRF per standard practice since they don't rely on ambient cookie
auth for mutation. Session fixation protection on login (session ID
regenerated). Idle timeout + absolute timeout both enforced.

## 4. File Upload Security

Resume uploads (doc 07): MIME-type allow-list (PDF, DOCX only) enforced by
content-sniffing, not just filename extension; size limits; storage
outside the web root with access mediated only through
`storage.StorageService` (no direct file-path exposure); uploaded files
scanned for known-malicious patterns (macro-laden DOCX, embedded
executables) before parsing — parsing itself runs with library-level
protections against zip-bomb/XXE-style attacks in DOCX (which is a zip)
and PDF parsing.

## 5. Document Storage & Encryption

Resumes and generated documents (cover letters, tailored resumes) encrypted
at rest (storage-provider-level encryption at minimum, application-level
envelope encryption for the object storage bucket in production, doc 33).
Encryption keys managed via a secrets manager (doc 33 §Secrets), never
embedded in config files or source.

## 6. Secrets

All credentials (DB, Redis, Ollama endpoint if remote, OAuth client
secrets, any future job-source API keys) loaded from environment/secrets
manager at startup, never committed, never logged (doc 29 §Do not log
secrets).

## 7. OAuth

Standard authorization-code flow with PKCE for any future frontend that
isn't a confidential client; state parameter validated to prevent CSRF on
the OAuth callback; scopes requested are the minimum needed (identity
only, unless a specific integration like doc 18 §3 email tracking is
separately, explicitly authorized with its own narrower scope).

## 8. Browser Session Security (doc 14 cross-ref)

Each `BrowserSession` is isolated per `ApplicationSession` (doc 14 §4);
cookies/storage from one candidate's browser context are never accessible
to another's. If an application platform requires the candidate's own
login (e.g., a personal ATS account), those credentials are handled via a
dedicated, encrypted credential-vault flow — never stored in plaintext, and
entered by the user directly into the live browser session where feasible
rather than passed through the application layer at all (minimizes
exposure surface).

## 9. Web-Facing Attack Surface

- **XSS**: Thymeleaf's default output-escaping is not disabled anywhere
  templates render candidate/job/company text (which can contain
  attacker-influenced content since job postings are third-party text);
  React frontend (future) uses standard escaping/no `dangerouslySetInnerHTML`
  on untrusted content.
- **SSRF**: any server-side fetch of a user-supplied or job-posting-
  supplied URL (company page enrichment, doc 09 §2; job source career-page
  adapter, doc 08) goes through an egress-controlled fetcher with an
  IP/hostname allow-and-deny policy (blocks internal/private IP ranges,
  cloud metadata endpoints) — never a raw outbound HTTP client on
  arbitrary input.
- **SQL injection**: parameterized queries / JPA throughout; no string-
  concatenated SQL, including in dynamic filter-building for search
  endpoints (doc 05 `/jobs` filters).
- **Prompt injection**: doc 23, dedicated document.

## 10. Audit Logging

Every security-relevant event (login, logout, permission denial, password
change, admin action, application submission, manual-handoff completion)
produces an `AuditEvent` (doc 04) — append-only, no delete/update grant on
that table for the application's own DB role, viewable by the user for
their own data (`GET /settings/audit`, doc 05).

## 11. Threats Detailed Elsewhere

AI-specific threats (prompt injection, data exfiltration via generated
content, malicious documents/pages as attack vectors) are covered in doc
23; the full structured threat model (STRIDE-style enumeration with
mitigations) is doc 25; privacy-specific controls (retention, deletion,
export, minimization) are doc 24.
