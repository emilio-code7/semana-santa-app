CREATE TABLE cruceta (
    id UUID PRIMARY KEY,
    procesion_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE UNIQUE INDEX idx_cruceta_procesion_id ON cruceta(procesion_id);

CREATE TABLE cruceta_item (
    id UUID PRIMARY KEY,
    cruceta_id UUID NOT NULL REFERENCES cruceta(id) ON DELETE CASCADE,
    marcha_id UUID NOT NULL,
    order_index INTEGER NOT NULL,
    notes VARCHAR(1000),
    UNIQUE(cruceta_id, order_index)
);

CREATE INDEX idx_cruceta_item_cruceta_id ON cruceta_item(cruceta_id);
CREATE INDEX idx_cruceta_item_marcha_id ON cruceta_item(marcha_id);
