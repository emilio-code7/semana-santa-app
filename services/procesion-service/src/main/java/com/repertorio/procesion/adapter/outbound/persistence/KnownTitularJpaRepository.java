package com.repertorio.procesion.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface KnownTitularJpaRepository extends JpaRepository<KnownTitularEntity, UUID> {
}
