CREATE TABLE procesion (
    id UUID PRIMARY KEY,
    hermandad_id UUID NOT NULL,
    fecha DATE NOT NULL,
    hora TIME NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'PLANIFICADA',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_procesion_hermandad_id ON procesion(hermandad_id);
