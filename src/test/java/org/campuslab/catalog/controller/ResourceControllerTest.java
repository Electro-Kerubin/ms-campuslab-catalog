package org.campuslab.catalog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.campuslab.catalog.dto.BookingStockDTO;
import org.campuslab.catalog.dto.ResourceRequestDTO;
import org.campuslab.catalog.dto.ResourceResponseDTO;
import org.campuslab.catalog.dto.StockUpdateDTO;
import org.campuslab.catalog.entity.ResourceStatus;
import org.campuslab.catalog.entity.ResourceType;
import org.campuslab.catalog.security.SecurityConfig;
import org.campuslab.catalog.service.ResourceService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ResourceController.class)
@Import(SecurityConfig.class)
class ResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ResourceService resourceService;

    @MockBean
    private JwtDecoder jwtDecoder;

    private final ResourceResponseDTO resourceResponse = new ResourceResponseDTO(
            10L, 1L, "Lab Química", 2L, "Equipos de laboratorio", "Microscopio",
            ResourceType.EQUIPO, ResourceStatus.DISPONIBLE,
            5, 3, 1, "Olympus", "CX23", "SN-001", null, null, null);

    @Test
    void getResources_autenticado_conFiltroLabId_deberiaDevolver200() throws Exception {
        when(resourceService.findAll(5L)).thenReturn(List.of(resourceResponse));

        mockMvc.perform(get("/api/catalog/resources")
                        .queryParam("labId", "5")
                        .with(user("tecnico").roles("TECNICO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Microscopio"))
                .andExpect(jsonPath("$[0].quantityAvailable").value(3));

        verify(resourceService).findAll(5L);
    }

    @Test
    void crearResource_conRolAdmin_deberiaDevolver201() throws Exception {
        when(resourceService.create(any(ResourceRequestDTO.class))).thenReturn(resourceResponse);

        String body = """
                {
                  "labId": 1,
                  "categoryId": 2,
                  "name": "Microscopio",
                  "resourceType": "EQUIPO",
                  "quantityTotal": 5,
                  "reorderThreshold": 1,
                  "equipment": { "brand": "Olympus", "model": "CX23", "serialNumber": "SN-001" }
                }
                """;

        mockMvc.perform(post("/api/catalog/resources")
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serialNumber").value("SN-001"));

        ArgumentCaptor<ResourceRequestDTO> capturado = ArgumentCaptor.forClass(ResourceRequestDTO.class);
        verify(resourceService).create(capturado.capture());
        org.assertj.core.api.Assertions.assertThat(capturado.getValue().equipment().serialNumber())
                .isEqualTo("SN-001");
    }

    @Test
    void ajustarStock_conRolAdmin_deberiaPasarDeltaYBookingId() throws Exception {
        when(resourceService.adjustStock(eq(10L), any(StockUpdateDTO.class))).thenReturn(resourceResponse);

        String body = """
                { "delta": -2, "referenceBookingId": 77, "note": "Reserva aprobada" }
                """;

        mockMvc.perform(put("/api/catalog/resources/{id}/stock", 10L)
                        .with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        ArgumentCaptor<StockUpdateDTO> capturado = ArgumentCaptor.forClass(StockUpdateDTO.class);
        verify(resourceService).adjustStock(eq(10L), capturado.capture());
        org.assertj.core.api.Assertions.assertThat(capturado.getValue().delta()).isEqualTo(-2);
        org.assertj.core.api.Assertions.assertThat(capturado.getValue().referenceBookingId()).isEqualTo(77L);
    }

    @Test
    void crearResource_conRolEstudiante_deberiaDevolver403() throws Exception {
        mockMvc.perform(post("/api/catalog/resources")
                        .with(user("estudiante").roles("ESTUDIANTE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(resourceService);
    }

    // ---------- Stock por reserva (lo llama ms-bookings con el JWT del técnico/admin que aprueba) ----------

    @Test
    void tomarStockDeReserva_conRolTecnico_deberiaDevolver200() throws Exception {
        when(resourceService.holdStockForBooking(10L, 77L))
                .thenReturn(new BookingStockDTO(10L, 77L, true, true, 2));

        mockMvc.perform(post("/api/catalog/resources/{id}/booking-stock/{bookingId}", 10L, 77L)
                        .with(user("tecnico").roles("TECNICO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changed").value(true))
                .andExpect(jsonPath("$.quantityAvailable").value(2));

        verify(resourceService).holdStockForBooking(10L, 77L);
    }

    @Test
    void liberarStockDeReserva_conRolTecnico_deberiaDevolver200() throws Exception {
        when(resourceService.releaseStockForBooking(10L, 77L))
                .thenReturn(new BookingStockDTO(10L, 77L, true, true, 3));

        mockMvc.perform(delete("/api/catalog/resources/{id}/booking-stock/{bookingId}", 10L, 77L)
                        .with(user("tecnico").roles("TECNICO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityAvailable").value(3));

        verify(resourceService).releaseStockForBooking(10L, 77L);
    }

    @Test
    void tomarYLiberarStockDeReserva_conRolAdmin_deberiaDevolver200() throws Exception {
        when(resourceService.holdStockForBooking(10L, 77L))
                .thenReturn(new BookingStockDTO(10L, 77L, true, true, 2));
        when(resourceService.releaseStockForBooking(10L, 77L))
                .thenReturn(new BookingStockDTO(10L, 77L, true, true, 3));

        mockMvc.perform(post("/api/catalog/resources/{id}/booking-stock/{bookingId}", 10L, 77L)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/catalog/resources/{id}/booking-stock/{bookingId}", 10L, 77L)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void stockDeReserva_conRolEstudianteOSinSesion_deberiaRechazarse() throws Exception {
        mockMvc.perform(post("/api/catalog/resources/{id}/booking-stock/{bookingId}", 10L, 77L)
                        .with(user("estudiante").roles("ESTUDIANTE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/catalog/resources/{id}/booking-stock/{bookingId}", 10L, 77L)
                        .with(user("estudiante").roles("ESTUDIANTE")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/catalog/resources/{id}/booking-stock/{bookingId}", 10L, 77L))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(resourceService);
    }

    @Test
    void tomarStockDeReserva_sinUnidades_deberiaDevolver409ConElMensaje() throws Exception {
        when(resourceService.holdStockForBooking(10L, 77L))
                .thenThrow(new IllegalStateException("No hay stock disponible de 'Microscopio' (0 de 3)"));

        mockMvc.perform(post("/api/catalog/resources/{id}/booking-stock/{bookingId}", 10L, 77L)
                        .with(user("tecnico").roles("TECNICO")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("No hay stock disponible de 'Microscopio' (0 de 3)"));
    }

    @Test
    void ajustarStockLibre_conRolTecnico_sigueSiendoSoloDeAdmin() throws Exception {
        // El técnico solo puede mover stock a través de las reservas, no ajustarlo a mano.
        mockMvc.perform(put("/api/catalog/resources/{id}/stock", 10L)
                        .with(user("tecnico").roles("TECNICO"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"delta\": 5 }"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(resourceService);
    }
}
