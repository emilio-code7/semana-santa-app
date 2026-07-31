package com.repertorio.marcha.adapter.outbound.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CrucetaProgressionJpaRepository extends JpaRepository<CrucetaProgressionEntity, UUID> {

    Optional<CrucetaProgressionEntity> findByCrucetaIdAndPasoId(UUID crucetaId, UUID pasoId);

    void deleteByCrucetaId(UUID crucetaId);
}
