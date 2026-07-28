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
        return jpaRepository.findByHermandadId(hermandadId, pageable).map(HermandadMemberEntity::toDomain);
    }

    @Override
    public Optional<HermandadMember> findByUserIdAndHermandadId(String userId, UUID hermandadId) {
        return jpaRepository.findByUserIdAndHermandadId(userId, hermandadId).map(HermandadMemberEntity::toDomain);
    }

    @Override
    public HermandadMember save(HermandadMember member) {
        if (member.getId() == null) {
            // New entity: construct fresh
            var entity = HermandadMemberEntity.from(member);
            var saved = jpaRepository.save(entity);
            jpaRepository.flush();
            return saved.toDomain();
        }
        // Existing entity: load managed instance, copy mutable fields, preserve version
        var managed = jpaRepository.findById(member.getId())
                .orElseThrow(() -> new IllegalArgumentException("HermandadMember not found: " + member.getId()));
        managed.setRole(member.getRole());
        jpaRepository.flush();
        return managed.toDomain();
    }

    @Override
    public void delete(HermandadMember member) {
        if (member.getId() == null) {
            throw new IllegalArgumentException("Cannot delete a HermandadMember without an id");
        }
        var managed = jpaRepository.findById(member.getId())
                .orElseThrow(() -> new IllegalArgumentException("HermandadMember not found: " + member.getId()));
        jpaRepository.delete(managed);
    }
}
