package org.campuslab.catalog.dto;

import java.time.LocalDateTime;

public record CategoryResponseDTO(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt
) {}
