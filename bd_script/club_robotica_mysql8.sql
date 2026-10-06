-- ============================================================
-- ROBO_GEST - Club de Robótica (FP-UNE)
-- Esquema de Base de Datos para MySQL 8.0+ (InnoDB, UTF-8)
-- ============================================================

CREATE DATABASE IF NOT EXISTS club_robotica
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE club_robotica;

-- ============================================================
-- 1. TABLA: integrante
-- ============================================================
CREATE TABLE IF NOT EXISTS integrante (
    integrante_id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    apellido VARCHAR(100) NOT NULL,
    ci VARCHAR(20) NOT NULL,
    carnet_universitario VARCHAR(50) NOT NULL,
    telefono VARCHAR(30),
    email VARCHAR(254) NOT NULL,
    fecha_ingreso DATE NOT NULL DEFAULT (CURRENT_DATE),
    estado VARCHAR(10) NOT NULL DEFAULT 'ACTIVO',
    nfc_uid VARCHAR(100),
    carrera VARCHAR(120),
    rol VARCHAR(30) NOT NULL DEFAULT 'MIEMBRO',
    password_hash VARCHAR(255),

    CONSTRAINT pk_integrante PRIMARY KEY (integrante_id),
    CONSTRAINT uq_integrante_ci UNIQUE (ci),
    CONSTRAINT uq_integrante_carnet UNIQUE (carnet_universitario),
    CONSTRAINT uq_integrante_email UNIQUE (email),
    CONSTRAINT uq_integrante_nfc UNIQUE (nfc_uid),
    CONSTRAINT ck_integrante_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 2. TABLA: asistencia (Control IoT - ESP32 con RFID)
-- ============================================================
CREATE TABLE IF NOT EXISTS asistencia (
    asistencia_id BIGINT NOT NULL AUTO_INCREMENT,
    integrante_id BIGINT NOT NULL,
    fecha_hora DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    tipo_marcacion VARCHAR(10) NOT NULL,
    dispositivo_utilizado VARCHAR(100),

    CONSTRAINT pk_asistencia PRIMARY KEY (asistencia_id),
    CONSTRAINT fk_asistencia_integrante
        FOREIGN KEY (integrante_id)
        REFERENCES integrante(integrante_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT ck_asistencia_tipo CHECK (tipo_marcacion IN ('ENTRADA', 'SALIDA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_asistencia_integrante_fecha
    ON asistencia (integrante_id, fecha_hora);

-- ============================================================
-- 3. TABLA: categoria
-- ============================================================
CREATE TABLE IF NOT EXISTS categoria (
    categoria_id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500),

    CONSTRAINT pk_categoria PRIMARY KEY (categoria_id),
    CONSTRAINT uq_categoria_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 4. TABLA: material (Inventario)
-- ============================================================
CREATE TABLE IF NOT EXISTS material (
    material_id BIGINT NOT NULL AUTO_INCREMENT,
    categoria_id BIGINT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(1000),
    marca VARCHAR(100),
    modelo VARCHAR(100),
    cantidad_total INT NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    ubicacion VARCHAR(200),

    CONSTRAINT pk_material PRIMARY KEY (material_id),
    CONSTRAINT fk_material_categoria
        FOREIGN KEY (categoria_id)
        REFERENCES categoria(categoria_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT ck_material_cantidad CHECK (cantidad_total >= 0),
    CONSTRAINT ck_material_estado CHECK (estado IN ('ACTIVO', 'MANTENIMIENTO', 'BAJA'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_material_categoria ON material (categoria_id);
CREATE INDEX idx_material_nombre ON material (nombre);

-- ============================================================
-- 5. TABLA: proyecto
-- ============================================================
CREATE TABLE IF NOT EXISTS proyecto (
    proyecto_id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(1000),
    fecha_inicio DATE NOT NULL,
    fecha_fin DATE,
    estado VARCHAR(20) NOT NULL DEFAULT 'PLANIFICADO',

    CONSTRAINT pk_proyecto PRIMARY KEY (proyecto_id),
    CONSTRAINT ck_proyecto_fechas CHECK (fecha_fin IS NULL OR fecha_fin >= fecha_inicio),
    CONSTRAINT ck_proyecto_estado CHECK (estado IN ('PLANIFICADO', 'ACTIVO', 'FINALIZADO', 'CANCELADO'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ============================================================
-- 6. TABLA: integrante_proyecto (N:M)
-- ============================================================
CREATE TABLE IF NOT EXISTS integrante_proyecto (
    integrante_id BIGINT NOT NULL,
    proyecto_id BIGINT NOT NULL,
    fecha_incorporacion DATE NOT NULL DEFAULT (CURRENT_DATE),

    CONSTRAINT pk_integrante_proyecto PRIMARY KEY (integrante_id, proyecto_id),
    CONSTRAINT fk_ip_integrante
        FOREIGN KEY (integrante_id)
        REFERENCES integrante(integrante_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_ip_proyecto
        FOREIGN KEY (proyecto_id)
        REFERENCES proyecto(proyecto_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_ip_proyecto ON integrante_proyecto (proyecto_id);

-- ============================================================
-- 7. TABLA: prestamo
-- ============================================================
CREATE TABLE IF NOT EXISTS prestamo (
    prestamo_id BIGINT NOT NULL AUTO_INCREMENT,
    integrante_id BIGINT NOT NULL,
    proyecto_id BIGINT NULL,
    fecha_solicitud DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    fecha_entrega DATETIME(6) NULL,
    fecha_devolucion_prevista DATETIME(6) NULL,
    fecha_devolucion_real DATETIME(6) NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'SOLICITADO',
    descripcion VARCHAR(1000),

    CONSTRAINT pk_prestamo PRIMARY KEY (prestamo_id),
    CONSTRAINT fk_prestamo_integrante
        FOREIGN KEY (integrante_id)
        REFERENCES integrante(integrante_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT fk_prestamo_proyecto
        FOREIGN KEY (proyecto_id)
        REFERENCES proyecto(proyecto_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL,

    CONSTRAINT ck_prestamo_estado CHECK (
        estado IN (
            'SOLICITADO',
            'APROBADO',
            'ENTREGADO',
            'DEVUELTO',
            'RECHAZADO',
            'VENCIDO',
            'CANCELADO'
        )
    ),
    CONSTRAINT ck_prestamo_fecha_entrega CHECK (
        fecha_entrega IS NULL OR fecha_entrega >= fecha_solicitud
    ),
    CONSTRAINT ck_prestamo_fecha_prevista CHECK (
        fecha_devolucion_prevista IS NULL
        OR fecha_devolucion_prevista >= COALESCE(fecha_entrega, fecha_solicitud)
    ),
    CONSTRAINT ck_prestamo_fecha_real CHECK (
        fecha_devolucion_real IS NULL
        OR fecha_devolucion_real >= COALESCE(fecha_entrega, fecha_solicitud)
    ),
    CONSTRAINT ck_prestamo_aprobado_prevista CHECK (
        estado NOT IN ('APROBADO', 'ENTREGADO', 'VENCIDO', 'DEVUELTO')
        OR fecha_devolucion_prevista IS NOT NULL
    ),
    CONSTRAINT ck_prestamo_entregado_fecha CHECK (
        estado NOT IN ('ENTREGADO', 'VENCIDO', 'DEVUELTO')
        OR fecha_entrega IS NOT NULL
    ),
    CONSTRAINT ck_prestamo_devuelto_fecha CHECK (
        estado <> 'DEVUELTO'
        OR fecha_devolucion_real IS NOT NULL
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_prestamo_integrante ON prestamo (integrante_id);
CREATE INDEX idx_prestamo_proyecto ON prestamo (proyecto_id);
CREATE INDEX idx_prestamo_estado ON prestamo (estado);

-- ============================================================
-- 8. TABLA: detalle_prestamo (Items asociados al préstamo)
-- ============================================================
CREATE TABLE IF NOT EXISTS detalle_prestamo (
    prestamo_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    cantidad INT NOT NULL,

    CONSTRAINT pk_detalle_prestamo PRIMARY KEY (prestamo_id, material_id),
    CONSTRAINT fk_dp_prestamo
        FOREIGN KEY (prestamo_id)
        REFERENCES prestamo(prestamo_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_dp_material
        FOREIGN KEY (material_id)
        REFERENCES material(material_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT ck_dp_cantidad CHECK (cantidad > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_dp_material ON detalle_prestamo (material_id);

-- ============================================================
-- 9. TABLA: proyecto_material (Materiales planificados)
-- ============================================================
CREATE TABLE IF NOT EXISTS proyecto_material (
    proyecto_id BIGINT NOT NULL,
    material_id BIGINT NOT NULL,
    cantidad_planificada INT NULL,

    CONSTRAINT pk_proyecto_material PRIMARY KEY (proyecto_id, material_id),
    CONSTRAINT fk_pm_proyecto
        FOREIGN KEY (proyecto_id)
        REFERENCES proyecto(proyecto_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,
    CONSTRAINT fk_pm_material
        FOREIGN KEY (material_id)
        REFERENCES material(material_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,
    CONSTRAINT ck_pm_cantidad CHECK (cantidad_planificada IS NULL OR cantidad_planificada > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_pm_material ON proyecto_material (material_id);

-- ============================================================
-- 10. VISTA: vw_stock_material (Cálculo dinámico de stock)
-- ============================================================
CREATE OR REPLACE VIEW vw_stock_material AS
SELECT
    m.material_id,
    m.nombre,
    m.cantidad_total,
    COALESCE(
        SUM(
            CASE
                WHEN p.estado IN ('APROBADO', 'ENTREGADO', 'VENCIDO')
                THEN dp.cantidad
                ELSE 0
            END
        ),
        0
    ) AS cantidad_reservada,
    m.cantidad_total
    - COALESCE(
        SUM(
            CASE
                WHEN p.estado IN ('APROBADO', 'ENTREGADO', 'VENCIDO')
                THEN dp.cantidad
                ELSE 0
            END
        ),
        0
    ) AS cantidad_disponible
FROM material m
LEFT JOIN detalle_prestamo dp
    ON dp.material_id = m.material_id
LEFT JOIN prestamo p
    ON p.prestamo_id = dp.prestamo_id
GROUP BY
    m.material_id,
    m.nombre,
    m.cantidad_total;

-- ============================================================
-- 11. VISTA: vw_prestamos_vencidos
-- ============================================================
CREATE OR REPLACE VIEW vw_prestamos_vencidos AS
SELECT
    p.prestamo_id,
    p.integrante_id,
    p.proyecto_id,
    p.fecha_solicitud,
    p.fecha_entrega,
    p.fecha_devolucion_prevista,
    p.fecha_devolucion_real,
    p.estado,
    p.descripcion
FROM prestamo p
WHERE p.estado = 'ENTREGADO'
  AND p.fecha_devolucion_real IS NULL
  AND p.fecha_devolucion_prevista < CURRENT_TIMESTAMP;

-- ============================================================
-- 12. PROCEDIMIENTOS ALMACENADOS (Stored Procedures en MySQL 8)
--
-- Nota de Arquitectura: La lógica de negocio transaccional se
-- diseñará preferentemente en la capa de servicios de Java (ACID con
-- @Transactional y bloqueo pesimista PESSIMISTIC_WRITE). Sin embargo,
-- se proporcionan estos Stored Procedures como alternativa nativa en BD.
-- ============================================================

DELIMITER //

CREATE PROCEDURE sp_aprobar_prestamo(
    IN p_prestamo_id BIGINT,
    IN p_fecha_devolucion_prevista DATETIME(6)
)
proc_label: BEGIN
    DECLARE v_estado VARCHAR(20);
    DECLARE v_cant_detalles INT DEFAULT 0;
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_material_id BIGINT;
    DECLARE v_cantidad_solicitada INT;
    DECLARE v_cantidad_total INT;
    DECLARE v_material_estado VARCHAR(20);
    DECLARE v_cantidad_ocupada BIGINT;

    -- Cursor para iterar los materiales solicitados
    DECLARE cur_items CURSOR FOR
        SELECT material_id, cantidad
        FROM detalle_prestamo
        WHERE prestamo_id = p_prestamo_id
        ORDER BY material_id;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    -- Manejador de rollback en caso de error SQL
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        RESIGNAL;
    END;

    -- Validar fecha futura
    IF p_fecha_devolucion_prevista IS NULL OR p_fecha_devolucion_prevista <= CURRENT_TIMESTAMP THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'La fecha de devolución prevista debe ser futura';
    END IF;

    START TRANSACTION;

    -- Bloqueo pesimista del préstamo
    SELECT estado
      INTO v_estado
      FROM prestamo
     WHERE prestamo_id = p_prestamo_id
       FOR UPDATE;

    IF v_estado IS NULL THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El préstamo no existe';
    END IF;

    IF v_estado <> 'SOLICITADO' THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Solo se puede aprobar un préstamo en estado SOLICITADO';
    END IF;

    -- Verificar existencia de items
    SELECT COUNT(*)
      INTO v_cant_detalles
      FROM detalle_prestamo
     WHERE prestamo_id = p_prestamo_id;

    IF v_cant_detalles = 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'El préstamo no contiene materiales asociados';
    END IF;

    -- Iterar y bloquear materiales involucrados
    OPEN cur_items;
    item_loop: LOOP
        FETCH cur_items INTO v_material_id, v_cantidad_solicitada;
        IF done THEN
            LEAVE item_loop;
        END IF;

        -- Bloqueo del material
        SELECT cantidad_total, estado
          INTO v_cantidad_total, v_material_estado
          FROM material
         WHERE material_id = v_material_id
           FOR UPDATE;

        IF v_material_estado <> 'ACTIVO' THEN
            CLOSE cur_items;
            ROLLBACK;
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Uno de los materiales no se encuentra ACTIVO';
        END IF;

        -- Calcular reservas actuales activas
        SELECT COALESCE(SUM(dp.cantidad), 0)
          INTO v_cantidad_ocupada
          FROM detalle_prestamo dp
          JOIN prestamo p ON p.prestamo_id = dp.prestamo_id
         WHERE dp.material_id = v_material_id
           AND p.estado IN ('APROBADO', 'ENTREGADO', 'VENCIDO');

        -- Validar disponibilidad de stock
        IF (v_cantidad_solicitada + v_cantidad_ocupada) > v_cantidad_total THEN
            CLOSE cur_items;
            ROLLBACK;
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT = 'Stock insuficiente para cubrir el préstamo del material';
        END IF;
    END LOOP item_loop;
    CLOSE cur_items;

    -- Transicionar estado
    UPDATE prestamo
       SET estado = 'APROBADO',
           fecha_devolucion_prevista = p_fecha_devolucion_prevista
     WHERE prestamo_id = p_prestamo_id;

    COMMIT;
END proc_label //

CREATE PROCEDURE sp_entregar_prestamo(
    IN p_prestamo_id BIGINT
)
BEGIN
    DECLARE v_rows_affected INT;

    START TRANSACTION;

    UPDATE prestamo
       SET estado = 'ENTREGADO',
           fecha_entrega = CURRENT_TIMESTAMP(6)
     WHERE prestamo_id = p_prestamo_id
       AND estado = 'APROBADO';

    SET v_rows_affected = ROW_COUNT();

    IF v_rows_affected = 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Solo se puede entregar un préstamo en estado APROBADO';
    ELSE
        COMMIT;
    END IF;
END //

CREATE PROCEDURE sp_devolver_prestamo(
    IN p_prestamo_id BIGINT
)
BEGIN
    DECLARE v_rows_affected INT;

    START TRANSACTION;

    UPDATE prestamo
       SET estado = 'DEVUELTO',
           fecha_devolucion_real = CURRENT_TIMESTAMP(6)
     WHERE prestamo_id = p_prestamo_id
       AND estado IN ('ENTREGADO', 'VENCIDO');

    SET v_rows_affected = ROW_COUNT();

    IF v_rows_affected = 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Solo se puede devolver un préstamo ENTREGADO o VENCIDO';
    ELSE
        COMMIT;
    END IF;
END //

CREATE PROCEDURE sp_cancelar_prestamo(
    IN p_prestamo_id BIGINT
)
BEGIN
    DECLARE v_rows_affected INT;

    START TRANSACTION;

    UPDATE prestamo
       SET estado = 'CANCELADO'
     WHERE prestamo_id = p_prestamo_id
       AND estado IN ('SOLICITADO', 'APROBADO');

    SET v_rows_affected = ROW_COUNT();

    IF v_rows_affected = 0 THEN
        ROLLBACK;
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Solo se puede cancelar un préstamo SOLICITADO o APROBADO';
    ELSE
        COMMIT;
    END IF;
END //

DELIMITER ;
