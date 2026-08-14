# 04 — Database Design

Primary store: **PostgreSQL 16+** with **pgvector** for embeddings. Redis is
used for caching, queues, and ephemeral session state (browser session
tokens, workflow locks) — not system-of-record data. All table/column names
below match doc 03 entity names exactly.

## 1. Conventions

- Primary keys: `UUID` (`gen_random_uuid()`), except append-only event
  tables which additionally carry a `BIGSERIAL` for cheap ordering.
- Timestamps: `TIMESTAMPTZ`, always UTC.
- Soft delete only where doc 24 (privacy) requires retention windows;
  otherwise hard delete on user-initiated deletion (doc 24 §Deletion).
- Every table has `created_at`; mutable tables also have `updated_at`
  maintained by trigger.
- JSON fields use `JSONB` with a documented shape (referenced from doc 03),
  never free-form.

## 2. Schema Groups

### 2.1 Identity & Preferences

```sql
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email CITEXT UNIQUE NOT NULL,
  password_hash TEXT,
  auth_provider TEXT NOT NULL DEFAULT 'LOCAL',
  status TEXT NOT NULL DEFAULT 'ACTIVE'
    CHECK (status IN ('ACTIVE','SUSPENDED','DELETED')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  last_login_at TIMESTAMPTZ
);

CREATE TABLE candidate_profiles (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  headline TEXT,
  seniority_estimate TEXT,
  domain_classification TEXT[] DEFAULT '{}',
  career_trajectory_summary TEXT,
  strengths TEXT[] DEFAULT '{}',
  weaknesses TEXT[] DEFAULT '{}',
  skill_gaps TEXT[] DEFAULT '{}',
  transferable_skills TEXT[] DEFAULT '{}',
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE job_preferences (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
  target_roles TEXT[] DEFAULT '{}',
  excluded_roles TEXT[] DEFAULT '{}',
  locations TEXT[] DEFAULT '{}',
  work_mode TEXT NOT NULL DEFAULT 'ANY'
    CHECK (work_mode IN ('REMOTE','HYBRID','ON_SITE','ANY')),
  min_salary NUMERIC,
  preferred_salary NUMERIC,
  employment_type TEXT[] DEFAULT '{}',
  experience_level TEXT,
  company_allow_list TEXT[] DEFAULT '{}',
  company_deny_list TEXT[] DEFAULT '{}',
  technologies TEXT[] DEFAULT '{}',
  industries TEXT[] DEFAULT '{}',
  relocation_willing BOOLEAN DEFAULT false,
  work_authorization TEXT,
  application_frequency TEXT,
  automation_mode TEXT NOT NULL DEFAULT 'FULLY_MANUAL'
    CHECK (automation_mode IN
      ('FULLY_MANUAL','APPROVE_EVERY_APPLICATION','APPROVE_PER_COMPANY',
       'APPROVE_PER_BATCH','AUTO_WITHIN_RULES')),
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### 2.2 Candidate Evidence

```sql
CREATE TABLE resumes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
  original_filename TEXT NOT NULL,
  storage_ref TEXT NOT NULL,
  mime_type TEXT NOT NULL,
  is_master BOOLEAN NOT NULL DEFAULT false,
  parse_status TEXT NOT NULL DEFAULT 'PENDING'
    CHECK (parse_status IN ('PENDING','PARSED','FAILED')),
  uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- exactly one is_master=true per candidate, enforced via partial unique index
CREATE UNIQUE INDEX ux_resumes_one_master
  ON resumes (candidate_profile_id) WHERE is_master;

CREATE TABLE resume_versions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  resume_id UUID REFERENCES resumes(id) ON DELETE SET NULL,
  candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
  job_id UUID REFERENCES jobs(id) ON DELETE SET NULL,
  label TEXT NOT NULL,
  content JSONB NOT NULL,
  generation_reason TEXT,
  is_master_snapshot BOOLEAN NOT NULL DEFAULT false,
  generated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_resume_versions_candidate ON resume_versions(candidate_profile_id);
CREATE INDEX ix_resume_versions_job ON resume_versions(job_id);

