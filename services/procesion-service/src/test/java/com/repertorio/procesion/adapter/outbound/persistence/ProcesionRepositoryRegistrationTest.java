package com.repertorio.procesion.adapter.outbound.persistence;

import com.repertorio.common.outbox.OutboxEventJpaRepository;
import com.repertorio.procesion.ProcesionServiceApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test: verifies that both the service-specific {@link ProcesionJpaRepository}
 * and the shared {@link OutboxEventJpaRepository} are registered in the application context
 * when booting with an in-memory H2 database.
 * <p>
 * Unlike {@link ProcesionRepositoryIntegrationTest}, this test does NOT mock
 * {@code OutboxEventJpaRepository} — it asserts the real bean is present.
 * Flyway, Eureka client, and real Kafka are disabled for isolation.
 */
@SpringBootTest(classes = ProcesionServiceApplication.class, properties = {
    "spring.datasource.url=jdbc:h2:mem:regtest;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "eureka.client.enabled=false"
})
class ProcesionRepositoryRegistrationTest {

    @Autowired
    private ApplicationContext context;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    void procesionJpaRepositoryIsRegistered() {
        assertThat(context.getBean(ProcesionJpaRepository.class)).isNotNull();
    }

    @Test
    void outboxEventJpaRepositoryIsRegistered() {
        assertThat(context.getBean(OutboxEventJpaRepository.class)).isNotNull();
    }

    @Test
    void bothRepositoriesAreRegistered() {
        assertThat(context.getBean(ProcesionJpaRepository.class)).isNotNull();
        assertThat(context.getBean(OutboxEventJpaRepository.class)).isNotNull();
    }
}
