package org.campuslab.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Catalog Service Application
 *
 * Punto de entrada para el microservicio de gestión del catálogo.
 *
 * Responsabilidades:
 * - CRUD de laboratorios, equipos e insumos
 * - Gestión de stock y disponibilidad de recursos
 * - Gestión de cupo/capacidad de laboratorios
 * - Persistencia en PostgreSQL
 * - Consultas públicas de disponibilidad
 */
@SpringBootApplication
public class CatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }

}
