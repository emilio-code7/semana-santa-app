package com.repertorio.hermandad.adapter.inbound.rest.controller;

import com.repertorio.hermandad.adapter.config.TestCacheConfig;
import com.repertorio.hermandad.application.service.HermandadService;
import com.repertorio.hermandad.domain.model.HermandadMemberNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HermandadController.class)
@Import(TestCacheConfig.class)
@WithMockUser
class HermandadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HermandadService hermandadService;

    @Test
    void deleteMemberReturns204() throws Exception {
        var hermandadId = UUID.randomUUID();
        var userId = "user-123";

        doNothing().when(hermandadService).removeMember(hermandadId, userId);

        mockMvc.perform(delete("/api/hermandades/{hermandadId}/members/{userId}", hermandadId, userId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteMemberReturns404WhenMemberNotFound() throws Exception {
        var hermandadId = UUID.randomUUID();
        var userId = "user-unknown";

        doThrow(new HermandadMemberNotFoundException(hermandadId, userId))
                .when(hermandadService).removeMember(hermandadId, userId);

        mockMvc.perform(delete("/api/hermandades/{hermandadId}/members/{userId}", hermandadId, userId)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}
