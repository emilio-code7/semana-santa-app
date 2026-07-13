package com.repertorio.hermandad.adapter.outbound.keycloak;

import com.repertorio.hermandad.application.port.UserExistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakUserExistenceAdapter implements UserExistencePort {

    private static final String SEMANA_SANTA_REALM = "semana-santa";

    private final Keycloak keycloakClient;

    @Override
    public boolean exists(String userId) {
        try {
            keycloakClient.realm(SEMANA_SANTA_REALM).users().get(userId).toRepresentation();
            log.debug("User {} exists in Keycloak", userId);
            return true;
        } catch (jakarta.ws.rs.NotFoundException e) {
            log.debug("User {} not found in Keycloak", userId);
            return false;
        } catch (Exception e) {
            log.warn("Error checking user {} existence in Keycloak", userId, e);
            return false;
        }
    }
}
