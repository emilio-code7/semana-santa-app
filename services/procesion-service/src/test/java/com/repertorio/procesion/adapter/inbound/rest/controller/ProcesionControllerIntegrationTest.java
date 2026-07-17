package com.repertorio.procesion.adapter.inbound.rest.controller;

import com.repertorio.common.JdbcIntegrationTestBase;
import com.repertorio.common.outbox.OutboxEventJpaRepository;
import com.repertorio.procesion.adapter.outbound.persistence.ProcesionEntity;
import com.repertorio.procesion.adapter.outbound.persistence.ProcesionJpaRepository;
import com.repertorio.procesion.domain.model.Procesion;
import com.repertorio.procesion.domain.model.ProcesionStatus;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProcesionControllerIntegrationTest extends JdbcIntegrationTestBase {

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                System.getenv().getOrDefault("IT_DATASOURCE_URL", "jdbc:postgresql://localhost:5434/procesion_db"));
        registry.add("spring.datasource.username", () ->
                System.getenv().getOrDefault("IT_DATASOURCE_USERNAME", "postgres"));
        registry.add("spring.datasource.password", () ->
                System.getenv().getOrDefault("IT_DATASOURCE_PASSWORD", "postgres"));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProcesionJpaRepository procesionRepo;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockitoBean
    private OutboxEventJpaRepository outboxEventJpaRepository;

    private UUID hermandadId;

    @BeforeEach
    void setUp() {
        hermandadId = UUID.randomUUID();
    }

    @Test
    void createProcesionReturns201() throws Exception {
        mockMvc.perform(post("/api/procesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "hermandadId": "%s",
                                    "date": "2026-04-13",
                                    "time": "18:00:00"
                                }
                                """.formatted(hermandadId))
                        .with(jwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hermandadId").value(hermandadId.toString()))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void getProcesionReturns200() throws Exception {
        var procesion = procesionRepo.save(
                new ProcesionEntity(null, hermandadId, LocalDate.of(2026, 4, 13), LocalTime.of(18, 0),
                        ProcesionStatus.PLANNED, null, null));

        mockMvc.perform(get("/api/procesiones/{id}", procesion.getId())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(procesion.getId().toString()))
                .andExpect(jsonPath("$.hermandadId").value(hermandadId.toString()))
                .andExpect(jsonPath("$.status").value("PLANNED"));
    }

    @Test
    void getProcesionReturns404() throws Exception {
        mockMvc.perform(get("/api/procesiones/{id}", UUID.randomUUID())
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listByHermandadReturnsPage() throws Exception {
        for (int i = 0; i < 3; i++) {
            procesionRepo.save(new ProcesionEntity(null, hermandadId,
                    LocalDate.of(2026, 4, 13).plusDays(i), LocalTime.of(18, 0),
                    ProcesionStatus.PLANNED, null, null));
        }

        mockMvc.perform(get("/api/procesiones")
                        .param("hermandadId", hermandadId.toString())
                        .param("page", "0")
                        .param("size", "2")
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void changeStatusReturns200() throws Exception {
        var procesion = procesionRepo.save(
                new ProcesionEntity(null, hermandadId, LocalDate.of(2026, 4, 13), LocalTime.of(18, 0),
                        ProcesionStatus.PLANNED, null, null));

        mockMvc.perform(patch("/api/procesiones/{id}/status", procesion.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "newStatus": "IN_PROGRESS" }
                                """)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void changeStatusReturns400OnInvalidTransition() throws Exception {
        var procesion = procesionRepo.save(
                new ProcesionEntity(null, hermandadId, LocalDate.of(2026, 4, 13), LocalTime.of(18, 0),
                        ProcesionStatus.PLANNED, null, null));

        mockMvc.perform(patch("/api/procesiones/{id}/status", procesion.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "newStatus": "COMPLETED" }
                                """)
                        .with(jwt()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteProcesionReturns204() throws Exception {
        var procesion = procesionRepo.save(
                new ProcesionEntity(null, hermandadId, LocalDate.of(2026, 4, 13), LocalTime.of(18, 0),
                        ProcesionStatus.PLANNED, null, null));

        mockMvc.perform(delete("/api/procesiones/{id}", procesion.getId())
                        .with(jwt()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/procesiones/{id}", procesion.getId())
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/procesiones/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
