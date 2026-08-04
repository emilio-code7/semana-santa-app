ALTER TABLE outbox_event ADD COLUMN claimed_by VARCHAR(100);
ALTER TABLE outbox_event ADD COLUMN claimed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE outbox_event ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE outbox_event ADD COLUMN next_attempt_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE outbox_event ADD COLUMN last_error TEXT;
ALTER TABLE outbox_event ADD COLUMN terminal BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_outbox_eligible ON outbox_event (processed, terminal, created_at);
CREATE INDEX idx_outbox_aggregate_order ON outbox_event (aggregate_id, processed, created_at);
