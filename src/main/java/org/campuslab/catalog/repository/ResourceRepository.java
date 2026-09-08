package org.campuslab.catalog.repository;

import java.util.List;

import org.campuslab.catalog.entity.Resource;
import org.campuslab.catalog.entity.ResourceType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByLabId(Long labId);
    List<Resource> findByLabIdAndResourceType(Long labId, ResourceType resourceType);
    boolean existsByLabId(Long labId);
    boolean existsByLabIdAndName(Long labId, String name);
}
