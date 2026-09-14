-- ms-campuslab-catalog: esquema inicial (labs, recursos, stock)
-- Modelo normalizado (3FN). Motor: PostgreSQL.

CREATE TABLE resource_categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_resource_categories_name UNIQUE (name)
);

CREATE TABLE labs (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(120) NOT NULL,
    location   VARCHAR(150),
    capacity   INTEGER CHECK (capacity >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_labs_name UNIQUE (name)
);

-- resource_type distingue si el recurso es una sala reservable, un equipo o un insumo consumible.
-- status refleja disponibilidad operativa del recurso.
CREATE TABLE resources (
    id          BIGSERIAL PRIMARY KEY,
    lab_id      BIGINT NOT NULL REFERENCES labs (id),
    category_id BIGINT NOT NULL REFERENCES resource_categories (id),
    name        VARCHAR(150) NOT NULL,
    resource_type VARCHAR(20) NOT NULL
        CHECK (resource_type IN ('SALA', 'EQUIPO', 'INSUMO')),
    status      VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE'
        CHECK (status IN ('DISPONIBLE', 'EN_USO', 'MANTENIMIENTO', 'BAJA')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_resources_lab_name UNIQUE (lab_id, name)
);

CREATE INDEX idx_resources_lab_id ON resources (lab_id);
CREATE INDEX idx_resources_category_id ON resources (category_id);
CREATE INDEX idx_resources_status ON resources (status);

-- Extensión 1-a-1: solo aplica a resources.resource_type = 'EQUIPO'.
CREATE TABLE equipment_details (
    resource_id   BIGINT PRIMARY KEY REFERENCES resources (id) ON DELETE CASCADE,
    brand         VARCHAR(100),
    model         VARCHAR(100),
    serial_number VARCHAR(100),
    CONSTRAINT uq_equipment_details_serial UNIQUE (serial_number)
);

-- Extensión 1-a-1: solo aplica a resources.resource_type = 'INSUMO'.
CREATE TABLE supply_details (
    resource_id      BIGINT PRIMARY KEY REFERENCES resources (id) ON DELETE CASCADE,
    unit_of_measure  VARCHAR(30) NOT NULL
);

-- Stock/cupo por recurso (equipos e insumos). El cupo disminuye al aprobar una reserva.
CREATE TABLE resource_stock (
    resource_id         BIGINT PRIMARY KEY REFERENCES resources (id) ON DELETE CASCADE,
    quantity_total       INTEGER NOT NULL CHECK (quantity_total >= 0),
    quantity_available   INTEGER NOT NULL CHECK (quantity_available >= 0),
    reorder_threshold    INTEGER NOT NULL DEFAULT 0 CHECK (reorder_threshold >= 0),
    CONSTRAINT ck_resource_stock_available_le_total CHECK (quantity_available <= quantity_total)
);

-- Historial de movimientos de stock (entradas, salidas, reservas, devoluciones, ajustes).
-- reference_booking_id apunta a bookings.bookings.id (otro microservicio/BD: sin FK física).
CREATE TABLE resource_stock_movements (
    id                   BIGSERIAL PRIMARY KEY,
    resource_id          BIGINT NOT NULL REFERENCES resources (id),
    movement_type        VARCHAR(20) NOT NULL
        CHECK (movement_type IN ('ENTRADA', 'SALIDA', 'RESERVA', 'DEVOLUCION', 'AJUSTE')),
    quantity             INTEGER NOT NULL CHECK (quantity <> 0),
    reference_booking_id BIGINT,
    occurred_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    note                 VARCHAR(255)
);

CREATE INDEX idx_resource_stock_movements_resource_id ON resource_stock_movements (resource_id);
CREATE INDEX idx_resource_stock_movements_occurred_at ON resource_stock_movements (occurred_at);
