-- doc 07 §9: user-facing reason when parsing fails (never silently empty)
ALTER TABLE resumes ADD COLUMN parse_failure_reason TEXT;
