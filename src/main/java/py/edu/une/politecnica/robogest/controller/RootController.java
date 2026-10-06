package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador raiz para endpoints informativos y verificacion de estado de la API.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class RootController {

    @GET
    public Response getApiInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("aplicacion", "Robo_Gest API");
        info.put("estado", "ONLINE");
        info.put("version", "1.0.0");
        info.put("institucion", "Club de Robotica - FP-UNE");
        info.put("endpoints_disponibles", List.of(
                "POST /api/auth/login",
                "POST /api/asistencia/marcar",
                "POST /api/prestamos/{id}/aprobar"
        ));
        return Response.ok(info).build();
    }
}
