package org.campuslab.catalog.repository;

import org.campuslab.catalog.entity.ResourceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceCategoryRepository extends JpaRepository<ResourceCategory, Long> {
    boolean existsByName(String name);
}
