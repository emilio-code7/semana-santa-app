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
        var existing = crucetaRepository.findByProcesionId(procesionId);
        existing.ifPresent(c -> crucetaRepository.deleteByProcesionId(procesionId));

        var cruceta = new Cruceta(procesionId, items);
        cruceta = crucetaRepository.save(cruceta);
        eventPublisher.publish(new CrucetaDefinedEvent(cruceta.getId(), procesionId, items.size()));
        return cruceta;
    }

    @Transactional(readOnly = true)
    public Cruceta getCruceta(UUID procesionId) {
        return crucetaRepository.findByProcesionId(procesionId)
                .orElseThrow(() -> new CrucetaNotFoundException(procesionId));
    }
}
