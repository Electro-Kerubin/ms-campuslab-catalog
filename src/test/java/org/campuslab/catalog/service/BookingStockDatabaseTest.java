package org.campuslab.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;

import org.campuslab.catalog.dto.BookingStockDTO;
import org.campuslab.catalog.dto.ResourceRequestDTO;
import org.campuslab.catalog.dto.ResourceResponseDTO;
import org.campuslab.catalog.entity.Lab;
import org.campuslab.catalog.entity.MovementType;
import org.campuslab.catalog.entity.ResourceStock;
import org.campuslab.catalog.entity.ResourceStockMovement;
import org.campuslab.catalog.entity.ResourceType;
import org.campuslab.catalog.repository.LabRepository;
import org.campuslab.catalog.repository.ResourceCategoryRepository;
import org.campuslab.catalog.repository.ResourceStockMovementRepository;
import org.campuslab.catalog.repository.ResourceStockRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Stock por reserva contra PostgreSQL real: valida las consultas (suma neta de movimientos, bloqueo de fila),
 * los check constraints del esquema y la seguridad por rol de punta a punta.
 *
 * Usa la misma BD de desarrollo (DB_HOST/DB_PORT/DB_NAME/DB_USER/DB_PASSWORD, como application.yml), pero cada
 * test corre en una transacción que se revierte: no deja datos. Si la BD no está disponible, los tests se omiten.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BookingStockDatabaseTest {

    private static final String HOST = env("DB_HOST", "localhost");
    private static final String PORT = env("DB_PORT", "5432");
    private static final String NAME = env("DB_NAME", "catalog");
    private static final String USER = env("DB_USER", "catalog");
    private static final String PASSWORD = env("DB_PASSWORD", "catalog");

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + HOST + ":" + PORT + "/" + NAME);
        registry.add("spring.datasource.username", () -> USER);
        registry.add("spring.datasource.password", () -> PASSWORD);
    }

    @BeforeAll
    static void soloConBaseDeDatos() {
        boolean disponible;
        try (Connection ignored = DriverManager.getConnection(
                "jdbc:postgresql://" + HOST + ":" + PORT + "/" + NAME, USER, PASSWORD)) {
            disponible = true;
        } catch (Exception ex) {
            disponible = false;
        }
        assumeTrue(disponible, "PostgreSQL del catálogo no disponible en " + HOST + ":" + PORT + ": se omiten los tests");
    }

    @MockBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ResourceService service;
    @Autowired
    private LabRepository labRepository;
    @Autowired
    private ResourceCategoryRepository categoryRepository;
    @Autowired
    private ResourceStockRepository stockRepository;
    @Autowired
    private ResourceStockMovementRepository movementRepository;
    @PersistenceContext
    private EntityManager em;

    private Long labId;
    private Long categoryId;

    @BeforeEach
    void datosBase() {
        labId = labRepository.save(Lab.builder().name("Lab test " + UUID.randomUUID()).location("test").capacity(10).build()).getId();
        categoryId = categoryRepository.findAll().get(0).getId();
    }

    private Long crearEquipo(int total) {
        String sufijo = UUID.randomUUID().toString();
        ResourceResponseDTO creado = service.create(new ResourceRequestDTO(labId, categoryId, "Equipo " + sufijo,
                ResourceType.EQUIPO, null, total, 0,
                new ResourceRequestDTO.EquipmentData("Marca", "Modelo", "SN-" + sufijo), null));
        return creado.id();
    }

    /** Escribe lo pendiente y descarta la caché de Hibernate para releer de verdad desde la BD. */
    private int disponibleEnBd(Long resourceId) {
        em.flush();
        em.clear();
        ResourceStock stock = stockRepository.findByResourceId(resourceId).orElseThrow();
        return stock.getQuantityAvailable();
    }

    private List<ResourceStockMovement> movimientos(Long resourceId) {
        em.flush();
        em.clear();
        return movementRepository.findByResourceIdOrderByOccurredAtDesc(resourceId);
    }

    @Test
    void aprobarDescuentaUnaUnidadYDevolverLaRepone() {
        Long equipo = crearEquipo(3);

        BookingStockDTO tomado = service.holdStockForBooking(equipo, 501L);
        assertThat(tomado.changed()).isTrue();
        assertThat(disponibleEnBd(equipo)).isEqualTo(2);

        BookingStockDTO liberado = service.releaseStockForBooking(equipo, 501L);
        assertThat(liberado.changed()).isTrue();
        assertThat(disponibleEnBd(equipo)).isEqualTo(3);

        List<ResourceStockMovement> historial = movimientos(equipo);
        assertThat(historial).extracting(ResourceStockMovement::getMovementType)
                .containsExactlyInAnyOrder(MovementType.RESERVA, MovementType.DEVOLUCION);
        assertThat(historial).extracting(ResourceStockMovement::getReferenceBookingId).containsOnly(501L);
    }

    @Test
    void repetirLaMismaOperacionNoDescuentaDeMasNiDevuelveDeMas() {
        Long equipo = crearEquipo(3);

        service.holdStockForBooking(equipo, 501L);
        BookingStockDTO repetido = service.holdStockForBooking(equipo, 501L);
        assertThat(repetido.changed()).isFalse();
        assertThat(disponibleEnBd(equipo)).isEqualTo(2);

        service.releaseStockForBooking(equipo, 501L);
        BookingStockDTO repetidoLiberar = service.releaseStockForBooking(equipo, 501L);
        assertThat(repetidoLiberar.changed()).isFalse();
        assertThat(disponibleEnBd(equipo)).isEqualTo(3);

        assertThat(movimientos(equipo)).hasSize(2);
    }

    @Test
    void liberarUnaReservaQueNuncaTomoStockNoSumaNada() {
        Long equipo = crearEquipo(3);

        BookingStockDTO resultado = service.releaseStockForBooking(equipo, 999L);

        assertThat(resultado.changed()).isFalse();
        assertThat(disponibleEnBd(equipo)).isEqualTo(3);
        assertThat(movimientos(equipo)).isEmpty();
    }

    @Test
    void cadaReservaTomaSuPropiaUnidadYSeAgotaAlLlegarAlTotal() {
        Long equipo = crearEquipo(2);

        service.holdStockForBooking(equipo, 1L);
        service.holdStockForBooking(equipo, 2L);
        assertThat(disponibleEnBd(equipo)).isZero();

        assertThatThrownBy(() -> service.holdStockForBooking(equipo, 3L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No hay stock disponible")
                .hasMessageContaining("0 de 2");
        assertThat(disponibleEnBd(equipo)).isZero();

        // Al liberar una, otra reserva puede tomarla
        service.releaseStockForBooking(equipo, 1L);
        assertThat(service.holdStockForBooking(equipo, 3L).changed()).isTrue();
        assertThat(disponibleEnBd(equipo)).isZero();
    }

    @Test
    void liberarUnaReservaNoAfectaLasUnidadesDeOtra() {
        Long equipo = crearEquipo(3);
        service.holdStockForBooking(equipo, 1L);
        service.holdStockForBooking(equipo, 2L);

        service.releaseStockForBooking(equipo, 1L);

        assertThat(disponibleEnBd(equipo)).isEqualTo(2);
        // La reserva 2 sigue teniendo su unidad: repetir su toma no descuenta otra
        assertThat(service.holdStockForBooking(equipo, 2L).changed()).isFalse();
    }

    @Test
    void unaSalaNoDescuentaStock() {
        Long sala = service.create(new ResourceRequestDTO(labId, categoryId, "Sala " + UUID.randomUUID(),
                ResourceType.SALA, null, null, null, null, null)).id();

        BookingStockDTO resultado = service.holdStockForBooking(sala, 1L);

        assertThat(resultado.stockManaged()).isFalse();
        assertThat(movimientos(sala)).isEmpty();
    }

    @Test
    void unTecnicoPuedeTomarYLiberarPorHttp_yUnEstudianteNo() throws Exception {
        Long equipo = crearEquipo(2);

        mockMvc.perform(post("/api/catalog/resources/{id}/booking-stock/{b}", equipo, 7L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ESTUDIANTE"))))
                .andExpect(status().isForbidden());
        assertThat(disponibleEnBd(equipo)).isEqualTo(2);

        mockMvc.perform(post("/api/catalog/resources/{id}/booking-stock/{b}", equipo, 7L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECNICO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.changed").value(true))
                .andExpect(jsonPath("$.quantityAvailable").value(1));
        assertThat(disponibleEnBd(equipo)).isEqualTo(1);

        mockMvc.perform(delete("/api/catalog/resources/{id}/booking-stock/{b}", equipo, 7L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECNICO"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityAvailable").value(2));
        assertThat(disponibleEnBd(equipo)).isEqualTo(2);
    }

    @Test
    void sinStockPorHttpDevuelve409ConElMensajeParaElUsuario() throws Exception {
        Long equipo = crearEquipo(1);
        service.holdStockForBooking(equipo, 1L);

        mockMvc.perform(post("/api/catalog/resources/{id}/booking-stock/{b}", equipo, 2L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECNICO"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("No hay stock disponible")));
    }

    @Test
    void recursoInexistentePorHttpDevuelve404() throws Exception {
        mockMvc.perform(post("/api/catalog/resources/{id}/booking-stock/{b}", 987654321L, 1L)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound());
    }

    private static String env(String name, String defecto) {
        String valor = System.getenv(name);
        return valor == null || valor.isBlank() ? defecto : valor;
    }
}
