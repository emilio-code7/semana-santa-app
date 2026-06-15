package com.repertorio.hermandad.domain.repository;

import com.repertorio.hermandad.domain.model.HermandadMember;

import java.util.List;
import java.util.UUID;

public interface HermandadMemberRepository {
    List<HermandadMember> findByHermandadId(UUID hermandadId);
}
