package com.repertorio.marcha.adapter.inbound.rest.controller;

import com.repertorio.marcha.adapter.inbound.kafka.ProcesionEventConsumer;
import com.repertorio.common.outbox.OutboxEventJpaRepository;
import com.repertorio.marcha.adapter.outbound.persistence.KnownPasoJpaRepository;
import com.repertorio.marcha.adapter.outbound.persistence.KnownRouteSectionJpaRepository;
import com.repertorio.common.JdbcIntegrationTestBase;
import com.jayway.jsonpath.JsonPath;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
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
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasoId").value(pasoId.toString()))
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void getCrucetaReturns404() throws Exception {
        var hermandadId = UUID.randomUUID();
        var procesionId = UUID.randomUUID();
        var pasoId = UUID.randomUUID();

        mockMvc.perform(get("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta",
                        hermandadId, procesionId, pasoId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta",
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void concurrentReplaceNever500sAndLoserIs409() throws Exception {
        var hermandadId = UUID.randomUUID();
        var procesionId = UUID.randomUUID();
        var pasoId = UUID.randomUUID();
        var marchaId = UUID.fromString("a0000001-0000-0000-0000-000000000001");
        when(knownPasoJpaRepository.existsById(pasoId)).thenReturn(true);
        when(knownRouteSectionJpaRepository.existsById(routeSectionId)).thenReturn(true);

        // Seed the cruceta once so both concurrent replaces hit the replace path
        var seedPayload = """
                {"items":[{"marchaId":"%s","routeSectionId":"%s","sequenceWithinSection":0,"notes":"seed"}]}
                """.formatted(marchaId, routeSectionId);
        mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta",
                        hermandadId, procesionId, pasoId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(seedPayload))
                .andExpect(status().isOk());

        // Two concurrent replaces with DIFFERENT payloads
        var payloadA = """
                {"items":[{"marchaId":"%s","routeSectionId":"%s","sequenceWithinSection":0,"notes":"concurrent-A"}]}
                """.formatted(marchaId, routeSectionId);
        var payloadB = """
                {"items":[{"marchaId":"%s","routeSectionId":"%s","sequenceWithinSection":1,"notes":"concurrent-B"}]}
                """.formatted(marchaId, routeSectionId);

        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        var statuses = Collections.synchronizedList(new ArrayList<Integer>());
        try {
            var taskA = executor.submit(() -> {
                ready.countDown();
                start.await();
                return replaceCruceta(hermandadId, procesionId, pasoId, payloadA);
            });
            var taskB = executor.submit(() -> {
                ready.countDown();
                start.await();
                return replaceCruceta(hermandadId, procesionId, pasoId, payloadB);
            });

            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            statuses.add(taskA.get(30, TimeUnit.SECONDS));
            statuses.add(taskB.get(30, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        // The race may legally be last-write-wins (both 2xx) or one loser 409 — it must NEVER be 5xx
        assertFalse(statuses.stream().anyMatch(s -> s >= 500), "no 5xx under the race, got: " + statuses);
        assertTrue(statuses.stream().allMatch(s -> s < 300 || s == 409),
                "every non-2xx status must be 409, got: " + statuses);
        assertTrue(statuses.stream().anyMatch(s -> s < 300),
                "at least one concurrent replace must win, got: " + statuses);

        // Final state must be consistent: exactly one cruceta, one item, matching exactly one payload
        var getResponse = mockMvc.perform(get("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta",
                        hermandadId, procesionId, pasoId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasoId").value(pasoId.toString()))
                .andReturn();

        var items = JsonPath.parse(getResponse.getResponse().getContentAsString()).read("$.items", List.class);
        assertEquals(1, items.size(), "exactly one item after the race, not a mix of payloads");
        String notes = JsonPath.parse(getResponse.getResponse().getContentAsString()).read("$.items[0].notes");
        int sequence = JsonPath.parse(getResponse.getResponse().getContentAsString())
                .read("$.items[0].sequenceWithinSection");
        if ("concurrent-A".equals(notes)) {
            assertEquals(0, sequence);
        } else {
            assertEquals("concurrent-B", notes, "notes must match exactly one of the two payloads");
            assertEquals(1, sequence);
        }
    }

    private int replaceCruceta(UUID hermandadId, UUID procesionId, UUID pasoId, String payload) throws Exception {
        return mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta",
                        hermandadId, procesionId, pasoId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andReturn().getResponse().getStatus();
    }
}
