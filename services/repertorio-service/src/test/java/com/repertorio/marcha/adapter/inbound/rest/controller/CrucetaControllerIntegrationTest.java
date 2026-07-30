package com.repertorio.marcha.adapter.inbound.rest.controller;

import com.repertorio.marcha.adapter.inbound.kafka.ProcesionEventConsumer;
import com.repertorio.common.outbox.OutboxEventJpaRepository;
import com.repertorio.marcha.adapter.outbound.persistence.KnownPasoJpaRepository;
import com.repertorio.marcha.adapter.outbound.persistence.KnownRouteSectionJpaRepository;
import com.repertorio.common.JdbcIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CrucetaControllerIntegrationTest extends JdbcIntegrationTestBase {

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                System.getenv().getOrDefault("IT_DATASOURCE_URL", "jdbc:postgresql://localhost:5433/repertorio_db"));
        registry.add("spring.datasource.username", () ->
                System.getenv().getOrDefault("IT_DATASOURCE_USERNAME", "postgres"));
        registry.add("spring.datasource.password", () ->
                System.getenv().getOrDefault("IT_DATASOURCE_PASSWORD", "postgres"));
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockitoBean
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @MockitoBean
    private ProcesionEventConsumer procesionEventConsumer;

    @MockitoBean
    private KnownPasoJpaRepository knownPasoJpaRepository;

    @MockitoBean
    private KnownRouteSectionJpaRepository knownRouteSectionJpaRepository;

    private final UUID routeSectionId = UUID.randomUUID();

    @Test
    void defineCrucetaReturns200() throws Exception {
        var hermandadId = UUID.randomUUID();
        var procesionId = UUID.randomUUID();
        var pasoId = UUID.randomUUID();
        var marchaId = UUID.fromString("a0000001-0000-0000-0000-000000000001");
        when(knownPasoJpaRepository.existsById(pasoId)).thenReturn(true);
        when(knownRouteSectionJpaRepository.existsById(routeSectionId)).thenReturn(true);

        mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta",
                        hermandadId, procesionId, pasoId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"marchaId":"%s","routeSectionId":"%s","sequenceWithinSection":0}]}
                                """.formatted(marchaId, routeSectionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasoId").value(pasoId.toString()))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void getCrucetaReturns200() throws Exception {
        var hermandadId = UUID.randomUUID();
        var procesionId = UUID.randomUUID();
        var pasoId = UUID.randomUUID();
        var marchaId = UUID.fromString("a0000001-0000-0000-0000-000000000001");
        when(knownPasoJpaRepository.existsById(pasoId)).thenReturn(true);
        when(knownRouteSectionJpaRepository.existsById(routeSectionId)).thenReturn(true);

        // First define the cruceta
        mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta",
                        hermandadId, procesionId, pasoId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"marchaId":"%s","routeSectionId":"%s","sequenceWithinSection":0}]}
                                """.formatted(marchaId, routeSectionId)))
                .andExpect(status().isOk());

        // Then retrieve it
        mockMvc.perform(get("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta",
                        hermandadId, procesionId, pasoId)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasoId").value(pasoId.toString()))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void getCrucetaReturns404() throws Exception {
        var pasoId = UUID.randomUUID();

        mockMvc.perform(get("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta",
                        UUID.randomUUID(), UUID.randomUUID(), pasoId)
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta",
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
