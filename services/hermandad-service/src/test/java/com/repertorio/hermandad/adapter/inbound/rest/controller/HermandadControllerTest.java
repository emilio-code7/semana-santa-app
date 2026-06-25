package com.repertorio.hermandad.adapter.inbound.rest.controller;

import com.repertorio.hermandad.adapter.config.HermandadSecurityService;
import com.repertorio.hermandad.adapter.config.SecurityConfig;
import com.repertorio.hermandad.adapter.config.TestCacheConfig;
import com.repertorio.hermandad.adapter.inbound.rest.dto.AddMemberRequest;
import com.repertorio.hermandad.adapter.inbound.rest.dto.HermandadResponse;
import com.repertorio.hermandad.application.service.HermandadService;
import com.repertorio.hermandad.domain.model.HermandadMember;
import com.repertorio.hermandad.domain.model.HermandadMemberNotFoundException;
import com.repertorio.hermandad.domain.model.HermandadRole;
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
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HermandadController.class)
@Import({TestCacheConfig.class, SecurityConfig.class, HermandadSecurityService.class})
class HermandadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HermandadService hermandadService;

    @MockitoBean
    private HermandadMemberRepository hermandadMemberRepository;

    private final UUID hermandadId = UUID.randomUUID();

    // --- Auth: bootstrap endpoint is open to any authenticated user ---

    @Test
    void createHermandadReturns201ForAuthenticatedUser() throws Exception {
        when(hermandadService.createHermandad(any(), any())).thenReturn(
                new HermandadResponse(hermandadId, "Test", "Sevilla", 2020, null, Instant.now()));

        mockMvc.perform(post("/api/hermandades")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Test","city":"Sevilla","foundedYear":2020}
                                """))
                .andExpect(status().isCreated());
    }

    // --- Auth: admin endpoints reject non-admin ---

    @Test
    void addMemberReturns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(post("/api/hermandades/{id}/members", hermandadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"u1","role":"MUSICIAN"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addMemberReturns403WhenNoMembershipClaim() throws Exception {
        mockMvc.perform(post("/api/hermandades/{id}/members", hermandadId)
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"u1","role":"MUSICIAN"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void addMemberReturns403WhenWrongRole() throws Exception {
        mockMvc.perform(post("/api/hermandades/{id}/members", hermandadId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_MUSICIAN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"u1","role":"MUSICIAN"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void addMemberReturns201WhenAdmin() throws Exception {
        when(hermandadService.addMember(eq(hermandadId), any(AddMemberRequest.class)))
                .thenReturn(new HermandadMember(hermandadId, "u1", HermandadRole.MUSICIAN));

        mockMvc.perform(post("/api/hermandades/{id}/members", hermandadId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"u1","role":"MUSICIAN"}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    void deleteMemberReturns403WhenNoMembershipClaim() throws Exception {
        mockMvc.perform(delete("/api/hermandades/{id}/members/{userId}", hermandadId, "u1")
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteMemberReturns403WhenWrongRole() throws Exception {
        mockMvc.perform(delete("/api/hermandades/{id}/members/{userId}", hermandadId, "u1")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_MUSICIAN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMembersReturns403WhenNoMembershipClaim() throws Exception {
        mockMvc.perform(get("/api/hermandades/{id}/members", hermandadId)
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void changeRoleReturns403WhenNoMembershipClaim() throws Exception {
        mockMvc.perform(patch("/api/hermandades/{id}/members/{userId}/role", hermandadId, "u1")
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"BAND_DIRECTOR"}
                                """))
                .andExpect(status().isForbidden());
    }

    // --- Auth: admin endpoints succeed with admin role ---

    @Test
    void deleteMemberReturns204WhenAdmin() throws Exception {
        doNothing().when(hermandadService).removeMember(hermandadId, "user-123");

        mockMvc.perform(delete("/api/hermandades/{hermandadId}/members/{userId}", hermandadId, "user-123")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteMemberReturns404WhenMemberNotFound() throws Exception {
        var userId = "user-unknown";
        doThrow(new HermandadMemberNotFoundException(hermandadId, userId))
                .when(hermandadService).removeMember(hermandadId, userId);

        mockMvc.perform(delete("/api/hermandades/{hermandadId}/members/{userId}", hermandadId, userId)
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("HERMANDAD_" + hermandadId + "_HERMANDAD_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    // --- Auth: public endpoints don't need auth ---

    @Test
    void getHermandadReturns200WithoutAuth() throws Exception {
        when(hermandadService.findHermandadById(hermandadId))
                .thenReturn(new HermandadResponse(hermandadId, "Test", "Sevilla", 2020, null, Instant.now()));

        mockMvc.perform(get("/api/hermandades/{id}", hermandadId))
                .andExpect(status().isOk());
    }
}
