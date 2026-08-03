package com.repertorio.marcha.adapter.inbound.rest.controller;

import com.repertorio.marcha.adapter.config.security.JwtAuthenticationConverter;
import com.repertorio.marcha.adapter.config.security.RepertorioSecurityService;
import com.repertorio.marcha.adapter.config.security.SecurityConfig;
import com.repertorio.marcha.adapter.inbound.rest.dto.AdvanceCurrentRequest;
import com.repertorio.marcha.adapter.inbound.rest.dto.RunSheetResponse;
import com.repertorio.marcha.application.service.CrucetaService;
import com.repertorio.marcha.domain.model.Cruceta;
import com.repertorio.marcha.domain.model.CrucetaItem;
import com.repertorio.marcha.domain.model.CrucetaNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CrucetaController.class)
@Import({SecurityConfig.class, RepertorioSecurityService.class})
class CrucetaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CrucetaService crucetaService;

    private final UUID hermandadId = UUID.randomUUID();
    private final UUID procesionId = UUID.randomUUID();
    private final UUID pasoId = UUID.randomUUID();
    private final UUID marchaId = UUID.randomUUID();
    private final UUID routeSectionId = UUID.randomUUID();

    private Cruceta buildCruceta() {
        var items = List.of(new CrucetaItem(marchaId, routeSectionId, 1, "Opening"));
        return new Cruceta(pasoId, items);
    }

    private JwtRequestPostProcessor adminJwt(String hermandadIdInClaim) {
        return adminJwt(hermandadIdInClaim, "HERMANDAD_ADMIN");
    }

    private JwtRequestPostProcessor adminJwt(String hermandadIdInClaim, String role) {
        var memberships = "[{\"hermandadId\":\"" + hermandadIdInClaim + "\",\"role\":\"" + role + "\"}]";
        return jwt().jwt(b -> b.subject("user-1").claim("hermandad_memberships", memberships))
                .authorities(j -> new JwtAuthenticationConverter().convert(j).getAuthorities());
    }

    @Test
    void getCrucetaReturns200() throws Exception {
        var cruceta = buildCruceta();
        when(crucetaService.getCruceta(pasoId)).thenReturn(cruceta);

        mockMvc.perform(get("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta",
                        hermandadId, procesionId, pasoId)
                        .with(adminJwt(hermandadId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasoId").value(pasoId.toString()));
    }

    @Test
    void getCrucetaReturns403WhenAdminOfDifferentHermandad() throws Exception {
        var otherHermandadId = UUID.randomUUID();
        when(crucetaService.getCruceta(pasoId)).thenReturn(buildCruceta());
        mockMvc.perform(get("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta",
                        hermandadId, procesionId, pasoId)
                        .with(adminJwt(otherHermandadId.toString())))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCrucetaReturns404WhenNotFound() throws Exception {
        when(crucetaService.getCruceta(pasoId))
                .thenThrow(new CrucetaNotFoundException(pasoId));

        mockMvc.perform(get("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta",
                        hermandadId, procesionId, pasoId)
                        .with(adminJwt(hermandadId.toString())))
                .andExpect(status().isNotFound());
    }

    @Test
    void defineCrucetaReturns200ForAdmin() throws Exception {
        var cruceta = buildCruceta();
        when(crucetaService.defineCruceta(eq(pasoId), any())).thenReturn(cruceta);

        mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta",
                        hermandadId, procesionId, pasoId)
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                "HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"marchaId":"%s","routeSectionId":"%s","sequenceWithinSection":1,"notes":"Opening"}]}
                                """.formatted(marchaId, routeSectionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasoId").value(pasoId.toString()));
    }

    @Test
    void defineCrucetaReturns200ForAdminClaim() throws Exception {
        var cruceta = buildCruceta();
        when(crucetaService.defineCruceta(eq(pasoId), any())).thenReturn(cruceta);

        mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta",
                        hermandadId, procesionId, pasoId)
                        .with(adminJwt(hermandadId.toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"marchaId":"%s","routeSectionId":"%s","sequenceWithinSection":1,"notes":"Opening"}]}
                                """.formatted(marchaId, routeSectionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasoId").value(pasoId.toString()));
    }

    @Test
    void defineCrucetaReturns403WhenClaimHasNoAdminRole() throws Exception {
        mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta",
                        hermandadId, procesionId, pasoId)
                        .with(adminJwt(hermandadId.toString(), "MUSICIAN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"marchaId":"%s","routeSectionId":"%s","sequenceWithinSection":1}]}
                                """.formatted(marchaId, routeSectionId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void defineCrucetaReturns403WhenAdminOfDifferentHermandad() throws Exception {
        var otherHermandadId = UUID.randomUUID();
        mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta",
                        hermandadId, procesionId, pasoId)
                        .with(adminJwt(otherHermandadId.toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"marchaId":"%s","routeSectionId":"%s","sequenceWithinSection":1}]}
                                """.formatted(marchaId, routeSectionId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void defineCrucetaReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta",
                        hermandadId, procesionId, pasoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"marchaId":"%s","routeSectionId":"%s","sequenceWithinSection":1}]}
                                """.formatted(marchaId, routeSectionId)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void defineCrucetaReturns403WhenNotAdmin() throws Exception {
        mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta",
                        hermandadId, procesionId, pasoId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"items":[{"marchaId":"%s","routeSectionId":"%s","sequenceWithinSection":1}]}
                                """.formatted(marchaId, routeSectionId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getRunSheetReturns200() throws Exception {
        var runSheet = new RunSheetResponse(pasoId, List.of());
        when(crucetaService.getRunSheet(procesionId, pasoId)).thenReturn(runSheet);

        mockMvc.perform(get("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta/run-sheet",
                        hermandadId, procesionId, pasoId)
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                "HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasoId").value(pasoId.toString()));
    }

    @Test
    void getRunSheetReturns403WhenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta/run-sheet",
                        hermandadId, procesionId, pasoId)
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void advanceCurrentReturns200() throws Exception {
        var runSheet = new RunSheetResponse(pasoId, List.of());
        when(crucetaService.advanceCurrent(eq(procesionId), eq(pasoId), eq(routeSectionId), eq(null)))
                .thenReturn(runSheet);

        var request = new AdvanceCurrentRequest(routeSectionId, null);
        mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta/current",
                        hermandadId, procesionId, pasoId)
                        .with(jwt().authorities(new SimpleGrantedAuthority(
                                "HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasoId").value(pasoId.toString()));
    }

    @Test
    void advanceCurrentReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta/current",
                        hermandadId, procesionId, pasoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"routeSectionId":"%s"}
                                """.formatted(routeSectionId)))
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
