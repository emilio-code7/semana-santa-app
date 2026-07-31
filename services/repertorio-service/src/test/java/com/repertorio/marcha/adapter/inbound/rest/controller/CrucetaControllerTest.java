package com.repertorio.marcha.adapter.inbound.rest.controller;

import com.repertorio.marcha.adapter.config.security.RepertorioSecurityService;
import com.repertorio.marcha.adapter.config.security.SecurityConfig;
import com.repertorio.marcha.application.service.CrucetaService;
import com.repertorio.marcha.domain.model.Cruceta;
import com.repertorio.marcha.domain.model.CrucetaItem;
import com.repertorio.marcha.domain.model.CrucetaNotFoundException;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CrucetaController.class)
@Import({SecurityConfig.class, RepertorioSecurityService.class})
class CrucetaControllerTest {

    @Autowired
    private MockMvc mockMvc;

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

    @Test
    void getCrucetaReturns200() throws Exception {
        var cruceta = buildCruceta();
        when(crucetaService.getCruceta(pasoId)).thenReturn(cruceta);

        mockMvc.perform(get("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta",
                        hermandadId, procesionId, pasoId)
                        .with(jwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasoId").value(pasoId.toString()));
    }

    @Test
    void getCrucetaReturns404WhenNotFound() throws Exception {
        when(crucetaService.getCruceta(pasoId))
                .thenThrow(new CrucetaNotFoundException(pasoId));

        mockMvc.perform(get("/api/hermandades/{hid}/procesiones/{pid}/pasos/{pasoId}/cruceta",
                        hermandadId, procesionId, pasoId)
                        .with(jwt()))
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
}
