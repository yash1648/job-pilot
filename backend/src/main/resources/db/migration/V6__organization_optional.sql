-- Real-model extraction (doc 07 §2): organization is optional for projects,
-- certifications and achievements — a project need not name an employer and a
-- certification's issuer is often absent from the extracted item. Relax the
-- NOT NULL so genuine model output (which omits organization for these types)
-- persists instead of failing the whole resume parse.
ALTER TABLE experiences ALTER COLUMN organization DROP NOT NULL;
ALTER TABLE educations ALTER COLUMN organization DROP NOT NULL;
ALTER TABLE projects ALTER COLUMN organization DROP NOT NULL;
ALTER TABLE certifications ALTER COLUMN organization DROP NOT NULL;
ALTER TABLE achievements ALTER COLUMN organization DROP NOT NULL;
