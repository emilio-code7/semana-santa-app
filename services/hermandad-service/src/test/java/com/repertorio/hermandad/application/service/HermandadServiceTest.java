package com.repertorio.hermandad.application.service;

import com.repertorio.hermandad.adapter.inbound.rest.dto.CreateHermandadRequest;
import com.repertorio.hermandad.adapter.inbound.rest.dto.AddMemberRequest;
import com.repertorio.hermandad.application.port.DomainEvent;
import com.repertorio.hermandad.application.port.DomainEventPublisher;
import com.repertorio.hermandad.domain.event.HermandadCreatedEvent;
import com.repertorio.hermandad.domain.model.Hermandad;
import com.repertorio.hermandad.domain.model.HermandadMember;
import com.repertorio.hermandad.domain.model.HermandadMemberNotFoundException;
import com.repertorio.hermandad.domain.model.HermandadAlreadyExistsException;
import com.repertorio.hermandad.domain.model.HermandadRole;
import com.repertorio.hermandad.domain.repository.HermandadMemberRepository;
import com.repertorio.hermandad.domain.repository.HermandadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HermandadServiceTest {

    @Mock
    private HermandadRepository hermandadRepository;

    @Mock
    private HermandadMemberRepository hermandadMemberRepository;

    @Mock
    private DomainEventPublisher domainEventPublisher;

    @InjectMocks
    private HermandadService hermandadService;

    @Captor
    private ArgumentCaptor<DomainEvent> domainEventCaptor;

    @Test
    void createHermandadPublishesEvent() {
        var request = new CreateHermandadRequest("Macarena", "Sevilla", 1932, null);
        Hermandad saved = new Hermandad("Macarena", "Sevilla", 1932, null);
        when(hermandadRepository.save(any())).thenReturn(saved);

        hermandadService.createHermandad(request, "creator-id");

        verify(hermandadMemberRepository).save(any());
        verify(domainEventPublisher).publish(domainEventCaptor.capture());
        assertThat(domainEventCaptor.getValue()).isInstanceOf(HermandadCreatedEvent.class);
    }

    @Test
    void addMemberPublishesDomainEvent() {
        UUID hermandadId = UUID.randomUUID();
        AddMemberRequest request = new AddMemberRequest("user-123", HermandadRole.MUSICIAN);
        HermandadMember savedMember = new HermandadMember(hermandadId, "user-123", HermandadRole.MUSICIAN);

        when(hermandadRepository.existsById(hermandadId)).thenReturn(true);
        when(hermandadMemberRepository.save(any())).thenReturn(savedMember);

        hermandadService.addMember(hermandadId, request);

        verify(domainEventPublisher).publish(domainEventCaptor.capture());
        var event = domainEventCaptor.getValue();
        assertThat(event.aggregateType()).isEqualTo("hermandad-member");
        assertThat(event.eventType()).isEqualTo("MEMBER_ADDED");
    }

    @Test
    void changeRolePublishesDomainEvent() {
        UUID hermandadId = UUID.randomUUID();
        String userId = "user-123";
        HermandadMember member = new HermandadMember(hermandadId, userId, HermandadRole.MUSICIAN);

        when(hermandadMemberRepository.findByUserIdAndHermandadId(userId, hermandadId))
                .thenReturn(Optional.of(member));
        when(hermandadMemberRepository.save(any())).thenReturn(member);

        hermandadService.changeRole(hermandadId, userId, HermandadRole.CAPATAZ);

        verify(domainEventPublisher).publish(domainEventCaptor.capture());
        var event = domainEventCaptor.getValue();
        assertThat(event.aggregateType()).isEqualTo("hermandad-member");
        assertThat(event.eventType()).isEqualTo("MEMBER_ROLE_CHANGED");
    }

    @Test
    void removeMemberPublishesDomainEvent() {
        UUID hermandadId = UUID.randomUUID();
        String userId = "user-123";
        HermandadMember member = new HermandadMember(hermandadId, userId, HermandadRole.MUSICIAN);

        when(hermandadMemberRepository.findByUserIdAndHermandadId(userId, hermandadId))
                .thenReturn(Optional.of(member));

        hermandadService.removeMember(hermandadId, userId);

        verify(hermandadMemberRepository).delete(member);
        verify(domainEventPublisher).publish(domainEventCaptor.capture());
        var event = domainEventCaptor.getValue();
        assertThat(event.aggregateType()).isEqualTo("hermandad-member");
        assertThat(event.eventType()).isEqualTo("MEMBER_REMOVED");
    }

    @Test
    void removeMemberThrowsWhenMemberNotFound() {
        UUID hermandadId = UUID.randomUUID();
        String userId = "user-123";

        when(hermandadMemberRepository.findByUserIdAndHermandadId(userId, hermandadId))
                .thenReturn(Optional.empty());

        assertThrows(HermandadMemberNotFoundException.class,
                () -> hermandadService.removeMember(hermandadId, userId));
    }

    @Test
    void createHermandadThrowsWhenNameAlreadyExists() {
        var request = new CreateHermandadRequest("Macarena", "Sevilla", 1932, null);
        Hermandad saved = new Hermandad("Macarena", "Sevilla", 1932, null);
        when(hermandadRepository.existsByName("Macarena")).thenReturn(false, true);
        when(hermandadRepository.save(any())).thenReturn(saved);

        hermandadService.createHermandad(request, "creator-id");

        assertThrows(HermandadAlreadyExistsException.class,
                () -> hermandadService.createHermandad(request, "creator-id"));
    }
}
