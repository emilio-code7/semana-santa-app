package com.repertorio.hermandad.domain.repository;

import com.repertorio.hermandad.domain.model.HermandadMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface HermandadMemberRepository {
    HermandadMember save(HermandadMember member);
    Page<HermandadMember> findByHermandadId(UUID hermandadId, Pageable pageable);
    Optional<HermandadMember> findByUserIdAndHermandadId(String userId, UUID hermandadId);
    void delete(HermandadMember member);
}
