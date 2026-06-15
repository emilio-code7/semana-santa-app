package com.repertorio.hermandad.domain.repository;

import com.repertorio.hermandad.domain.model.Hermandad;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HermandadRepository extends JpaRepository<Hermandad, UUID> {
}
