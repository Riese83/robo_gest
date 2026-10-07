package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import py.edu.une.politecnica.robogest.dao.IntegranteDAO;
import py.edu.une.politecnica.robogest.dao.impl.IntegranteDAOJpaImpl;
import py.edu.une.politecnica.robogest.dto.IntegranteResponseDTO;
import py.edu.une.politecnica.robogest.entity.Integrante;

import java.util.List;

/**
 * Controlador REST para la consulta del padron de integrantes del club.
 */
@Path("/integrantes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IntegranteController {

    private final IntegranteDAO integranteDAO;

    public IntegranteController() {
        this.integranteDAO = new IntegranteDAOJpaImpl();
    }

    public IntegranteController(IntegranteDAO integranteDAO) {
        this.integranteDAO = integranteDAO;
    }

    @GET
    public Response listarIntegrantes() {
        List<Integrante> integrantes = integranteDAO.findAll();
        List<IntegranteResponseDTO> dtos = integrantes.stream()
                .map(IntegranteResponseDTO::fromEntity)
                .toList();
        return Response.ok(dtos).build();
    }
}
