package com.repertorio.hermandad.adapter.outbound.persistence;

import com.repertorio.hermandad.domain.model.HermandadMember;
import com.repertorio.hermandad.domain.repository.HermandadMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HermandadMemberRepositoryAdapter implements HermandadMemberRepository {

    private final HermandadMemberJpaRepository jpaRepository;

    @Override
    public Page<HermandadMember> findByHermandadId(UUID hermandadId, Pageable pageable) {
        return jpaRepository.findByHermandadId(hermandadId, pageable);
    }

    @Override
    public Optional<HermandadMember> findByUserIdAndHermandadId(String userId, UUID hermandadId) {
        return jpaRepository.findByUserIdAndHermandadId(userId, hermandadId);
    }

    @Override
    public HermandadMember save(HermandadMember member) {
        return jpaRepository.save(member);
    }

    @Override
    public void delete(HermandadMember member) {
        jpaRepository.delete(member);
    }
}
