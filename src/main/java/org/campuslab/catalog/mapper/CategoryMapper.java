package org.campuslab.catalog.mapper;

import org.campuslab.catalog.dto.CategoryResponseDTO;
import org.campuslab.catalog.entity.ResourceCategory;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponseDTO toDto(ResourceCategory category) {
        return new CategoryResponseDTO(category.getId(), category.getName(),
                category.getDescription(), category.getCreatedAt());
    }
}
