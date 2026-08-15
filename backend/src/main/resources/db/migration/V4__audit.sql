-- TASK-JP-0006: audit trail (doc 04 §2.6, doc 22 §10, doc 29 §1)
-- Append-only audit log. The application DB role must never be able to
-- UPDATE/DELETE rows (doc 22 §10). Enforced at the DB level by a trigger so
-- it holds regardless of which role connects (incl. the schema owner used in
-- dev/test). Production should additionally connect via a restricted role
-- that only holds INSERT/SELECT on this table.

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

-- Append-only enforcement: reject any UPDATE/DELETE on audit_events.
CREATE OR REPLACE FUNCTION audit_events_append_only()
RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'audit_events is append-only: UPDATE/DELETE are forbidden'
    USING ERRCODE = '42501';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_events_append_only
  BEFORE UPDATE OR DELETE ON audit_events
  FOR EACH STATEMENT
  EXECUTE FUNCTION audit_events_append_only();
