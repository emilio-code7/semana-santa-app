package com.repertorio.hermandad.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "hermandad")
@Getter
public class Hermandad {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;
    @Column(nullable = false, unique = true)
    private String name;
    @Column(nullable = false)
    private String city;
    @Column(nullable = false)
    private int foundedYear;
    @Column
    private String keycloakGroupId;
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Hermandad() {}

    public Hermandad(String name, String city, int foundedYear) {
        this.name = name;
        this.city = city;
        this.foundedYear = foundedYear;
    }

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }

}
