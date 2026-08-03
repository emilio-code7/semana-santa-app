package com.repertorio.procesion.adapter.inbound.rest.controller;

import com.repertorio.procesion.adapter.config.JwtAuthenticationConverter;
import com.repertorio.procesion.adapter.config.ProcesionSecurityService;
import com.repertorio.procesion.adapter.config.SecurityConfig;
import com.repertorio.procesion.application.service.PasoService;
import com.repertorio.procesion.domain.model.Paso;
import com.repertorio.procesion.domain.model.ProcesionNotFoundException;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasoController.class)
@Import({SecurityConfig.class, ProcesionSecurityService.class})
class PasoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasoService pasoService;

    private final UUID hermandadId = UUID.randomUUID();
    private final UUID procesionId = UUID.randomUUID();
    private final UUID titularId = UUID.randomUUID();

    private Paso buildPaso(int position) {
        return Paso.create(procesionId, position, titularId, null);
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

    // --- GET pasos ---

    @Test
    void getPasosReturns200ForAuthenticatedUser() throws Exception {
        when(pasoService.getPasos(hermandadId, procesionId))
                .thenReturn(List.of(buildPaso(1), buildPaso(2)));

        mockMvc.perform(get("/api/hermandades/{hid}/procesiones/{pid}/pasos", hermandadId, procesionId)
                        .with(memberJwt(hermandadId.toString())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasos").isArray())
                .andExpect(jsonPath("$.pasos.length()").value(2));
    }

    @Test
    void getPasosReturns403ForCrossTenant() throws Exception {
        var otherHermandad = UUID.randomUUID();
        when(pasoService.getPasos(hermandadId, procesionId))
                .thenReturn(List.of(buildPaso(1), buildPaso(2)));

        mockMvc.perform(get("/api/hermandades/{hid}/procesiones/{pid}/pasos", hermandadId, procesionId)
                        .with(memberJwt(otherHermandad.toString())))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPasosReturns403WhenAuthenticatedButNotMember() throws Exception {
        mockMvc.perform(get("/api/hermandades/{hid}/procesiones/{pid}/pasos", hermandadId, procesionId)
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPasosReturns404WhenProcesionNotFound() throws Exception {
        when(pasoService.getPasos(hermandadId, procesionId))
                .thenThrow(new ProcesionNotFoundException(procesionId));

        mockMvc.perform(get("/api/hermandades/{hid}/procesiones/{pid}/pasos", hermandadId, procesionId)
                        .with(memberJwt(hermandadId.toString())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPasosReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/hermandades/{hid}/procesiones/{pid}/pasos", hermandadId, procesionId))
                .andExpect(status().isUnauthorized());
    }

    // --- PUT pasos ---

    @Test
    void replacePasosReturns200ForAuthenticatedUser() throws Exception {
        var items = List.of(
                new PasoService.PasoItem(null, 1, titularId, null),
                new PasoService.PasoItem(null, 2, titularId, "Second")
        );
        when(pasoService.replacePasos(eq(hermandadId), eq(procesionId), any()))
                .thenReturn(List.of(buildPaso(1), buildPaso(2)));

        mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/pasos", hermandadId, procesionId)
                        .with(adminJwt(hermandadId.toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "pasos": [
                                        {"position": 1, "titularId": "%s"},
                                        {"position": 2, "titularId": "%s", "notes": "Second"}
                                    ]
                                }
                                """.formatted(titularId, titularId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasos").isArray());
    }

    @Test
    void replacePasosReturns400ForDuplicatePosition() throws Exception {
        when(pasoService.replacePasos(eq(hermandadId), eq(procesionId), any()))
                .thenThrow(new IllegalArgumentException("Duplicate position in paso list"));

        mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/pasos", hermandadId, procesionId)
                        .with(adminJwt(hermandadId.toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "pasos": [
                                        {"position": 1, "titularId": "%s"},
                                        {"position": 1, "titularId": "%s"}
                                    ]
                                }
                                """.formatted(titularId, titularId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void replacePasosReturns403ForCrossTenant() throws Exception {
        var otherHermandad = UUID.randomUUID();
        when(pasoService.replacePasos(eq(hermandadId), eq(procesionId), any()))
                .thenReturn(List.of(buildPaso(1)));

        mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/pasos", hermandadId, procesionId)
                        .with(adminJwt(otherHermandad.toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pasos": [{"position": 1, "titularId": "%s"}]}
                                """.formatted(titularId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void replacePasosReturns403ForMemberRole() throws Exception {
        mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/pasos", hermandadId, procesionId)
                        .with(memberJwt(hermandadId.toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pasos": [{"position": 1, "titularId": "%s"}]}
                                """.formatted(titularId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void replacePasosReturns404WhenProcesionNotFound() throws Exception {
        when(pasoService.replacePasos(eq(hermandadId), eq(procesionId), any()))
                .thenThrow(new ProcesionNotFoundException(procesionId));

        mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/pasos", hermandadId, procesionId)
                        .with(adminJwt(hermandadId.toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pasos": [{"position": 1, "titularId": "%s"}]}
                                """.formatted(titularId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void replacePasosReturns400ForEmptyList() throws Exception {
        mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/pasos", hermandadId, procesionId)
                        .with(adminJwt(hermandadId.toString()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pasos": []}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void replacePasosReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(put("/api/hermandades/{hid}/procesiones/{pid}/pasos", hermandadId, procesionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pasos": [{"position": 1, "titularId": "%s"}]}
                                """.formatted(titularId)))
                .andExpect(status().isUnauthorized());
    }
}
