# Robo_Gest - Sistema de Gestión del Club de Robótica (FP-UNE)

Sistema de gestión y portal web para el Club de Robótica de la **Facultad Politécnica - Universidad Nacional del Este (FP-UNE)**.

---

## 📁 Estructura del Proyecto

```text
Robo_Gest/
├── .gitignore                         # Reglas de exclusión de Git
├── docker-compose.yml                 # Servicio de base de datos MySQL 8.0
├── README.md                          # Documentación del proyecto
├── bd_script/
│   └── club_robotica_mysql8.sql       # Esquema DDL, vistas y procedimientos almacenados
└── club-robotica_front/
    └── club-robotica/                 # Frontend y Landing Page
        ├── index.html                 # Página principal / Inicio
        ├── objetivos.html             # Misión y objetivos
        ├── categorias.html            # Categorías de robótica
        ├── contacto.html              # Formulario y canales de contacto
        ├── login.html                 # Pantalla de acceso para integrantes
        ├── css/                       # Hojas de estilo modulares
        ├── js/                        # Scripts e interactividad (Vanilla JS)
        └── assets/images/             # Recursos gráficos e imágenes
```

---

## 🗄️ Base de Datos

El sistema utiliza **MySQL 8.0** con soporte para:
- **Gestión de Integrantes y Asistencia IoT**: Registro de miembros y fichajes mediante ESP32 con RFID/NFC.
- **Inventario de Materiales**: Control de componentes, categorías, estados y ubicaciones.
- **Proyectos y Equipos**: Asignación de miembros a proyectos.
- **Préstamos Transaccionales**: Control de reservas, estados de préstamo y procedimientos almacenados con bloqueo pesimista (`FOR UPDATE`).

### Levantar la Base de Datos con Docker

Para iniciar el contenedor de base de datos con inicialización automática:

```bash
docker compose up -d
```

- **Host**: `localhost`
- **Puerto**: `3306`
- **Base de Datos**: `club_robotica`
- **Usuario**: `root`
- **Contraseña**: `root_password`

Para detener el servicio:

```bash
docker compose down
```

---

## 🌐 Landing Page

El frontend está desarrollado con tecnologías web estándar (**HTML5**, **CSS3**, **JavaScript**):
- Diseño responsivo y modular.
- Carrusel dinámico en el Home.
- Control de visibilidad en el formulario de inicio de sesión.

Para previsualizarlo, basta con abrir `club-robotica_front/club-robotica/index.html` en cualquier navegador web o usar una extensión de servidor local como *Live Server*.
