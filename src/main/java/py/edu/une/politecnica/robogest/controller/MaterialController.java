package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import py.edu.une.politecnica.robogest.dao.CategoriaDAO;
import py.edu.une.politecnica.robogest.dao.MaterialDAO;
import py.edu.une.politecnica.robogest.dao.impl.CategoriaDAOJpaImpl;
import py.edu.une.politecnica.robogest.dao.impl.MaterialDAOJpaImpl;
import py.edu.une.politecnica.robogest.dto.MaterialRequestDTO;
import py.edu.une.politecnica.robogest.dto.MaterialResponseDTO;
import py.edu.une.politecnica.robogest.entity.Categoria;
import py.edu.une.politecnica.robogest.entity.Material;
import py.edu.une.politecnica.robogest.entity.enums.EstadoMaterialEnum;
import py.edu.une.politecnica.robogest.security.SecurityUtils;

import java.util.List;
import java.util.Objects;

/**
 * Controlador REST con operaciones CRUD completas para la gestion de materiales en el inventario.
 */
@Path("/materiales")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MaterialController {

    private final MaterialDAO materialDAO;
    private final CategoriaDAO categoriaDAO;
    private final py.edu.une.politecnica.robogest.dao.DetallePrestamoDAO detallePrestamoDAO;

    public MaterialController() {
        this.materialDAO = new MaterialDAOJpaImpl();
        this.categoriaDAO = new CategoriaDAOJpaImpl();
        this.detallePrestamoDAO = new py.edu.une.politecnica.robogest.dao.impl.DetallePrestamoDAOJpaImpl();
    }

    public MaterialController(MaterialDAO materialDAO) {
        this.materialDAO = Objects.requireNonNull(materialDAO, "materialDAO no puede ser null");
        this.categoriaDAO = new CategoriaDAOJpaImpl();
        this.detallePrestamoDAO = new py.edu.une.politecnica.robogest.dao.impl.DetallePrestamoDAOJpaImpl();
    }

    public MaterialController(MaterialDAO materialDAO, CategoriaDAO categoriaDAO) {
        this.materialDAO = Objects.requireNonNull(materialDAO, "materialDAO no puede ser null");
        this.categoriaDAO = Objects.requireNonNull(categoriaDAO, "categoriaDAO no puede ser null");
        this.detallePrestamoDAO = new py.edu.une.politecnica.robogest.dao.impl.DetallePrestamoDAOJpaImpl();
    }

    public MaterialController(MaterialDAO materialDAO, CategoriaDAO categoriaDAO, py.edu.une.politecnica.robogest.dao.DetallePrestamoDAO detallePrestamoDAO) {
        this.materialDAO = Objects.requireNonNull(materialDAO, "materialDAO no puede ser null");
        this.categoriaDAO = Objects.requireNonNull(categoriaDAO, "categoriaDAO no puede ser null");
        this.detallePrestamoDAO = Objects.requireNonNull(detallePrestamoDAO, "detallePrestamoDAO no puede ser null");
    }

    @GET
    public Response listarMateriales() {
        List<Material> materiales = materialDAO.findAll();
        List<MaterialResponseDTO> dtos = materiales.stream()
                .map(m -> {
                    int reservas = 0;
                    try {
                        reservas = detallePrestamoDAO.getCantidadReservadaPorMaterial(m.getId());
                    } catch (Exception ignored) {
                    }
                    return MaterialResponseDTO.fromEntity(m, reservas);
                })
                .toList();
        return Response.ok(dtos).build();
    }

    @GET
    @Path("/{id}")
    public Response obtenerMaterial(@PathParam("id") Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID del material no puede ser null");
        }
        Material material = materialDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Material no encontrado con ID: " + id));
        int reservas = 0;
        try {
            reservas = detallePrestamoDAO.getCantidadReservadaPorMaterial(material.getId());
        } catch (Exception ignored) {
        }
        return Response.ok(MaterialResponseDTO.fromEntity(material, reservas)).build();
    }

    @POST
    public Response crearMaterial(MaterialRequestDTO dto, @HeaderParam("Authorization") String authHeader) {
        SecurityUtils.validarRolAdministrativo(authHeader);
        if (dto == null) {
            throw new IllegalArgumentException("El cuerpo de la solicitud no puede estar vacio");
        }
        if (dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del material es obligatorio");
        }
        if (dto.getCategoriaId() == null) {
            throw new IllegalArgumentException("Debe seleccionar una categoria valida");
        }
        if (dto.getCantidadTotal() != null && dto.getCantidadTotal() < 0) {
            throw new IllegalArgumentException("La cantidad total no puede ser negativa");
        }

        Categoria categoria = categoriaDAO.findById(dto.getCategoriaId())
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la categoria con ID: " + dto.getCategoriaId()));

        EstadoMaterialEnum estado = EstadoMaterialEnum.ACTIVO;
        if (dto.getEstado() != null && !dto.getEstado().trim().isEmpty()) {
            try {
                estado = EstadoMaterialEnum.valueOf(dto.getEstado().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Estado de material invalido: " + dto.getEstado());
            }
        }

        Material material = new Material();
        material.setCategoria(categoria);
        material.setNombre(dto.getNombre().trim());
        material.setDescripcion(dto.getDescripcion());
        material.setMarca(dto.getMarca());
        material.setModelo(dto.getModelo());
        material.setCantidadTotal(dto.getCantidadTotal() != null ? dto.getCantidadTotal() : 0);
        material.setEstado(estado);
        material.setUbicacion(dto.getUbicacion());

        Material guardado = materialDAO.save(material);
        return Response.status(Response.Status.CREATED)
                .entity(MaterialResponseDTO.fromEntity(guardado))
                .build();
    }

    @PUT
    @Path("/{id}")
    public Response actualizarMaterial(@PathParam("id") Long id, MaterialRequestDTO dto, @HeaderParam("Authorization") String authHeader) {
        SecurityUtils.validarRolAdministrativo(authHeader);
        if (id == null) {
            throw new IllegalArgumentException("El ID del material no puede ser null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de actualizacion no pueden estar vacios");
        }

        Material material = materialDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Material no encontrado con ID: " + id));

        if (dto.getNombre() != null && !dto.getNombre().trim().isEmpty()) {
            material.setNombre(dto.getNombre().trim());
        }
        if (dto.getCategoriaId() != null) {
            Categoria categoria = categoriaDAO.findById(dto.getCategoriaId())
                    .orElseThrow(() -> new IllegalArgumentException("Categoria no encontrada con ID: " + dto.getCategoriaId()));
            material.setCategoria(categoria);
        }
        if (dto.getDescripcion() != null) {
            material.setDescripcion(dto.getDescripcion());
        }
        if (dto.getMarca() != null) {
            material.setMarca(dto.getMarca());
        }
        if (dto.getModelo() != null) {
            material.setModelo(dto.getModelo());
        }
        if (dto.getCantidadTotal() != null) {
            if (dto.getCantidadTotal() < 0) {
                throw new IllegalArgumentException("La cantidad no puede ser negativa");
            }
            material.setCantidadTotal(dto.getCantidadTotal());
        }
        if (dto.getEstado() != null && !dto.getEstado().trim().isEmpty()) {
            try {
                material.setEstado(EstadoMaterialEnum.valueOf(dto.getEstado().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Estado de material invalido: " + dto.getEstado());
            }
        }
        if (dto.getUbicacion() != null) {
            material.setUbicacion(dto.getUbicacion());
        }

        Material actualizado = materialDAO.update(material);
        return Response.ok(MaterialResponseDTO.fromEntity(actualizado)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response eliminarMaterial(@PathParam("id") Long id, @HeaderParam("Authorization") String authHeader) {
        SecurityUtils.validarRolAdministrativo(authHeader);
        if (id == null) {
            throw new IllegalArgumentException("El ID del material no puede ser null");
        }
        Material material = materialDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Material no encontrado con ID: " + id));

        try {
            materialDAO.delete(material);
            return Response.noContent().build();
        } catch (Exception e) {
            throw new IllegalStateException("No es posible eliminar el material porque esta referenciado en prestamos o proyectos. Cambie su estado a 'BAJA' o 'MANTENIMIENTO' en su lugar.");
        }
    }

    public Response crearMaterial(MaterialRequestDTO dto) {
        return crearMaterial(dto, SecurityUtils.INTERNAL_ADMIN_TOKEN);
    }

    public Response actualizarMaterial(Long id, MaterialRequestDTO dto) {
        return actualizarMaterial(id, dto, SecurityUtils.INTERNAL_ADMIN_TOKEN);
    }

    public Response eliminarMaterial(Long id) {
        return eliminarMaterial(id, SecurityUtils.INTERNAL_ADMIN_TOKEN);
    }
}
