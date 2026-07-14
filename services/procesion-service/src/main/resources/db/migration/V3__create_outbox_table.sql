CREATE TABLE outbox_event
(
    id             UUID                     NOT NULL PRIMARY KEY,
    aggregate_type VARCHAR(50)              NOT NULL,
    aggregate_id   UUID                     NOT NULL,
    event_type     VARCHAR(50)              NOT NULL,
    payload        TEXT                     NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at   TIMESTAMP WITH TIME ZONE,
    processed      BOOLEAN                  NOT NULL DEFAULT FALSE
);
