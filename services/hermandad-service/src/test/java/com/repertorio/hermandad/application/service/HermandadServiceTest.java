package com.repertorio.hermandad.application.service;

import com.repertorio.hermandad.adapter.inbound.rest.dto.AddMemberRequest;
import com.repertorio.hermandad.application.port.EventPublisher;
import com.repertorio.hermandad.domain.event.MemberAddedEvent;
import com.repertorio.hermandad.domain.model.HermandadMember;
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
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HermandadServiceTest {

    @Mock
    private HermandadRepository hermandadRepository;

    @Mock
    private HermandadMemberRepository hermandadMemberRepository;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private HermandadService hermandadService;

    @Captor
    private ArgumentCaptor<MemberAddedEvent> memberAddedEventCaptor;

    @Test
    void addMemberPublishesOutboxEvent() {
        UUID hermandadId = UUID.randomUUID();
        AddMemberRequest request = new AddMemberRequest("user-123", HermandadRole.MUSICIAN);
        HermandadMember savedMember = new HermandadMember(hermandadId, "user-123", HermandadRole.MUSICIAN);

        when(hermandadRepository.existsById(hermandadId)).thenReturn(true);
        when(hermandadMemberRepository.save(any())).thenReturn(savedMember);

        hermandadService.addMember(hermandadId, request);

        verify(eventPublisher).publish(
                eq("hermandad-member"),
                isNull(),
                eq("MEMBER_ADDED"),
                any(MemberAddedEvent.class)
        );
    }

    @Test
    void addMemberPublishesSpringEvent() {
        UUID hermandadId = UUID.randomUUID();
        AddMemberRequest request = new AddMemberRequest("user-123", HermandadRole.MUSICIAN);
        HermandadMember savedMember = new HermandadMember(hermandadId, "user-123", HermandadRole.MUSICIAN);

        when(hermandadRepository.existsById(hermandadId)).thenReturn(true);
        when(hermandadMemberRepository.save(any())).thenReturn(savedMember);

        hermandadService.addMember(hermandadId, request);

        verify(applicationEventPublisher).publishEvent(memberAddedEventCaptor.capture());
        assertThat(memberAddedEventCaptor.getValue().userId()).isEqualTo("user-123");
        assertThat(memberAddedEventCaptor.getValue().role()).isEqualTo(HermandadRole.MUSICIAN);
    }
}
