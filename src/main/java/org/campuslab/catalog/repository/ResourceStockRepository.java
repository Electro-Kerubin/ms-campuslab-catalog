package org.campuslab.catalog.repository;

import java.util.Optional;

import org.campuslab.catalog.entity.ResourceStock;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceStockRepository extends JpaRepository<ResourceStock, Long> {
    Optional<ResourceStock> findByResourceId(Long resourceId);
}
