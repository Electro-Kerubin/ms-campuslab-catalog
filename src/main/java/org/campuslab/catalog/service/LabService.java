package org.campuslab.catalog.service;

import java.util.List;

import org.campuslab.catalog.dto.LabRequestDTO;
import org.campuslab.catalog.dto.LabResponseDTO;
import org.campuslab.catalog.entity.Lab;
import org.campuslab.catalog.exception.ResourceNotFoundException;
import org.campuslab.catalog.mapper.LabMapper;
import org.campuslab.catalog.repository.LabRepository;
import org.campuslab.catalog.repository.ResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class LabService {

    private final LabRepository labRepository;
    private final ResourceRepository resourceRepository;
    private final LabMapper mapper;

    @Transactional(readOnly = true)
    public List<LabResponseDTO> findAll() {
        return labRepository.findAll().stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public LabResponseDTO findById(Long id) {
        return mapper.toDto(getLab(id));
    }

    public LabResponseDTO create(LabRequestDTO dto) {
        if (labRepository.existsByName(dto.name())) {
            throw new IllegalStateException("Ya existe un laboratorio con el nombre: " + dto.name());
        }
        return mapper.toDto(labRepository.save(mapper.toEntity(dto)));
    }

    public LabResponseDTO update(Long id, LabRequestDTO dto) {
        Lab lab = getLab(id);
        lab.setName(dto.name());
        lab.setLocation(dto.location());
        lab.setCapacity(dto.capacity());
        return mapper.toDto(labRepository.save(lab));
    }

    public void delete(Long id) {
        Lab lab = getLab(id);
        if (resourceRepository.existsByLabId(id)) {
            throw new IllegalStateException("No se puede eliminar: el laboratorio tiene recursos asociados");
        }
        labRepository.delete(lab);
    }

    private Lab getLab(Long id) {
        return labRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratorio no encontrado: " + id));
    }
}
