package org.campuslab.catalog.dto;

import java.time.LocalDateTime;

public record LabResponseDTO(
        Long id,
        String name,
        String location,
        Integer capacity,
        LocalDateTime createdAt
) {}
