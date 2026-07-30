ALTER TABLE known_procesion ADD COLUMN IF NOT EXISTS date DATE;
ALTER TABLE known_procesion ADD COLUMN IF NOT EXISTS time TIME;
ALTER TABLE known_procesion ADD COLUMN IF NOT EXISTS plan_finalized_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE known_paso (
    id UUID PRIMARY KEY,
    procesion_id UUID NOT NULL REFERENCES known_procesion(procesion_id),
    position INT NOT NULL,
    titular_id UUID NOT NULL
);
CREATE INDEX idx_known_paso_procesion_id ON known_paso(procesion_id);

CREATE TABLE known_route_section (
    id UUID PRIMARY KEY,
    procesion_id UUID NOT NULL REFERENCES known_procesion(procesion_id),
    name VARCHAR(255) NOT NULL,
    position INT NOT NULL,
    notes VARCHAR(1000)
);
CREATE INDEX idx_known_route_section_procesion_id ON known_route_section(procesion_id);
