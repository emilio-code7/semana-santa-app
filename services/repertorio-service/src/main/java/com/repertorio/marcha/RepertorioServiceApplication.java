package com.repertorio.marcha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
@EnableDiscoveryClient
@EnableKafka
@EnableScheduling
@EnableMethodSecurity
public class RepertorioServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RepertorioServiceApplication.class, args);
    }
}