CREATE TABLE skills (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  normalized_name TEXT NOT NULL,
  category TEXT,
  proficiency TEXT,
  years_experience NUMERIC,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_skills_candidate_norm ON skills(candidate_profile_id, normalized_name);

CREATE TABLE skill_evidence (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
  source_type TEXT NOT NULL
    CHECK (source_type IN ('EXPERIENCE','PROJECT','CERTIFICATION','EDUCATION')),
  source_id UUID NOT NULL,
  excerpt TEXT NOT NULL,
  confidence NUMERIC NOT NULL CHECK (confidence BETWEEN 0 AND 1)
);
CREATE INDEX ix_skill_evidence_skill ON skill_evidence(skill_id);

-- experiences / educations / projects / certifications / achievements
-- share one shape; created as five explicit tables (not one polymorphic
-- table) so FK integrity and per-type fields stay simple.
CREATE TABLE experiences (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  organization TEXT NOT NULL,
  start_date DATE,
  end_date DATE,
  description TEXT,
  extracted_skills TEXT[] DEFAULT '{}',
  raw_source_excerpt TEXT
);
-- educations, projects, certifications, achievements: identical pattern,
-- omitted here for brevity, defined in migration V2__evidence_tables.sql

CREATE TABLE candidate_embeddings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  candidate_profile_id UUID NOT NULL UNIQUE REFERENCES candidate_profiles(id) ON DELETE CASCADE,
  vector vector(1536) NOT NULL,
  model_version TEXT NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_candidate_embeddings_vector
  ON candidate_embeddings USING ivfflat (vector vector_cosine_ops) WITH (lists = 100);
```

### 2.3 Jobs & Companies

```sql
CREATE TABLE companies (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  normalized_name TEXT NOT NULL,
  industry TEXT,
  size_range TEXT,
  hq_location TEXT,
  website TEXT,
  technology_stack TEXT[] DEFAULT '{}',
  product_summary TEXT,
  sourced_from TEXT[] DEFAULT '{}',
  last_enriched_at TIMESTAMPTZ
);
CREATE UNIQUE INDEX ux_companies_normalized_name ON companies(normalized_name);

CREATE TABLE job_sources (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  source_type TEXT NOT NULL
    CHECK (source_type IN ('API','ATS','CAREER_PAGE','FEED','PARTNER')),
  platform TEXT NOT NULL,
  application_method_capability TEXT NOT NULL
    CHECK (application_method_capability IN
      ('AUTO_CAPABLE','ASSISTED_CAPABLE','MANUAL_ONLY')),
  auth_requirements TEXT,
  known_limitations TEXT,
  rate_limit_policy_ref TEXT,
  last_successful_sync_at TIMESTAMPTZ,
  is_active BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE jobs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_source_id UUID NOT NULL REFERENCES job_sources(id),
  company_id UUID REFERENCES companies(id),
  canonical_identity TEXT NOT NULL,
  title TEXT NOT NULL,
  normalized_title TEXT NOT NULL,
  location TEXT,
  normalized_location TEXT,
  salary_min NUMERIC,
  salary_max NUMERIC,
  salary_currency TEXT,
  employment_type TEXT,
  seniority TEXT,
  technologies TEXT[] DEFAULT '{}',
  domain TEXT,
  responsibilities TEXT[] DEFAULT '{}',
  required_skills TEXT[] DEFAULT '{}',
  preferred_skills TEXT[] DEFAULT '{}',
  experience_requirement TEXT,
  education_requirement TEXT,
  application_method TEXT NOT NULL
    CHECK (application_method IN ('AUTO_CAPABLE','ASSISTED_CAPABLE','MANUAL_ONLY')),
  application_url TEXT,
  posted_at TIMESTAMPTZ,
  expires_at TIMESTAMPTZ,
  discovered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  status TEXT NOT NULL DEFAULT 'ACTIVE'
    CHECK (status IN ('ACTIVE','EXPIRED','REMOVED')),
  potential_concerns TEXT[] DEFAULT '{}'
);
CREATE UNIQUE INDEX ux_jobs_canonical_identity ON jobs(canonical_identity);
CREATE INDEX ix_jobs_company ON jobs(company_id);
CREATE INDEX ix_jobs_status ON jobs(status);
CREATE INDEX ix_jobs_normalized_title ON jobs(normalized_title);

CREATE TABLE job_requirements (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
  type TEXT NOT NULL CHECK (type IN ('SKILL','EXPERIENCE','EDUCATION','CERTIFICATION','OTHER')),
  text TEXT NOT NULL,
  is_required BOOLEAN NOT NULL DEFAULT true,
  extracted_entity TEXT
);

CREATE TABLE job_embeddings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_id UUID NOT NULL UNIQUE REFERENCES jobs(id) ON DELETE CASCADE,
  vector vector(1536) NOT NULL,
  model_version TEXT NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_job_embeddings_vector
  ON job_embeddings USING ivfflat (vector vector_cosine_ops) WITH (lists = 200);

CREATE TABLE company_embeddings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  company_id UUID NOT NULL UNIQUE REFERENCES companies(id) ON DELETE CASCADE,
  vector vector(1536) NOT NULL,
  model_version TEXT NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### 2.4 Matching & Strategy

```sql
CREATE TABLE job_matches (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
  job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
  overall_match NUMERIC NOT NULL,
  technical_match NUMERIC,
  experience_match NUMERIC,
  project_match NUMERIC,
  education_match NUMERIC,
  preference_match NUMERIC,
  career_value NUMERIC,
  application_difficulty NUMERIC,
  opportunity_score NUMERIC,
  model_version TEXT NOT NULL,
  computed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_job_matches_candidate_job ON job_matches(candidate_profile_id, job_id);
CREATE INDEX ix_job_matches_opportunity ON job_matches(candidate_profile_id, opportunity_score DESC);

CREATE TABLE match_reasons (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  job_match_id UUID NOT NULL REFERENCES job_matches(id) ON DELETE CASCADE,
  category TEXT NOT NULL CHECK (category IN ('STRONG','MODERATE','MISSING')),
  skill_or_factor TEXT NOT NULL,
  explanation TEXT NOT NULL
);

CREATE TABLE career_strategies (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
  primary_targets TEXT[] DEFAULT '{}',
  secondary_targets TEXT[] DEFAULT '{}',
  stretch_targets TEXT[] DEFAULT '{}',
  allocation_plan JSONB,
  rationale TEXT,
  generated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  superseded_by UUID REFERENCES career_strategies(id)
);

CREATE TABLE career_memories (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
  category TEXT NOT NULL,
  statement TEXT NOT NULL,
  evidence_refs JSONB DEFAULT '[]',
  confidence NUMERIC,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### 2.5 Applications

```sql
CREATE TABLE applications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
  job_id UUID NOT NULL REFERENCES jobs(id),
  state TEXT NOT NULL DEFAULT 'DISCOVERED' CHECK (state IN (
    'DISCOVERED','ANALYZING','MATCHED','SELECTED','PREPARING','READY',
    'WAITING_FOR_APPROVAL','APPLYING','WAITING_FOR_USER','SUBMITTED',
    'TRACKING','INTERVIEW','OFFER','REJECTED','WITHDRAWN','FAILED')),
  mode TEXT CHECK (mode IN ('AUTO','ASSISTED','MANUAL')),
  resume_version_id UUID REFERENCES resume_versions(id),
  cover_letter_document_id UUID,
  decided_by TEXT CHECK (decided_by IN ('SYSTEM','USER')),
  approval_required_reason TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- duplicate-application prevention (domain invariant #3, doc 03):
CREATE UNIQUE INDEX ux_applications_active_per_job
  ON applications (candidate_profile_id, job_id)
  WHERE state NOT IN ('WITHDRAWN','FAILED');
CREATE INDEX ix_applications_state ON applications(state);

CREATE TABLE application_plans (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  application_id UUID NOT NULL UNIQUE REFERENCES applications(id) ON DELETE CASCADE,
  should_apply BOOLEAN NOT NULL,
  rationale TEXT,
  emphasized_projects TEXT[] DEFAULT '{}',
  emphasized_experience TEXT[] DEFAULT '{}',
  cover_letter_strategy TEXT,
  questions_requiring_care TEXT[] DEFAULT '{}',
  automation_feasible BOOLEAN,
  approval_required BOOLEAN
);

CREATE TABLE application_documents (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
  type TEXT NOT NULL CHECK (type IN ('RESUME','COVER_LETTER','OTHER')),
  resume_version_id UUID REFERENCES resume_versions(id),
  storage_ref TEXT NOT NULL,
  generated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE application_answers (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
  question TEXT NOT NULL,
  classification TEXT,
  answer_text TEXT,
  evidence_refs JSONB DEFAULT '[]',
  confidence NUMERIC CHECK (confidence BETWEEN 0 AND 1),
  requires_user_review BOOLEAN NOT NULL DEFAULT false,
  reviewed_by_user BOOLEAN NOT NULL DEFAULT false
);
-- invariant #2 (doc 03) enforced at service layer on write, not by a
-- portable CHECK, since CONFIDENCE_THRESHOLD is config-driven.

CREATE TABLE application_sessions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  application_id UUID NOT NULL UNIQUE REFERENCES applications(id) ON DELETE CASCADE,
  status TEXT NOT NULL CHECK (status IN ('ACTIVE','PAUSED','COMPLETED','ABANDONED')),
  current_url TEXT,
  browser_session_ref TEXT,
  form_state JSONB,
  completed_fields JSONB DEFAULT '[]',
  pending_fields JSONB DEFAULT '[]',
  last_error_ref TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE application_actions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  application_session_id UUID NOT NULL REFERENCES application_sessions(id) ON DELETE CASCADE,
  action_type TEXT NOT NULL,
  target_selector TEXT,
  proposed_by TEXT NOT NULL DEFAULT 'AI',
  validated_by TEXT NOT NULL DEFAULT 'POLICY_ENGINE',
  result TEXT CHECK (result IN ('SUCCESS','FAILURE')),
  executed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_application_actions_session ON application_actions(application_session_id);

CREATE TABLE application_events (
  id BIGSERIAL PRIMARY KEY,
  application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
  event_type TEXT NOT NULL,
  payload JSONB,
  occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_application_events_app ON application_events(application_id, occurred_at);

CREATE TABLE application_attempts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
  attempt_number INT NOT NULL,
  outcome TEXT CHECK (outcome IN ('SUCCESS','FAILED','MANUAL_HANDOFF')),
  failure_reason TEXT,
  started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  ended_at TIMESTAMPTZ
);

CREATE TABLE application_outcomes (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  application_id UUID NOT NULL UNIQUE REFERENCES applications(id) ON DELETE CASCADE,
  outcome_type TEXT NOT NULL CHECK (outcome_type IN
    ('INTERVIEW','OFFER','REJECTED','WITHDRAWN','NO_RESPONSE')),
  occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  notes TEXT,
  source TEXT CHECK (source IN ('USER_REPORTED','EMAIL_INTEGRATION'))
);

CREATE TABLE workflows (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  application_id UUID NOT NULL UNIQUE REFERENCES applications(id) ON DELETE CASCADE,
  current_step_id UUID,
  status TEXT
);

CREATE TABLE workflow_steps (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  workflow_id UUID NOT NULL REFERENCES workflows(id) ON DELETE CASCADE,
  step_type TEXT NOT NULL,
  status TEXT NOT NULL CHECK (status IN ('PENDING','IN_PROGRESS','DONE','SKIPPED','FAILED')),
  resume_token TEXT,
  started_at TIMESTAMPTZ,
  completed_at TIMESTAMPTZ
);
```

### 2.6 Cross-Cutting

```sql
CREATE TABLE notifications (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
  type TEXT NOT NULL,
  payload JSONB,
  channel TEXT NOT NULL CHECK (channel IN ('IN_APP','EMAIL','DESKTOP')),
  read_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_notifications_unread ON notifications(candidate_profile_id) WHERE read_at IS NULL;

CREATE TABLE analytics_snapshots (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
  period_start TIMESTAMPTZ NOT NULL,
  period_end TIMESTAMPTZ NOT NULL,
  metrics JSONB NOT NULL,
  computed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_events (
  id BIGSERIAL PRIMARY KEY,
  actor_type TEXT NOT NULL CHECK (actor_type IN ('USER','SYSTEM','AI_AGENT')),
  actor_id TEXT,
  event_type TEXT NOT NULL,
  entity_type TEXT NOT NULL,
  entity_id TEXT NOT NULL,
  payload JSONB,
  occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_entity ON audit_events(entity_type, entity_id);
-- append-only: no UPDATE/DELETE grants for the application DB role.
```

## 3. Migrations

Managed with Flyway, one directory per module namespace
(`V{n}__{module}_{description}.sql`), applied in the module dependency
order from doc 02 §3 so foreign keys never point forward. Every migration
is additive-first (new column nullable → backfill → constrain) to support
zero-downtime deploys (doc 33).

## 4. Vector Index Strategy

`ivfflat` with cosine distance for candidate/job/company embeddings at
current scale; `lists` tuned per table by row-count (documented in doc 31).
Re-index job is scheduled after any bulk re-embedding (model version
upgrade) — tracked via `model_version` columns so stale vs. fresh vectors
are distinguishable during a migration window, never silently mixed in a
similarity query.

## 5. Retention & Deletion Hooks

Tables holding personal data (`resumes`, `resume_versions`,
`application_documents`, `application_answers`) are enumerated explicitly in
doc 24 with retention periods and cascade-delete behavior on account
deletion; `ON DELETE CASCADE` from `candidate_profiles` is the default but
`audit_events` deliberately does **not** cascade — audit history survives
account deletion in anonymized form per doc 24.
