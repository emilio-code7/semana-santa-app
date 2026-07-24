package com.repertorio.hermandad.adapter.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the exact property values in application-aws.yml for the hermandad service.
 * Loads only the YAML file — no Spring Boot application context.
 */
class AwsYamlConfigTest {

    private final YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

    @Test
    void kafkaAutoConfigUsesSpringBoot41Class() throws Exception {
        var sources = loader.load("aws", new ClassPathResource("application-aws.yml"));
        assertThat(sources).isNotEmpty();

        var props = sources.get(0);
        assertThat(props.getProperty("spring.autoconfigure.exclude[0]"))
                .isEqualTo("org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration");
    }

    @Test
    void redisAutoConfigExclusionsUseSpringBoot41Classes() throws Exception {
        var sources = loader.load("aws", new ClassPathResource("application-aws.yml"));
        assertThat(sources).isNotEmpty();

        var props = sources.get(0);
        assertThat(props.getProperty("spring.autoconfigure.exclude[1]"))
                .isEqualTo("org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration");
        assertThat(props.getProperty("spring.autoconfigure.exclude[2]"))
                .isEqualTo("org.springframework.boot.data.redis.autoconfigure.DataRedisRepositoriesAutoConfiguration");
    }

    @Test
    void jwkSetUriUsesCognitoWellKnownJwksPath() throws Exception {
        var sources = loader.load("aws", new ClassPathResource("application-aws.yml"));
        assertThat(sources).isNotEmpty();

        var props = sources.get(0);
        assertThat(props.getProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri"))
                .isEqualTo("${COGNITO_ISSUER_URL}/.well-known/jwks.json");
    }

    @Test
    void cognitoUserPoolIdUsesCorrectEnvVar() throws Exception {
        var sources = loader.load("aws", new ClassPathResource("application-aws.yml"));
        assertThat(sources).isNotEmpty();

        var props = sources.get(0);
        assertThat(props.getProperty("cognito.user-pool-id"))
                .isEqualTo("${COGNITO_USER_POOL_ID}");
    }

    @Test
    void regionIsEuSouth2() throws Exception {
        var sources = loader.load("aws", new ClassPathResource("application-aws.yml"));
        assertThat(sources).isNotEmpty();

        var props = sources.get(0);
        assertThat(props.getProperty("spring.cloud.aws.region.static"))
                .isEqualTo("eu-south-2");
    }

    @Test
    void sqsQueueMappingsAreConfigured() throws Exception {
        var sources = loader.load("aws", new ClassPathResource("application-aws.yml"));
        assertThat(sources).isNotEmpty();

        var props = sources.get(0);
        assertThat(props.getProperty("spring.cloud.aws.sqs.queue.hermandad-events"))
                .isEqualTo("${SPRING_CLOUD_AWS_SQS_QUEUE_HERMANDAD_EVENTS:hermandad-events}");
        assertThat(props.getProperty("spring.cloud.aws.sqs.queue.hermandad-member-events"))
                .isEqualTo("${SPRING_CLOUD_AWS_SQS_QUEUE_HERMANDAD_MEMBER_EVENTS:hermandad-member-events}");
        assertThat(props.getProperty("spring.cloud.aws.sqs.queue.procesion-events"))
                .isEqualTo("${SPRING_CLOUD_AWS_SQS_QUEUE_PROCESION_EVENTS:procesion-events}");
        assertThat(props.getProperty("spring.cloud.aws.sqs.queue.marcha-events"))
                .isEqualTo("${SPRING_CLOUD_AWS_SQS_QUEUE_MARCHA_EVENTS:marcha-events}");
    }
}
