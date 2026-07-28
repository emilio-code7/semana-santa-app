package com.repertorio.marcha.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Marcha {

    private final UUID id;
    private int version;
    private String title;
    private String composer;
    private BandType bandType;
    private int durationSeconds;
    private Integer compositionYear;
    private String youtubeUrl;
    private Instant createdAt;
    private Instant updatedAt;

    // JPA reconstruction
    protected Marcha() {
        this.id = null;
    }

    public Marcha(String title, String composer, BandType bandType, int durationSeconds,
                  Integer compositionYear, String youtubeUrl) {
        this.id = UUID.randomUUID();
        this.version = 0;
        this.title = requireNonBlank(title, "title");
        this.composer = requireNonBlank(composer, "composer");
        this.bandType = requireNonNull(bandType, "bandType");
        this.durationSeconds = requirePositive(durationSeconds, "durationSeconds");
        this.compositionYear = compositionYear;
        this.youtubeUrl = youtubeUrl;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public static Marcha create(String title, String composer, BandType bandType,
                                 int durationSeconds, Integer compositionYear, String youtubeUrl) {
        return new Marcha(title, composer, bandType, durationSeconds, compositionYear, youtubeUrl);
    }

    public void update(String title, String composer, BandType bandType,
                       int durationSeconds, Integer compositionYear, String youtubeUrl) {
        this.title = requireNonBlank(title, "title");
        this.composer = requireNonBlank(composer, "composer");
        this.bandType = requireNonNull(bandType, "bandType");
        this.durationSeconds = requirePositive(durationSeconds, "durationSeconds");
        this.compositionYear = compositionYear;
        this.youtubeUrl = youtubeUrl;
        this.updatedAt = Instant.now();
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static <T> T requireNonNull(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }

    private static int requirePositive(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    // ponytail: reconstruct for adapter mapping — private all-args, only reachable via static factory
    private Marcha(UUID id, int version, String title, String composer, BandType bandType,
                   int durationSeconds, Integer compositionYear, String youtubeUrl,
                   Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.version = version;
        this.title = title;
        this.composer = composer;
        this.bandType = bandType;
        this.durationSeconds = durationSeconds;
        this.compositionYear = compositionYear;
        this.youtubeUrl = youtubeUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Marcha reconstruct(UUID id, int version, String title, String composer, BandType bandType,
                                      int durationSeconds, Integer compositionYear, String youtubeUrl,
                                      Instant createdAt, Instant updatedAt) {
        return new Marcha(id, version, title, composer, bandType, durationSeconds, compositionYear,
                youtubeUrl, createdAt, updatedAt);
    }

    // Getters
    public UUID getId() { return id; }
    public int getVersion() { return version; }
    public String getTitle() { return title; }
    public String getComposer() { return composer; }
    public BandType getBandType() { return bandType; }
    public int getDurationSeconds() { return durationSeconds; }
    public Integer getCompositionYear() { return compositionYear; }
    public String getYoutubeUrl() { return youtubeUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
