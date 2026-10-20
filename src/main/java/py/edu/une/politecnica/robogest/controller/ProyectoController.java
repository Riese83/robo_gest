package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import py.edu.une.politecnica.robogest.dao.*;
import py.edu.une.politecnica.robogest.dao.impl.*;
import py.edu.une.politecnica.robogest.dto.*;
import py.edu.une.politecnica.robogest.entity.*;
import py.edu.une.politecnica.robogest.entity.enums.EstadoProyectoEnum;
import py.edu.une.politecnica.robogest.security.SecurityUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Controlador REST para la gestion de proyectos del club de robotica.
 */
@Path("/proyectos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProyectoController {

    private final ProyectoDAO proyectoDAO;
    private final IntegranteProyectoDAO integranteProyectoDAO;
    private final ProyectoMaterialDAO proyectoMaterialDAO;
    private final IntegranteDAO integranteDAO;
    private final MaterialDAO materialDAO;
    private final DetallePrestamoDAO detallePrestamoDAO;

    public ProyectoController() {
        this.proyectoDAO = new ProyectoDAOJpaImpl();
        this.integranteProyectoDAO = new IntegranteProyectoDAOJpaImpl();
        this.proyectoMaterialDAO = new ProyectoMaterialDAOJpaImpl();
        this.integranteDAO = new IntegranteDAOJpaImpl();
        this.materialDAO = new MaterialDAOJpaImpl();
        this.detallePrestamoDAO = new DetallePrestamoDAOJpaImpl();
    }

    public ProyectoController(ProyectoDAO proyectoDAO) {
        this.proyectoDAO = Objects.requireNonNull(proyectoDAO, "proyectoDAO no puede ser null");
        this.integranteProyectoDAO = new IntegranteProyectoDAOJpaImpl();
        this.proyectoMaterialDAO = new ProyectoMaterialDAOJpaImpl();
        this.integranteDAO = new IntegranteDAOJpaImpl();
        this.materialDAO = new MaterialDAOJpaImpl();
        this.detallePrestamoDAO = new DetallePrestamoDAOJpaImpl();
    }

    public ProyectoController(ProyectoDAO proyectoDAO, IntegranteProyectoDAO integranteProyectoDAO,
                              ProyectoMaterialDAO proyectoMaterialDAO, IntegranteDAO integranteDAO,
                              MaterialDAO materialDAO, DetallePrestamoDAO detallePrestamoDAO) {
        this.proyectoDAO = Objects.requireNonNull(proyectoDAO, "proyectoDAO no puede ser null");
        this.integranteProyectoDAO = Objects.requireNonNull(integranteProyectoDAO, "integranteProyectoDAO no puede ser null");
        this.proyectoMaterialDAO = Objects.requireNonNull(proyectoMaterialDAO, "proyectoMaterialDAO no puede ser null");
        this.integranteDAO = Objects.requireNonNull(integranteDAO, "integranteDAO no puede ser null");
        this.materialDAO = Objects.requireNonNull(materialDAO, "materialDAO no puede ser null");
        this.detallePrestamoDAO = Objects.requireNonNull(detallePrestamoDAO, "detallePrestamoDAO no puede ser null");
    }

    private List<ProyectoParticipanteDTO> mapearParticipantes(Long proyectoId) {
        try {
            return integranteProyectoDAO.findByProyectoId(proyectoId).stream()
                    .map(ip -> {
                        Integrante integ = ip.getIntegrante();
                        return new ProyectoParticipanteDTO(
                                integ.getId(),
                                integ.getNombre() + " " + integ.getApellido(),
                                integ.getCi(),
                                integ.getCarrera() != null ? integ.getCarrera().name() : "GENERAL",
                                integ.getEmail(),
                                ip.getFechaIncorporacion()
                        );
                    })
                    .toList();
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private List<ProyectoMaterialDTO> mapearMateriales(Long proyectoId) {
        try {
            return proyectoMaterialDAO.findByProyectoId(proyectoId).stream()
                    .map(pm -> {
                        Material mat = pm.getMaterial();
                        int reservas = 0;
                        try {
                            reservas = detallePrestamoDAO.getCantidadReservadaPorMaterial(mat.getId());
                        } catch (Exception ignored) {
                        }
                        int total = (mat.getCantidadTotal() != null) ? mat.getCantidadTotal() : 0;
                        int disp = Math.max(0, total - reservas);
                        String cat = (mat.getCategoria() != null) ? mat.getCategoria().getNombre() : "General";
                        return new ProyectoMaterialDTO(
                                mat.getId(),
                                mat.getNombre(),
                                cat,
                                pm.getCantidadPlanificada(),
                                disp
                        );
                    })
                    .toList();
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    @GET
    public Response listarProyectos() {
        List<Proyecto> proyectos = proyectoDAO.findAll();
        List<ProyectoResponseDTO> dtos = proyectos.stream()
                .map(p -> ProyectoResponseDTO.fromEntity(p, mapearParticipantes(p.getId()), mapearMateriales(p.getId())))
                .toList();
        return Response.ok(dtos).build();
    }

    @GET
    @Path("/{id}")
    public Response obtenerProyecto(@PathParam("id") Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID del proyecto no puede ser null");
        }
        Proyecto proyecto = proyectoDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado con ID: " + id));
        return Response.ok(ProyectoResponseDTO.fromEntity(proyecto, mapearParticipantes(id), mapearMateriales(id))).build();
    }

    @POST
    public Response crearProyecto(ProyectoRequestDTO dto, @HeaderParam("Authorization") String authHeader) {
        SecurityUtils.validarRolAdministrativo(authHeader);
        if (dto == null || dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del proyecto es obligatorio");
        }

        LocalDate fechaInicio = dto.getFechaInicio() != null ? dto.getFechaInicio() : LocalDate.now();
        EstadoProyectoEnum estado = EstadoProyectoEnum.PLANIFICADO;
        if (dto.getEstado() != null && !dto.getEstado().trim().isEmpty()) {
            try {
                estado = EstadoProyectoEnum.valueOf(dto.getEstado().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Estado de proyecto invalido: " + dto.getEstado());
            }
        }

        Proyecto proyecto = new Proyecto(dto.getNombre().trim(), dto.getDescripcion(), fechaInicio, dto.getFechaFin(), estado);
        Proyecto guardado = proyectoDAO.save(proyecto);

        return Response.status(Response.Status.CREATED)
                .entity(ProyectoResponseDTO.fromEntity(guardado))
                .build();
    }

    @PUT
    @Path("/{id}")
    public Response actualizarProyecto(@PathParam("id") Long id, ProyectoRequestDTO dto, @HeaderParam("Authorization") String authHeader) {
        SecurityUtils.validarRolAdministrativo(authHeader);
        if (id == null) {
            throw new IllegalArgumentException("El ID del proyecto no puede ser null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de actualizacion no pueden estar vacios");
        }

        Proyecto proyecto = proyectoDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado con ID: " + id));

        if (dto.getNombre() != null && !dto.getNombre().trim().isEmpty()) {
            proyecto.setNombre(dto.getNombre().trim());
        }
        if (dto.getDescripcion() != null) {
            proyecto.setDescripcion(dto.getDescripcion());
        }
        if (dto.getFechaInicio() != null) {
            proyecto.setFechaInicio(dto.getFechaInicio());
        }
        if (dto.getFechaFin() != null) {
            proyecto.setFechaFin(dto.getFechaFin());
        }
        if (dto.getEstado() != null && !dto.getEstado().trim().isEmpty()) {
            try {
                proyecto.setEstado(EstadoProyectoEnum.valueOf(dto.getEstado().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Estado invalido: " + dto.getEstado());
            }
        }

        Proyecto actualizado = proyectoDAO.update(proyecto);
        return Response.ok(ProyectoResponseDTO.fromEntity(actualizado)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response eliminarProyecto(@PathParam("id") Long id, @HeaderParam("Authorization") String authHeader) {
        SecurityUtils.validarRolAdministrativo(authHeader);
        if (id == null) {
            throw new IllegalArgumentException("El ID del proyecto no puede ser null");
        }
        Proyecto proyecto = proyectoDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado con ID: " + id));

        try {
            proyectoDAO.delete(proyecto);
            return Response.noContent().build();
        } catch (Exception e) {
            proyecto.setEstado(EstadoProyectoEnum.CANCELADO);
            proyectoDAO.update(proyecto);
            return Response.ok("El proyecto posee asignaciones activas; su estado fue cambiado a CANCELADO.").build();
        }
    }

    @GET
    @Path("/{id}/integrantes")
    public Response listarIntegrantesProyecto(@PathParam("id") Long id) {
        if (id == null) throw new IllegalArgumentException("El ID del proyecto no puede ser null");
        return Response.ok(mapearParticipantes(id)).build();
    }

    @POST
    @Path("/{id}/integrantes/{integranteId}")
    @Consumes(MediaType.WILDCARD)
    public Response asignarIntegranteProyecto(@PathParam("id") Long id,
                                              @PathParam("integranteId") Long integranteId,
                                              @HeaderParam("Authorization") String authHeader) {
        SecurityUtils.validarRolAdministrativo(authHeader);
        Proyecto proyecto = proyectoDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado con ID: " + id));
        Integrante integrante = integranteDAO.findById(integranteId)
                .orElseThrow(() -> new NotFoundException("Integrante no encontrado con ID: " + integranteId));

        IntegranteProyectoId ipId = new IntegranteProyectoId(integranteId, id);
        if (integranteProyectoDAO.findById(ipId).isEmpty()) {
            IntegranteProyecto ip = new IntegranteProyecto(integrante, proyecto, LocalDate.now());
            integranteProyectoDAO.save(ip);
        }
        return Response.ok(mapearParticipantes(id)).build();
    }

    @DELETE
    @Path("/{id}/integrantes/{integranteId}")
    @Consumes(MediaType.WILDCARD)
    public Response desasignarIntegranteProyecto(@PathParam("id") Long id,
                                                 @PathParam("integranteId") Long integranteId,
                                                 @HeaderParam("Authorization") String authHeader) {
        SecurityUtils.validarRolAdministrativo(authHeader);
        IntegranteProyectoId ipId = new IntegranteProyectoId(integranteId, id);
        integranteProyectoDAO.findById(ipId).ifPresent(integranteProyectoDAO::delete);
        return Response.ok(mapearParticipantes(id)).build();
    }

    @GET
    @Path("/{id}/materiales")
    public Response listarMaterialesProyecto(@PathParam("id") Long id) {
        if (id == null) throw new IllegalArgumentException("El ID del proyecto no puede ser null");
        return Response.ok(mapearMateriales(id)).build();
    }

    @POST
    @Path("/{id}/materiales")
    public Response asignarMaterialProyecto(@PathParam("id") Long id,
                                            Map<String, Object> body,
                                            @HeaderParam("Authorization") String authHeader) {
        SecurityUtils.validarRolAdministrativo(authHeader);
        if (body == null || body.get("materialId") == null) {
            throw new IllegalArgumentException("Debe especificar 'materialId'");
        }
        Long materialId = Long.valueOf(body.get("materialId").toString());
        int cantidad = 1;
        Object cantObj = body.get("cantidadPlanificada") != null ? body.get("cantidadPlanificada") : body.get("cantidad");
        if (cantObj != null) {
            try {
                cantidad = Integer.parseInt(cantObj.toString());
            } catch (Exception ignored) {
            }
        }

        Proyecto proyecto = proyectoDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Proyecto no encontrado con ID: " + id));
        Material material = materialDAO.findById(materialId)
                .orElseThrow(() -> new NotFoundException("Material no encontrado con ID: " + materialId));

        ProyectoMaterialId pmId = new ProyectoMaterialId(id, materialId);
        var existenteOpt = proyectoMaterialDAO.findById(pmId);
        if (existenteOpt.isPresent()) {
            ProyectoMaterial pm = existenteOpt.get();
            pm.setCantidadPlanificada(cantidad);
            proyectoMaterialDAO.update(pm);
        } else {
            ProyectoMaterial pm = new ProyectoMaterial(proyecto, material, cantidad);
            proyectoMaterialDAO.save(pm);
        }
        return Response.ok(mapearMateriales(id)).build();
    }

    @DELETE
    @Path("/{id}/materiales/{materialId}")
    @Consumes(MediaType.WILDCARD)
    public Response desasignarMaterialProyecto(@PathParam("id") Long id,
                                               @PathParam("materialId") Long materialId,
                                               @HeaderParam("Authorization") String authHeader) {
        SecurityUtils.validarRolAdministrativo(authHeader);
        ProyectoMaterialId pmId = new ProyectoMaterialId(id, materialId);
        proyectoMaterialDAO.findById(pmId).ifPresent(proyectoMaterialDAO::delete);
        return Response.ok(mapearMateriales(id)).build();
    }

    public Response crearProyecto(ProyectoRequestDTO dto) {
        return crearProyecto(dto, SecurityUtils.INTERNAL_ADMIN_TOKEN);
    }

    public Response actualizarProyecto(Long id, ProyectoRequestDTO dto) {
        return actualizarProyecto(id, dto, SecurityUtils.INTERNAL_ADMIN_TOKEN);
    }

    public Response eliminarProyecto(Long id) {
        return eliminarProyecto(id, SecurityUtils.INTERNAL_ADMIN_TOKEN);
    }
}
