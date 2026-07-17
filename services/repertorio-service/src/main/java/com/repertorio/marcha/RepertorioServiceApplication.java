package com.repertorio.marcha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.repertorio.marcha", "com.repertorio.common.outbox"})
@EnableJpaRepositories(basePackages = {"com.repertorio.marcha", "com.repertorio.common.outbox"})
@EntityScan("com.repertorio.marcha")
@EnableDiscoveryClient
@EnableKafka
@EnableScheduling
public class RepertorioServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RepertorioServiceApplication.class, args);
    }
}
