package com.repertorio.hermandad.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TitularJpaRepository extends JpaRepository<TitularEntity, UUID> {
    List<TitularEntity> findByHermandadId(UUID hermandadId);
}
