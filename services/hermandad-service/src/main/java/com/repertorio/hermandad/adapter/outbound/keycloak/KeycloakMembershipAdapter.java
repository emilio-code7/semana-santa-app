package com.repertorio.hermandad.adapter.outbound.keycloak;

import com.repertorio.hermandad.application.port.MembershipPort;
import com.repertorio.hermandad.domain.model.HermandadRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Profile("!aws")
@RequiredArgsConstructor
@Slf4j
public class KeycloakMembershipAdapter implements MembershipPort {

    private static final String SEMANA_SANTA_REALM = "semana-santa";

    private final Keycloak keycloakClient;

    @Override
    public void assignRole(String userId, UUID hermandadId, HermandadRole role) {
        assignRole(userId, role);
    }

    public void assignRole(String userId, HermandadRole hermandadRole) {
        log.info("Assigning role {} to user {}", hermandadRole, userId);
        try {
            var keycloakRole = keycloakClient.realm(SEMANA_SANTA_REALM)
                    .roles()
                    .get(hermandadRole.name())
                    .toRepresentation();

            keycloakClient.realm(SEMANA_SANTA_REALM)
                    .users()
                    .get(userId)
                    .roles()
                    .realmLevel()
                    .add(List.of(keycloakRole));

            log.info("Assigned role {} to user {}", hermandadRole, userId);
        } catch (jakarta.ws.rs.WebApplicationException e) {
            log.error("Keycloak error assigning role {} to user {}", hermandadRole, userId, e);
            throw e;
        } catch (RuntimeException e) {
            log.error("Unexpected error assigning role {} to user {}", hermandadRole, userId, e);
            throw e;
        }
    }
}
