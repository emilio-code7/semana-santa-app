package com.repertorio.marcha.application.service;

import com.repertorio.marcha.application.port.DomainEventPublisher;
import com.repertorio.marcha.domain.event.CrucetaDefinedEvent;
import com.repertorio.marcha.domain.model.*;
import com.repertorio.marcha.domain.port.CrucetaRepository;
import com.repertorio.marcha.domain.port.KnownProcesionRepository;
import com.repertorio.marcha.domain.port.MarchaRepository;
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
    private final MarchaRepository marchaRepository;

    @Transactional
    public Cruceta defineCruceta(UUID pasoId, List<CrucetaItem> items) {
        if (!knownProcesionRepository.existsPasoById(pasoId)) {
            throw new PasoNotFoundException(pasoId);
        }
        for (var item : items) {
            if (!marchaRepository.existsById(item.getMarchaId())) {
                throw new MarchaNotFoundException(item.getMarchaId());
            }
            if (!knownProcesionRepository.existsRouteSectionById(item.getRouteSectionId())) {
                throw new IllegalArgumentException("Route section not found: " + item.getRouteSectionId());
            }
        }
        // Load existing aggregate and redefine in place — preserves aggregate ID and revision
        var cruceta = crucetaRepository.findByPasoId(pasoId)
                .map(existing -> {
                    existing.redefine(items);
                    return crucetaRepository.save(existing);
                })
                .orElseGet(() -> {
                    var newCruceta = new Cruceta(pasoId, items);
                    return crucetaRepository.save(newCruceta);
                });
        eventPublisher.publish(new CrucetaDefinedEvent(cruceta.getId(), pasoId, items.size()));
        return cruceta;
    }

    @Transactional(readOnly = true)
    public Cruceta getCruceta(UUID pasoId) {
        return crucetaRepository.findByPasoId(pasoId)
                .orElseThrow(() -> new CrucetaNotFoundException(pasoId));
    }
}
