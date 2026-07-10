package com.repertorio.procesion.adapter.outbound.persistence;

import com.repertorio.procesion.domain.model.Procesion;
import com.repertorio.procesion.domain.repository.ProcesionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProcesionRepositoryAdapter implements ProcesionRepository {

    private final ProcesionJpaRepository jpaRepository;

    @Override
    public Procesion save(Procesion procesion) {
        return jpaRepository.save(procesion);
    }

    @Override
    public Optional<Procesion> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Page<Procesion> findByHermandadId(UUID hermandadId, Pageable pageable) {
        return jpaRepository.findByHermandadId(hermandadId, pageable);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
