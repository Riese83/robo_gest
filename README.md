# Robo_Gest - Sistema Integral de Gestión del Club de Robótica (FP-UNE)

[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Jakarta EE 10](https://img.shields.io/badge/Jakarta%20EE-10-blue.svg)](https://jakarta.ee/)
[![Hibernate JPA](https://img.shields.io/badge/Hibernate-6.5-59666C.svg)](https://hibernate.org/)
[![MySQL 8.0](https://img.shields.io/badge/MySQL-8.0-4479A1.svg)](https://www.mysql.com/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg)](https://www.docker.com/)
[![Gemini AI](https://img.shields.io/badge/Google%20Gemini-Flash%20Lite-8E75C4.svg)](https://ai.google.dev/)
[![Tests](https://img.shields.io/badge/Tests-74%20Passing-success.svg)](https://junit.org/junit5/)

Plataforma web integral y sistema de gestión académica, inventario y asistencias IoT desarrollada para el **Club de Robótica de la Facultad Politécnica - Universidad Nacional del Este (FP-UNE)**.

---

## 📋 Tabla de Contenidos

1. [Descripción General](#-descripción-general)
2. [Módulos del Sistema](#-módulos-del-sistema)
3. [Arquitectura Tecnológica](#-arquitectura-tecnológica)
4. [Estructura del Repositorio](#-estructura-del-repositorio)
5. [Credenciales de Demostración](#-credenciales-de-demostración)
6. [Instalación y Despliegue](#-instalación-y-despliegue)
7. [Pruebas Unitarias y Calidad](#-pruebas-unitarias-y-calidad)
8. [Seguridad y Respaldos](#-seguridad-y-respaldos)

---

## 🎯 Descripción General

**Robo_Gest** centraliza y automatiza los procesos operativos del Club de Robótica de la FP-UNE:
- **Control de Inventario y Componentes:** Gestión de microcontroladores, sensores, actuadores y kits mecánicos con control estricto de disponibilidad.
- **Préstamos Transaccionales:** Solicitud, aprobación y devolución de materiales con bloqueo de concurrencia y prevención de sobre-reserva.
- **Control de Asistencias IoT:** Registro de entrada y salida mediante tarjetas RFID/NFC escaneadas por microcontroladores ESP32/Arduino, calculando automáticamente las horas de permanencia en el laboratorio.
- **Cuadro de Honor & Tiers:** Clasificación automática de integrantes por horas de dedicación en taller y competencias (Tiers S, A, B, C, D).
- **Gestión de Proyectos:** Seguimiento de proyectos activos, integrantes asignados con sus roles y lista de componentes planificados.
- **Asistente Virtual "Polito" (IA):** Chatbot inteligente contextualizado con la información del club, impulsado por Google Gemini.
- **Portal Web y Landing Page:** Difusión de actividades, fotos oficiales de competencias, objetivos institucionales, enlace oficial a [Instagram (@cdr_fpune)](https://www.instagram.com/cdr_fpune/) y políticas de privacidad de datos.

---

## 🧩 Módulos del Sistema

### 1. Portal Público y Landing Page
- Diseño responsivo adaptado a dispositivos móviles y de escritorio.
- Carrusel de fotos reales de eventos y competencias del Club de Robótica.
- Sección de categorías de robótica (Sumo, Seguidor de Línea, Fútbol de Robots, etc.).
- Página oficial de Políticas de Privacidad y formulario de contacto.

### 2. Panel de Control (Dashboard SPA)
- **Indicadores Clave (KPIs):** Préstamos activos, asistencias registradas, stock de componentes e integrantes registrados.
- **Horas Acumuladas del Miembro:** Widget que muestra en tiempo real las horas de taller y sesiones registradas del usuario autenticado.
- **Mis Proyectos Activos:** Visualización de los proyectos en los que participa el usuario, con fecha de inicio, estado, compañeros de equipo y materiales vinculados.
- **Cuadro de Honor & Tier List:** Ranking exclusivo para Directiva y Administradores que clasifica a los miembros según su dedicación en el club (Tier S: >100h, Tier A: 60-100h, Tier B: 30-59h, Tier C: 10-29h, Tier D: <10h).
- **Exportación:** Generación de reportes y rankings en formato CSV / Excel.

### 3. Inventario y Préstamos de Materiales
- Catálogo de componentes categorizados (Sensores, Motores, Microcontroladores, etc.).
- Búsqueda interactiva en tiempo real y filtrado por estado.
- Circuito de préstamos: `SOLICITADO` ➔ `APROBADO` / `RECHAZADO` ➔ `DEVUELTO`.
- Control de stock atómico con transacciones JPA gestionadas y procedimientos pesimistas (`FOR UPDATE`).

### 4. Control de Asistencias IoT
- Fichaje automático por tarjeta RFID/NFC (frecuencia 13.56 MHz / RC522).
- Módulo de marcación manual de contingencia para administradores.
- Cálculo diferencial de horas entre marcación de `ENTRADA` y `SALIDA`.

### 5. Asistente con Inteligencia Artificial ("Polito")
- Integración segura vía backend con Google Gemini Flash Lite.
- Prompt de sistema ajustado para respuestas concisas, amables y especializadas en robótica y normativas del club.

---

## 🛠️ Arquitectura Tecnológica

| Capa | Tecnologías |
|---|---|
| **Backend** | Java 21 LTS, Jakarta EE 10, Eclipse Jersey 3.1 (JAX-RS), Jackson 2.17 |
| **Persistencia** | Hibernate ORM 6.5, JPA, MySQL Connector/J 8.3 |
| **Seguridad** | Auth0 Java-JWT 4.4, BCrypt Password Hashing, Filtros CORS & RBAC |
| **Base de Datos** | MySQL 8.0 (InnoDB, Transacciones ACID, Stored Procedures, Vistas) |
| **Frontend** | Vanilla JavaScript (ES6+), HTML5 Semántico, CSS3 Modular, Bootstrap 5.3, Bootstrap Icons |
| **IoT / Hardware** | C++ / Arduino (.ino) para ESP32 / Arduino UNO + Lector RC522 RFID |
| **Contenedores** | Docker & Docker Compose (Tomcat 10.1 JDK 21 + MySQL 8.0) |
| **Pruebas** | JUnit 5, Mockito, Surefire Plugin (74 tests unitarios) |

---

## 📂 Estructura del Repositorio

```text
Robo_Gest/
├── pom.xml                               # Configuración de compilación Maven y dependencias
├── docker-compose.yml                    # Orquestación de contenedores (Tomcat + MySQL)
├── README.md                             # Documentación técnica del proyecto
├── backup_mysql.sh                       # Script de respaldo automatizado de base de datos (Linux)
├── backup_mysql.bat                      # Script de respaldo automatizado de base de datos (Windows)
│
├── bd_script/                            # Scripts de Base de Datos
│   ├── club_robotica_mysql8.sql          # DDL: Tablas, relaciones, vistas y stored procedures
│   └── seed_data.sql                     # Datos semilla con usuarios de prueba y stock inicial
│
├── lector_codigo/                        # Módulo IoT
│   └── lector_codigo.ino                 # Firmware Arduino/ESP32 para lectura de tarjetas RFID/NFC
│
├── club-robotica_front/                  # Frontend / Interfaz de Usuario
│   ├── index.html                        # Portal web principal y carrusel de eventos
│   ├── dashboard.html                    # Single Page Application (SPA) del Dashboard
│   ├── login.html                        # Acceso y autenticación de integrantes
│   ├── privacidad.html                   # Políticas de privacidad y protección de datos
│   ├── categorias.html                   # Categorías de robótica
│   ├── objetivos.html                    # Misión, visión y objetivos del club
│   ├── contacto.html                     # Canales de comunicación y formulario
│   ├── css/                              # Hojas de estilo modulares
│   ├── js/                               # Controladores JS (auth.js, dashboard.js, main.js)
│   └── assets/images/                    # Identidad gráfica, fotos del club y avatares de Polito
│
└── src/                                  # Backend Java
    ├── main/
    │   ├── java/py/edu/une/politecnica/robogest/
    │   │   ├── controller/               # Endpoints REST (Auth, Asistencia, Prestamo, Material, etc.)
    │   │   ├── dao/                      # Capa de Acceso a Datos (Interfaces e implementaciones JPA)
    │   │   ├── dto/                      # Data Transfer Objects (Requests y Responses)
    │   │   ├── entity/                   # Entidades JPA (Integrante, Material, Prestamo, etc.)
    │   │   ├── security/                 # Proveedor JWT y utilidades de autorización RBAC
    │   │   ├── service/                  # Capa de lógica de negocio e integraciones (IA Gemini)
    │   │   └── util/                     # Gestión del EntityManagerFactory
    │   ├── resources/META-INF/
    │   │   └── persistence.xml           # Configuración de unidad de persistencia JPA
    │   └── webapp/WEB-INF/
    │       └── web.xml                   # Descriptor de despliegue para Apache Tomcat
    └── test/                             # Batería de Pruebas Unitarias
        └── java/...                      # 74 pruebas unitarias con JUnit 5 y Mockito
```

---

## 🔑 Credenciales de Demostración

Para evaluar los diferentes niveles de acceso según el Control de Acceso Basado en Roles (RBAC):

| Rol | Correo Electrónico | Cédula (CI) | Contraseña | Permisos Destacados |
|---|---|---|---|---|
| **ADMIN** | `admin@fpune.edu.py` | `1234567` | `admin123` | Control total del sistema, gestión de integrantes, inventario, reportes y backups |
| **DIRECTIVA** | `directiva@fpune.edu.py` | `2345678` | `admin123` | Aprobación de préstamos, Cuadro de Honor & Tiers, asignación de proyectos |
| **MIEMBRO** | `miembro@fpune.edu.py` | `3456789` | `admin123` | Solicitud de préstamos, horas personales acumuladas, widget de proyectos propios |

---

## 🚀 Instalación y Despliegue

### Requisitos Previos
- **Java Development Kit (JDK):** Versión 21 o superior.
- **Apache Maven:** Versión 3.9 o superior.
- **Docker & Docker Compose:** Para despliegue contenerizado.
- **MySQL 8.0:** (En caso de ejecución sin Docker).

### 1. Iniciar con Docker Compose (Recomendado)

Inicia la base de datos MySQL y el contenedor de Tomcat con la aplicación:

```bash
docker compose up -d
```

- **Portal Web:** `http://localhost:8080/robo_gest/`
- **Dashboard:** `http://localhost:8080/robo_gest/dashboard.html`
- **API REST Base:** `http://localhost:8080/robo_gest/api/`
- **Base de Datos MySQL:** Puerto `3306`, Usuario `root`, Contraseña `root_password`

### 2. Compilación del Backend y Empaquetado WAR

Para compilar el proyecto y generar el entregable `.war`:

```bash
# Compilar y empaquetar
mvn clean package -DskipTests

# El artefacto se genera en:
# target/robo_gest.war
```

---

## 🧪 Pruebas Unitarias y Calidad

El proyecto cuenta con una cobertura integral de pruebas unitarias que validan controladores REST, capas de servicio, hashing de seguridad y reglas de negocio:

```bash
mvn test
```

**Resultado de las pruebas:**
```text
[INFO] Results:
[INFO] 
[INFO] Tests run: 74, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] BUILD SUCCESS
```

---

## 🔒 Seguridad y Respaldos

- **Hashing de Contraseñas:** Encriptación irreversible mediante BCrypt con factor de costo 10.
- **Autenticación sin Estado (Stateless):** Emisión y verificación de tokens JSON Web Token (JWT) con expiración controlada y firma criptográfica.
- **Protección de Datos:** Las llamadas al asistente IA se realizan exclusivamente desde el backend, resguardando las API keys sin exposición al cliente.
- **Copias de Seguridad Automatizadas:**
  - En Linux / macOS: `./backup_mysql.sh`
  - En Windows: `backup_mysql.bat`
  - Genera volcados SQL comprimidos (`.sql.gz`) etiquetados con fecha y hora.

---

## 👥 Autoría y Créditos

Proyecto desarrollado para el **Club de Robótica de la Facultad Politécnica (FP-UNE)**.
- **Institución:** Facultad Politécnica - Universidad Nacional del Este (UNE).
- **Ciudad del Este, Paraguay.**
