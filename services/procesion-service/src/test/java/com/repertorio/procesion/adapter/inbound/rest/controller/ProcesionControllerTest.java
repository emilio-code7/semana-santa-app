package com.repertorio.procesion.adapter.inbound.rest.controller;

import com.repertorio.procesion.adapter.config.SecurityConfig;
import com.repertorio.procesion.application.service.ProcesionService;
import com.repertorio.procesion.domain.model.Procesion;
import com.repertorio.procesion.domain.model.ProcesionEstado;
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
        return Procesion.crear(hermandadId, LocalDate.of(2026, 4, 13), LocalTime.of(18, 0));
    }

    // --- CREATE ---

    @Test
    void crearProcesionReturns201ForAuthenticatedUser() throws Exception {
        when(procesionService.crearProcesion(any(), any(), any())).thenReturn(buildProcesion());

        mockMvc.perform(post("/api/procesiones")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hermandadId":"%s","fecha":"2026-04-13","hora":"18:00:00"}
                                """.formatted(hermandadId)))
                .andExpect(status().isCreated());
    }

    @Test
    void crearProcesionReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/procesiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"hermandadId":"%s","fecha":"2026-04-13","hora":"18:00:00"}
                                """.formatted(hermandadId)))
                .andExpect(status().isUnauthorized());
    }

    // --- GET BY ID ---

    @Test
    void obtenerProcesionReturns200ForAuthenticatedUser() throws Exception {
        when(procesionService.obtenerProcesion(procesionId)).thenReturn(buildProcesion());

        mockMvc.perform(get("/api/procesiones/{id}", procesionId)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hermandadId").value(hermandadId.toString()));
    }

    @Test
    void obtenerProcesionReturns404WhenNotFound() throws Exception {
        when(procesionService.obtenerProcesion(procesionId))
                .thenThrow(new ProcesionNotFoundException(procesionId));

        mockMvc.perform(get("/api/procesiones/{id}", procesionId)
                        .with(jwt()))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("no encontrada")));
    }

    @Test
    void obtenerProcesionReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/procesiones/{id}", procesionId))
                .andExpect(status().isUnauthorized());
    }

    // --- LIST BY HERMANDAD ---

    @Test
    void listarPorHermandadReturns200ForAuthenticatedUser() throws Exception {
        Page<Procesion> page = new PageImpl<>(List.of(buildProcesion()));
        when(procesionService.listarPorHermandad(eq(hermandadId), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/procesiones")
                        .param("hermandadId", hermandadId.toString())
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void listarPorHermandadReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/procesiones")
                        .param("hermandadId", hermandadId.toString()))
                .andExpect(status().isUnauthorized());
    }

    // --- CHANGE ESTADO ---

    @Test
    void cambiarEstadoReturns200ForAuthenticatedUser() throws Exception {
        when(procesionService.cambiarEstado(procesionId, ProcesionEstado.EN_CURSO))
                .thenReturn(buildProcesion());

        mockMvc.perform(patch("/api/procesiones/{id}/estado", procesionId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nuevoEstado":"EN_CURSO"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void cambiarEstadoReturns400OnInvalidTransition() throws Exception {
        when(procesionService.cambiarEstado(procesionId, ProcesionEstado.EN_CURSO))
                .thenThrow(new IllegalArgumentException(
                        "No se puede cambiar estado de PLANIFICADA a EN_CURSO"));

        mockMvc.perform(patch("/api/procesiones/{id}/estado", procesionId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nuevoEstado":"EN_CURSO"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cambiarEstadoReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(patch("/api/procesiones/{id}/estado", procesionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nuevoEstado":"EN_CURSO"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // --- DELETE ---

    @Test
    void eliminarProcesionReturns204ForAuthenticatedUser() throws Exception {
        mockMvc.perform(delete("/api/procesiones/{id}", procesionId)
                        .with(jwt()))
                .andExpect(status().isNoContent());
    }

    @Test
    void eliminarProcesionReturns404WhenNotFound() throws Exception {
        doThrow(new ProcesionNotFoundException(procesionId))
                .when(procesionService).eliminarProcesion(procesionId);

        mockMvc.perform(delete("/api/procesiones/{id}", procesionId)
                        .with(jwt()))
                .andExpect(status().isNotFound());
    }

    @Test
    void eliminarProcesionReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(delete("/api/procesiones/{id}", procesionId))
                .andExpect(status().isUnauthorized());
    }
}
