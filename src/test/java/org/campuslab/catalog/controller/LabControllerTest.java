package org.campuslab.catalog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.campuslab.catalog.dto.LabRequestDTO;
import org.campuslab.catalog.dto.LabResponseDTO;
import org.campuslab.catalog.security.SecurityConfig;
import org.campuslab.catalog.service.LabService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LabController.class)
@Import(SecurityConfig.class)
class LabControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LabService labService;

    @MockBean
    private JwtDecoder jwtDecoder;

    private final LabResponseDTO labResponse =
            new LabResponseDTO(1L, "Lab Química", "Edificio A, piso 1, sala 101", 30, null);

    @Test
    void getLabs_sinToken_deberiaDevolver401() throws Exception {
        mockMvc.perform(get("/api/catalog/labs"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(labService);
    }

    @Test
    void getLabs_autenticado_deberiaDevolver200YLista() throws Exception {
        when(labService.findAll()).thenReturn(List.of(labResponse));

        mockMvc.perform(get("/api/catalog/labs").with(user("estudiante").roles("ESTUDIANTE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Lab Química"))
                .andExpect(jsonPath("$[0].capacity").value(30));
    }

    @Test
    void crearLab_conRolAdmin_deberiaDevolver200() throws Exception {
        when(labService.create(any(LabRequestDTO.class))).thenReturn(labResponse);

        String body = """
                {
                  "name": "Lab Química",
                  "location": "Edificio A, piso 1, sala 101",
                  "capacity": 30
                }
                """;

        mockMvc.perform(post("/api/catalog/labs")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(labService).create(any(LabRequestDTO.class));
    }

    @Test
    void crearLab_conRolEstudiante_deberiaDevolver403() throws Exception {
        mockMvc.perform(post("/api/catalog/labs")
                        .with(user("estudiante").roles("ESTUDIANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"X\", \"location\": \"Y\", \"capacity\": 10}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(labService);
    }
}
