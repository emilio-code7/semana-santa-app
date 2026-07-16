package com.repertorio.hermandad.adapter.outbound.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HermandadMemberJpaRepository extends JpaRepository<HermandadMemberEntity, UUID> {
    Page<HermandadMemberEntity> findByHermandadId(UUID id, Pageable pageable);
    Optional<HermandadMemberEntity> findByUserIdAndHermandadId(String userId, UUID hermandadId);
}
