package org.campuslab.catalog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.campuslab.catalog.dto.LabRequestDTO;
import org.campuslab.catalog.dto.LabResponseDTO;
import org.campuslab.catalog.entity.Lab;
import org.campuslab.catalog.exception.ResourceNotFoundException;
import org.campuslab.catalog.mapper.LabMapper;
import org.campuslab.catalog.repository.LabRepository;
import org.campuslab.catalog.repository.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LabServiceTest {

    @Mock
    private LabRepository labRepository;

    @Mock
    private ResourceRepository resourceRepository;

    private LabService service;

    @BeforeEach
    void setUp() {
        service = new LabService(labRepository, resourceRepository, new LabMapper());
    }

    private Lab lab(Long id) {
        return Lab.builder().id(id).name("Lab Química")
                .location("Edificio A, piso 1, sala 101").capacity(30).build();
    }

    @Test
    void create_deberiaGuardarCuandoElNombreNoExiste() {
        LabRequestDTO dto = new LabRequestDTO("Lab Química", "Edificio A, piso 1, sala 101", 30);
        when(labRepository.existsByName("Lab Química")).thenReturn(false);
        when(labRepository.save(any(Lab.class))).thenReturn(lab(1L));

        LabResponseDTO respuesta = service.create(dto);

        assertThat(respuesta.name()).isEqualTo("Lab Química");
        verify(labRepository).save(any(Lab.class));
    }

    @Test
    void create_deberiaRechazarNombreDuplicado() {
        when(labRepository.existsByName("Lab Química")).thenReturn(true);
        LabRequestDTO dto = new LabRequestDTO("Lab Química", "Edificio A, piso 1, sala 101", 30);

        assertThatThrownBy(() -> service.create(dto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Ya existe un laboratorio");

        verify(labRepository, never()).save(any());
    }

    @Test
    void findById_deberiaLanzarNotFoundCuandoNoExiste() {
        when(labRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void delete_deberiaRechazarSiTieneRecursosAsociados() {
        when(labRepository.findById(1L)).thenReturn(Optional.of(lab(1L)));
        when(resourceRepository.existsByLabId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("recursos asociados");

        verify(labRepository, never()).delete(any());
    }

    @Test
    void delete_deberiaEliminarCuandoNoTieneRecursos() {
        when(labRepository.findById(1L)).thenReturn(Optional.of(lab(1L)));
        when(resourceRepository.existsByLabId(1L)).thenReturn(false);

        service.delete(1L);

        verify(labRepository).delete(any(Lab.class));
    }
}
