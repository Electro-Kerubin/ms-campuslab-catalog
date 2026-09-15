package org.campuslab.catalog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resource_stock")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ResourceStock {
    @Id
    private Long resourceId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id")
    private Resource resource;

    @Column(name = "quantity_total", nullable = false)
    private Integer quantityTotal;

    @Column(name = "quantity_available", nullable = false)
    private Integer quantityAvailable;

    @Column(name = "reorder_threshold", nullable = false)
    private Integer reorderThreshold;
}
