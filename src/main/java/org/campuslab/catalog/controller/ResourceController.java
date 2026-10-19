package org.campuslab.catalog.controller;

import java.util.List;

import org.campuslab.catalog.dto.BookingStockDTO;
import org.campuslab.catalog.dto.ResourceRequestDTO;
import org.campuslab.catalog.dto.ResourceResponseDTO;
import org.campuslab.catalog.dto.ResourceUpdateDTO;
import org.campuslab.catalog.dto.StockUpdateDTO;
import org.campuslab.catalog.service.ResourceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/catalog/resources")
public class ResourceController {

    private final ResourceService service;

    public ResourceController(ResourceService service) {
        this.service = service;
    }

    @GetMapping
    public List<ResourceResponseDTO> getAll(@RequestParam(required = false) Long labId) {
        return service.findAll(labId);
    }

    @GetMapping("/{id}")
    public ResourceResponseDTO getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResourceResponseDTO create(@Valid @RequestBody ResourceRequestDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public ResourceResponseDTO update(@PathVariable Long id, @Valid @RequestBody ResourceUpdateDTO dto) {
        return service.update(id, dto);
    }

    @PutMapping("/{id}/stock")
    public ResourceResponseDTO adjustStock(@PathVariable Long id, @Valid @RequestBody StockUpdateDTO dto) {
        return service.adjustStock(id, dto);
    }

    /** ms-campuslab-bookings: una reserva se aprobó -> toma 1 unidad del equipo. Idempotente por reserva. */
    @PostMapping("/{id}/booking-stock/{bookingId}")
    public BookingStockDTO holdStockForBooking(@PathVariable Long id, @PathVariable Long bookingId) {
        return service.holdStockForBooking(id, bookingId);
    }

    /** ms-campuslab-bookings: la reserva se devolvió o se canceló -> devuelve la unidad. Idempotente. */
    @DeleteMapping("/{id}/booking-stock/{bookingId}")
    public BookingStockDTO releaseStockForBooking(@PathVariable Long id, @PathVariable Long bookingId) {
        return service.releaseStockForBooking(id, bookingId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
