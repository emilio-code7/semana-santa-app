package com.repertorio.hermandad.adapter.outbound.cognito;

import com.repertorio.hermandad.domain.model.HermandadRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GroupExistsException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CognitoMembershipAdapterTest {

    private static final String POOL_ID = "eu-south-2_testPool";

    @Mock
    private CognitoIdentityProviderClient client;

    @InjectMocks
    private CognitoMembershipAdapter adapter;

    @Captor
    private ArgumentCaptor<CreateGroupRequest> createGroupCaptor;

    @Captor
    private ArgumentCaptor<AdminAddUserToGroupRequest> addUserCaptor;

    @Test
    void createsGroupWithExactNameThenAddsUser() {
        var userId = "test-user";
        var hermandadId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        var role = HermandadRole.CAPATAZ;
        var expectedGroup = "HERMANDAD_550e8400-e29b-41d4-a716-446655440000_CAPATAZ";

        adapter = new CognitoMembershipAdapter(client, POOL_ID);
        adapter.assignRole(userId, hermandadId, role);

        verify(client).createGroup(createGroupCaptor.capture());
        assertThat(createGroupCaptor.getValue().userPoolId()).isEqualTo(POOL_ID);
        assertThat(createGroupCaptor.getValue().groupName()).isEqualTo(expectedGroup);

        verify(client).adminAddUserToGroup(addUserCaptor.capture());
        assertThat(addUserCaptor.getValue().userPoolId()).isEqualTo(POOL_ID);
        assertThat(addUserCaptor.getValue().username()).isEqualTo(userId);
        assertThat(addUserCaptor.getValue().groupName()).isEqualTo(expectedGroup);
    }

    @Test
    void ignoresGroupExistsExceptionAndStillAddsUser() {
        var userId = "test-user";
        var hermandadId = UUID.randomUUID();
        var role = HermandadRole.MUSICIAN;

        adapter = new CognitoMembershipAdapter(client, POOL_ID);
        doThrow(GroupExistsException.builder().build())
                .when(client).createGroup(any(CreateGroupRequest.class));

        adapter.assignRole(userId, hermandadId, role);

        verify(client).createGroup(any(CreateGroupRequest.class));
        verify(client).adminAddUserToGroup(any(AdminAddUserToGroupRequest.class));
    }

    @Test
    void propagatesOtherCognitoExceptions() {
        var userId = "test-user";
        var hermandadId = UUID.randomUUID();
        var role = HermandadRole.BAND_DIRECTOR;

        adapter = new CognitoMembershipAdapter(client, POOL_ID);
        doThrow(new RuntimeException("Unexpected Cognito error"))
                .when(client).createGroup(any(CreateGroupRequest.class));

        assertThatThrownBy(() -> adapter.assignRole(userId, hermandadId, role))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unexpected Cognito error");
    }
}
