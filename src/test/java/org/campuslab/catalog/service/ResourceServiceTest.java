package org.campuslab.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.campuslab.catalog.dto.BookingStockDTO;
import org.campuslab.catalog.dto.ResourceRequestDTO;
import org.campuslab.catalog.dto.ResourceResponseDTO;
import org.campuslab.catalog.dto.StockUpdateDTO;
import org.campuslab.catalog.entity.EquipmentDetail;
import org.campuslab.catalog.entity.Lab;
import org.campuslab.catalog.entity.MovementType;
import org.campuslab.catalog.entity.Resource;
import org.campuslab.catalog.entity.ResourceCategory;
import org.campuslab.catalog.entity.ResourceStatus;
import org.campuslab.catalog.entity.ResourceStock;
import org.campuslab.catalog.entity.ResourceStockMovement;
import org.campuslab.catalog.entity.ResourceType;
import org.campuslab.catalog.exception.ResourceNotFoundException;
import org.campuslab.catalog.mapper.ResourceMapper;
import org.campuslab.catalog.repository.EquipmentDetailRepository;
import org.campuslab.catalog.repository.LabRepository;
import org.campuslab.catalog.repository.ResourceCategoryRepository;
import org.campuslab.catalog.repository.ResourceRepository;
import org.campuslab.catalog.repository.ResourceStockMovementRepository;
import org.campuslab.catalog.repository.ResourceStockRepository;
import org.campuslab.catalog.repository.SupplyDetailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    @Mock
    private ResourceRepository resourceRepository;
    @Mock
    private ResourceCategoryRepository categoryRepository;
    @Mock
    private LabRepository labRepository;
    @Mock
    private EquipmentDetailRepository equipmentDetailRepository;
    @Mock
    private SupplyDetailRepository supplyDetailRepository;
    @Mock
    private ResourceStockRepository stockRepository;
    @Mock
    private ResourceStockMovementRepository movementRepository;

    private ResourceService service;

    @BeforeEach
    void setUp() {
        service = new ResourceService(resourceRepository, categoryRepository, labRepository,
                equipmentDetailRepository, supplyDetailRepository, stockRepository,
                movementRepository, new ResourceMapper());
    }

    private Lab lab() {
        return Lab.builder().id(1L).name("Lab Química").location("Edificio A").capacity(30).build();
    }

    private ResourceCategory categoria() {
        return ResourceCategory.builder().id(2L).name("Equipos de laboratorio").build();
    }

    private Resource equipo(Long id) {
        return Resource.builder().id(id).lab(lab()).category(categoria())
                .name("Microscopio").resourceType(ResourceType.EQUIPO).build();
    }

    private ResourceStock stock(Integer total, Integer disponible) {
        return ResourceStock.builder().quantityTotal(total)
                .quantityAvailable(disponible).reorderThreshold(0).build();
    }

    @Test
    void create_equipoDeberiaGuardarDetalleYStock() {
        ResourceRequestDTO dto = new ResourceRequestDTO(1L, 2L, "Microscopio", ResourceType.EQUIPO,
                null, 5, 1,
                new ResourceRequestDTO.EquipmentData("Olympus", "CX23", "SN-001"), null);

        when(labRepository.findById(1L)).thenReturn(Optional.of(lab()));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(categoria()));
        when(resourceRepository.existsByLabIdAndName(1L, "Microscopio")).thenReturn(false);
        when(equipmentDetailRepository.existsBySerialNumber("SN-001")).thenReturn(false);
        when(resourceRepository.save(any(Resource.class))).thenAnswer(inv -> {
            Resource r = inv.getArgument(0);
            ReflectionTestUtils.setField(r, "id", 10L);
            return r;
        });
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(equipo(10L)));
        when(stockRepository.findByResourceId(10L)).thenReturn(Optional.of(stock(5, 5)));
        when(equipmentDetailRepository.findById(10L)).thenReturn(Optional.of(
                EquipmentDetail.builder().brand("Olympus").model("CX23").serialNumber("SN-001").build()));

        ResourceResponseDTO respuesta = service.create(dto);

        assertThat(respuesta.serialNumber()).isEqualTo("SN-001");
        assertThat(respuesta.quantityAvailable()).isEqualTo(5);
        verify(equipmentDetailRepository).save(any(EquipmentDetail.class));
        verify(stockRepository).save(any(ResourceStock.class));
    }

    @Test
    void create_equipoSinNumeroDeSerieDeberiaRechazarse() {
        ResourceRequestDTO dto = new ResourceRequestDTO(1L, 2L, "Microscopio", ResourceType.EQUIPO,
                null, 5, null,
                new ResourceRequestDTO.EquipmentData("Olympus", "CX23", null), null);

        when(labRepository.findById(1L)).thenReturn(Optional.of(lab()));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(categoria()));
        when(resourceRepository.existsByLabIdAndName(1L, "Microscopio")).thenReturn(false);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("número de serie");
    }

    @Test
    void create_nombreDuplicadoEnElMismoLabDeberiaRechazarse() {
        ResourceRequestDTO dto = new ResourceRequestDTO(1L, 2L, "Microscopio", ResourceType.EQUIPO,
                null, 5, null, null, null);

        when(labRepository.findById(1L)).thenReturn(Optional.of(lab()));
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(categoria()));
        when(resourceRepository.existsByLabIdAndName(1L, "Microscopio")).thenReturn(true);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe un recurso");
    }

    @Test
    void create_labInexistenteDeberiaLanzarNotFound() {
        ResourceRequestDTO dto = new ResourceRequestDTO(99L, 2L, "Microscopio", ResourceType.EQUIPO,
                null, 5, null, null, null);

        when(labRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void adjustStock_deberiaRechazarCuandoElResultadoQuedaNegativo() {
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(equipo(10L)));
        when(stockRepository.findByResourceId(10L)).thenReturn(Optional.of(stock(5, 2)));

        assertThatThrownBy(() -> service.adjustStock(10L, new StockUpdateDTO(-3, null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Stock insuficiente");
    }

    @Test
    void adjustStock_deberiaRechazarCuandoSeExcedeElTotal() {
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(equipo(10L)));
        when(stockRepository.findByResourceId(10L)).thenReturn(Optional.of(stock(5, 4)));

        assertThatThrownBy(() -> service.adjustStock(10L, new StockUpdateDTO(2, null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Excede la cantidad total");
    }

    @Test
    void adjustStock_reservaDeberiaDescontarYRegistrarMovimiento() {
        Resource recurso = equipo(10L);
        ResourceStock st = stock(5, 3);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(recurso));
        when(stockRepository.findByResourceId(10L)).thenReturn(Optional.of(st));

        ResourceResponseDTO respuesta = service.adjustStock(10L, new StockUpdateDTO(-2, 77L, "Reserva aprobada"));

        assertThat(respuesta.quantityAvailable()).isEqualTo(1);
        assertThat(st.getQuantityAvailable()).isEqualTo(1);

        ArgumentCaptor<ResourceStockMovement> movimiento = ArgumentCaptor.forClass(ResourceStockMovement.class);
        verify(movementRepository).save(movimiento.capture());
        assertThat(movimiento.getValue().getMovementType()).isEqualTo(MovementType.RESERVA);
        assertThat(movimiento.getValue().getQuantity()).isEqualTo(-2);
        assertThat(movimiento.getValue().getReferenceBookingId()).isEqualTo(77L);
    }

    @Test
    void adjustStock_devolucionDeberiaSumarYRegistrarMovimiento() {
        ResourceStock st = stock(5, 3);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(equipo(10L)));
        when(stockRepository.findByResourceId(10L)).thenReturn(Optional.of(st));

        service.adjustStock(10L, new StockUpdateDTO(2, 77L, null));

        assertThat(st.getQuantityAvailable()).isEqualTo(5);

        ArgumentCaptor<ResourceStockMovement> movimiento = ArgumentCaptor.forClass(ResourceStockMovement.class);
        verify(movementRepository).save(movimiento.capture());
        assertThat(movimiento.getValue().getMovementType()).isEqualTo(MovementType.DEVOLUCION);
    }

    @Test
    void adjustStock_salaSinStockDeberiaRechazarse() {
        Resource sala = Resource.builder().id(20L).lab(lab()).category(categoria())
                .name("Sala húmeda").resourceType(ResourceType.SALA).build();
        when(resourceRepository.findById(20L)).thenReturn(Optional.of(sala));
        when(stockRepository.findByResourceId(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.adjustStock(20L, new StockUpdateDTO(-1, null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no maneja stock");
    }

    // ---------- Stock por reserva: lo que dispara ms-bookings al aprobar / devolver / cancelar ----------

    /** Lo que el repositorio informa como "neto de RESERVA+DEVOLUCION de esta reserva": -1 = tiene 1 unidad tomada. */
    private void reservaTieneTomadas(long unidades) {
        when(movementRepository.netQuantityForBooking(eq(10L), eq(77L), anyCollection())).thenReturn(-unidades);
    }

    @Test
    void holdStockForBooking_equipoDescuentaUnaUnidadYRegistraElMovimiento() {
        ResourceStock st = stock(5, 3);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(equipo(10L)));
        when(stockRepository.findByResourceIdForUpdate(10L)).thenReturn(Optional.of(st));

        BookingStockDTO resultado = service.holdStockForBooking(10L, 77L);

        assertThat(st.getQuantityAvailable()).isEqualTo(2);
        assertThat(resultado.stockManaged()).isTrue();
        assertThat(resultado.changed()).isTrue();
        assertThat(resultado.quantityAvailable()).isEqualTo(2);

        ArgumentCaptor<ResourceStockMovement> movimiento = ArgumentCaptor.forClass(ResourceStockMovement.class);
        verify(movementRepository).save(movimiento.capture());
        assertThat(movimiento.getValue().getMovementType()).isEqualTo(MovementType.RESERVA);
        assertThat(movimiento.getValue().getQuantity()).isEqualTo(-1);
        assertThat(movimiento.getValue().getReferenceBookingId()).isEqualTo(77L);
        verify(stockRepository).save(st);
    }

    @Test
    void holdStockForBooking_esIdempotenteSiLaReservaYaTieneLaUnidad() {
        ResourceStock st = stock(5, 2);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(equipo(10L)));
        when(stockRepository.findByResourceIdForUpdate(10L)).thenReturn(Optional.of(st));
        reservaTieneTomadas(1);

        BookingStockDTO resultado = service.holdStockForBooking(10L, 77L);

        assertThat(resultado.changed()).isFalse();
        assertThat(st.getQuantityAvailable()).isEqualTo(2);
        verify(movementRepository, never()).save(any());
        verify(stockRepository, never()).save(any());
    }

    @Test
    void holdStockForBooking_sinUnidadesDisponiblesLanzaConflicto() {
        ResourceStock st = stock(3, 0);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(equipo(10L)));
        when(stockRepository.findByResourceIdForUpdate(10L)).thenReturn(Optional.of(st));

        assertThatThrownBy(() -> service.holdStockForBooking(10L, 77L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No hay stock disponible de 'Microscopio'")
                .hasMessageContaining("0 de 3");

        assertThat(st.getQuantityAvailable()).isZero();
        verify(movementRepository, never()).save(any());
    }

    @Test
    void holdStockForBooking_salaNoDescuentaNada() {
        Resource sala = Resource.builder().id(20L).lab(lab()).category(categoria())
                .name("Sala húmeda").resourceType(ResourceType.SALA).build();
        when(resourceRepository.findById(20L)).thenReturn(Optional.of(sala));

        BookingStockDTO resultado = service.holdStockForBooking(20L, 77L);

        assertThat(resultado.stockManaged()).isFalse();
        assertThat(resultado.changed()).isFalse();
        verifyNoInteractions(stockRepository, movementRepository);
    }

    @Test
    void holdStockForBooking_insumoNoDescuentaNada() {
        Resource insumo = Resource.builder().id(30L).lab(lab()).category(categoria())
                .name("Guantes").resourceType(ResourceType.INSUMO).build();
        when(resourceRepository.findById(30L)).thenReturn(Optional.of(insumo));

        BookingStockDTO resultado = service.holdStockForBooking(30L, 77L);

        assertThat(resultado.stockManaged()).isFalse();
        verifyNoInteractions(stockRepository, movementRepository);
    }

    @Test
    void holdStockForBooking_recursoInexistenteLanzaNotFound() {
        when(resourceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.holdStockForBooking(99L, 77L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void releaseStockForBooking_devuelveLaUnidadTomadaYRegistraElMovimiento() {
        ResourceStock st = stock(5, 2);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(equipo(10L)));
        when(stockRepository.findByResourceIdForUpdate(10L)).thenReturn(Optional.of(st));
        reservaTieneTomadas(1);

        BookingStockDTO resultado = service.releaseStockForBooking(10L, 77L);

        assertThat(st.getQuantityAvailable()).isEqualTo(3);
        assertThat(resultado.changed()).isTrue();
        assertThat(resultado.quantityAvailable()).isEqualTo(3);

        ArgumentCaptor<ResourceStockMovement> movimiento = ArgumentCaptor.forClass(ResourceStockMovement.class);
        verify(movementRepository).save(movimiento.capture());
        assertThat(movimiento.getValue().getMovementType()).isEqualTo(MovementType.DEVOLUCION);
        assertThat(movimiento.getValue().getQuantity()).isEqualTo(1);
        assertThat(movimiento.getValue().getReferenceBookingId()).isEqualTo(77L);
    }

    @Test
    void releaseStockForBooking_esIdempotenteSiLaReservaNoTieneNadaTomado() {
        ResourceStock st = stock(5, 5);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(equipo(10L)));
        when(stockRepository.findByResourceIdForUpdate(10L)).thenReturn(Optional.of(st));
        reservaTieneTomadas(0);

        BookingStockDTO resultado = service.releaseStockForBooking(10L, 77L);

        assertThat(resultado.changed()).isFalse();
        assertThat(st.getQuantityAvailable()).isEqualTo(5);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void releaseStockForBooking_nuncaSuperaElTotal() {
        // Aunque algo haya dejado el disponible en el tope, devolver no lo pasa del total (check de la BD).
        ResourceStock st = stock(5, 5);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(equipo(10L)));
        when(stockRepository.findByResourceIdForUpdate(10L)).thenReturn(Optional.of(st));
        reservaTieneTomadas(1);

        service.releaseStockForBooking(10L, 77L);

        assertThat(st.getQuantityAvailable()).isEqualTo(5);
    }

    @Test
    void releaseStockForBooking_salaNoTocaNada() {
        Resource sala = Resource.builder().id(20L).lab(lab()).category(categoria())
                .name("Sala húmeda").resourceType(ResourceType.SALA).build();
        when(resourceRepository.findById(20L)).thenReturn(Optional.of(sala));

        BookingStockDTO resultado = service.releaseStockForBooking(20L, 77L);

        assertThat(resultado.stockManaged()).isFalse();
        verifyNoInteractions(stockRepository, movementRepository);
    }

    // ---------- Estado del equipo según su stock ----------

    private Resource equipoConEstado(ResourceStatus estado) {
        Resource r = equipo(10L);
        r.setStatus(estado);
        return r;
    }

    @Test
    void holdStockForBooking_alAgotarLaUltimaUnidadElEquipoPasaAEnUso() {
        Resource recurso = equipoConEstado(ResourceStatus.DISPONIBLE);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(recurso));
        when(stockRepository.findByResourceIdForUpdate(10L)).thenReturn(Optional.of(stock(3, 1)));

        service.holdStockForBooking(10L, 77L);

        assertThat(recurso.getStatus()).isEqualTo(ResourceStatus.EN_USO);
    }

    @Test
    void holdStockForBooking_conUnidadesRestantesSigueDisponible() {
        Resource recurso = equipoConEstado(ResourceStatus.DISPONIBLE);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(recurso));
        when(stockRepository.findByResourceIdForUpdate(10L)).thenReturn(Optional.of(stock(3, 2)));

        service.holdStockForBooking(10L, 77L);

        assertThat(recurso.getStatus()).isEqualTo(ResourceStatus.DISPONIBLE);
    }

    @Test
    void releaseStockForBooking_alRecuperarUnaUnidadVuelveADisponible() {
        Resource recurso = equipoConEstado(ResourceStatus.EN_USO);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(recurso));
        when(stockRepository.findByResourceIdForUpdate(10L)).thenReturn(Optional.of(stock(3, 0)));
        reservaTieneTomadas(1);

        service.releaseStockForBooking(10L, 77L);

        assertThat(recurso.getStatus()).isEqualTo(ResourceStatus.DISPONIBLE);
    }

    @Test
    void mantenimientoYBajaNuncaSeCambianPorElStock() {
        for (ResourceStatus manual : new ResourceStatus[] {ResourceStatus.MANTENIMIENTO, ResourceStatus.BAJA}) {
            Resource recurso = equipoConEstado(manual);
            when(resourceRepository.findById(10L)).thenReturn(Optional.of(recurso));
            when(stockRepository.findByResourceIdForUpdate(10L)).thenReturn(Optional.of(stock(3, 1)));

            service.holdStockForBooking(10L, 77L); // deja 0 unidades

            assertThat(recurso.getStatus()).isEqualTo(manual);
        }
    }

    @Test
    void adjustStock_manualTambienActualizaElEstado() {
        Resource recurso = equipoConEstado(ResourceStatus.DISPONIBLE);
        when(resourceRepository.findById(10L)).thenReturn(Optional.of(recurso));
        when(stockRepository.findByResourceId(10L)).thenReturn(Optional.of(stock(5, 2)));

        service.adjustStock(10L, new StockUpdateDTO(-2, null, null));
        assertThat(recurso.getStatus()).isEqualTo(ResourceStatus.EN_USO);

        service.adjustStock(10L, new StockUpdateDTO(1, null, null));
        assertThat(recurso.getStatus()).isEqualTo(ResourceStatus.DISPONIBLE);
    }
}
