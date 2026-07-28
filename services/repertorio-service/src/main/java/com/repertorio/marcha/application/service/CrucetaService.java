package com.repertorio.marcha.application.service;

import com.repertorio.marcha.application.port.DomainEventPublisher;
import com.repertorio.marcha.domain.event.CrucetaDefinedEvent;
import com.repertorio.marcha.domain.model.Cruceta;
import com.repertorio.marcha.domain.model.CrucetaItem;
import com.repertorio.marcha.domain.model.CrucetaNotFoundException;
import com.repertorio.marcha.domain.model.ProcesionNotFoundException;
import com.repertorio.marcha.domain.port.CrucetaRepository;
import com.repertorio.marcha.domain.port.KnownProcesionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CrucetaService {

    private final CrucetaRepository crucetaRepository;
    private final DomainEventPublisher eventPublisher;
    private final KnownProcesionRepository knownProcesionRepository;

    @Transactional
    public Cruceta defineCruceta(UUID procesionId, List<CrucetaItem> items) {
        if (!knownProcesionRepository.existsByProcesionId(procesionId)) {
            throw new ProcesionNotFoundException(procesionId);
        }
        // Load existing aggregate and redefine in place — preserves aggregate ID and revision
        var cruceta = crucetaRepository.findByProcesionId(procesionId)
                .map(existing -> {
                    existing.redefine(items);
                    return crucetaRepository.save(existing);
                })
                .orElseGet(() -> {
                    var newCruceta = new Cruceta(procesionId, items);
                    return crucetaRepository.save(newCruceta);
                });
        eventPublisher.publish(new CrucetaDefinedEvent(cruceta.getId(), procesionId, items.size()));
        return cruceta;
    }

    @Transactional(readOnly = true)
    public Cruceta getCruceta(UUID procesionId) {
        return crucetaRepository.findByProcesionId(procesionId)
                .orElseThrow(() -> new CrucetaNotFoundException(procesionId));
    }
}
