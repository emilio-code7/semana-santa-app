package com.repertorio.procesion.adapter.inbound.rest.controller;

import com.repertorio.procesion.adapter.config.SecurityConfig;
import com.repertorio.procesion.application.service.PasoService;
import com.repertorio.procesion.domain.model.ForbiddenException;
import com.repertorio.procesion.domain.model.Paso;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PasoController.class)
@Import(SecurityConfig.class)
class PasoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasoService pasoService;

    private final UUID hermandadId = UUID.randomUUID();
    private final UUID procesionId = UUID.randomUUID();

    private String pasosPath() {
        return "/api/hermandades/{hid}/procesiones/{pid}/pasos"
                .replace("{hid}", hermandadId.toString())
                .replace("{pid}", procesionId.toString());
    }

    @Test
    void getPasosReturns200() throws Exception {
        when(pasoService.getPasos(hermandadId, procesionId))
                .thenReturn(List.of(Paso.create(procesionId, 0, UUID.randomUUID(), null)));

        mockMvc.perform(get(pasosPath()).with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasos").isArray())
                .andExpect(jsonPath("$.pasos.length()").value(1));
    }

    @Test
    void getPasosReturns403OnCrossTenant() throws Exception {
        when(pasoService.getPasos(hermandadId, procesionId))
                .thenThrow(new ForbiddenException("Cross-tenant"));

        mockMvc.perform(get(pasosPath()).with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void replacePasosReturns200() throws Exception {
        var pasoId = UUID.randomUUID();
        var titularId = UUID.randomUUID();
        when(pasoService.replacePasos(eq(hermandadId), eq(procesionId), any()))
                .thenReturn(List.of(
                        Paso.reconstruct(pasoId, procesionId, 0, titularId, null,
                                java.time.Instant.now(), java.time.Instant.now())));

        mockMvc.perform(put(pasosPath())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pasos": [{"position": 0, "titularId": "%s"}]}
                                """.formatted(titularId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasos[0].id").value(pasoId.toString()));
    }

    @Test
    void replacePasosReturns409OnFinalized() throws Exception {
        when(pasoService.replacePasos(eq(hermandadId), eq(procesionId), any()))
                .thenThrow(new IllegalStateException("Plan is already finalized"));

        mockMvc.perform(put(pasosPath())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pasos": [{"position": 0, "titularId": "%s"}]}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isConflict());
    }

    @Test
    void getPasosReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get(pasosPath()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void replacePasosReturns400WhenMissingTitularId() throws Exception {
        mockMvc.perform(put(pasosPath())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pasos": [{"position": 0}]}
                                """))
                .andExpect(status().isBadRequest());
    }
}
