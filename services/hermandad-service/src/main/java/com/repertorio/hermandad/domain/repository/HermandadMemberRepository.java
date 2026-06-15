package com.repertorio.hermandad.domain.repository;

import com.repertorio.hermandad.domain.model.HermandadMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface HermandadMemberRepository extends JpaRepository<HermandadMember, UUID> {
    List<HermandadMember> findByHermandadId(UUID hermandadId);
}
