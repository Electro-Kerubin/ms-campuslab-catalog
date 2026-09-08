package org.campuslab.catalog.mapper;

import org.campuslab.catalog.dto.ResourceResponseDTO;
import org.campuslab.catalog.entity.EquipmentDetail;
import org.campuslab.catalog.entity.Resource;
import org.campuslab.catalog.entity.ResourceStock;
import org.campuslab.catalog.entity.SupplyDetail;
import org.springframework.stereotype.Component;

@Component
public class ResourceMapper {

    public ResourceResponseDTO toDto(Resource resource, ResourceStock stock,
                                     EquipmentDetail equipment, SupplyDetail supply) {
        return new ResourceResponseDTO(
                resource.getId(),
                resource.getLab().getId(),
                resource.getLab().getName(),
                resource.getCategory().getId(),
                resource.getCategory().getName(),
                resource.getName(),
                resource.getResourceType(),
                resource.getStatus(),
                stock != null ? stock.getQuantityTotal() : null,
                stock != null ? stock.getQuantityAvailable() : null,
                stock != null ? stock.getReorderThreshold() : null,
                equipment != null ? equipment.getBrand() : null,
                equipment != null ? equipment.getModel() : null,
                equipment != null ? equipment.getSerialNumber() : null,
                supply != null ? supply.getUnitOfMeasure() : null,
                resource.getCreatedAt(),
                resource.getUpdatedAt()
        );
    }
}
