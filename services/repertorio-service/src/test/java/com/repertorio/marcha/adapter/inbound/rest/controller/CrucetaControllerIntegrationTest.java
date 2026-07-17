package com.repertorio.marcha.adapter.inbound.rest.controller;

import com.repertorio.marcha.adapter.inbound.kafka.ProcesionEventConsumer;
import com.repertorio.common.outbox.OutboxEventJpaRepository;
import com.repertorio.marcha.adapter.outbound.persistence.KnownProcesionJpaRepository;
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
    private KnownProcesionJpaRepository knownProcesionJpaRepository;

    @Test
    void defineCrucetaReturns200() throws Exception {
        var hermandadId = UUID.randomUUID();
        var procesionId = UUID.randomUUID();
        when(knownProcesionJpaRepository.existsByProcesionId(procesionId)).thenReturn(true);

        mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/cruceta",
                        hermandadId, procesionId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"marchaId":"a0000001-0000-0000-0000-000000000001","orderIndex":0}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.procesionId").value(procesionId.toString()))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void getCrucetaReturns200() throws Exception {
        var hermandadId = UUID.randomUUID();
        var procesionId = UUID.randomUUID();
        when(knownProcesionJpaRepository.existsByProcesionId(procesionId)).thenReturn(true);

        // First define the cruceta
        mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/cruceta",
                        hermandadId, procesionId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"marchaId":"a0000001-0000-0000-0000-000000000001","orderIndex":0}]}
                                """))
                .andExpect(status().isOk());

        // Then retrieve it
        mockMvc.perform(get("/api/hermandades/{hid}/procesiones/{pid}/cruceta",
                        hermandadId, procesionId)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.procesionId").value(procesionId.toString()))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void getCrucetaReturns404() throws Exception {
        var procesionId = UUID.randomUUID();

        mockMvc.perform(get("/api/hermandades/{hid}/procesiones/{pid}/cruceta",
                        UUID.randomUUID(), procesionId)
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/hermandades/{hid}/procesiones/{pid}/cruceta",
                        UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
