package com.repertorio.marcha.adapter.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Enables Kafka listener annotation processing only in the local (non-AWS) profile.
 * Under the AWS profile, Kafka auto-configuration is excluded via application-aws.yml
 * and SQS replaces Kafka for messaging.
 */
@Configuration
@Profile("!aws")
@EnableKafka
public class KafkaConfig {
}
