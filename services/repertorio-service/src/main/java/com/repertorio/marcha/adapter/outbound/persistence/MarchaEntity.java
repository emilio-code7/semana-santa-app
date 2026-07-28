package com.repertorio.marcha.adapter.outbound.persistence;

import com.repertorio.marcha.domain.model.BandType;
import jakarta.persistence.*;
import org.springframework.data.domain.Persistable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "marcha")
public class MarchaEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Transient
    private boolean isNew = true;

    @Version
    private int version;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "composer", nullable = false)
    private String composer;

    @Enumerated(EnumType.STRING)
    @Column(name = "band_type", nullable = false, length = 30)
    private BandType bandType;

    @Column(name = "duration_seconds", nullable = false)
    private int durationSeconds;

    @Column(name = "composition_year")
    private Integer compositionYear;

    @Column(name = "youtube_url", length = 512)
    private String youtubeUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected MarchaEntity() {}

    public MarchaEntity(UUID id, String title, String composer, BandType bandType,
                        int durationSeconds, Integer compositionYear, String youtubeUrl,
                        Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.composer = composer;
        this.bandType = bandType;
        this.durationSeconds = durationSeconds;
        this.compositionYear = compositionYear;
        this.youtubeUrl = youtubeUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Override
    public UUID getId() { return id; }

    @Override
    public boolean isNew() { return isNew; }

    @PostLoad
    void markNotNew() { this.isNew = false; }

    public String getTitle() { return title; }
    public String getComposer() { return composer; }
    public BandType getBandType() { return bandType; }
    public int getDurationSeconds() { return durationSeconds; }
    public Integer getCompositionYear() { return compositionYear; }
    public int getVersion() { return version; }
    public String getYoutubeUrl() { return youtubeUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // ponytail: package-private setters for adapter use only
    void setTitle(String title) { this.title = title; }
    void setComposer(String composer) { this.composer = composer; }
    void setBandType(BandType bandType) { this.bandType = bandType; }
    void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    void setCompositionYear(Integer compositionYear) { this.compositionYear = compositionYear; }
    void setYoutubeUrl(String youtubeUrl) { this.youtubeUrl = youtubeUrl; }
    void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
