CREATE TABLE processed_event (
    event_id UUID PRIMARY KEY,
    consumer_name VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);
