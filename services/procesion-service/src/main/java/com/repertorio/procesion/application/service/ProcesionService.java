package com.repertorio.procesion.application.service;

import com.repertorio.procesion.application.port.DomainEventPublisher;
import com.repertorio.procesion.domain.event.ProcesionCreatedEvent;
import com.repertorio.procesion.domain.event.ProcesionStatusChangedEvent;
import com.repertorio.procesion.domain.model.Procesion;
import com.repertorio.procesion.domain.model.ProcesionNotFoundException;
import com.repertorio.procesion.domain.model.ProcesionStatus;
import com.repertorio.procesion.domain.repository.ProcesionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProcesionService {

    private final ProcesionRepository procesionRepository;
    private final DomainEventPublisher eventPublisher;

    @Transactional
    public Procesion createProcesion(UUID hermandadId, LocalDate date, LocalTime time) {
        var procesion = Procesion.create(hermandadId, date, time);
        procesion = procesionRepository.save(procesion);
        eventPublisher.publish(new ProcesionCreatedEvent(procesion.getId(), hermandadId, date, time));
        return procesion;
    }

    @Transactional(readOnly = true)
    public Procesion getProcesion(UUID id) {
        return procesionRepository.findById(id)
                .orElseThrow(() -> new ProcesionNotFoundException(id));
    }

    @Transactional
    public Procesion changeStatus(UUID id, ProcesionStatus newStatus) {
        var procesion = getProcesion(id);
        var previousStatus = procesion.getStatus();
        procesion.changeStatus(newStatus);
        procesion = procesionRepository.save(procesion);
        eventPublisher.publish(new ProcesionStatusChangedEvent(id, procesion.getHermandadId(), previousStatus, newStatus));
        return procesion;
    }

    @Transactional(readOnly = true)
    public Page<Procesion> listByHermandad(UUID hermandadId, Pageable pageable) {
        return procesionRepository.findByHermandadId(hermandadId, pageable);
    }

    @Transactional
    public void deleteProcesion(UUID id) {
        var procesion = procesionRepository.findById(id)
                .orElseThrow(() -> new ProcesionNotFoundException(id));
        procesionRepository.delete(procesion);
    }
}
