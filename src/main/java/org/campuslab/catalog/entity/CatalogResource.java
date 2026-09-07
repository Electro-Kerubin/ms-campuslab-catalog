package org.campuslab.catalog.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table (name = "catalog_resource")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder 
public class CatalogResource {
    @Id 
    @GeneratedValue (strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated (EnumType.STRING)
    @Column (nullable = false, length = 20)
    private TipoRecurso tipo; //enum: Laboratiorio, equipo e insumo

    @Column (nullable = false, length = 120)
    private String nombre;

    @Column (length = 500)
    private String descripcion;

    @Column (name = "stock_cupo", nullable = false)
    private Integer stockCupo;

    @Enumerated (EnumType.STRING)
    @Column (nullable = false, length = 15)
    private EstadoRecurso estado; //para saber si esta activo, inactivo o en mantenimiento

    @Column (name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column (name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist 
    void onCreate(){
        this.createdAt = this.updatedAt = LocalDateTime.now();
        if (this.estado == null) this.estado = EstadoRecurso.ACTIVO;
    }

    @PreUpdate 
    void onUpdate(){
        this.updatedAt = localDateTime.now();
    }

}
