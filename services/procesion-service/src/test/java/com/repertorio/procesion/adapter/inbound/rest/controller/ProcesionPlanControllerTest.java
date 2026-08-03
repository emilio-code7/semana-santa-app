package com.repertorio.procesion.adapter.inbound.rest.controller;

import com.repertorio.procesion.adapter.config.JwtAuthenticationConverter;
import com.repertorio.procesion.adapter.config.SecurityConfig;
import com.repertorio.procesion.application.service.PasoService;
import com.repertorio.procesion.application.service.ProcesionService;
import com.repertorio.procesion.domain.model.Paso;
import com.repertorio.procesion.domain.model.Procesion;
import com.repertorio.procesion.domain.model.RouteSection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProcesionPlanController.class)
@Import(SecurityConfig.class)
class ProcesionPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProcesionService procesionService;

    @MockitoBean
    private PasoService pasoService;

    private final UUID hermandadId = UUID.randomUUID();
    private final UUID procesionId = UUID.randomUUID();

    private String routePath() {
        return "/api/hermandades/{hid}/procesiones/{pid}/route"
                .replace("{hid}", hermandadId.toString())
                .replace("{pid}", procesionId.toString());
    }

    private String finalizePath() {
        return "/api/hermandades/{hid}/procesiones/{pid}/plan/finalize"
                .replace("{hid}", hermandadId.toString())
                .replace("{pid}", procesionId.toString());
    }

    private JwtRequestPostProcessor memberJwt(String hermandadIdInClaim) {
        return memberJwt(hermandadIdInClaim, "MUSICIAN");
    }

    private JwtRequestPostProcessor memberJwt(String hermandadIdInClaim, String role) {
        var memberships = "[{\"hermandadId\":\"" + hermandadIdInClaim + "\",\"role\":\"" + role + "\"}]";
        return jwt().jwt(b -> b.subject("user-1").claim("hermandad_memberships", memberships))
                .authorities(j -> new JwtAuthenticationConverter().convert(j).getAuthorities());
    }

    private JwtRequestPostProcessor adminJwt(String hermandadIdInClaim) {
        return memberJwt(hermandadIdInClaim, "HERMANDAD_ADMIN");
    }

    // --- Route Sections ---

    @Test
    void getRouteSectionsReturns200() throws Exception {
        when(procesionService.getRouteSections(hermandadId, procesionId))
                .thenReturn(List.of(
                        RouteSection.create(procesionId, "Section A", 0, null),
                        RouteSection.create(procesionId, "Section B", 1, "notes")));

        mockMvc.perform(get(routePath()).with(memberJwt(hermandadId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections").isArray())
                .andExpect(jsonPath("$.sections.length()").value(2))
                .andExpect(jsonPath("$.sections[0].name").value("Section A"));
    }

    @Test
    void getRouteSectionsReturns403OnCrossTenant() throws Exception {
        var otherHermandad = UUID.randomUUID();
        when(procesionService.getRouteSections(hermandadId, procesionId))
                .thenReturn(List.of(RouteSection.create(procesionId, "Section A", 0, null)));

        mockMvc.perform(get(routePath()).with(memberJwt(otherHermandad.toString())))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRouteSectionsReturns403WhenAuthenticatedButNotMember() throws Exception {
        mockMvc.perform(get(routePath()).with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRouteSectionsReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get(routePath()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void replaceRouteSectionsReturns200() throws Exception {
        var sectionId = UUID.randomUUID();
        when(procesionService.replaceRouteSections(eq(hermandadId), eq(procesionId), any()))
                .thenReturn(List.of(
                        RouteSection.reconstruct(sectionId, procesionId, "Section A", 0, null,
                                java.time.Instant.now(), java.time.Instant.now())));

        mockMvc.perform(put(routePath())
                        .with(adminJwt(hermandadId.toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sections": [{"name": "Section A", "position": 0}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections[0].id").value(sectionId.toString()));
    }

    @Test
    void replaceRouteSectionsReturns409OnFinalized() throws Exception {
        when(procesionService.replaceRouteSections(eq(hermandadId), eq(procesionId), any()))
                .thenThrow(new IllegalStateException("Plan is already finalized"));

        mockMvc.perform(put(routePath())
                        .with(adminJwt(hermandadId.toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sections": [{"name": "Section A", "position": 0}]}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void replaceRouteSectionsReturns400WhenNameBlank() throws Exception {
        mockMvc.perform(put(routePath())
                        .with(adminJwt(hermandadId.toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sections": [{"name": "", "position": 0}]}
                                """))
                .andExpect(status().isBadRequest());
    }

    // --- Finalization ---

    @Test
    void finalizePlanReturns200() throws Exception {
        var procesion = Procesion.create(hermandadId, LocalDate.now(), LocalTime.now());
        procesion.finalizePlan();
        when(procesionService.finalizePlan(hermandadId, procesionId)).thenReturn(procesion);
        when(pasoService.getPasos(hermandadId, procesionId))
                .thenReturn(List.of(Paso.create(procesionId, 0, UUID.randomUUID(), null)));
        when(procesionService.getRouteSections(hermandadId, procesionId))
                .thenReturn(List.of(RouteSection.create(procesionId, "Section", 0, null)));

        mockMvc.perform(post(finalizePath()).with(adminJwt(hermandadId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planFinalizedAt").isNotEmpty())
                .andExpect(jsonPath("$.pasoCount").value(1))
                .andExpect(jsonPath("$.routeSectionCount").value(1));
    }

    @Test
    void finalizePlanReturns403OnCrossTenant() throws Exception {
        var otherHermandad = UUID.randomUUID();
        var procesion = Procesion.create(hermandadId, LocalDate.now(), LocalTime.now());
        procesion.finalizePlan();
        when(procesionService.finalizePlan(hermandadId, procesionId)).thenReturn(procesion);

        mockMvc.perform(post(finalizePath()).with(adminJwt(otherHermandad.toString())))
                .andExpect(status().isForbidden());
    }

    @Test
    void finalizePlanReturns400WhenNoPasos() throws Exception {
        when(procesionService.finalizePlan(hermandadId, procesionId))
                .thenThrow(new IllegalStateException("at least one paso"));

        mockMvc.perform(post(finalizePath()).with(adminJwt(hermandadId.toString())))
                .andExpect(status().isConflict());
    }

    @Test
    void finalizePlanReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post(finalizePath()))
                .andExpect(status().isUnauthorized());
    }
}
