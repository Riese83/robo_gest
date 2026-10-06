-- ============================================================
-- ROBO_GEST - Datos Iniciales de Prueba (Seed Data)
-- Contraseña general para todos los usuarios: admin123
-- Hash BCrypt: $2a$10$6nrKoB5bcsDqXXfmFo5daeM/iWb9MIExLfE.ck0hAjlKMjSZAS9QS
-- ============================================================

USE club_robotica;

-- 1. INTEGRANTES DE PRUEBA
INSERT INTO integrante (
    integrante_id, nombre, apellido, ci, carnet_universitario, telefono, email,
    fecha_ingreso, estado, nfc_uid, carrera, rol, password_hash
) VALUES
(
    1, 'Carlos', 'García', '1234567', 'FP-2022-001', '0981111222', 'admin@fpune.edu.py',
    '2023-01-15', 'ACTIVO', 'A1B2C3D4', 'INGENIERIA_DE_SISTEMAS', 'ADMIN',
    '$2a$10$6nrKoB5bcsDqXXfmFo5daeM/iWb9MIExLfE.ck0hAjlKMjSZAS9QS'
),
(
    2, 'Valeria', 'Ríos', '2345678', 'FP-2022-045', '0982222333', 'directiva@fpune.edu.py',
    '2023-03-10', 'ACTIVO', 'E5F6G7H8', 'INGENIERIA_ELECTRICA', 'DIRECTIVA',
    '$2a$10$6nrKoB5bcsDqXXfmFo5daeM/iWb9MIExLfE.ck0hAjlKMjSZAS9QS'
),
(
    3, 'Mateo', 'Benítez', '3456789', 'FP-2023-112', '0983333444', 'miembro@fpune.edu.py',
    '2024-02-20', 'ACTIVO', 'J9K0L1M2', 'ANALISIS_DE_SISTEMAS', 'MIEMBRO',
    '$2a$10$6nrKoB5bcsDqXXfmFo5daeM/iWb9MIExLfE.ck0hAjlKMjSZAS9QS'
)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    rol = VALUES(rol),
    estado = VALUES(estado);

-- 2. CATEGORÍAS
INSERT INTO categoria (categoria_id, nombre, descripcion) VALUES
(1, 'Microcontroladores y Placas', 'Placas de desarrollo Arduino, ESP32, STM32 y Raspberry Pi'),
(2, 'Sensores y Módulos', 'Sensores ultrasónicos, infrarrojos, giroscopios y módulos de comunicación'),
(3, 'Herramientas y Equipos', 'Osciloscopios, cautines, multímetros y fuentes reguladas')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- 3. MATERIALES EN INVENTARIO
INSERT INTO material (
    material_id, categoria_id, nombre, descripcion, marca, modelo, cantidad_total, estado, ubicacion
) VALUES
(1, 1, 'Arduino Mega 2560 R3', 'Placa basada en ATmega2560 con 54 pines digitales', 'Arduino', 'Mega R3', 15, 'ACTIVO', 'Estante A - Gaveta 1'),
(2, 1, 'ESP32 DevKit V1', 'Módulo SoC con Wi-Fi y Bluetooth BLE 4.2', 'Espressif', 'ESP-WROOM-32', 20, 'ACTIVO', 'Estante A - Gaveta 2'),
(3, 2, 'Sensor Ultrasonido HC-SR04', 'Sensor de medición de distancia por ultrasonido', 'ElecFreaks', 'HC-SR04', 30, 'ACTIVO', 'Estante B - Gaveta 4'),
(4, 2, 'Módulo Lector RFID RC522', 'Kit lector de tarjetas y llaveros NFC/RFID 13.56MHz', 'NXP', 'RC522', 12, 'ACTIVO', 'Estante B - Gaveta 5')
ON DUPLICATE KEY UPDATE cantidad_total = VALUES(cantidad_total);

-- 4. PROYECTOS DEL CLUB
INSERT INTO proyecto (proyecto_id, nombre, descripcion, fecha_inicio, fecha_fin, estado) VALUES
(1, 'Brazo Robótico Autónomo', 'Brazo manipulador de 6 grados de libertad para laboratorio', '2026-03-01', '2026-11-30', 'ACTIVO'),
(2, 'Sistema de Asistencia IoT RFID', 'Dispositivo ESP32 con pantalla OLED y lector NFC para el club', '2026-02-15', '2026-06-30', 'ACTIVO')
ON DUPLICATE KEY UPDATE nombre = VALUES(nombre);

-- 5. ASOCIACIÓN INTEGRANTE - PROYECTO
INSERT INTO integrante_proyecto (integrante_id, proyecto_id, fecha_incorporacion) VALUES
(3, 1, '2026-03-05'),
(2, 2, '2026-02-15')
ON DUPLICATE KEY UPDATE fecha_incorporacion = VALUES(fecha_incorporacion);

-- 6. PRÉSTAMO DE PRUEBA PENDIENTE DE APROBACIÓN
INSERT INTO prestamo (
    prestamo_id, integrante_id, proyecto_id, fecha_solicitud, fecha_entrega,
    fecha_devolucion_prevista, fecha_devolucion_real, estado, descripcion
) VALUES
(
    1, 3, 1, NOW(), NULL, DATE_ADD(NOW(), INTERVAL 7 DAY), NULL,
    'SOLICITADO', 'Préstamo para pruebas de cinemática inversa en el brazo robótico'
)
ON DUPLICATE KEY UPDATE estado = VALUES(estado);

-- 7. DETALLE DEL PRÉSTAMO
INSERT INTO detalle_prestamo (prestamo_id, material_id, cantidad) VALUES
(1, 1, 2), -- 2 Arduinos Mega
(1, 3, 4)  -- 4 Sensores HC-SR04
ON DUPLICATE KEY UPDATE cantidad = VALUES(cantidad);
