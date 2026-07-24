package com.repertorio.hermandad.application.event;

import com.repertorio.hermandad.application.port.MembershipPort;
import com.repertorio.hermandad.domain.event.MemberAddedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Async("taskExecutor")
@RequiredArgsConstructor
@Slf4j
public class MemberAddedListener {

    private final MembershipPort membershipPort;

    @EventListener
    public void handleMemberAddedEvent(MemberAddedEvent event) {
        log.info("MemberAddedEvent received {}", event);
        try {
            membershipPort.assignRole(event.userId(), event.hermandadId(), event.role());
        } catch (RuntimeException e) {
            log.error("Error assigning role {} to user {}", event.userId(), event.role(), e);
        }
    }

}
