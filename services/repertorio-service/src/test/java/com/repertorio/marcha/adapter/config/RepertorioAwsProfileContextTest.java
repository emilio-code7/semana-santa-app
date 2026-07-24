package com.repertorio.marcha.adapter.config;

import com.repertorio.marcha.adapter.outbound.messaging.KafkaMessageSender;
import com.repertorio.marcha.adapter.outbound.messaging.SqsMessageSender;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifies that under the "aws" profile:
 * <ul>
 *   <li>{@code @Profile("aws")} beans (SqsMessageSender) are present</li>
 *   <li>{@code @Profile("!aws")} beans (KafkaMessageSender) are absent</li>
 * </ul>
 * No real Kafka, SQS, or database infrastructure is required.
 */
@SpringBootTest(classes = {
        SqsMessageSender.class,
        KafkaMessageSender.class,
        RepertorioAwsProfileContextTest.TestConfig.class
}, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("aws")
class RepertorioAwsProfileContextTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public SqsTemplate sqsTemplate() {
            return mock(SqsTemplate.class);
        }
    }

    @Autowired(required = false)
    private SqsMessageSender sqsMessageSender;

    @Autowired(required = false)
    private KafkaMessageSender kafkaMessageSender;

    @Autowired(required = false)
    private KafkaTemplate<?, ?> kafkaTemplate;

    @Test
    void sqsBeansArePresentUnderAwsProfile() {
        assertThat(sqsMessageSender).isNotNull();
    }

    @Test
    void kafkaBeansAreAbsentUnderAwsProfile() {
        assertThat(kafkaMessageSender).isNull();
        assertThat(kafkaTemplate).isNull();
    }
}
