package com.repertorio.hermandad.adapter.outbound.keycloak;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakUserExistenceAdapterTest {

    @Mock
    private Keycloak keycloakClient;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @InjectMocks
    private KeycloakUserExistenceAdapter adapter;

    @Test
    void existsReturnsTrueWhenUserFound() {
        var userId = "existing-user";
        when(keycloakClient.realm("semana-santa")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(new UserRepresentation());

        boolean result = adapter.exists(userId);

        assertThat(result).isTrue();
    }

    @Test
    void existsReturnsFalseWhenUserNotFound() {
        var userId = "nonexistent-user";
        when(keycloakClient.realm("semana-santa")).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.get(userId)).thenReturn(userResource);
        when(userResource.toRepresentation()).thenThrow(new jakarta.ws.rs.NotFoundException("Not found"));

        boolean result = adapter.exists(userId);

        assertThat(result).isFalse();
    }
}
