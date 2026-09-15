package org.campuslab.catalog.dto;

import jakarta.validation.constraints.NotNull;

public record StockUpdateDTO(
        @NotNull Integer delta,
        Long referenceBookingId,
        String note
) {}
