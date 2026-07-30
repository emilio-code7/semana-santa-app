CREATE TABLE paso (
    id            UUID                     NOT NULL PRIMARY KEY,
    version       BIGINT                   NOT NULL DEFAULT 0,
    procesion_id  UUID                     NOT NULL,
    position      INTEGER                  NOT NULL,
    titular_id    UUID                     NOT NULL,
    notes         TEXT,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_paso_procesion_position UNIQUE (procesion_id, position),
    CONSTRAINT fk_paso_procesion FOREIGN KEY (procesion_id) REFERENCES procesion(id),
    CONSTRAINT fk_paso_titular FOREIGN KEY (titular_id) REFERENCES known_titular(id)
);

CREATE INDEX idx_paso_procesion_id ON paso(procesion_id);
CREATE INDEX idx_paso_titular_id ON paso(titular_id);
