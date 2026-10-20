package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.mindrot.jbcrypt.BCrypt;
import py.edu.une.politecnica.robogest.dao.IntegranteDAO;
import py.edu.une.politecnica.robogest.dao.impl.IntegranteDAOJpaImpl;
import py.edu.une.politecnica.robogest.dto.IntegranteRequestDTO;
import py.edu.une.politecnica.robogest.dto.IntegranteResponseDTO;
import py.edu.une.politecnica.robogest.entity.Integrante;
import py.edu.une.politecnica.robogest.entity.enums.CarreraEnum;
import py.edu.une.politecnica.robogest.entity.enums.EstadoIntegranteEnum;
import py.edu.une.politecnica.robogest.entity.enums.RolEnum;

import py.edu.une.politecnica.robogest.security.JwtTokenProvider;
import py.edu.une.politecnica.robogest.security.SecurityUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Controlador REST con operaciones CRUD completas para la gestion del padron de integrantes.
 */
@Path("/integrantes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IntegranteController {

    private final IntegranteDAO integranteDAO;
    private final JwtTokenProvider jwtTokenProvider;

    public IntegranteController() {
        this.integranteDAO = new IntegranteDAOJpaImpl();
        this.jwtTokenProvider = new JwtTokenProvider();
    }

    public IntegranteController(IntegranteDAO integranteDAO) {
        this.integranteDAO = Objects.requireNonNull(integranteDAO, "integranteDAO no puede ser null");
        this.jwtTokenProvider = new JwtTokenProvider();
    }

    public IntegranteController(IntegranteDAO integranteDAO, JwtTokenProvider jwtTokenProvider) {
        this.integranteDAO = Objects.requireNonNull(integranteDAO, "integranteDAO no puede ser null");
        this.jwtTokenProvider = Objects.requireNonNull(jwtTokenProvider, "jwtTokenProvider no puede ser null");
    }

    @GET
    @Path("/perfil")
    public Response obtenerMiPerfil(@HeaderParam("Authorization") String authHeader) {
        Integrante integrante = extraerIntegranteAutenticado(authHeader);
        return Response.ok(IntegranteResponseDTO.fromEntity(integrante)).build();
    }

    @PUT
    @Path("/perfil")
    public Response actualizarMiPerfil(@HeaderParam("Authorization") String authHeader, IntegranteRequestDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de actualizacion no pueden estar vacios");
        }
        Integrante integrante = extraerIntegranteAutenticado(authHeader);

        if (dto.getNombre() != null && !dto.getNombre().trim().isEmpty()) {
            integrante.setNombre(dto.getNombre().trim());
        }
        if (dto.getApellido() != null && !dto.getApellido().trim().isEmpty()) {
            integrante.setApellido(dto.getApellido().trim());
        }
        if (dto.getTelefono() != null) {
            integrante.setTelefono(dto.getTelefono().trim());
        }
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            String nuevoEmail = dto.getEmail().trim().toLowerCase();
            if (!nuevoEmail.equalsIgnoreCase(integrante.getEmail())) {
                Optional<Integrante> existente = integranteDAO.findByEmail(nuevoEmail);
                if (existente.isPresent() && !existente.get().getId().equals(integrante.getId())) {
                    throw new IllegalArgumentException("Ya existe otro integrante con el email: " + nuevoEmail);
                }
                integrante.setEmail(nuevoEmail);
            }
        }
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            integrante.setPasswordHash(BCrypt.hashpw(dto.getPassword().trim(), BCrypt.gensalt(10)));
        }

        Integrante actualizado = integranteDAO.update(integrante);
        return Response.ok(IntegranteResponseDTO.fromEntity(actualizado)).build();
    }

    private Integrante extraerIntegranteAutenticado(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new WebApplicationException("Token de autorizacion ausente o formato invalido", Response.Status.UNAUTHORIZED);
        }
        String token = authHeader.substring("Bearer ".length()).trim();
        if (!jwtTokenProvider.validateToken(token)) {
            throw new WebApplicationException("Token de autorizacion invalido o expirado", Response.Status.UNAUTHORIZED);
        }
        Long integranteId = jwtTokenProvider.getIntegranteIdFromToken(token);
        if (integranteId != null) {
            Optional<Integrante> opt = integranteDAO.findById(integranteId);
            if (opt.isPresent()) {
                return opt.get();
            }
        }
        String email = jwtTokenProvider.getUsernameFromToken(token);
        if (email != null) {
            return integranteDAO.findByEmail(email)
                    .orElseThrow(() -> new NotFoundException("Integrante autenticado no encontrado"));
        }
        throw new NotFoundException("Integrante autenticado no encontrado");
    }

    @GET
    public Response listarIntegrantes() {
        List<Integrante> integrantes = integranteDAO.findAll();
        List<IntegranteResponseDTO> dtos = integrantes.stream()
                .map(IntegranteResponseDTO::fromEntity)
                .toList();
        return Response.ok(dtos).build();
    }

    @GET
    @Path("/{id}")
    public Response obtenerIntegrante(@PathParam("id") Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID del integrante no puede ser null");
        }
        Integrante integrante = integranteDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Integrante no encontrado con ID: " + id));
        return Response.ok(IntegranteResponseDTO.fromEntity(integrante)).build();
    }

    @POST
    public Response crearIntegrante(IntegranteRequestDTO dto, @HeaderParam("Authorization") String authHeader) {
        SecurityUtils.validarRolAdministrativo(authHeader);
        if (dto == null) {
            throw new IllegalArgumentException("El cuerpo de la solicitud no puede estar vacio");
        }
        if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        if (dto.getApellido() == null || dto.getApellido().trim().isEmpty()) {
            throw new IllegalArgumentException("El apellido es obligatorio");
        }
        if (dto.getCi() == null || dto.getCi().trim().isEmpty()) {
            throw new IllegalArgumentException("El numero de cedula (CI) es obligatorio");
        }
        if (dto.getCarnetUniversitario() == null || dto.getCarnetUniversitario().trim().isEmpty()) {
            throw new IllegalArgumentException("El carnet universitario es obligatorio");
        }
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("El correo electronico es obligatorio");
        }

        String ci = dto.getCi().trim();
        if (integranteDAO.findByCi(ci).isPresent()) {
            throw new IllegalArgumentException("Ya existe un integrante registrado con la cedula (CI): " + ci);
        }

        String email = dto.getEmail().trim().toLowerCase();
        if (integranteDAO.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Ya existe un integrante registrado con el email: " + email);
        }

        CarreraEnum carrera = null;
        if (dto.getCarrera() != null && !dto.getCarrera().trim().isEmpty()) {
            try {
                carrera = CarreraEnum.valueOf(dto.getCarrera().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Carrera invalida: " + dto.getCarrera());
            }
        }

        RolEnum rol = RolEnum.MIEMBRO;
        if (dto.getRol() != null && !dto.getRol().trim().isEmpty()) {
            try {
                rol = RolEnum.valueOf(dto.getRol().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Rol invalido: " + dto.getRol());
            }
        }

        EstadoIntegranteEnum estado = EstadoIntegranteEnum.ACTIVO;
        if (dto.getEstado() != null && !dto.getEstado().trim().isEmpty()) {
            try {
                estado = EstadoIntegranteEnum.valueOf(dto.getEstado().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Estado invalido: " + dto.getEstado());
            }
        }

        String rawPassword = (dto.getPassword() != null && !dto.getPassword().trim().isEmpty())
                ? dto.getPassword().trim()
                : ci;
        String passwordHash = BCrypt.hashpw(rawPassword, BCrypt.gensalt(10));

        Integrante nuevo = new Integrante();
        nuevo.setNombre(dto.getNombre().trim());
        nuevo.setApellido(dto.getApellido().trim());
        nuevo.setCi(ci);
        nuevo.setCarnetUniversitario(dto.getCarnetUniversitario().trim());
        nuevo.setTelefono(dto.getTelefono() != null ? dto.getTelefono().trim() : null);
        nuevo.setEmail(email);
        nuevo.setFechaIngreso(LocalDate.now());
        nuevo.setCarrera(carrera);
        nuevo.setRol(rol);
        nuevo.setEstado(estado);
        String nfcUidVal = null;
        if (dto.getNfcUid() != null && !dto.getNfcUid().trim().isEmpty()) {
            nfcUidVal = dto.getNfcUid().trim().toUpperCase();
            Optional<Integrante> existenteNfc = integranteDAO.findByNfcUid(nfcUidVal);
            if (existenteNfc.isPresent()) {
                Integrante otro = existenteNfc.get();
                otro.setNfcUid(null);
                integranteDAO.update(otro);
            }
        }
        nuevo.setNfcUid(nfcUidVal);
        nuevo.setPasswordHash(passwordHash);

        Integrante guardado = integranteDAO.save(nuevo);
        return Response.status(Response.Status.CREATED)
                .entity(IntegranteResponseDTO.fromEntity(guardado))
                .build();
    }

    @PUT
    @Path("/{id}")
    public Response actualizarIntegrante(@PathParam("id") Long id, IntegranteRequestDTO dto, @HeaderParam("Authorization") String authHeader) {
        SecurityUtils.validarRolAdministrativo(authHeader);
        if (id == null) {
            throw new IllegalArgumentException("El ID del integrante no puede ser null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de actualizacion no pueden estar vacios");
        }

        Integrante integrante = integranteDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Integrante no encontrado con ID: " + id));

        if (dto.getCi() != null && !dto.getCi().trim().isEmpty()) {
            String nuevoCi = dto.getCi().trim();
            if (!nuevoCi.equalsIgnoreCase(integrante.getCi())) {
                Optional<Integrante> existente = integranteDAO.findByCi(nuevoCi);
                if (existente.isPresent() && !existente.get().getId().equals(id)) {
                    throw new IllegalArgumentException("Ya existe otro integrante con la cedula (CI): " + nuevoCi);
                }
                integrante.setCi(nuevoCi);
            }
        }

        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            String nuevoEmail = dto.getEmail().trim().toLowerCase();
            if (!nuevoEmail.equalsIgnoreCase(integrante.getEmail())) {
                Optional<Integrante> existente = integranteDAO.findByEmail(nuevoEmail);
                if (existente.isPresent() && !existente.get().getId().equals(id)) {
                    throw new IllegalArgumentException("Ya existe otro integrante con el email: " + nuevoEmail);
                }
                integrante.setEmail(nuevoEmail);
            }
        }

        if (dto.getNombre() != null && !dto.getNombre().trim().isEmpty()) {
            integrante.setNombre(dto.getNombre().trim());
        }
        if (dto.getApellido() != null && !dto.getApellido().trim().isEmpty()) {
            integrante.setApellido(dto.getApellido().trim());
        }
        if (dto.getCarnetUniversitario() != null && !dto.getCarnetUniversitario().trim().isEmpty()) {
            integrante.setCarnetUniversitario(dto.getCarnetUniversitario().trim());
        }
        if (dto.getTelefono() != null) {
            integrante.setTelefono(dto.getTelefono().trim());
        }
        if (dto.getCarrera() != null && !dto.getCarrera().trim().isEmpty()) {
            try {
                integrante.setCarrera(CarreraEnum.valueOf(dto.getCarrera().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Carrera invalida: " + dto.getCarrera());
            }
        }
        if (dto.getRol() != null && !dto.getRol().trim().isEmpty()) {
            try {
                integrante.setRol(RolEnum.valueOf(dto.getRol().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Rol invalido: " + dto.getRol());
            }
        }
        if (dto.getEstado() != null && !dto.getEstado().trim().isEmpty()) {
            try {
                integrante.setEstado(EstadoIntegranteEnum.valueOf(dto.getEstado().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Estado invalido: " + dto.getEstado());
            }
        }
        if (dto.getNfcUid() != null) {
            if (!dto.getNfcUid().trim().isEmpty()) {
                String nuevoNfc = dto.getNfcUid().trim().toUpperCase();
                Optional<Integrante> existenteNfc = integranteDAO.findByNfcUid(nuevoNfc);
                if (existenteNfc.isPresent() && !existenteNfc.get().getId().equals(id)) {
                    Integrante otro = existenteNfc.get();
                    otro.setNfcUid(null);
                    integranteDAO.update(otro);
                }
                integrante.setNfcUid(nuevoNfc);
            } else {
                integrante.setNfcUid(null);
            }
        }
        if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
            integrante.setPasswordHash(BCrypt.hashpw(dto.getPassword().trim(), BCrypt.gensalt(10)));
        }

        Integrante actualizado = integranteDAO.update(integrante);
        return Response.ok(IntegranteResponseDTO.fromEntity(actualizado)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response eliminarIntegrante(@PathParam("id") Long id, @HeaderParam("Authorization") String authHeader) {
        SecurityUtils.validarRolAdministrativo(authHeader);
        if (id == null) {
            throw new IllegalArgumentException("El ID del integrante no puede ser null");
        }
        Integrante integrante = integranteDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Integrante no encontrado con ID: " + id));

        try {
            integranteDAO.delete(integrante);
            return Response.noContent().build();
        } catch (Exception e) {
            // Si existen registros relacionados de prestamos o asistencias, aplicar baja logica
            integrante.setEstado(EstadoIntegranteEnum.INACTIVO);
            integranteDAO.update(integrante);
            return Response.ok()
                    .entity("El integrante posee registros historicos (asistencias/prestamos); su estado fue actualizado a INACTIVO.")
                    .build();
        }
    }

    public Response crearIntegrante(IntegranteRequestDTO dto) {
        return crearIntegrante(dto, SecurityUtils.INTERNAL_ADMIN_TOKEN);
    }

    public Response actualizarIntegrante(Long id, IntegranteRequestDTO dto) {
        return actualizarIntegrante(id, dto, SecurityUtils.INTERNAL_ADMIN_TOKEN);
    }

    public Response eliminarIntegrante(Long id) {
        return eliminarIntegrante(id, SecurityUtils.INTERNAL_ADMIN_TOKEN);
    }
}
