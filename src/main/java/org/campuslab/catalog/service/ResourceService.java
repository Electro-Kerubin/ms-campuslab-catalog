package org.campuslab.catalog.service;

import java.util.List;

import org.campuslab.catalog.dto.ResourceRequestDTO;
import org.campuslab.catalog.dto.ResourceResponseDTO;
import org.campuslab.catalog.dto.ResourceUpdateDTO;
import org.campuslab.catalog.dto.StockUpdateDTO;
import org.campuslab.catalog.entity.EquipmentDetail;
import org.campuslab.catalog.entity.Lab;
import org.campuslab.catalog.entity.MovementType;
import org.campuslab.catalog.entity.Resource;
import org.campuslab.catalog.entity.ResourceCategory;
import org.campuslab.catalog.entity.ResourceStock;
import org.campuslab.catalog.entity.ResourceStockMovement;
import org.campuslab.catalog.entity.ResourceType;
import org.campuslab.catalog.entity.SupplyDetail;
import org.campuslab.catalog.exception.ResourceNotFoundException;
import org.campuslab.catalog.mapper.ResourceMapper;
import org.campuslab.catalog.repository.EquipmentDetailRepository;
import org.campuslab.catalog.repository.LabRepository;
import org.campuslab.catalog.repository.ResourceCategoryRepository;
import org.campuslab.catalog.repository.ResourceRepository;
import org.campuslab.catalog.repository.ResourceStockMovementRepository;
import org.campuslab.catalog.repository.ResourceStockRepository;
import org.campuslab.catalog.repository.SupplyDetailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final ResourceCategoryRepository categoryRepository;
    private final LabRepository labRepository;
    private final EquipmentDetailRepository equipmentDetailRepository;
    private final SupplyDetailRepository supplyDetailRepository;
    private final ResourceStockRepository stockRepository;
    private final ResourceStockMovementRepository movementRepository;
    private final ResourceMapper mapper;

    @Transactional(readOnly = true)
    public List<ResourceResponseDTO> findAll(Long labId) {
        List<Resource> resources = labId != null
                ? resourceRepository.findByLabId(labId)
                : resourceRepository.findAll();
        return resources.stream().map(this::toDtoWithDetails).toList();
    }

    @Transactional(readOnly = true)
    public ResourceResponseDTO findById(Long id) {
        return toDtoWithDetails(getResource(id));
    }

    public ResourceResponseDTO create(ResourceRequestDTO dto) {
        Lab lab = labRepository.findById(dto.labId())
                .orElseThrow(() -> new ResourceNotFoundException("Laboratorio no encontrado: " + dto.labId()));
        ResourceCategory category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada: " + dto.categoryId()));

        if (resourceRepository.existsByLabIdAndName(dto.labId(), dto.name())) {
            throw new IllegalStateException("Ya existe un recurso llamado '" + dto.name() + "' en ese laboratorio");
        }
        validatePerType(dto);

        Resource resource = Resource.builder()
                .lab(lab)
                .category(category)
                .name(dto.name())
                .resourceType(dto.resourceType())
                .status(dto.status())
                .build();
        resource = resourceRepository.save(resource);

        if (dto.resourceType() == ResourceType.EQUIPO) {
            equipmentDetailRepository.save(EquipmentDetail.builder()
                    .resource(resource)
                    .brand(dto.equipment().brand())
                    .model(dto.equipment().model())
                    .serialNumber(dto.equipment().serialNumber())
                    .build());
        }
        if (dto.resourceType() == ResourceType.INSUMO) {
            supplyDetailRepository.save(SupplyDetail.builder()
                    .resource(resource)
                    .unitOfMeasure(dto.unitOfMeasure())
                    .build());
        }
        if (dto.quantityTotal() != null) {
            stockRepository.save(ResourceStock.builder()
                    .resource(resource)
                    .quantityTotal(dto.quantityTotal())
                    .quantityAvailable(dto.quantityTotal())
                    .reorderThreshold(dto.reorderThreshold() != null ? dto.reorderThreshold() : 0)
                    .build());
        }
        return findById(resource.getId());
    }

    public ResourceResponseDTO update(Long id, ResourceUpdateDTO dto) {
        Resource resource = getResource(id);
        resource.setName(dto.name());
        resource.setStatus(dto.status());
        return toDtoWithDetails(resourceRepository.save(resource));
    }

    public ResourceResponseDTO adjustStock(Long id, StockUpdateDTO dto) {
        Resource resource = getResource(id);
        ResourceStock stock = stockRepository.findByResourceId(id)
                .orElseThrow(() -> new IllegalStateException("El recurso '" + resource.getName()
                        + "' no maneja stock (tipo " + resource.getResourceType() + ")"));

        int nuevoDisponible = stock.getQuantityAvailable() + dto.delta();
        if (nuevoDisponible < 0) {
            throw new IllegalStateException("Stock insuficiente: disponible="
                    + stock.getQuantityAvailable() + ", delta=" + dto.delta());
        }
        if (nuevoDisponible > stock.getQuantityTotal()) {
            throw new IllegalStateException("Excede la cantidad total (" + stock.getQuantityTotal()
                    + "): resultado=" + nuevoDisponible);
        }
        stock.setQuantityAvailable(nuevoDisponible);
        stockRepository.save(stock);
        movementRepository.save(toMovement(resource, dto));
        return toDtoWithDetails(resource);
    }

    public void delete(Long id) {
        resourceRepository.delete(getResource(id));
    }

    private void validatePerType(ResourceRequestDTO dto) {
        if (dto.resourceType() == ResourceType.EQUIPO) {
            if (dto.equipment() == null || isBlank(dto.equipment().serialNumber())) {
                throw new IllegalStateException("Los EQUIPOS requieren 'equipment' con número de serie");
            }
            if (equipmentDetailRepository.existsBySerialNumber(dto.equipment().serialNumber())) {
                throw new IllegalStateException("Número de serie ya registrado: " + dto.equipment().serialNumber());
            }
            if (dto.quantityTotal() == null) {
                throw new IllegalStateException("Los EQUIPOS requieren 'quantityTotal'");
            }
        }
        if (dto.resourceType() == ResourceType.INSUMO) {
            if (isBlank(dto.unitOfMeasure())) {
                throw new IllegalStateException("Los INSUMOS requieren 'unitOfMeasure'");
            }
            if (dto.quantityTotal() == null) {
                throw new IllegalStateException("Los INSUMOS requieren 'quantityTotal'");
            }
        }
    }

    private ResourceStockMovement toMovement(Resource resource, StockUpdateDTO dto) {

        MovementType type;
        if (dto.referenceBookingId() != null) {
            type = dto.delta() < 0 ? MovementType.RESERVA : MovementType.DEVOLUCION;
        } else {
            type = dto.delta() < 0 ? MovementType.SALIDA : MovementType.ENTRADA;
        }
        return ResourceStockMovement.builder()
                .resource(resource)
                .movementType(type)
                .quantity(dto.delta())
                .referenceBookingId(dto.referenceBookingId())
                .note(dto.note())
                .build();
    }

    private ResourceResponseDTO toDtoWithDetails(Resource resource) {
        return mapper.toDto(
                resource,
                stockRepository.findByResourceId(resource.getId()).orElse(null),
                equipmentDetailRepository.findById(resource.getId()).orElse(null),
                supplyDetailRepository.findById(resource.getId()).orElse(null));
    }

    private Resource getResource(Long id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Recurso no encontrado: " + id));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
