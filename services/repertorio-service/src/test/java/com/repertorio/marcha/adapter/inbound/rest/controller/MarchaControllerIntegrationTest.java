package com.repertorio.marcha.adapter.inbound.rest.controller;

import com.repertorio.marcha.adapter.inbound.kafka.ProcesionEventConsumer;
import com.repertorio.marcha.adapter.outbound.outbox.OutboxEventJpaRepository;
import com.repertorio.marcha.adapter.outbound.persistence.MarchaJpaRepository;
import com.repertorio.marcha.domain.model.BandType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MarchaControllerIntegrationTest {

    private static final String POSTGRES_JDBC = System.getenv().getOrDefault(
            "IT_DATASOURCE_URL", "jdbc:postgresql://localhost:5433/repertorio_db");
    private static final String POSTGRES_USER = System.getenv().getOrDefault(
            "IT_DATASOURCE_USERNAME", "postgres");
    private static final String POSTGRES_PASS = System.getenv().getOrDefault(
            "IT_DATASOURCE_PASSWORD", "postgres");

    @BeforeAll
    static void checkPostgresRunning() {
        try (var c = java.sql.DriverManager.getConnection(POSTGRES_JDBC, POSTGRES_USER, POSTGRES_PASS)) {
            // connection OK
        } catch (Exception e) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false,
                    "PostgreSQL not reachable at " + POSTGRES_JDBC + " — skipping integration test");
        }
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES_JDBC);
        registry.add("spring.datasource.username", () -> POSTGRES_USER);
        registry.add("spring.datasource.password", () -> POSTGRES_PASS);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MarchaJpaRepository marchaRepo;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockitoBean
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @MockitoBean
    private ProcesionEventConsumer procesionEventConsumer;

    @Test
    void createMarchaReturns201() throws Exception {
        mockMvc.perform(post("/api/marchas")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"IT-Create-Test","composer":"Test Composer",\
                                "bandType":"BANDA_PALIO","durationSeconds":300}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("IT-Create-Test"))
                .andExpect(jsonPath("$.composer").value("Test Composer"))
                .andExpect(jsonPath("$.bandType").value("BANDA_PALIO"))
                .andExpect(jsonPath("$.durationSeconds").value(300));
    }

    @Test
    void getMarchaReturns200() throws Exception {
        var saved = marchaRepo.save(new com.repertorio.marcha.adapter.outbound.persistence.MarchaEntity(
                UUID.randomUUID(), "IT-Get-Test", "Composer", BandType.BANDA_PALIO,
                300, null, null, Instant.now(), Instant.now()));

        mockMvc.perform(get("/api/marchas/{id}", saved.getId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.title").value("IT-Get-Test"));
    }

    @Test
    void listMarchasReturns200() throws Exception {
        marchaRepo.save(new com.repertorio.marcha.adapter.outbound.persistence.MarchaEntity(
                UUID.randomUUID(), "IT-List-1", "Composer A", BandType.BANDA_PALIO,
                300, null, null, Instant.now(), Instant.now()));
        marchaRepo.save(new com.repertorio.marcha.adapter.outbound.persistence.MarchaEntity(
                UUID.randomUUID(), "IT-List-2", "Composer B", BandType.AGRUPACION_MUSICAL,
                400, null, null, Instant.now(), Instant.now()));

        mockMvc.perform(get("/api/marchas")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].title").isNotEmpty());
    }

    @Test
    void deleteMarchaReturns204ThenGetReturns404() throws Exception {
        var saved = marchaRepo.save(new com.repertorio.marcha.adapter.outbound.persistence.MarchaEntity(
                UUID.randomUUID(), "IT-Delete-Test", "Composer", BandType.BANDA_PALIO,
                300, null, null, Instant.now(), Instant.now()));

        mockMvc.perform(delete("/api/marchas/{id}", saved.getId())
                        .with(jwt()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/marchas/{id}", saved.getId())
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createMarchaReturns400() throws Exception {
        mockMvc.perform(post("/api/marchas")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","composer":"","bandType":"BANDA_PALIO","durationSeconds":300}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/marchas"))
                .andExpect(status().isUnauthorized());
    }
}
