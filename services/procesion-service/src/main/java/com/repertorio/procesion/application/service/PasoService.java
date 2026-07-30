package com.repertorio.procesion.application.service;

import com.repertorio.procesion.domain.model.ForbiddenException;
import com.repertorio.procesion.domain.model.Paso;
import com.repertorio.procesion.domain.model.ProcesionNotFoundException;
import com.repertorio.procesion.domain.port.KnownTitularRepository;
import com.repertorio.procesion.domain.repository.PasoRepository;
import com.repertorio.procesion.domain.repository.ProcesionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PasoService {

    private final PasoRepository pasoRepository;
    private final ProcesionRepository procesionRepository;
    private final KnownTitularRepository knownTitularRepository;

    @Transactional
    public List<Paso> replacePasos(UUID hermandadId, UUID procesionId, List<PasoItem> items) {

        // 1. Validate procesion exists and belongs to hermandad
        var procesion = procesionRepository.findById(procesionId)
                .orElseThrow(() -> new ProcesionNotFoundException(procesionId));
        if (!procesion.getHermandadId().equals(hermandadId)) {
            throw new ForbiddenException("Procesion does not belong to this hermandad");
        }
        if (procesion.isPlanFinalized()) {
            throw new IllegalStateException("Plan is already finalized — pasos are immutable");
        }


        // 2. Validate unique positions
        var positions = items.stream().map(PasoItem::position).toList();
        if (new HashSet<>(positions).size() != positions.size()) {
            throw new IllegalArgumentException("Duplicate position in paso list");
        }

        // 3. Validate each titular exists and belongs to the same hermandad
        for (var item : items) {
            var titular = knownTitularRepository.findById(item.titularId())
                    .orElseThrow(() -> new ForbiddenException("Titular not accessible"));
            if (!titular.getHermandadId().equals(hermandadId)) {
                throw new ForbiddenException("Titular does not belong to this hermandad");
            }
        }

        // 4. Delete existing pasos, then recreate
        pasoRepository.deleteByProcesionId(procesionId);

        var result = new ArrayList<Paso>();
        for (var item : items) {
            var id = item.id() != null ? item.id() : UUID.randomUUID();
            // ponytail: timestamps always refresh; stable IDs and order are the idempotency contract
            var paso = Paso.reconstruct(id, procesionId, item.position(), item.titularId(),
                    item.notes(), Instant.now(), Instant.now());
            result.add(pasoRepository.save(paso));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<Paso> getPasos(UUID hermandadId, UUID procesionId) {
        var procesion = procesionRepository.findById(procesionId)
                .orElseThrow(() -> new ProcesionNotFoundException(procesionId));
        if (!procesion.getHermandadId().equals(hermandadId)) {
            throw new ForbiddenException("Procesion does not belong to this hermandad");
        }
        return pasoRepository.findByProcesionId(procesionId);
    }

    // ponytail: inner record instead of a separate DTO import from the service layer
    public record PasoItem(UUID id, int position, UUID titularId, String notes) {}
}
