CREATE TABLE known_titular (
    id           UUID                     NOT NULL PRIMARY KEY,
    hermandad_id UUID                     NOT NULL,
    name         VARCHAR(255)             NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_known_titular_hermandad_id ON known_titular(hermandad_id);
