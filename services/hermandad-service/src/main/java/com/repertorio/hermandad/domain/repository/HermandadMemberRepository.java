package com.repertorio.hermandad.domain.repository;

import com.repertorio.hermandad.domain.model.HermandadMember;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HermandadMemberRepository {
    HermandadMember save(HermandadMember member);
    List<HermandadMember> findByHermandadId(UUID hermandadId);
    Optional<HermandadMember> findByUserIdAndHermandadId(String userId, UUID hermandadId);
}
