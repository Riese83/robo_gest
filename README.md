# Robo_Gest - Sistema de Gestion del Club de Robotica (FP-UNE)

Sistema de gestion y portal web para el Club de Robotica de la **Facultad Politecnica - Universidad Nacional del Este (FP-UNE)**.

---

## 📁 Estructura del Proyecto

```text
Robo_Gest/
├── .gitignore                         # Reglas de exclusion de Git
├── docker-compose.yml                 # Servicio de base de datos MySQL 8.0
├── README.md                          # Documentacion del proyecto
├── bd_script/
│   └── club_robotica_mysql8.sql       # Esquema DDL, vistas y procedimientos almacenados
└── club-robotica_front/               # Frontend y Landing Page
    ├── index.html                     # Pagina principal / Inicio
    ├── objetivos.html                 # Mision y objetivos
    ├── categorias.html                # Categorias de robotica
    ├── contacto.html                  # Formulario y canales de contacto
    ├── login.html                     # Pantalla de acceso para integrantes
    ├── css/                           # Hojas de estilo modulares
    ├── js/                            # Scripts e interactividad (Vanilla JS)
    └── assets/images/                 # Recursos graficos e imagenes
```

---

## 🗄️ Base de Datos

El sistema utiliza **MySQL 8.0** con soporte para:
- **Gestion de Integrantes y Asistencia IoT**: Registro de miembros y fichajes mediante ESP32 con RFID/NFC.
- **Inventario de Materiales**: Control de componentes, categorias, estados y ubicaciones.
- **Proyectos y Equipos**: Asignacion de miembros a proyectos.
- **Prestamos Transaccionales**: Control de reservas, estados de prestamo y procedimientos almacenados con bloqueo pesimista (`FOR UPDATE`).

### Levantar la Base de Datos con Docker

Para iniciar el contenedor de base de datos con inicializacion automatica:

```bash
docker compose up -d
```

- **Host**: `localhost`
- **Puerto**: `3306`
- **Base de Datos**: `club_robotica`
- **Usuario**: `root`
- **Contrasena**: `root_password`

Para detener el servicio:

```bash
docker compose down
```

---

## 🌐 Landing Page

El frontend esta desarrollado con tecnologias web estandar (**HTML5**, **CSS3**, **JavaScript**):
- Diseno responsivo y modular.
- Carrusel dinamico en el Home.
- Control de visibilidad en el formulario de inicio de sesion.

Para previsualizarlo, basta con abrir `club-robotica_front/index.html` en cualquier navegador web o usar una extension de servidor local como *Live Server*.
