package org.campuslab.catalog.repository;

import org.campuslab.catalog.entity.Lab;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabRepository extends JpaRepository<Lab, Long> {
    boolean existsByName(String name);
}
