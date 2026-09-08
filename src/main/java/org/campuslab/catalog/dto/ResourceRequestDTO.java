package org.campuslab.catalog.dto;

import org.campuslab.catalog.entity.ResourceStatus;
import org.campuslab.catalog.entity.ResourceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ResourceRequestDTO(
        @NotNull Long labId,
        @NotNull Long categoryId,
        @NotBlank @Size(max = 150) String name,
        @NotNull ResourceType resourceType,
        ResourceStatus status,
        @PositiveOrZero Integer quantityTotal,
        @PositiveOrZero Integer reorderThreshold,
        EquipmentData equipment,
        @Size(max = 30) String unitOfMeasure
) {
    public record EquipmentData(
            @Size(max = 100) String brand,
            @Size(max = 100) String model,
            @Size(max = 100) String serialNumber
    ) {}
}
