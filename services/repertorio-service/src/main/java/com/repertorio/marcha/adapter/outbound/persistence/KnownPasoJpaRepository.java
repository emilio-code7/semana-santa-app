package com.repertorio.marcha.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KnownPasoJpaRepository extends JpaRepository<KnownPasoEntity, UUID> {

    List<KnownPasoEntity> findByProcesionId(UUID procesionId);

    void deleteByProcesionId(UUID procesionId);
}
