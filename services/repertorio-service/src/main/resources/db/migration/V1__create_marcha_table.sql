CREATE TABLE marcha (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    composer VARCHAR(255) NOT NULL,
    band_type VARCHAR(30) NOT NULL,
    duration_seconds INTEGER NOT NULL,
    composition_year INTEGER,
    youtube_url VARCHAR(512),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_marcha_band_type ON marcha(band_type);
CREATE INDEX idx_marcha_composer ON marcha(composer);
