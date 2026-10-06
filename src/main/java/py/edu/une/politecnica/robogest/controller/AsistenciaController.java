package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import py.edu.une.politecnica.robogest.dao.impl.AsistenciaDAOJpaImpl;
import py.edu.une.politecnica.robogest.dao.impl.IntegranteDAOJpaImpl;
import py.edu.une.politecnica.robogest.dto.MarcacionRequestDTO;
import py.edu.une.politecnica.robogest.dto.MarcacionResponseDTO;
import py.edu.une.politecnica.robogest.entity.Asistencia;
import py.edu.une.politecnica.robogest.service.AsistenciaService;
import py.edu.une.politecnica.robogest.service.impl.AsistenciaServiceImpl;
import py.edu.une.politecnica.robogest.util.JpaUtil;

import java.util.Objects;

/**
 * Controlador REST para el registro de marcaciones IoT (ESP32 con lector RFID/NFC).
 */
@Path("/asistencia")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    /**
     * Constructor por defecto para el contenedor JAX-RS / Servlets.
     */
    public AsistenciaController() {
        this.asistenciaService = new AsistenciaServiceImpl(
                new IntegranteDAOJpaImpl(),
                new AsistenciaDAOJpaImpl()
        );
    }

    /**
     * Constructor para inyeccion de dependencias.
     */
    public AsistenciaController(AsistenciaService asistenciaService) {
        this.asistenciaService = Objects.requireNonNull(asistenciaService, "asistenciaService no puede ser null");
    }

    /**
     * Endpoint para recepcion de marcaciones IoT desde el ESP32.
     * POST /api/asistencia/marcar
     */
    @POST
    @Path("/marcar")
    public Response marcar(MarcacionRequestDTO request) {
        if (request == null || request.getNfcUid() == null || request.getNfcUid().isBlank()) {
            throw new IllegalArgumentException("El campo 'nfcUid' es requerido para procesar la marcacion");
        }

        Asistencia asistencia = asistenciaService.registrarMarcacion(
                request.getNfcUid(),
                request.getDispositivo()
        );

        String mensaje = String.format("Marcacion de %s registrada con exito para %s %s",
                asistencia.getTipoMarcacion(),
                asistencia.getIntegrante().getNombre(),
                asistencia.getIntegrante().getApellido()
        );

        MarcacionResponseDTO responseDTO = new MarcacionResponseDTO(
                asistencia.getId(),
                asistencia.getIntegrante().getId(),
                asistencia.getIntegrante().getNombre() + " " + asistencia.getIntegrante().getApellido(),
                asistencia.getIntegrante().getCarnetUniversitario(),
                asistencia.getTipoMarcacion().name(),
                asistencia.getFechaHora(),
                asistencia.getDispositivoUtilizado(),
                mensaje
        );

        return Response.ok(responseDTO).build();
    }
}
