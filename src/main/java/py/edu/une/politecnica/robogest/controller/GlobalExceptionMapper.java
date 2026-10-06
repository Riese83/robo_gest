package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import py.edu.une.politecnica.robogest.dto.ErrorResponseDTO;
import py.edu.une.politecnica.robogest.exception.*;

/**
 * Manejador global de excepciones para transformar errores de negocio en respuestas HTTP estructuradas.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    @Context
    private UriInfo uriInfo;

    public GlobalExceptionMapper() {
    }

    public GlobalExceptionMapper(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    @Override
    public Response toResponse(Throwable exception) {
        String path = (uriInfo != null && uriInfo.getRequestUri() != null)
                ? uriInfo.getRequestUri().getPath()
                : "/api";

        Response.Status status;
        String errorName;
        String message = exception.getMessage() != null ? exception.getMessage() : "Error interno del servidor";

        if (exception instanceof CredencialesInvalidasException) {
            status = Response.Status.UNAUTHORIZED;
            errorName = "Unauthorized";
        } else if (exception instanceof IntegranteInactivoException) {
            status = Response.Status.FORBIDDEN;
            errorName = "Forbidden";
        } else if (exception instanceof IntegranteNoEncontradoException || exception instanceof PrestamoNoEncontradoException) {
            status = Response.Status.NOT_FOUND;
            errorName = "Not Found";
        } else if (exception instanceof StockInsuficienteException) {
            status = Response.Status.CONFLICT; // 409 Conflict ante colision de stock
            errorName = "Conflict";
        } else if (exception instanceof EstadoPrestamoInvalidoException) {
            status = Response.Status.BAD_REQUEST;
            errorName = "Bad Request";
        } else if (exception instanceof SecurityException) {
            status = Response.Status.FORBIDDEN;
            errorName = "Forbidden";
        } else if (exception instanceof IllegalArgumentException || exception instanceof IllegalStateException) {
            status = Response.Status.BAD_REQUEST;
            errorName = "Bad Request";
        } else {
            status = Response.Status.INTERNAL_SERVER_ERROR;
            errorName = "Internal Server Error";
            message = "Ocurrio un error inesperado al procesar la solicitud";
        }

        ErrorResponseDTO errorDTO = new ErrorResponseDTO(status.getStatusCode(), errorName, message, path);

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(errorDTO)
                .build();
    }
}
