package org.campuslab.catalog.controller;

import java.util.List;

import org.campuslab.catalog.dto.CategoryResponseDTO;
import org.campuslab.catalog.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoryResponseDTO> getAll() {
        return service.findAll();
    }
}
