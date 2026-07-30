package com.repertorio.hermandad.application.service;

import com.repertorio.hermandad.adapter.inbound.rest.dto.CreateTitularRequest;
import com.repertorio.hermandad.adapter.inbound.rest.dto.TitularResponse;
import com.repertorio.hermandad.adapter.inbound.rest.dto.UpdateTitularRequest;
import com.repertorio.hermandad.application.port.DomainEventPublisher;
import com.repertorio.hermandad.domain.event.TitularCreatedEvent;
import com.repertorio.hermandad.domain.event.TitularUpdatedEvent;
import com.repertorio.hermandad.domain.model.Titular;
import com.repertorio.hermandad.domain.model.TitularNotFoundException;
import com.repertorio.hermandad.domain.repository.TitularRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TitularService {

    private final TitularRepository titularRepository;
    private final DomainEventPublisher domainEventPublisher;

    @Transactional
    public TitularResponse createTitular(UUID hermandadId, CreateTitularRequest request) {
        var titular = new Titular(request.name(), request.description(), hermandadId);
        titular = titularRepository.save(titular);
        domainEventPublisher.publish(new TitularCreatedEvent(
                titular.getId(), hermandadId, titular.getName(), titular.getDescription()));
        return TitularResponse.from(titular);
    }

    @Transactional(readOnly = true)
    public TitularResponse getTitular(UUID hermandadId, UUID id) {
        var titular = titularRepository.findById(id)
                .orElseThrow(() -> new TitularNotFoundException(id));
        if (!titular.getHermandadId().equals(hermandadId)) {
            throw new TitularNotFoundException(id);
        }
        return TitularResponse.from(titular);
    }

    @Transactional(readOnly = true)
    public List<TitularResponse> listTitulares(UUID hermandadId) {
        return titularRepository.findByHermandadId(hermandadId).stream()
                .map(TitularResponse::from)
                .toList();
    }

    @Transactional
    public TitularResponse updateTitular(UUID hermandadId, UUID id, UpdateTitularRequest request) {
        var titular = titularRepository.findById(id)
                .orElseThrow(() -> new TitularNotFoundException(id));
        if (!titular.getHermandadId().equals(hermandadId)) {
            throw new TitularNotFoundException(id);
        }
        titular.update(request.name(), request.description());
        titular = titularRepository.save(titular);
        domainEventPublisher.publish(new TitularUpdatedEvent(
                titular.getId(), hermandadId, titular.getName(), titular.getDescription()));
        return TitularResponse.from(titular);
    }
}
