package com.repertorio.hermandad.application.event;

import com.repertorio.hermandad.application.port.MembershipPort;
import com.repertorio.hermandad.domain.event.MemberAddedEvent;
import com.repertorio.hermandad.domain.model.HermandadRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MemberAddedListenerTest {

    @Mock
    private MembershipPort membershipPort;

    @InjectMocks
    private MemberAddedListener listener;

    @Test
    void delegatesUserIdHermandadIdAndRole() {
        var userId = "test-user";
        var hermandadId = UUID.randomUUID();
        var role = HermandadRole.CAPATAZ;
        var event = new MemberAddedEvent(UUID.randomUUID(), hermandadId, userId, role);

        listener.handleMemberAddedEvent(event);

        verify(membershipPort).assignRole(userId, hermandadId, role);
    }

    @Test
    void swallowsAndLogsRuntimeExceptionFromAdapter() {
        var event = new MemberAddedEvent(UUID.randomUUID(), UUID.randomUUID(), "user123", HermandadRole.MUSICIAN);
        doThrow(new RuntimeException("adapter failed"))
                .when(membershipPort).assignRole(any(), any(), any());

        listener.handleMemberAddedEvent(event);

        verify(membershipPort).assignRole(any(), any(), any());
    }
}
