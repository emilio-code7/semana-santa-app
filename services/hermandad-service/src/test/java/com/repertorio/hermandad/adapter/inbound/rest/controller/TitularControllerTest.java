package com.repertorio.hermandad.adapter.inbound.rest.controller;

import com.repertorio.hermandad.adapter.config.HermandadSecurityService;
import com.repertorio.hermandad.adapter.config.SecurityConfig;
import com.repertorio.hermandad.adapter.config.TestCacheConfig;
import com.repertorio.hermandad.adapter.inbound.rest.dto.CreateTitularRequest;
import com.repertorio.hermandad.adapter.inbound.rest.dto.TitularResponse;
import com.repertorio.hermandad.adapter.inbound.rest.dto.UpdateTitularRequest;
import com.repertorio.hermandad.application.service.TitularService;
import com.repertorio.hermandad.domain.model.TitularNotFoundException;
import com.repertorio.hermandad.domain.repository.HermandadMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TitularController.class)
@Import({TestCacheConfig.class, SecurityConfig.class, HermandadSecurityService.class})
class TitularControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TitularService titularService;

    @MockitoBean
    private HermandadMemberRepository hermandadMemberRepository;

    private final UUID hermandadId = UUID.randomUUID();
    private final UUID titularId = UUID.randomUUID();

    private TitularResponse sampleResponse() {
        return new TitularResponse(titularId, hermandadId, "Jesus", null, Instant.now(), Instant.now());
    }

    // --- 401 when unauthenticated ---

    @Test
    void createTitularReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/hermandades/{hid}/titulares", hermandadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jesus\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listTitularesReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/hermandades/{hid}/titulares", hermandadId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTitularReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/hermandades/{hid}/titulares/{id}", hermandadId, titularId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateTitularReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(put("/api/hermandades/{hid}/titulares/{id}", hermandadId, titularId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isUnauthorized());
    }

    // --- Write endpoints: 403 when not admin/capataz ---

    @Test
    void createTitularReturns403WhenNotCapatazOrAdmin() throws Exception {
        mockMvc.perform(post("/api/hermandades/{hid}/titulares", hermandadId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_MUSICIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jesus\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateTitularReturns403WhenNotCapatazOrAdmin() throws Exception {
        mockMvc.perform(put("/api/hermandades/{hid}/titulares/{id}", hermandadId, titularId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_MUSICIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isForbidden());
    }

    // --- Read endpoints: 403 when not member of this hermandad ---

    @Test
    void listTitularesReturns403WhenNotMember() throws Exception {
        mockMvc.perform(get("/api/hermandades/{hid}/titulares", hermandadId)
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTitularReturns403WhenNotMember() throws Exception {
        mockMvc.perform(get("/api/hermandades/{hid}/titulares/{id}", hermandadId, titularId)
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    // --- Create succeeds with CAPATAZ ---

    @Test
    void createTitularReturns201WhenCapataz() throws Exception {
        when(titularService.createTitular(eq(hermandadId), any(CreateTitularRequest.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/hermandades/{hid}/titulares", hermandadId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_CAPATAZ")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jesus\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    void createTitularReturns201WhenAdmin() throws Exception {
        when(titularService.createTitular(eq(hermandadId), any(CreateTitularRequest.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(post("/api/hermandades/{hid}/titulares", hermandadId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jesus\"}"))
                .andExpect(status().isCreated());
    }

    // --- Read succeeds with any member role ---

    @Test
    void listTitularesReturns200WhenMember() throws Exception {
        when(titularService.listTitulares(hermandadId)).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/hermandades/{hid}/titulares", hermandadId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_MUSICIAN"))))
                .andExpect(status().isOk());
    }

    @Test
    void getTitularReturns200WhenMember() throws Exception {
        when(titularService.getTitular(hermandadId, titularId)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/hermandades/{hid}/titulares/{id}", hermandadId, titularId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_BAND_DIRECTOR"))))
                .andExpect(status().isOk());
    }

    // --- Update succeeds with CAPATAZ ---

    @Test
    void updateTitularReturns200WhenCapataz() throws Exception {
        when(titularService.updateTitular(eq(hermandadId), eq(titularId), any(UpdateTitularRequest.class)))
                .thenReturn(sampleResponse());

        mockMvc.perform(put("/api/hermandades/{hid}/titulares/{id}", hermandadId, titularId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_CAPATAZ")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isOk());
    }

    // --- 404 handling ---

    @Test
    void getTitularReturns404WhenNotFound() throws Exception {
        when(titularService.getTitular(hermandadId, titularId))
                .thenThrow(new TitularNotFoundException(titularId));

        mockMvc.perform(get("/api/hermandades/{hid}/titulares/{id}", hermandadId, titularId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_CAPATAZ"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTitularReturns404WhenNotFound() throws Exception {
        when(titularService.updateTitular(eq(hermandadId), eq(titularId), any(UpdateTitularRequest.class)))
                .thenThrow(new TitularNotFoundException(titularId));

        mockMvc.perform(put("/api/hermandades/{hid}/titulares/{id}", hermandadId, titularId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_CAPATAZ")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Updated\"}"))
                .andExpect(status().isNotFound());
    }
}
