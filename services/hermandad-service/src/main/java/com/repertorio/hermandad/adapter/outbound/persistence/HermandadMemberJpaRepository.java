package com.repertorio.hermandad.adapter.outbound.persistence;

import com.repertorio.hermandad.domain.model.HermandadMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface HermandadMemberJpaRepository extends JpaRepository<HermandadMember, UUID> {
    Page<HermandadMember> findByHermandadId(UUID id, Pageable pageable);
    Optional<HermandadMember> findByUserIdAndHermandadId(String userId, UUID hermandadId);
}
