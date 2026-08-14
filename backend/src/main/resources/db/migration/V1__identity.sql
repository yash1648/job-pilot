-- TASK-JP-0002: identity & preferences (doc 04 §2.1)
-- Applied first — no forward FKs. Data minimization per doc 24 §2.

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS citext;

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
