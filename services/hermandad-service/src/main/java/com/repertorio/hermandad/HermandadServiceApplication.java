package com.repertorio.hermandad;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.repertorio.hermandad", "com.repertorio.common.outbox"})
@EnableJpaRepositories(basePackages = {"com.repertorio.hermandad", "com.repertorio.common.outbox"})
@EntityScan("com.repertorio.hermandad")
@EnableDiscoveryClient
@EnableScheduling
@EnableCaching
public class HermandadServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HermandadServiceApplication.class, args);
    }
}
