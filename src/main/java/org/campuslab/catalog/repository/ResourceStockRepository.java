package org.campuslab.catalog.repository;

import java.util.Optional;

import org.campuslab.catalog.entity.ResourceStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface ResourceStockRepository extends JpaRepository<ResourceStock, Long> {
    Optional<ResourceStock> findByResourceId(Long resourceId);

    /**
     * Igual que {@link #findByResourceId} pero bloquea la fila hasta el fin de la transacción:
     * dos aprobaciones simultáneas del mismo equipo no pueden leer el mismo "disponible" y
     * descontar solo una vez.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ResourceStock s where s.resourceId = :resourceId")
    Optional<ResourceStock> findByResourceIdForUpdate(@Param("resourceId") Long resourceId);
}
