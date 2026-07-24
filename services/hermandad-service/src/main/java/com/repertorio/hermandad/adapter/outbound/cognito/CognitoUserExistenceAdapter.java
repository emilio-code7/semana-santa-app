package com.repertorio.hermandad.adapter.outbound.cognito;

import com.repertorio.hermandad.application.port.UserExistencePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.UserNotFoundException;

@Slf4j
@Service
@Profile("aws")
public class CognitoUserExistenceAdapter implements UserExistencePort {

    private final CognitoIdentityProviderClient client;
    private final String userPoolId;

    public CognitoUserExistenceAdapter(CognitoIdentityProviderClient client,
                                       @Value("${cognito.user-pool-id}") String userPoolId) {
        this.client = client;
        this.userPoolId = userPoolId;
    }

    @Override
    public boolean exists(String userId) {
        try {
            client.adminGetUser(AdminGetUserRequest.builder()
                    .userPoolId(userPoolId)
                    .username(userId)
                    .build());
            log.debug("User {} exists in Cognito", userId);
            return true;
        } catch (UserNotFoundException e) {
            log.debug("User {} not found in Cognito", userId);
            return false;
        }
    }
}
