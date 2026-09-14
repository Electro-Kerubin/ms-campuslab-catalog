package org.campuslab.catalog.dto;

import org.campuslab.catalog.entity.ResourceStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ResourceUpdateDTO(
        @NotBlank @Size(max = 150) String name,
        @NotNull ResourceStatus status
) {}
