package org.campuslab.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record LabRequestDTO(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 150) String location,
        @NotNull @PositiveOrZero Integer capacity
) {}
