package com.repertorio.marcha.adapter.inbound.rest.controller;

import com.repertorio.marcha.adapter.config.security.SecurityConfig;
import com.repertorio.marcha.application.service.MarchaService;
import com.repertorio.marcha.domain.model.BandType;
import com.repertorio.marcha.domain.model.Marcha;
import com.repertorio.marcha.domain.model.MarchaNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MarchaController.class)
@Import(SecurityConfig.class)
class MarchaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MarchaService marchaService;

    private final UUID marchaId = UUID.randomUUID();

    private Marcha buildMarcha() {
        var m = Marcha.create("Amarguras", "Manuel López Farfán", BandType.BANDA_PALIO, 420, 1919, null);
        // use reflection to set id for predictable test
        return Marcha.reconstruct(marchaId, m.getTitle(), m.getComposer(), m.getBandType(),
                m.getDurationSeconds(), m.getCompositionYear(), m.getYoutubeUrl(),
                m.getCreatedAt(), m.getUpdatedAt());
    }

    @Test
    void createMarchaReturns201() throws Exception {
        var marcha = buildMarcha();
        when(marchaService.createMarcha(anyString(), anyString(), any(), anyInt(), any(), any())).thenReturn(marcha);

        mockMvc.perform(post("/api/marchas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Amarguras","composer":"Manuel López Farfán",\
                                "bandType":"BANDA_PALIO","durationSeconds":420,"compositionYear":1919}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(marchaId.toString()));
    }

    @Test
    void getMarchaReturns200() throws Exception {
        when(marchaService.getMarcha(marchaId)).thenReturn(Optional.of(buildMarcha()));

        mockMvc.perform(get("/api/marchas/{id}", marchaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Amarguras"));
    }

    @Test
    void getMarchaReturns404WhenNotFound() throws Exception {
        when(marchaService.getMarcha(marchaId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/marchas/{id}", marchaId))
                .andExpect(status().isNotFound());
    }

    @Test
    void listMarchasReturns200() throws Exception {
        when(marchaService.listMarchas()).thenReturn(List.of(buildMarcha()));

        mockMvc.perform(get("/api/marchas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void updateMarchaReturns200() throws Exception {
        var marcha = buildMarcha();
        when(marchaService.updateMarcha(eq(marchaId), anyString(), anyString(), any(), anyInt(), any(), any()))
                .thenReturn(marcha);

        mockMvc.perform(put("/api/marchas/{id}", marchaId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Updated","composer":"NewComposer",\
                                "bandType":"AGRUPACION_MUSICAL","durationSeconds":300}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Amarguras"));
    }

    @Test
    void deleteMarchaReturns204() throws Exception {
        doNothing().when(marchaService).deleteMarcha(marchaId);

        mockMvc.perform(delete("/api/marchas/{id}", marchaId))
                .andExpect(status().isNoContent());
    }
}
