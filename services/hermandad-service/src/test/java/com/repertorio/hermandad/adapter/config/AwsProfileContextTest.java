package com.repertorio.hermandad.adapter.config;

import com.repertorio.hermandad.adapter.outbound.cognito.CognitoMembershipAdapter;
import com.repertorio.hermandad.adapter.outbound.cognito.CognitoUserExistenceAdapter;
import com.repertorio.hermandad.adapter.outbound.keycloak.KeycloakMembershipAdapter;
import com.repertorio.hermandad.adapter.outbound.keycloak.KeycloakUserExistenceAdapter;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        AwsCacheConfig.class,
        CognitoConfig.class,
        KeycloakConfig.class,
        RedisConfig.class,
        CognitoMembershipAdapter.class,
        CognitoUserExistenceAdapter.class,
        KeycloakMembershipAdapter.class,
        KeycloakUserExistenceAdapter.class,
        AwsProfileContextTest.TestConfig.class
}, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("aws")
@TestPropertySource(properties = {
        "spring.cloud.aws.region.static=eu-south-2",
        "cognito.user-pool-id=test-pool"
})
class AwsProfileContextTest {

    @Autowired(required = false)
    private CognitoIdentityProviderClient cognitoIdentityProviderClient;

    @Autowired(required = false)
    private CognitoMembershipAdapter cognitoMembershipAdapter;

    @Autowired(required = false)
    private CognitoUserExistenceAdapter cognitoUserExistenceAdapter;

    @Autowired(required = false)
    private Keycloak keycloakClient;

    @Autowired(required = false)
    private KeycloakMembershipAdapter keycloakMembershipAdapter;

    @Autowired(required = false)
    private KeycloakUserExistenceAdapter keycloakUserExistenceAdapter;

    @Autowired(required = false)
    private RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer;

    @Autowired(required = false)
    private AwsCacheConfig awsCacheConfig;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Test
    void cognitoBeansArePresent() {
        assertThat(cognitoIdentityProviderClient).isNotNull();
        assertThat(cognitoMembershipAdapter).isNotNull();
        assertThat(cognitoUserExistenceAdapter).isNotNull();
    }

    @Test
    void keycloakBeansAreAbsent() {
        assertThat(keycloakClient).isNull();
        assertThat(keycloakMembershipAdapter).isNull();
        assertThat(keycloakUserExistenceAdapter).isNull();
    }

    @Test
    void redisBeansAreAbsentUnderAwsProfile() {
        assertThat(redisCacheManagerBuilderCustomizer).isNull();
    }

    @Test
    void awsCacheConfigProvidesConcurrentMapCacheManager() {
        assertThat(awsCacheConfig).isNotNull();
        assertThat(cacheManager).isNotNull();
        assertThat(cacheManager).isInstanceOf(ConcurrentMapCacheManager.class);
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
