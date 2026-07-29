package com.repertorio.hermandad.application.service;

import com.repertorio.common.event.DomainEvent;
import com.repertorio.hermandad.adapter.inbound.rest.dto.CreateTitularRequest;
import com.repertorio.hermandad.adapter.inbound.rest.dto.TitularResponse;
import com.repertorio.hermandad.adapter.inbound.rest.dto.UpdateTitularRequest;
import com.repertorio.hermandad.application.port.DomainEventPublisher;
import com.repertorio.hermandad.domain.event.TitularCreatedEvent;
import com.repertorio.hermandad.domain.event.TitularUpdatedEvent;
import com.repertorio.hermandad.domain.model.Titular;
import com.repertorio.hermandad.domain.model.TitularNotFoundException;
import com.repertorio.hermandad.domain.repository.TitularRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TitularServiceTest {

    @Mock
    private TitularRepository titularRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private TitularService titularService;

    @Captor
    private ArgumentCaptor<DomainEvent> eventCaptor;

    private final UUID hermandadId = UUID.randomUUID();
    private final CreateTitularRequest createRequest = new CreateTitularRequest("Jesus del Gran Poder", null);

    @Test
    void createTitularPersistsAndPublishesEvent() {
        var saved = new Titular("Jesus del Gran Poder", null, hermandadId);
        when(titularRepository.save(any())).thenReturn(saved);

        var response = titularService.createTitular(hermandadId, createRequest);

        assertThat(response.name()).isEqualTo("Jesus del Gran Poder");
        assertThat(response.hermandadId()).isEqualTo(hermandadId);
        verify(titularRepository).save(any());
        verify(domainEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(TitularCreatedEvent.class);
        var event = (TitularCreatedEvent) eventCaptor.getValue();
        assertThat(event.aggregateType()).isEqualTo("titular");
        assertThat(event.eventType()).isEqualTo("TITULAR_CREATED");
    }

    @Test
    void getTitularReturnsWhenOwnedByHermandad() {
        var id = UUID.randomUUID();
        var titular = new Titular("Jesus", null, hermandadId);
        when(titularRepository.findById(id)).thenReturn(Optional.of(titular));

        var response = titularService.getTitular(hermandadId, id);
        assertThat(response.name()).isEqualTo("Jesus");
    }

    @Test
    void getTitularThrowsWhenNotOwnedByHermandad() {
        var id = UUID.randomUUID();
        var otherHermandad = UUID.randomUUID();
        var titular = new Titular("Jesus", null, otherHermandad);
        when(titularRepository.findById(id)).thenReturn(Optional.of(titular));

        assertThrows(TitularNotFoundException.class,
                () -> titularService.getTitular(hermandadId, id));
    }

    @Test
    void getTitularThrowsWhenNotFound() {
        var id = UUID.randomUUID();
        when(titularRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TitularNotFoundException.class,
                () -> titularService.getTitular(hermandadId, id));
    }

    @Test
    void listTitularesReturnsByHermandad() {
        var titular = new Titular("Jesus", null, hermandadId);
        when(titularRepository.findByHermandadId(hermandadId)).thenReturn(List.of(titular));

        var result = titularService.listTitulares(hermandadId);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Jesus");
    }

    @Test
    void updateTitularUpdatesAndPublishesEvent() {
        var id = UUID.randomUUID();
        var titular = new Titular("Old", null, hermandadId);
        when(titularRepository.findById(id)).thenReturn(Optional.of(titular));
        when(titularRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var updateRequest = new UpdateTitularRequest("New", "Updated desc");
        var response = titularService.updateTitular(hermandadId, id, updateRequest);

        assertThat(response.name()).isEqualTo("New");
        assertThat(response.description()).isEqualTo("Updated desc");
        verify(domainEventPublisher).publish(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(TitularUpdatedEvent.class);
    }

    @Test
    void updateTitularThrowsWhenNotOwnedByHermandad() {
        var id = UUID.randomUUID();
        var otherHermandad = UUID.randomUUID();
        var titular = new Titular("Jesus", null, otherHermandad);
        when(titularRepository.findById(id)).thenReturn(Optional.of(titular));

        assertThrows(TitularNotFoundException.class,
                () -> titularService.updateTitular(hermandadId, id, new UpdateTitularRequest("New", null)));
    }

    @Test
    void updateTitularThrowsWhenNotFound() {
        var id = UUID.randomUUID();
        when(titularRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(TitularNotFoundException.class,
                () -> titularService.updateTitular(hermandadId, id, new UpdateTitularRequest("New", null)));
    }
}
