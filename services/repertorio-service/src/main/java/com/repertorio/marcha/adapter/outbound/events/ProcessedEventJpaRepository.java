package com.repertorio.marcha.adapter.outbound.events;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventEntity, UUID> {
}
