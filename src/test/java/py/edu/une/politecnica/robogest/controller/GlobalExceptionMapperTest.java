package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import py.edu.une.politecnica.robogest.dto.ErrorResponseDTO;
import py.edu.une.politecnica.robogest.exception.*;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionMapperTest {

    private GlobalExceptionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new GlobalExceptionMapper();
    }

    @Test
    @DisplayName("CredencialesInvalidasException debe mapearse a HTTP 401 Unauthorized")
    void testCredencialesInvalidas() {
        Response response = mapper.toResponse(new CredencialesInvalidasException("Credenciales invalidas"));
        assertEquals(401, response.getStatus());

        ErrorResponseDTO dto = (ErrorResponseDTO) response.getEntity();
        assertEquals(401, dto.getStatus());
        assertEquals("Unauthorized", dto.getError());
    }

    @Test
    @DisplayName("IntegranteInactivoException debe mapearse a HTTP 403 Forbidden")
    void testIntegranteInactivo() {
        Response response = mapper.toResponse(new IntegranteInactivoException("Integrante inactivo"));
        assertEquals(403, response.getStatus());

        ErrorResponseDTO dto = (ErrorResponseDTO) response.getEntity();
        assertEquals(403, dto.getStatus());
        assertEquals("Forbidden", dto.getError());
    }

    @Test
    @DisplayName("IntegranteNoEncontradoException debe mapearse a HTTP 404 Not Found")
    void testNoEncontrado() {
        Response response = mapper.toResponse(new IntegranteNoEncontradoException("No encontrado"));
        assertEquals(404, response.getStatus());

        ErrorResponseDTO dto = (ErrorResponseDTO) response.getEntity();
        assertEquals(404, dto.getStatus());
        assertEquals("Not Found", dto.getError());
    }

    @Test
    @DisplayName("StockInsuficienteException debe mapearse a HTTP 409 Conflict")
    void testStockInsuficiente() {
        Response response = mapper.toResponse(new StockInsuficienteException(1L, "Arduino", 5, 2));
        assertEquals(409, response.getStatus());

        ErrorResponseDTO dto = (ErrorResponseDTO) response.getEntity();
        assertEquals(409, dto.getStatus());
        assertEquals("Conflict", dto.getError());
        assertTrue(dto.getMessage().contains("Stock insuficiente"));
    }

    @Test
    @DisplayName("EstadoPrestamoInvalidoException debe mapearse a HTTP 400 Bad Request")
    void testEstadoInvalido() {
        Response response = mapper.toResponse(new EstadoPrestamoInvalidoException("Estado no valido"));
        assertEquals(400, response.getStatus());

        ErrorResponseDTO dto = (ErrorResponseDTO) response.getEntity();
        assertEquals(400, dto.getStatus());
        assertEquals("Bad Request", dto.getError());
    }

    @Test
    @DisplayName("SecurityException debe mapearse a HTTP 403 Forbidden")
    void testSecurityException() {
        Response response = mapper.toResponse(new SecurityException("Acceso denegado"));
        assertEquals(403, response.getStatus());

        ErrorResponseDTO dto = (ErrorResponseDTO) response.getEntity();
        assertEquals(403, dto.getStatus());
        assertEquals("Forbidden", dto.getError());
    }

    @Test
    @DisplayName("Excepcion no controlada debe mapearse a HTTP 500 Internal Server Error")
    void testErrorGenerico() {
        Response response = mapper.toResponse(new RuntimeException("Error inesperado en BD"));
        assertEquals(500, response.getStatus());

        ErrorResponseDTO dto = (ErrorResponseDTO) response.getEntity();
        assertEquals(500, dto.getStatus());
        assertEquals("Internal Server Error", dto.getError());
    }

    @Test
    @DisplayName("WebApplicationException (NotFoundException) debe conservar HTTP 404 Not Found")
    void testWebApplicationException() {
        Response response = mapper.toResponse(new jakarta.ws.rs.NotFoundException("Recurso no encontrado"));
        assertEquals(404, response.getStatus());

        ErrorResponseDTO dto = (ErrorResponseDTO) response.getEntity();
        assertEquals(404, dto.getStatus());
        assertEquals("Not Found", dto.getError());
    }
}
