ALTER TABLE institutional_inbox ADD COLUMN ack_attempts INT NOT NULL DEFAULT 0;
ALTER TABLE institutional_inbox ADD COLUMN ack_next_ms BIGINT NOT NULL DEFAULT 0;
ALTER TABLE institutional_inbox ADD COLUMN ack_blocked BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE institutional_inbox ADD COLUMN ack_last_error VARCHAR(40);
CREATE INDEX ix_ack_retry ON institutional_inbox(ack_pending,ack_blocked,ack_next_ms,created_ms,id);
