-- TASK-JP-0002: candidate evidence tables (doc 04 §2.2)
-- jobs(id) does not exist yet (Wave 3) — resume_versions.job_id FK is added
-- by the jobs migration per module-dependency order (doc 04 §3).

CREATE EXTENSION IF NOT EXISTS vector;

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
-- exactly one is_master=true per candidate (doc 04:94-95)
CREATE UNIQUE INDEX ux_resumes_one_master
  ON resumes (candidate_profile_id) WHERE is_master;

CREATE TABLE resume_versions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  resume_id UUID REFERENCES resumes(id) ON DELETE SET NULL,
  candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
  job_id UUID,  -- FK to jobs added in jobs migration (module dependency order)
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

CREATE TABLE educations (
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

CREATE TABLE projects (
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

CREATE TABLE certifications (
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

CREATE TABLE achievements (
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

CREATE TABLE candidate_embeddings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  candidate_profile_id UUID NOT NULL UNIQUE REFERENCES candidate_profiles(id) ON DELETE CASCADE,
  vector vector(1536) NOT NULL,
  model_version TEXT NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_candidate_embeddings_vector
  ON candidate_embeddings USING ivfflat (vector vector_cosine_ops) WITH (lists = 100);
