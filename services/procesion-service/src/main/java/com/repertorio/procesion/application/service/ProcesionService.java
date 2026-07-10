package com.repertorio.procesion.application.service;

import com.repertorio.procesion.application.port.DomainEventPublisher;
import com.repertorio.procesion.domain.event.ProcesionCreatedEvent;
import com.repertorio.procesion.domain.event.ProcesionEstadoChangedEvent;
import com.repertorio.procesion.domain.model.Procesion;
import com.repertorio.procesion.domain.model.ProcesionEstado;
import com.repertorio.procesion.domain.model.ProcesionNotFoundException;
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
    public Procesion crearProcesion(UUID hermandadId, LocalDate fecha, LocalTime hora) {
        var procesion = Procesion.crear(hermandadId, fecha, hora);
        procesion = procesionRepository.save(procesion);
        eventPublisher.publish(new ProcesionCreatedEvent(procesion.getId(), hermandadId, fecha, hora));
        return procesion;
    }

    @Transactional(readOnly = true)
    public Procesion obtenerProcesion(UUID id) {
        return procesionRepository.findById(id)
                .orElseThrow(() -> new ProcesionNotFoundException(id));
    }

    @Transactional
    public Procesion cambiarEstado(UUID id, ProcesionEstado nuevoEstado) {
        var procesion = obtenerProcesion(id);
        var estadoAnterior = procesion.getEstado();
        procesion.cambiarEstado(nuevoEstado);
        procesion = procesionRepository.save(procesion);
        eventPublisher.publish(new ProcesionEstadoChangedEvent(id, procesion.getHermandadId(), estadoAnterior, nuevoEstado));
        return procesion;
    }

    @Transactional(readOnly = true)
    public Page<Procesion> listarPorHermandad(UUID hermandadId, Pageable pageable) {
        return procesionRepository.findByHermandadId(hermandadId, pageable);
    }

    @Transactional
    public void eliminarProcesion(UUID id) {
        if (procesionRepository.findById(id).isEmpty()) {
            throw new ProcesionNotFoundException(id);
        }
        procesionRepository.deleteById(id);
    }
}
