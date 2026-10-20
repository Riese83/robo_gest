package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import py.edu.une.politecnica.robogest.dao.CategoriaDAO;
import py.edu.une.politecnica.robogest.dao.impl.CategoriaDAOJpaImpl;
import py.edu.une.politecnica.robogest.dto.CategoriaRequestDTO;
import py.edu.une.politecnica.robogest.dto.CategoriaResponseDTO;
import py.edu.une.politecnica.robogest.entity.Categoria;
import py.edu.une.politecnica.robogest.security.SecurityUtils;

import java.util.List;
import java.util.Objects;

/**
 * Controlador REST para el mantenimiento y consulta de categorias de inventario.
 */
@Path("/categorias")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CategoriaController {

    private final CategoriaDAO categoriaDAO;

    public CategoriaController() {
        this.categoriaDAO = new CategoriaDAOJpaImpl();
    }

    public CategoriaController(CategoriaDAO categoriaDAO) {
        this.categoriaDAO = Objects.requireNonNull(categoriaDAO, "categoriaDAO no puede ser null");
    }

    @GET
    public Response listarCategorias() {
        List<Categoria> categorias = categoriaDAO.findAll();
        List<CategoriaResponseDTO> dtos = categorias.stream()
                .map(CategoriaResponseDTO::fromEntity)
                .toList();
        return Response.ok(dtos).build();
    }

    @GET
    @Path("/{id}")
    public Response obtenerCategoria(@PathParam("id") Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID de la categoria no puede ser null");
        }
        Categoria categoria = categoriaDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoria no encontrada con ID: " + id));
        return Response.ok(CategoriaResponseDTO.fromEntity(categoria)).build();
    }

    @POST
    public Response crearCategoria(CategoriaRequestDTO dto, @HeaderParam("Authorization") String authHeader) {
        SecurityUtils.validarRolAdministrativo(authHeader);
        if (dto == null || dto.getNombre() == null || dto.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la categoria es obligatorio");
        }

        String nombre = dto.getNombre().trim();
        if (categoriaDAO.findByNombre(nombre).isPresent()) {
            throw new IllegalArgumentException("Ya existe una categoria con el nombre: " + nombre);
        }

        Categoria categoria = new Categoria(nombre, dto.getDescripcion());
        Categoria guardada = categoriaDAO.save(categoria);

        return Response.status(Response.Status.CREATED)
                .entity(CategoriaResponseDTO.fromEntity(guardada))
                .build();
    }

    @PUT
    @Path("/{id}")
    public Response actualizarCategoria(@PathParam("id") Long id, CategoriaRequestDTO dto, @HeaderParam("Authorization") String authHeader) {
        SecurityUtils.validarRolAdministrativo(authHeader);
        if (id == null) {
            throw new IllegalArgumentException("El ID de la categoria no puede ser null");
        }
        if (dto == null) {
            throw new IllegalArgumentException("Los datos de actualizacion no pueden estar vacios");
        }

        Categoria categoria = categoriaDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoria no encontrada con ID: " + id));

        if (dto.getNombre() != null && !dto.getNombre().trim().isEmpty()) {
            String nuevoNombre = dto.getNombre().trim();
            if (!nuevoNombre.equalsIgnoreCase(categoria.getNombre())) {
                if (categoriaDAO.findByNombre(nuevoNombre).isPresent()) {
                    throw new IllegalArgumentException("Ya existe otra categoria con el nombre: " + nuevoNombre);
                }
                categoria.setNombre(nuevoNombre);
            }
        }
        if (dto.getDescripcion() != null) {
            categoria.setDescripcion(dto.getDescripcion());
        }

        Categoria actualizada = categoriaDAO.update(categoria);
        return Response.ok(CategoriaResponseDTO.fromEntity(actualizada)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response eliminarCategoria(@PathParam("id") Long id, @HeaderParam("Authorization") String authHeader) {
        SecurityUtils.validarRolAdministrativo(authHeader);
        if (id == null) {
            throw new IllegalArgumentException("El ID de la categoria no puede ser null");
        }
        Categoria categoria = categoriaDAO.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoria no encontrada con ID: " + id));

        try {
            categoriaDAO.delete(categoria);
            return Response.noContent().build();
        } catch (Exception e) {
            throw new IllegalStateException("No se puede eliminar la categoria porque contiene materiales asignados.");
        }
    }

    public Response crearCategoria(CategoriaRequestDTO dto) {
        return crearCategoria(dto, SecurityUtils.INTERNAL_ADMIN_TOKEN);
    }

    public Response actualizarCategoria(Long id, CategoriaRequestDTO dto) {
        return actualizarCategoria(id, dto, SecurityUtils.INTERNAL_ADMIN_TOKEN);
    }

    public Response eliminarCategoria(Long id) {
        return eliminarCategoria(id, SecurityUtils.INTERNAL_ADMIN_TOKEN);
    }
}
