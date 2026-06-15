package com.repertorio.hermandad.application.event;

import com.repertorio.hermandad.application.service.KeycloakMembershipService;
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

    private final KeycloakMembershipService  keycloakMembershipService;

    @EventListener
    public void handleMemberAddedEvent(MemberAddedEvent event) {
        log.info("MemberAddedEvent received {}", event);
        try {
            keycloakMembershipService.assignRole(event.userId(), event.role());
        } catch (Exception e) {
            log.error("Error assigning role {} to user {}", event.userId(), event.role(), e);
        }
    }

}
