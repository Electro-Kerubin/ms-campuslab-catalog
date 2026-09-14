package org.campuslab.catalog.dto;

import java.time.LocalDateTime;

import org.campuslab.catalog.entity.ResourceStatus;
import org.campuslab.catalog.entity.ResourceType;

public record ResourceResponseDTO(
        Long id,
        Long labId,
        String labName,
        Long categoryId,
        String categoryName,
        String name,
        ResourceType resourceType,
        ResourceStatus status,
        Integer quantityTotal,
        Integer quantityAvailable,
        Integer reorderThreshold,
        String brand,
        String model,
        String serialNumber,
        String unitOfMeasure,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
