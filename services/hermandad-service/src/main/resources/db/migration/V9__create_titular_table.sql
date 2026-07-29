CREATE TABLE titular (
    id          UUID                     NOT NULL PRIMARY KEY,
    hermandad_id UUID                    NOT NULL REFERENCES hermandad(id),
    name        VARCHAR(255)             NOT NULL,
    description TEXT,
    version     INTEGER                  NOT NULL DEFAULT 0,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_titular_hermandad_id ON titular(hermandad_id);
