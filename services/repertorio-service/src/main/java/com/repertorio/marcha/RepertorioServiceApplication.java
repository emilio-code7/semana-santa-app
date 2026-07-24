package com.repertorio.marcha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.repertorio.marcha", "com.repertorio.common.outbox"})
@EntityScan({"com.repertorio.marcha", "com.repertorio.common.outbox"})
@EnableDiscoveryClient
@EnableScheduling
public class RepertorioServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RepertorioServiceApplication.class, args);
    }
}
