package com.repertorio.hermandad.adapter.config;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.regions.Region;

import static org.assertj.core.api.Assertions.assertThat;

class CognitoConfigTest {

    @Test
    void createsClientWithConfiguredRegion() {
        var config = new CognitoConfig("eu-south-2");
        var client = config.cognitoIdentityProviderClient();

        assertThat(client.serviceClientConfiguration().region())
                .isEqualTo(Region.of("eu-south-2"));

        client.close();
    }
}
