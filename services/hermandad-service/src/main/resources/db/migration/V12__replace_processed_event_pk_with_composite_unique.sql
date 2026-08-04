ALTER TABLE processed_event DROP CONSTRAINT IF EXISTS processed_event_pkey;
ALTER TABLE processed_event ADD CONSTRAINT uq_processed_event_consumer_event UNIQUE (consumer_name, event_id);
