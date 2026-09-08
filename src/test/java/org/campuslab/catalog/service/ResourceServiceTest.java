package org.campuslab.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.campuslab.catalog.dto.ResourceRequestDTO;
import org.campuslab.catalog.dto.ResourceResponseDTO;
import org.campuslab.catalog.dto.StockUpdateDTO;
import org.campuslab.catalog.entity.EquipmentDetail;
import org.campuslab.catalog.entity.Lab;
import org.campuslab.catalog.entity.MovementType;
import org.campuslab.catalog.entity.Resource;
import org.campuslab.catalog.entity.ResourceCategory;
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
}
