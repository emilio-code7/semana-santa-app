package com.repertorio.procesion.domain.port;

import com.repertorio.procesion.domain.model.KnownTitular;

import java.util.Optional;
import java.util.UUID;

public interface KnownTitularRepository {
    KnownTitular save(KnownTitular knownTitular);
    Optional<KnownTitular> findById(UUID id);
    boolean existsById(UUID id);
}
