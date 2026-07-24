package com.repertorio.hermandad.adapter.outbound.cognito;

import com.repertorio.hermandad.application.port.MembershipPort;
import com.repertorio.hermandad.domain.model.HermandadRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminAddUserToGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.CreateGroupRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.GroupExistsException;

import java.util.UUID;

@Slf4j
@Service
@Profile("aws")
public class CognitoMembershipAdapter implements MembershipPort {

    private final CognitoIdentityProviderClient client;
    private final String userPoolId;

    public CognitoMembershipAdapter(CognitoIdentityProviderClient client,
                                    @Value("${cognito.user-pool-id}") String userPoolId) {
        this.client = client;
        this.userPoolId = userPoolId;
    }

    @Override
    public void assignRole(String userId, UUID hermandadId, HermandadRole role) {
        var groupName = "HERMANDAD_" + hermandadId + "_" + role.name();
        log.info("Assigning role {} to user {} in group {}", role, userId, groupName);

        try {
            client.createGroup(CreateGroupRequest.builder()
                    .userPoolId(userPoolId)
                    .groupName(groupName)
                    .build());
        } catch (GroupExistsException e) {
            log.debug("Group {} already exists", groupName);
        }

        client.adminAddUserToGroup(AdminAddUserToGroupRequest.builder()
                .userPoolId(userPoolId)
                .username(userId)
                .groupName(groupName)
                .build());

        log.info("Assigned role {} to user {}", role, userId);
    }
}
