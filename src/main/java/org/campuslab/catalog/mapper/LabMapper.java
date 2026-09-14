package org.campuslab.catalog.mapper;

import org.campuslab.catalog.dto.LabRequestDTO;
import org.campuslab.catalog.dto.LabResponseDTO;
import org.campuslab.catalog.entity.Lab;
import org.springframework.stereotype.Component;

@Component
public class LabMapper {

    public LabResponseDTO toDto(Lab lab) {
        return new LabResponseDTO(lab.getId(), lab.getName(), lab.getLocation(),
                lab.getCapacity(), lab.getCreatedAt());
    }

    public Lab toEntity(LabRequestDTO dto) {
        return Lab.builder()
                .name(dto.name())
                .location(dto.location())
                .capacity(dto.capacity())
                .build();
    }
}
