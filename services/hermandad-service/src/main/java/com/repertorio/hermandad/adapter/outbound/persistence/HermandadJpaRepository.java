package com.repertorio.hermandad.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface HermandadJpaRepository extends JpaRepository<HermandadEntity, UUID> {
    boolean existsByName(String name);
}
