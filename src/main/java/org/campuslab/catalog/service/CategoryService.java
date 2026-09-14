package org.campuslab.catalog.service;

import java.util.List;

import org.campuslab.catalog.dto.CategoryResponseDTO;
import org.campuslab.catalog.mapper.CategoryMapper;
import org.campuslab.catalog.repository.ResourceCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CategoryService {

    private final ResourceCategoryRepository repository;
    private final CategoryMapper mapper;

    public List<CategoryResponseDTO> findAll() {
        return repository.findAll().stream().map(mapper::toDto).toList();
    }
}
