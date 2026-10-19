package org.campuslab.catalog.repository;

import java.util.Collection;
import java.util.List;

import org.campuslab.catalog.entity.MovementType;
import org.campuslab.catalog.entity.ResourceStockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResourceStockMovementRepository extends JpaRepository<ResourceStockMovement, Long> {
    List<ResourceStockMovement> findByResourceIdOrderByOccurredAtDesc(Long resourceId);

    /**
     * Suma neta de los movimientos de un tipo dado para una reserva y un recurso
     * (RESERVA resta, DEVOLUCION suma). Un resultado negativo = unidades que esa reserva
     * tiene tomadas; 0 = no tiene ninguna.
     */
    @Query("""
            select coalesce(sum(m.quantity), 0)
            from ResourceStockMovement m
            where m.resource.id = :resourceId
              and m.referenceBookingId = :bookingId
              and m.movementType in :types
            """)
    long netQuantityForBooking(@Param("resourceId") Long resourceId,
                               @Param("bookingId") Long bookingId,
                               @Param("types") Collection<MovementType> types);
}
