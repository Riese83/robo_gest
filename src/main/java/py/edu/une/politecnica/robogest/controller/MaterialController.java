package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import py.edu.une.politecnica.robogest.dao.MaterialDAO;
import py.edu.une.politecnica.robogest.dao.impl.MaterialDAOJpaImpl;
import py.edu.une.politecnica.robogest.dto.MaterialResponseDTO;
import py.edu.une.politecnica.robogest.entity.Material;

import java.util.List;

/**
 * Controlador REST para la consulta de inventario y stock de materiales.
 */
@Path("/materiales")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MaterialController {

    private final MaterialDAO materialDAO;

    public MaterialController() {
        this.materialDAO = new MaterialDAOJpaImpl();
    }

    public MaterialController(MaterialDAO materialDAO) {
        this.materialDAO = materialDAO;
    }

    @GET
    public Response listarMateriales() {
        List<Material> materiales = materialDAO.findAll();
        List<MaterialResponseDTO> dtos = materiales.stream()
                .map(MaterialResponseDTO::fromEntity)
                .toList();
        return Response.ok(dtos).build();
    }
}
