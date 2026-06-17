package com.repertorio.hermandad.adapter.outbound.persistence;

import com.repertorio.hermandad.domain.model.HermandadMember;
import com.repertorio.hermandad.domain.repository.HermandadMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HermandadMemberRepositoryAdapter implements HermandadMemberRepository {

    private final HermandadMemberJpaRepository jpaRepository;

    @Override
    public List<HermandadMember> findByHermandadId(UUID hermandadId) {
        return jpaRepository.findByHermandadId(hermandadId);
    }

    @Override
    public Optional<HermandadMember> findByUserIdAndHermandadId(String userId, UUID hermandadId) {
        return jpaRepository.findByUserIdAndHermandadId(userId, hermandadId);
    }

    @Override
    public HermandadMember save(HermandadMember member) {
        return jpaRepository.save(member);
    }
}
