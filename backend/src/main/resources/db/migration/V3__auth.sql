-- TASK-JP-0003: refresh token sessions (doc 22 §1)
-- Refresh tokens stored HASHED (SHA-256), revocable per-session, rotation
-- chain via replaced_by_token_hash (supports "log out everywhere").

CREATE TABLE refresh_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash TEXT NOT NULL UNIQUE,
  expires_at TIMESTAMPTZ NOT NULL,
  revoked_at TIMESTAMPTZ,
  replaced_by_token_hash TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens(user_id);
-- "log out everywhere": revoke all rows for a user
CREATE INDEX ix_refresh_tokens_user_revoked ON refresh_tokens(user_id) WHERE revoked_at IS NULL;
