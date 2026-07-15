CREATE TABLE known_procesion (
    procesion_id UUID PRIMARY KEY,
    hermandad_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_known_procesion_hermandad_id ON known_procesion(hermandad_id);
