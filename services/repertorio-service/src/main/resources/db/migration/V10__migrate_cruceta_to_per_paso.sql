-- Reset: old per-procesion cruceta data is incompatible with per-Paso model.
-- This is a forward-only reset migration — no rollback.
-- Drop tables first (item then parent) and recreate with the new schema.
DROP TABLE IF EXISTS cruceta_item;
DROP TABLE IF EXISTS cruceta;

CREATE TABLE cruceta (
    id UUID PRIMARY KEY,
    paso_id UUID NOT NULL,
    version INTEGER DEFAULT 0 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE UNIQUE INDEX idx_cruceta_paso_id ON cruceta(paso_id);

CREATE TABLE cruceta_item (
    id UUID PRIMARY KEY,
    cruceta_id UUID NOT NULL REFERENCES cruceta(id) ON DELETE CASCADE,
    marcha_id UUID NOT NULL,
    route_section_id UUID NOT NULL,
    sequence_within_section INT NOT NULL,
    notes VARCHAR(1000),
    version INTEGER DEFAULT 0 NOT NULL
);
CREATE INDEX idx_cruceta_item_cruceta_id ON cruceta_item(cruceta_id);
CREATE INDEX idx_cruceta_item_marcha_id ON cruceta_item(marcha_id);
CREATE INDEX idx_cruceta_item_route_section_id ON cruceta_item(route_section_id);
