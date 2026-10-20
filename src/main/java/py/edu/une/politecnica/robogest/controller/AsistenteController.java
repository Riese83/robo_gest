package py.edu.une.politecnica.robogest.controller;

import io.jsonwebtoken.Claims;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import py.edu.une.politecnica.robogest.dto.ConsultaIaRequestDTO;
import py.edu.une.politecnica.robogest.dto.ConsultaIaResponseDTO;
import py.edu.une.politecnica.robogest.security.SecurityUtils;
import py.edu.une.politecnica.robogest.service.AsistenteIaService;
import py.edu.une.politecnica.robogest.service.impl.AsistenteIaServiceImpl;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Controlador REST para el Asistente Inteligente de Robo_Gest (Google Gemini).
 * Guiado por el inventario real y reglamentos del Club de Robotica FP-UNE.
 */
@Path("/asistente")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AsistenteController {

    private final AsistenteIaService asistenteIaService;

    public AsistenteController() {
        this.asistenteIaService = new AsistenteIaServiceImpl();
    }

    public AsistenteController(AsistenteIaService asistenteIaService) {
        this.asistenteIaService = Objects.requireNonNull(asistenteIaService, "asistenteIaService no puede ser null");
    }

    /**
     * Endpoint para consultar el estado del servicio de IA (si hay una clave global configurada).
     */
    @GET
    @Path("/estado")
    public Response consultarEstado(@Context HttpHeaders headers) {
        SecurityUtils.validarAutenticado(headers.getHeaderString(HttpHeaders.AUTHORIZATION));

        String envKey = System.getenv("GEMINI_API_KEY");
        String propKey = System.getProperty("gemini.api.key");
        boolean configured = (envKey != null && !envKey.trim().isEmpty()) || (propKey != null && !propKey.trim().isEmpty());

        Map<String, Object> status = new HashMap<>();
        status.put("activo", true);
        status.put("proveedor", "Google Gemini Flash");
        status.put("claveServidorConfigurada", configured);
        status.put("mensaje", configured 
                ? "Servicio de IA activo con clave del servidor." 
                : "Servicio disponible; requiere clave de API provista por el usuario o servidor.");

        return Response.ok(status).build();
    }

    /**
     * Procesa una consulta conversacional con el asistente de IA.
     */
    @POST
    @Path("/chat")
    public Response chat(@Context HttpHeaders headers, ConsultaIaRequestDTO request) {
        Claims claims = SecurityUtils.validarAutenticado(headers.getHeaderString(HttpHeaders.AUTHORIZATION));

        Long usuarioId = null;
        Number idNum = claims.get("integranteId", Number.class);
        if (idNum != null) {
            usuarioId = idNum.longValue();
        }

        String nombreCompleto = claims.get("nombreCompleto", String.class);
        if (nombreCompleto == null || nombreCompleto.isBlank()) {
            nombreCompleto = claims.getSubject();
        }

        String rol = claims.get("rol", String.class);
        if (rol == null) {
            rol = "MIEMBRO";
        }

        // Solo ADMIN tiene permiso para sobreescribir la API Key o el modelo; los demas roles utilizan la configuracion del servidor
        if (!"ADMIN".equalsIgnoreCase(rol) && request != null) {
            request.setApiKey(null);
            request.setModelo(null);
        }

        ConsultaIaResponseDTO respuesta = asistenteIaService.consultar(request, usuarioId, nombreCompleto, rol);
        return Response.ok(respuesta).build();
    }
}
