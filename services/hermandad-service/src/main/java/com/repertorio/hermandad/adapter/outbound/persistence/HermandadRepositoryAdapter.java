package com.repertorio.hermandad.adapter.outbound.persistence;

import com.repertorio.hermandad.domain.model.Hermandad;
import com.repertorio.hermandad.domain.repository.HermandadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HermandadRepositoryAdapter implements HermandadRepository {
    private final HermandadJpaRepository jpaRepository;


    @Override
    public Hermandad save(Hermandad hermandad) {
        return jpaRepository.save(hermandad);
    }

    @Override
    public Optional<Hermandad> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public boolean existsByName(String name) {
        return jpaRepository.existsByName(name);
    }

    @Override
    public List<Hermandad> findAll() {
        return jpaRepository.findAll();
    }
}
