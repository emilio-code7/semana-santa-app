package com.repertorio.hermandad.adapter.inbound.rest.controller;

import com.repertorio.common.JdbcIntegrationTestBase;
import com.repertorio.hermandad.adapter.inbound.kafka.IdempotentEventConsumer;
import com.repertorio.hermandad.adapter.outbound.persistence.HermandadEntity;
import com.repertorio.hermandad.adapter.outbound.persistence.HermandadJpaRepository;
import com.repertorio.hermandad.adapter.outbound.persistence.HermandadMemberEntity;
import com.repertorio.hermandad.adapter.outbound.persistence.HermandadMemberJpaRepository;
import com.repertorio.hermandad.application.port.UserExistencePort;
import com.repertorio.hermandad.domain.model.HermandadRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HermandadControllerIntegrationTest extends JdbcIntegrationTestBase {

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                System.getenv().getOrDefault("IT_DATASOURCE_URL", "jdbc:postgresql://localhost:5432/hermandad_db"));
        registry.add("spring.datasource.username", () ->
                System.getenv().getOrDefault("IT_DATASOURCE_USERNAME", "postgres"));
        registry.add("spring.datasource.password", () ->
                System.getenv().getOrDefault("IT_DATASOURCE_PASSWORD", "postgres"));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HermandadJpaRepository hermandadRepo;

    @Autowired
    private HermandadMemberJpaRepository memberRepo;

    @MockitoBean
    private IdempotentEventConsumer idempotentEventConsumer;

    @MockitoBean
    private UserExistencePort userExistencePort;

    private UUID hermandadId;

    @BeforeEach
    void setUp() {
        when(userExistencePort.exists(anyString())).thenReturn(true);
        // Clean slate: create a fresh hermandad for each test
        var hermandad = hermandadRepo.save(
                new HermandadEntity(null, "IT-Controller-" + System.nanoTime(), "Sevilla", 2024, null, "test", null, null));
        hermandadId = hermandad.getId();
    }

    @Test
    void getWithPaginationReturnsCorrectStructure() throws Exception {
        IntStream.range(0, 5).forEach(i ->
                memberRepo.save(new HermandadMemberEntity(null, hermandadId, "user-" + i, HermandadRole.MUSICIAN, null, null))
        );

        mockMvc.perform(get("/api/hermandades/{id}/members", hermandadId)
                        .param("page", "0")
                        .param("size", "2")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.number").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.numberOfElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void getWithPageSize5Returns5Items() throws Exception {
        IntStream.range(0, 10).forEach(i ->
                memberRepo.save(new HermandadMemberEntity(null, hermandadId, "user-" + i, HermandadRole.MUSICIAN, null, null))
        );

        mockMvc.perform(get("/api/hermandades/{id}/members", hermandadId)
                        .param("page", "0")
                        .param("size", "5")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numberOfElements").value(5))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.content.length()").value(5));
    }

    @Test
    void defaultPageSize20WhenNoPaginationParams() throws Exception {
        IntStream.range(0, 5).forEach(i ->
                memberRepo.save(new HermandadMemberEntity(null, hermandadId, "user-" + i, HermandadRole.MUSICIAN, null, null))
        );

        mockMvc.perform(get("/api/hermandades/{id}/members", hermandadId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20));
    }
}
