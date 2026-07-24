package com.repertorio.hermandad.adapter.outbound.cognito;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CognitoUserExistenceAdapterTest {

    private static final String POOL_ID = "eu-south-2_testPool";

    @Mock
    private CognitoIdentityProviderClient client;

    @InjectMocks
    private CognitoUserExistenceAdapter adapter;

    @Test
    void existsReturnsTrueWhenUserFound() {
        var userId = "existing-user";
        adapter = new CognitoUserExistenceAdapter(client, POOL_ID);
        when(client.adminGetUser(any(AdminGetUserRequest.class)))
                .thenReturn(AdminGetUserResponse.builder().build());

        boolean result = adapter.exists(userId);

        assertThat(result).isTrue();
    }

    @Test
    void existsReturnsFalseWhenUserNotFound() {
        var userId = "nonexistent-user";
        adapter = new CognitoUserExistenceAdapter(client, POOL_ID);
        when(client.adminGetUser(any(AdminGetUserRequest.class)))
                .thenThrow(UserNotFoundException.builder().build());

        boolean result = adapter.exists(userId);

        assertThat(result).isFalse();
    }

    @Test
    void existsPropagatesOtherProviderExceptions() {
        var userId = "error-user";
        adapter = new CognitoUserExistenceAdapter(client, POOL_ID);
        when(client.adminGetUser(any(AdminGetUserRequest.class)))
                .thenThrow(new RuntimeException("Unexpected provider error"));

        assertThatThrownBy(() -> adapter.exists(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unexpected provider error");
    }

    @Test
    void passesCorrectPoolIdAndUsername() {
        var userId = "test-user";
        adapter = new CognitoUserExistenceAdapter(client, POOL_ID);
        when(client.adminGetUser(any(AdminGetUserRequest.class)))
                .thenReturn(AdminGetUserResponse.builder().build());

        adapter.exists(userId);

        var captor = org.mockito.ArgumentCaptor.forClass(AdminGetUserRequest.class);
        org.mockito.Mockito.verify(client).adminGetUser(captor.capture());
        assertThat(captor.getValue().userPoolId()).isEqualTo(POOL_ID);
        assertThat(captor.getValue().username()).isEqualTo(userId);
    }
}
