package com.repertorio.procesion.adapter.inbound.rest.controller;

import com.repertorio.procesion.adapter.config.SecurityConfig;
import com.repertorio.procesion.application.service.ProcesionService;
import com.repertorio.procesion.domain.model.Procesion;
import com.repertorio.procesion.domain.model.ProcesionStatus;
import com.repertorio.procesion.domain.model.ProcesionNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProcesionController.class)
@Import(SecurityConfig.class)
class ProcesionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProcesionService procesionService;

    private final UUID procesionId = UUID.randomUUID();
    private final UUID hermandadId = UUID.randomUUID();

    private Procesion buildProcesion() {
        return Procesion.create(hermandadId, LocalDate.of(2026, 4, 13), LocalTime.of(18, 0));
    }

    // --- CREATE ---

    @Test
    void createProcesionReturns201ForAuthenticatedUser() throws Exception {
        when(procesionService.createProcesion(any(), any(), any())).thenReturn(buildProcesion());

        mockMvc.perform(post("/api/procesiones")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hermandadId":"%s","date":"2026-04-13","time":"18:00:00"}
                                """.formatted(hermandadId)))
                .andExpect(status().isCreated());
    }

    @Test
    void createProcesionReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/procesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hermandadId":"%s","date":"2026-04-13","time":"18:00:00"}
                                """.formatted(hermandadId)))
                .andExpect(status().isUnauthorized());
    }

    // --- GET BY ID ---

    @Test
    void getProcesionReturns200ForAuthenticatedUser() throws Exception {
        when(procesionService.getProcesion(procesionId)).thenReturn(buildProcesion());

        mockMvc.perform(get("/api/procesiones/{id}", procesionId)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hermandadId").value(hermandadId.toString()));
    }

    @Test
    void getProcesionReturns404WhenNotFound() throws Exception {
        when(procesionService.getProcesion(procesionId))
                .thenThrow(new ProcesionNotFoundException(procesionId));

        mockMvc.perform(get("/api/procesiones/{id}", procesionId)
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("not found")));
    }

    @Test
    void getProcesionReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/procesiones/{id}", procesionId))
                .andExpect(status().isUnauthorized());
    }

    // --- LIST BY HERMANDAD ---

    @Test
    void listByHermandadReturns200ForAuthenticatedUser() throws Exception {
        Page<Procesion> page = new PageImpl<>(List.of(buildProcesion()));
        when(procesionService.listByHermandad(eq(hermandadId), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/procesiones")
                        .param("hermandadId", hermandadId.toString())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void listByHermandadReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/procesiones")
                        .param("hermandadId", hermandadId.toString()))
                .andExpect(status().isUnauthorized());
    }

    // --- CHANGE STATUS ---

    @Test
    void changeStatusReturns200ForAuthenticatedUser() throws Exception {
        when(procesionService.changeStatus(procesionId, ProcesionStatus.IN_PROGRESS))
                .thenReturn(buildProcesion());

        mockMvc.perform(patch("/api/procesiones/{id}/status", procesionId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newStatus":"IN_PROGRESS"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void changeStatusReturns400OnInvalidTransition() throws Exception {
        when(procesionService.changeStatus(procesionId, ProcesionStatus.IN_PROGRESS))
                .thenThrow(new IllegalArgumentException(
                        "Cannot transition from PLANNED to IN_PROGRESS"));

        mockMvc.perform(patch("/api/procesiones/{id}/status", procesionId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newStatus":"IN_PROGRESS"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void changeStatusReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(patch("/api/procesiones/{id}/status", procesionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newStatus":"IN_PROGRESS"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // --- DELETE ---

    @Test
    void deleteProcesionReturns204ForAuthenticatedUser() throws Exception {
        mockMvc.perform(delete("/api/procesiones/{id}", procesionId)
                        .with(jwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteProcesionReturns404WhenNotFound() throws Exception {
        doThrow(new ProcesionNotFoundException(procesionId))
                .when(procesionService).deleteProcesion(procesionId);

        mockMvc.perform(delete("/api/procesiones/{id}", procesionId)
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    // --- FINALIZE PLAN ---

    @Test
    void finalizePlanReturns200ForAuthenticatedUser() throws Exception {
        when(procesionService.finalizePlan(hermandadId, procesionId)).thenReturn(buildProcesion());

        mockMvc.perform(post("/api/procesiones/{id}/finalize-plan", procesionId)
                        .param("hermandadId", hermandadId.toString())
                        .with(jwt()))
                .andExpect(status().isOk());
    }

    @Test
    void finalizePlanReturns404WhenNotFound() throws Exception {
        when(procesionService.finalizePlan(hermandadId, procesionId))
                .thenThrow(new ProcesionNotFoundException(procesionId));

        mockMvc.perform(post("/api/procesiones/{id}/finalize-plan", procesionId)
                        .param("hermandadId", hermandadId.toString())
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void finalizePlanReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/procesiones/{id}/finalize-plan", procesionId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteProcesionReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(delete("/api/procesiones/{id}", procesionId))
                .andExpect(status().isUnauthorized());
    }

    // --- UNKNOWN PATH ---

    @Test
    void getUnknownPathReturns404WithApiError() throws Exception {
        mockMvc.perform(get("/api/not-a-real-endpoint")
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
