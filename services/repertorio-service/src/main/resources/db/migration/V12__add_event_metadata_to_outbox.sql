ALTER TABLE outbox_event ADD COLUMN event_id UUID;
ALTER TABLE outbox_event ADD COLUMN occurred_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE outbox_event ADD COLUMN schema_version INTEGER;

-- Backfill pre-existing rows: event_id from the serialized event JSON in payload;
-- occurred_at approximated by created_at (row persisted at event construction);
-- schema_version is 1 for all current events. No unsafe default IDs/timestamps.
-- ponytail: positional extraction instead of JSON_VALUE/->> - no JSON query function
-- exists in both H2 2.4 (slice tests parse this script) and PostgreSQL 16 (prod);
-- payloads are Jackson-serialized '"eventId":"<uuid>"' so the 36-char UUID sits at a
-- deterministic offset. One-shot migration; upgrade path: vendor-specific Flyway
-- location (classpath:db/migration/{vendor}) if payload formatting ever changes.
UPDATE outbox_event
   SET event_id = CAST(SUBSTRING(payload FROM (POSITION('"eventId"' IN payload) + 11) FOR 36) AS UUID),
       occurred_at = created_at,
       schema_version = 1
 WHERE event_id IS NULL;

ALTER TABLE outbox_event ALTER COLUMN event_id SET NOT NULL;
ALTER TABLE outbox_event ALTER COLUMN occurred_at SET NOT NULL;
ALTER TABLE outbox_event ALTER COLUMN schema_version SET NOT NULL;
