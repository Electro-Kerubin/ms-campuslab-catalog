package org.campuslab.catalog.repository;

import org.campuslab.catalog.entity.EquipmentDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentDetailRepository extends JpaRepository<EquipmentDetail, Long> {
    boolean existsBySerialNumber(String serialNumber);
}
