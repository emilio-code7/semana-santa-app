CREATE TABLE route_section (
    id            UUID                     NOT NULL PRIMARY KEY,
    version       BIGINT                   NOT NULL DEFAULT 0,
    procesion_id  UUID                     NOT NULL,
    name          VARCHAR(255)             NOT NULL,
    position      INTEGER                  NOT NULL,
    notes         TEXT,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_route_section_procesion FOREIGN KEY (procesion_id) REFERENCES procesion(id)
);

CREATE INDEX idx_route_section_procesion_id ON route_section(procesion_id);

ALTER TABLE procesion ADD COLUMN plan_finalized_at TIMESTAMP WITH TIME ZONE;
CREATE INDEX idx_procesion_plan_finalized ON procesion(plan_finalized_at);
