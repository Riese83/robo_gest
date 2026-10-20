package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import py.edu.une.politecnica.robogest.dao.*;
import py.edu.une.politecnica.robogest.dao.impl.*;
import py.edu.une.politecnica.robogest.dto.*;
import py.edu.une.politecnica.robogest.entity.*;
import py.edu.une.politecnica.robogest.entity.enums.EstadoMaterialEnum;
import py.edu.une.politecnica.robogest.entity.enums.EstadoPrestamoEnum;
import py.edu.une.politecnica.robogest.security.JwtTokenProvider;
import py.edu.une.politecnica.robogest.security.SecurityUtils;
import py.edu.une.politecnica.robogest.service.PrestamoService;
import py.edu.une.politecnica.robogest.service.impl.PrestamoServiceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Controlador REST protegido para la gestion, ciclo de vida CRUD y aprobacion transaccional de prestamos.
 */
@Path("/prestamos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PrestamoController {

    private static final Set<String> ROLES_AUTORIZADOS = Set.of("ADMIN", "DIRECTIVA", "COORDINADOR");

    private final PrestamoService prestamoService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PrestamoDAO prestamoDAO;
    private final IntegranteDAO integranteDAO;
    private final MaterialDAO materialDAO;
    private final ProyectoDAO proyectoDAO;
    private final DetallePrestamoDAO detallePrestamoDAO;

    /**
     * Constructor por defecto para el contenedor JAX-RS / Servlets.
     */
    public PrestamoController() {
        this.jwtTokenProvider = new JwtTokenProvider();
        this.prestamoDAO = new PrestamoDAOJpaImpl();
        this.integranteDAO = new IntegranteDAOJpaImpl();
        this.materialDAO = new MaterialDAOJpaImpl();
        this.proyectoDAO = new ProyectoDAOJpaImpl();
        this.detallePrestamoDAO = new DetallePrestamoDAOJpaImpl();
        this.prestamoService = new PrestamoServiceImpl(
                this.prestamoDAO,
                this.materialDAO,
                this.detallePrestamoDAO
        );
    }

    /**
     * Constructor para inyeccion de dependencias.
     */
    public PrestamoController(PrestamoService prestamoService, JwtTokenProvider jwtTokenProvider) {
        this.prestamoService = Objects.requireNonNull(prestamoService, "prestamoService no puede ser null");
        this.jwtTokenProvider = Objects.requireNonNull(jwtTokenProvider, "jwtTokenProvider no puede ser null");
        this.prestamoDAO = new PrestamoDAOJpaImpl();
        this.integranteDAO = new IntegranteDAOJpaImpl();
        this.materialDAO = new MaterialDAOJpaImpl();
        this.proyectoDAO = new ProyectoDAOJpaImpl();
        this.detallePrestamoDAO = new DetallePrestamoDAOJpaImpl();
    }

    public PrestamoController(PrestamoService prestamoService, JwtTokenProvider jwtTokenProvider, PrestamoDAO prestamoDAO) {
        this.prestamoService = Objects.requireNonNull(prestamoService, "prestamoService no puede ser null");
        this.jwtTokenProvider = Objects.requireNonNull(jwtTokenProvider, "jwtTokenProvider no puede ser null");
        this.prestamoDAO = Objects.requireNonNull(prestamoDAO, "prestamoDAO no puede ser null");
        this.integranteDAO = new IntegranteDAOJpaImpl();
        this.materialDAO = new MaterialDAOJpaImpl();
        this.proyectoDAO = new ProyectoDAOJpaImpl();
        this.detallePrestamoDAO = new DetallePrestamoDAOJpaImpl();
    }

    public PrestamoController(PrestamoService prestamoService, JwtTokenProvider jwtTokenProvider,
                              PrestamoDAO prestamoDAO, IntegranteDAO integranteDAO,
                              MaterialDAO materialDAO, ProyectoDAO proyectoDAO,
                              DetallePrestamoDAO detallePrestamoDAO) {
        this.prestamoService = Objects.requireNonNull(prestamoService, "prestamoService no puede ser null");
        this.jwtTokenProvider = Objects.requireNonNull(jwtTokenProvider, "jwtTokenProvider no puede ser null");
        this.prestamoDAO = Objects.requireNonNull(prestamoDAO, "prestamoDAO no puede ser null");
        this.integranteDAO = Objects.requireNonNull(integranteDAO, "integranteDAO no puede ser null");
        this.materialDAO = Objects.requireNonNull(materialDAO, "materialDAO no puede ser null");
        this.proyectoDAO = Objects.requireNonNull(proyectoDAO, "proyectoDAO no puede ser null");
        this.detallePrestamoDAO = Objects.requireNonNull(detallePrestamoDAO, "detallePrestamoDAO no puede ser null");
    }

    @GET
    public Response listarPrestamos(@HeaderParam("Authorization") String authHeader) {
        validarToken(authHeader);
        if (SecurityUtils.INTERNAL_ADMIN_TOKEN.equals(authHeader)) {
            List<Prestamo> prestamos = prestamoDAO.findAll();
            return Response.ok(prestamos.stream().map(PrestamoResponseDTO::fromEntity).toList()).build();
        }

        String token = authHeader.substring(7).trim();
        String rol = jwtTokenProvider.getRolFromToken(token);
        Long integranteId = jwtTokenProvider.getIntegranteIdFromToken(token);

        List<Prestamo> prestamos;
        if (rol != null && ROLES_AUTORIZADOS.contains(rol.toUpperCase())) {
            prestamos = prestamoDAO.findAll();
        } else if (integranteId != null) {
            prestamos = prestamoDAO.findByIntegranteId(integranteId);
        } else {
            String email = jwtTokenProvider.getUsernameFromToken(token);
            Integrante integrante = (email != null) ? integranteDAO.findByEmail(email).orElse(null) : null;
            prestamos = (integrante != null) ? prestamoDAO.findByIntegranteId(integrante.getId()) : List.of();
        }
        List<PrestamoResponseDTO> dtos = prestamos.stream()
                .map(PrestamoResponseDTO::fromEntity)
                .toList();
        return Response.ok(dtos).build();
    }

    @GET
    @Path("/{id}")
    public Response obtenerPrestamo(@PathParam("id") Long id, @HeaderParam("Authorization") String authHeader) {
        if (id == null) {
            throw new IllegalArgumentException("El ID del prestamo no puede ser null");
        }
        validarToken(authHeader);
        Prestamo prestamo = prestamoDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Prestamo no encontrado con ID: " + id));

        if (!SecurityUtils.INTERNAL_ADMIN_TOKEN.equals(authHeader)) {
            String token = authHeader.substring(7).trim();
            String rol = jwtTokenProvider.getRolFromToken(token);
            Long integranteId = jwtTokenProvider.getIntegranteIdFromToken(token);

            if (rol != null && !ROLES_AUTORIZADOS.contains(rol.toUpperCase())) {
                if (integranteId != null && prestamo.getIntegrante() != null && !integranteId.equals(prestamo.getIntegrante().getId())) {
                    throw new SecurityException("No tienes autorizacion para consultar las solicitudes de otro integrante.");
                }
            }
        }
        return Response.ok(PrestamoResponseDTO.fromEntity(prestamo)).build();
    }

    @POST
    public Response crearPrestamo(@HeaderParam("Authorization") String authHeader, CrearPrestamoRequestDTO dto) {
        validarToken(authHeader);
        if (!SecurityUtils.INTERNAL_ADMIN_TOKEN.equals(authHeader)) {
            String token = authHeader.substring(7).trim();
            String rol = jwtTokenProvider.getRolFromToken(token);
            Long authIntegranteId = jwtTokenProvider.getIntegranteIdFromToken(token);
            if (rol != null && !"ADMIN".equalsIgnoreCase(rol) && !"DIRECTIVA".equalsIgnoreCase(rol)) {
                if (authIntegranteId != null && dto != null && !authIntegranteId.equals(dto.getIntegranteId())) {
                    throw new SecurityException("No tienes autorizacion para solicitar prestamos a nombre de otro integrante.");
                }
            }
        }
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de la solicitud de prestamo no pueden estar vacios");
        }
        if (dto.getIntegranteId() == null) {
            throw new IllegalArgumentException("Debe especificar el integrante solicitante");
        }

        Integrante integrante = integranteDAO.findById(dto.getIntegranteId())
                .orElseThrow(() -> new IllegalArgumentException("Integrante no encontrado con ID: " + dto.getIntegranteId()));

        Proyecto proyecto = null;
        if (dto.getProyectoId() != null) {
            proyecto = proyectoDAO.findById(dto.getProyectoId())
                    .orElseThrow(() -> new IllegalArgumentException("Proyecto no encontrado con ID: " + dto.getProyectoId()));
        }

        // Recopilar items de materiales solicitados
        List<DetallePrestamoItemDTO> items = new ArrayList<>();
        if (dto.getDetalles() != null && !dto.getDetalles().isEmpty()) {
            items.addAll(dto.getDetalles());
        } else if (dto.getMaterialId() != null) {
            int cant = (dto.getCantidad() != null && dto.getCantidad() > 0) ? dto.getCantidad() : 1;
            items.add(new DetallePrestamoItemDTO(dto.getMaterialId(), cant));
        }

        if (items.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos un material para solicitar el prestamo");
        }

        LocalDateTime fechaDevolucion = dto.getFechaDevolucionPrevista() != null
                ? dto.getFechaDevolucionPrevista()
                : LocalDateTime.now().plusDays(7);

        // Crear la cabecera del prestamo
        Prestamo nuevoPrestamo = new Prestamo();
        nuevoPrestamo.setIntegrante(integrante);
        nuevoPrestamo.setProyecto(proyecto);
        nuevoPrestamo.setFechaSolicitud(LocalDateTime.now());
        nuevoPrestamo.setFechaDevolucionPrevista(fechaDevolucion);
        nuevoPrestamo.setEstado(EstadoPrestamoEnum.SOLICITADO);
        nuevoPrestamo.setDescripcion(dto.getDescripcion());

        Prestamo prestamoGuardado = prestamoDAO.save(nuevoPrestamo);

        // Asociar y persistir los detalles
        for (DetallePrestamoItemDTO item : items) {
            if (item.getMaterialId() == null) continue;
            int cantidadSolicitada = (item.getCantidad() != null && item.getCantidad() > 0) ? item.getCantidad() : 1;

            Material material = materialDAO.findById(item.getMaterialId())
                    .orElseThrow(() -> new IllegalArgumentException("Material no encontrado con ID: " + item.getMaterialId()));

            DetallePrestamo detalle = new DetallePrestamo();
            detalle.setPrestamo(prestamoGuardado);
            detalle.setMaterial(material);
            detalle.setCantidad(cantidadSolicitada);
            detalle.setId(new DetallePrestamoId(prestamoGuardado.getId(), material.getId()));

            detallePrestamoDAO.save(detalle);
            prestamoGuardado.getDetalles().add(detalle);
        }

        return Response.status(Response.Status.CREATED)
                .entity(PrestamoResponseDTO.fromEntity(prestamoGuardado))
                .build();
    }

    @PUT
    @Path("/{id}")
    public Response actualizarPrestamo(
            @PathParam("id") Long id,
            @HeaderParam("Authorization") String authHeader,
            ActualizarPrestamoRequestDTO dto
    ) {
        if (id == null) {
            throw new IllegalArgumentException("El ID del prestamo no puede ser null");
        }
        validarAutorizacion(authHeader);
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de actualizacion no pueden estar vacios");
        }

        Prestamo prestamo = prestamoDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Prestamo no encontrado con ID: " + id));

        if (dto.getEstado() != null && !dto.getEstado().trim().isEmpty()) {
            try {
                EstadoPrestamoEnum nuevoEstado = EstadoPrestamoEnum.valueOf(dto.getEstado().trim().toUpperCase());
                prestamo.setEstado(nuevoEstado);

                if (nuevoEstado == EstadoPrestamoEnum.ENTREGADO && prestamo.getFechaEntrega() == null) {
                    prestamo.setFechaEntrega(LocalDateTime.now());
                } else if (nuevoEstado == EstadoPrestamoEnum.DEVUELTO) {
                    if (prestamo.getFechaEntrega() == null) {
                        prestamo.setFechaEntrega(LocalDateTime.now().minusHours(1));
                    }
                    if (prestamo.getFechaDevolucionReal() == null) {
                        prestamo.setFechaDevolucionReal(LocalDateTime.now());
                    }
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Estado de prestamo invalido: " + dto.getEstado());
            }
        }

        if (dto.getFechaDevolucionPrevista() != null) {
            prestamo.setFechaDevolucionPrevista(dto.getFechaDevolucionPrevista());
        }
        if (dto.getFechaEntrega() != null) {
            prestamo.setFechaEntrega(dto.getFechaEntrega());
        }
        if (dto.getFechaDevolucionReal() != null) {
            prestamo.setFechaDevolucionReal(dto.getFechaDevolucionReal());
        }
        if (dto.getDescripcion() != null) {
            prestamo.setDescripcion(dto.getDescripcion());
        }

        Prestamo actualizado = prestamoDAO.update(prestamo);
        return Response.ok(PrestamoResponseDTO.fromEntity(actualizado)).build();
    }

    @DELETE
    @Path("/{id}")
    @Consumes(MediaType.WILDCARD)
    public Response eliminarPrestamo(@PathParam("id") Long id, @HeaderParam("Authorization") String authHeader) {
        if (id == null) {
            throw new IllegalArgumentException("El ID del prestamo no puede ser null");
        }
        SecurityUtils.validarRolAdministrativo(authHeader);

        Prestamo prestamo = prestamoDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Prestamo no encontrado con ID: " + id));

        try {
            prestamoDAO.delete(prestamo);
            return Response.noContent().build();
        } catch (Exception e) {
            prestamo.setEstado(EstadoPrestamoEnum.CANCELADO);
            prestamoDAO.update(prestamo);
            return Response.ok("El prestamo no pudo eliminarse fisicamente y fue marcado como CANCELADO.").build();
        }
    }

    /**
     * Endpoint protegido para aprobar un prestamo de materiales con bloqueo pesimista.
     * POST /api/prestamos/{id}/aprobar
     */
    @POST
    @Path("/{id}/aprobar")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response aprobarPrestamo(
            @PathParam("id") Long id,
            @HeaderParam("Authorization") String authHeader,
            AprobarPrestamoRequestDTO request
    ) {
        if (id == null) {
            throw new IllegalArgumentException("El ID del prestamo a aprobar no puede ser null");
        }

        // 1. Validar e interceptar la seguridad JWT
        validarAutorizacion(authHeader);

        // 2. Extraer fecha de devolucion prevista opcional
        LocalDateTime fechaLimite = (request != null) ? request.getFechaDevolucionPrevista() : null;

        // 3. Ejecutar logica de negocio transaccional con bloqueo pesimista
        Prestamo prestamoAprobado = (fechaLimite != null)
                ? prestamoService.aprobarPrestamo(id, fechaLimite)
                : prestamoService.aprobarPrestamo(id);

        // 4. Mapear a DTO para proteger el modelo de dominio
        PrestamoResponseDTO responseDTO = PrestamoResponseDTO.fromEntity(prestamoAprobado);

        return Response.ok(responseDTO).build();
    }

    /**
     * Endpoint protegido para registrar la entrega fisica de materiales.
     * POST /api/prestamos/{id}/entregar
     */
    @POST
    @Path("/{id}/entregar")
    @Consumes(MediaType.WILDCARD)
    public Response entregarPrestamo(
            @PathParam("id") Long id,
            @HeaderParam("Authorization") String authHeader
    ) {
        validarAutorizacion(authHeader);
        Prestamo entregado = prestamoService.entregarPrestamo(id);
        return Response.ok(PrestamoResponseDTO.fromEntity(entregado)).build();
    }

    /**
     * Endpoint protegido para registrar la devolucion real de materiales.
     * POST /api/prestamos/{id}/devolver
     */
    @POST
    @Path("/{id}/devolver")
    @Consumes(MediaType.WILDCARD)
    public Response devolverPrestamo(
            @PathParam("id") Long id,
            @HeaderParam("Authorization") String authHeader
    ) {
        validarAutorizacion(authHeader);
        Prestamo devuelto = prestamoService.devolverPrestamo(id);
        return Response.ok(PrestamoResponseDTO.fromEntity(devuelto)).build();
    }

    /**
     * Endpoint para cancelar una solicitud o prestamo aprobado.
     * POST /api/prestamos/{id}/cancelar
     */
    @POST
    @Path("/{id}/cancelar")
    @Consumes(MediaType.WILDCARD)
    public Response cancelarPrestamo(
            @PathParam("id") Long id,
            @HeaderParam("Authorization") String authHeader
    ) {
        validarToken(authHeader);
        String token = authHeader.substring(7).trim();
        String rol = jwtTokenProvider.getRolFromToken(token);
        Long integranteId = jwtTokenProvider.getIntegranteIdFromToken(token);

        Prestamo prestamo = prestamoDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Prestamo no encontrado con ID: " + id));

        if (rol == null || !ROLES_AUTORIZADOS.contains(rol.toUpperCase())) {
            if (integranteId != null && prestamo.getIntegrante() != null && !integranteId.equals(prestamo.getIntegrante().getId())) {
                throw new SecurityException("No puedes cancelar solicitudes pertenecientes a otro integrante.");
            }
        }

        Prestamo cancelado = prestamoService.cancelarPrestamo(id);
        return Response.ok(PrestamoResponseDTO.fromEntity(cancelado)).build();
    }

    /**
     * Endpoint protegido para rechazar una solicitud de prestamo.
     * POST /api/prestamos/{id}/rechazar
     */
    @POST
    @Path("/{id}/rechazar")
    @Consumes(MediaType.WILDCARD)
    public Response rechazarPrestamo(
            @PathParam("id") Long id,
            @HeaderParam("Authorization") String authHeader,
            java.util.Map<String, String> body
    ) {
        validarAutorizacion(authHeader);
        String motivo = (body != null) ? body.get("motivo") : null;
        Prestamo rechazado = prestamoService.rechazarPrestamo(id, motivo);
        return Response.ok(PrestamoResponseDTO.fromEntity(rechazado)).build();
    }

    /**
     * Valida el token JWT recibido.
     */
    private void validarToken(String authHeader) {
        if (SecurityUtils.INTERNAL_ADMIN_TOKEN.equals(authHeader)) {
            return;
        }
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new SecurityException("Cabecera Authorization ausente o invalida. Se requiere token 'Bearer <JWT>'");
        }
        String token = authHeader.substring(7).trim();
        if (!jwtTokenProvider.validateToken(token)) {
            throw new SecurityException("Token JWT invalido, alterado o expirado");
        }
    }

    private void validarAutorizacion(String authHeader) {
        validarToken(authHeader);
        String token = authHeader.substring(7).trim();
        String rol = jwtTokenProvider.getRolFromToken(token);
        if (rol == null || !ROLES_AUTORIZADOS.contains(rol.toUpperCase())) {
            throw new SecurityException(String.format(
                    "Acceso denegado: El rol '%s' no cuenta con privilegios suficientes. Se requiere rol ADMIN o DIRECTIVA.",
                    rol
            ));
        }
    }
}
