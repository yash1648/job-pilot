# 08 — Job Discovery

Tier: **CORE** (discovery + normalization), **ADVANCED** (additional ATS
adapters beyond the initial four). Owning module: `jobs`.

## 1. `JobSource` Abstraction

```java
public interface JobSource {
    JobSourceMetadata metadata();
    Stream<RawJobPosting> fetchSince(Instant lastSync) throws JobSourceException;
    ApplicationCapability capabilityFor(RawJobPosting posting);
}
```

`RawJobPosting` is source-native shape (whatever fields that API/ATS
returns); it is converted to the canonical `Job` domain entity (doc 03)
only through the Job Normalization stage — no module downstream of
discovery ever sees a `RawJobPosting`.

## 2. Source Categories & Initial Adapters

| Category | Initial adapters (CORE) | Additional (ADVANCED) |
|---|---|---|
| ATS | `GreenhouseSource`, `LeverSource`, `WorkableSource`, `AshbySource` | other ATS platforms as demand justifies |
| Company career page | `GenericCareerPageSource` (structured-data/sitemap based) | company-specific scrapers only where a page has no structured data and volume justifies bespoke handling |
| Job API / public feed | `GenericJobFeedSource` | additional partner feeds |
| Partner integration | — | FUTURE, contractual integrations |

Each adapter implements `JobSource` and declares its own
`JobSourceMetadata` (doc 03 `JobSource` entity fields): capability
(`AUTO_CAPABLE`/`ASSISTED_CAPABLE`/`MANUAL_ONLY`), auth requirements, rate
limit policy. Adapters never assume another adapter's capabilities are
available — this is the point of the abstraction (master spec §5).

## 3. Job Source Intelligence

`JobSourceCapabilityResolver` combines `JobSource.metadata()` with
per-posting signals (does this specific posting use an embedded ATS form
vs. an external redirect; does the ATS instance require login) to produce
the actual `Job.applicationMethod`, which can be narrower than the source's
general capability (e.g., a Greenhouse-hosted job behind an assessment
gate may resolve to `MANUAL_ONLY` even though Greenhouse is generally
`ASSISTED_CAPABLE`).

## 4. Job Normalization

```
RawJobPosting
   ↓ CanonicalIdentityService: hash(normalizedTitle + normalizedCompany +
        normalizedLocation + sourcePostingId-or-URL-fingerprint)
   ↓ dedup check against jobs.canonical_identity (unique index, doc 04)
   ↓ field normalization:
        title       → normalized_title (casing, seniority-token stripped
                       for grouping but preserved in title)
        company     → resolved/created Company row (normalized_name)
        location    → normalized_location (city/region/country, remote flag)
        salary      → currency-normalized range
        employment  → enum mapping (FULL_TIME/PART_TIME/CONTRACT/INTERN)
        seniority   → enum mapping from title + description signals
        technology  → normalized against the same skill taxonomy used in
                       doc 07, so job requirements and candidate skills are
                       directly comparable in doc 09
   ↓ Job persisted (new) or existing Job updated (re-seen posting) —
        never a duplicate Job row for the same canonical identity
```

If normalization cannot confidently resolve company/location/title (e.g.,
malformed source data), the Job is still persisted with the available
fields and a `potentialConcerns` entry (`"normalization_incomplete"`)
rather than dropped — downstream matching treats such jobs conservatively
(lower confidence, doc 09) instead of silently excluding them.

## 5. Duplicate Prevention

Two layers: `canonical_identity` uniqueness (doc 04) prevents duplicate
`Job` rows across sync runs and across sources reposting the same role; the
`ux_applications_active_per_job` constraint (doc 04) prevents duplicate
`Application` rows even if a normalization edge case ever let two Job rows
represent the same real posting — belt and suspenders, not a single point
of failure.

## 6. Scheduling & Rate Limits (cross-ref doc 33, doc 52)

Each `JobSource` sync runs on its own schedule respecting
`rateLimitPolicyRef`; a shared `RateLimiterService` (Redis-backed token
bucket) enforces per-source concurrency and request-rate ceilings.
Discovery never retries around a 429/rate-limit response with shortened
backoff — backoff only increases. On-demand discovery (`POST
/jobs/discover`, doc 05) is itself rate-limited per user to prevent
accidental hammering of a source.

## 7. Failure Handling

| Failure | Behavior |
|---|---|
| Source unreachable | `JobSourceException`, sync marked failed, `lastSuccessfulSyncAt` unchanged, retried on next scheduled window with backoff |
| Source returns malformed data | skip that posting, log + audit, continue processing the rest of the batch |
| Source auth expired | source marked inactive-pending-reauth, notification to admin/user if user-linked (e.g., a personal ATS credential) |
| Rate limit hit | halt remaining requests in the run, resume next window |

## 8. Non-Goals (restated from doc 01 §7)

No adapter bypasses login walls, solves CAPTCHAs, or scrapes a source in a
way that violates its stated terms. A source that cannot be accessed
legitimately is not integrated, full stop — this is a product boundary, not
a per-adapter judgment call.
