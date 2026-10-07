package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import py.edu.une.politecnica.robogest.dao.PrestamoDAO;
import py.edu.une.politecnica.robogest.dao.impl.DetallePrestamoDAOJpaImpl;
import py.edu.une.politecnica.robogest.dao.impl.MaterialDAOJpaImpl;
import py.edu.une.politecnica.robogest.dao.impl.PrestamoDAOJpaImpl;
import py.edu.une.politecnica.robogest.dto.AprobarPrestamoRequestDTO;
import py.edu.une.politecnica.robogest.dto.PrestamoResponseDTO;
import py.edu.une.politecnica.robogest.entity.Prestamo;
import py.edu.une.politecnica.robogest.security.JwtTokenProvider;
import py.edu.une.politecnica.robogest.service.PrestamoService;
import py.edu.une.politecnica.robogest.service.impl.PrestamoServiceImpl;
import py.edu.une.politecnica.robogest.util.JpaUtil;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Controlador REST protegido para la gestion y aprobacion transaccional de prestamos.
 */
@Path("/prestamos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PrestamoController {

    private static final Set<String> ROLES_AUTORIZADOS = Set.of("ADMIN", "DIRECTIVA", "COORDINADOR");

    private final PrestamoService prestamoService;
    private final JwtTokenProvider jwtTokenProvider;
    private final PrestamoDAO prestamoDAO;

    /**
     * Constructor por defecto para el contenedor JAX-RS / Servlets.
     */
    public PrestamoController() {
        this.jwtTokenProvider = new JwtTokenProvider();
        this.prestamoDAO = new PrestamoDAOJpaImpl();
        this.prestamoService = new PrestamoServiceImpl(
                this.prestamoDAO,
                new MaterialDAOJpaImpl(),
                new DetallePrestamoDAOJpaImpl()
        );
    }

    /**
     * Constructor para inyeccion de dependencias.
     */
    public PrestamoController(PrestamoService prestamoService, JwtTokenProvider jwtTokenProvider) {
        this.prestamoService = Objects.requireNonNull(prestamoService, "prestamoService no puede ser null");
        this.jwtTokenProvider = Objects.requireNonNull(jwtTokenProvider, "jwtTokenProvider no puede ser null");
        this.prestamoDAO = new PrestamoDAOJpaImpl();
    }

    public PrestamoController(PrestamoService prestamoService, JwtTokenProvider jwtTokenProvider, PrestamoDAO prestamoDAO) {
        this.prestamoService = Objects.requireNonNull(prestamoService, "prestamoService no puede ser null");
        this.jwtTokenProvider = Objects.requireNonNull(jwtTokenProvider, "jwtTokenProvider no puede ser null");
        this.prestamoDAO = Objects.requireNonNull(prestamoDAO, "prestamoDAO no puede ser null");
    }

    @GET
    public Response listarPrestamos(@HeaderParam("Authorization") String authHeader) {
        validarToken(authHeader);
        List<Prestamo> prestamos = prestamoDAO.findAll();
        List<PrestamoResponseDTO> dtos = prestamos.stream()
                .map(PrestamoResponseDTO::fromEntity)
                .toList();
        return Response.ok(dtos).build();
    }

    /**
     * Endpoint protegido para aprobar un prestamo de materiales.
     * POST /api/prestamos/{id}/aprobar
     *
     * Requiere cabecera: Authorization: Bearer <token_jwt>
     * Requiere que el token contenga el rol ADMIN o DIRECTIVA.
     */
    @POST
    @Path("/{id}/aprobar")
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
     * Simula la interceptacion de seguridad validando el token JWT y los roles autorizados.
     */
    private void validarToken(String authHeader) {
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
