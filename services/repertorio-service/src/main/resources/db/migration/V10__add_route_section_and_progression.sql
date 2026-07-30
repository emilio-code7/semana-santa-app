-- Add route_section_id to cruceta_item for per-section marcha grouping
ALTER TABLE cruceta_item ADD COLUMN route_section_id UUID;

-- Create cruceta_progression table for per-Paso progression tracking within a Cruceta
CREATE TABLE cruceta_progression (
    id UUID PRIMARY KEY,
    cruceta_id UUID NOT NULL REFERENCES cruceta(id) ON DELETE CASCADE,
    paso_id UUID NOT NULL,
    current_route_section_id UUID NOT NULL,
    current_cruceta_item_id UUID,
    UNIQUE(cruceta_id, paso_id)
);

CREATE INDEX idx_cruceta_progression_cruceta_id ON cruceta_progression(cruceta_id);
CREATE INDEX idx_cruceta_progression_paso_id ON cruceta_progression(paso_id);
