package com.repertorio.marcha.adapter.inbound.rest.controller;

import com.repertorio.marcha.adapter.config.security.RepertorioSecurityService;
import com.repertorio.marcha.adapter.config.security.SecurityConfig;
import com.repertorio.marcha.application.service.CrucetaService;
import com.repertorio.marcha.domain.model.Cruceta;
import com.repertorio.marcha.domain.model.CrucetaItem;
import com.repertorio.marcha.domain.model.CrucetaNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CrucetaController.class)
@Import({SecurityConfig.class, CrucetaControllerTest.TestSecurityConfig.class})
class CrucetaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CrucetaService crucetaService;

    @Autowired
    private RepertorioSecurityService repertorioSecurityService;

    private final UUID hermandadId = UUID.randomUUID();
    private final UUID procesionId = UUID.randomUUID();
    private final UUID marchaId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(repertorioSecurityService.isAdmin(any())).thenReturn(false);
    }

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean("repertorioSecurity")
        RepertorioSecurityService repertorioSecurityService() {
            return org.mockito.Mockito.mock(RepertorioSecurityService.class);
        }
    }

    private Cruceta buildCruceta() {
        var items = List.of(new CrucetaItem(marchaId, 1, "Opening"));
        return new Cruceta(procesionId, items);
    }

    @Test
    void getCrucetaReturns200() throws Exception {
        var cruceta = buildCruceta();
        when(crucetaService.getCruceta(procesionId)).thenReturn(cruceta);

        mockMvc.perform(get("/api/hermandades/{hid}/procesiones/{pid}/cruceta",
                        hermandadId, procesionId)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.procesionId").value(procesionId.toString()));
    }

    @Test
    void getCrucetaReturns404WhenNotFound() throws Exception {
        when(crucetaService.getCruceta(procesionId))
                .thenThrow(new CrucetaNotFoundException(procesionId));

        mockMvc.perform(get("/api/hermandades/{hid}/procesiones/{pid}/cruceta",
                        hermandadId, procesionId)
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void defineCrucetaReturns200ForAdmin() throws Exception {
        var cruceta = buildCruceta();
        when(crucetaService.defineCruceta(eq(procesionId), any())).thenReturn(cruceta);
        when(repertorioSecurityService.isAdmin(hermandadId)).thenReturn(true);

        mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/cruceta",
                        hermandadId, procesionId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"marchaId":"%s","orderIndex":1,"notes":"Opening"}]}
                                """.formatted(marchaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.procesionId").value(procesionId.toString()));
    }

    @Test
    void defineCrucetaReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/cruceta",
                        hermandadId, procesionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"marchaId":"%s","orderIndex":1}]}
                                """.formatted(marchaId)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void defineCrucetaReturns403WhenNotAdmin() throws Exception {
        mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/cruceta",
                        hermandadId, procesionId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"marchaId":"%s","orderIndex":1}]}
                                """.formatted(marchaId)))
                .andExpect(status().isForbidden());
    }
}
