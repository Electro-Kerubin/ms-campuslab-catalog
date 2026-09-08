package org.campuslab.catalog.repository;

import java.util.List;

import org.campuslab.catalog.entity.ResourceStockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceStockMovementRepository extends JpaRepository<ResourceStockMovement, Long> {
    List<ResourceStockMovement> findByResourceIdOrderByOccurredAtDesc(Long resourceId);
}
