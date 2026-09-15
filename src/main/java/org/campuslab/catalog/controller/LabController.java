package org.campuslab.catalog.controller;

import java.util.List;

import org.campuslab.catalog.dto.LabRequestDTO;
import org.campuslab.catalog.dto.LabResponseDTO;
import org.campuslab.catalog.service.LabService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/catalog/labs")
public class LabController {

    private final LabService service;

    public LabController(LabService service) {
        this.service = service;
    }

    @GetMapping
    public List<LabResponseDTO> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public LabResponseDTO getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public LabResponseDTO create(@Valid @RequestBody LabRequestDTO dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public LabResponseDTO update(@PathVariable Long id, @Valid @RequestBody LabRequestDTO dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
