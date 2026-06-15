package com.repertorio.hermandad.application.service;

import com.repertorio.hermandad.domain.model.HermandadRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakMembershipService {

    private static final String SEMANA_SANTA_REALM = "semana-santa";

    private final Keycloak keycloakClient;

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
        } catch (Exception e) {
            log.error("Error assigning role {} to user {}", hermandadRole, userId, e);
        }
    }
}
