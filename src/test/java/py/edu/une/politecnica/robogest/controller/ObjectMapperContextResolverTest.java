package py.edu.une.politecnica.robogest.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import py.edu.une.politecnica.robogest.dto.ErrorResponseDTO;
import py.edu.une.politecnica.robogest.dto.MarcacionResponseDTO;
import py.edu.une.politecnica.robogest.dto.PrestamoResponseDTO;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ObjectMapperContextResolverTest {

    private ObjectMapperContextResolver contextResolver;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        contextResolver = new ObjectMapperContextResolver();
        objectMapper = contextResolver.getContext(ObjectMapper.class);
    }

    @Test
    @DisplayName("Debe serializar ErrorResponseDTO con LocalDateTime sin lanzar InvalidDefinitionException")
    void testSerializarErrorResponseDTO() throws JsonProcessingException {
        ErrorResponseDTO dto = new ErrorResponseDTO(400, "Bad Request", "Error de prueba", "/api/test");
        dto.setTimestamp(LocalDateTime.of(2026, 10, 6, 15, 30, 0));

        String json = objectMapper.writeValueAsString(dto);

        assertNotNull(json);
        assertTrue(json.contains("\"timestamp\":\"2026-10-06T15:30:00\""));
        assertTrue(json.contains("\"status\":400"));
        assertTrue(json.contains("\"error\":\"Bad Request\""));
    }

    @Test
    @DisplayName("Debe serializar MarcacionResponseDTO con LocalDateTime correctamente")
    void testSerializarMarcacionResponseDTO() throws JsonProcessingException {
        MarcacionResponseDTO dto = new MarcacionResponseDTO(
                1L, 2L, "Juan Perez", "FP-01", "ENTRADA",
                LocalDateTime.of(2026, 10, 6, 8, 0, 0), "ESP32", "Exito"
        );

        String json = objectMapper.writeValueAsString(dto);

        assertNotNull(json);
        assertTrue(json.contains("\"fechaHora\":\"2026-10-06T08:00:00\""));
        assertTrue(json.contains("\"tipoMarcacion\":\"ENTRADA\""));
    }

    @Test
    @DisplayName("Debe serializar PrestamoResponseDTO con LocalDateTime correctamente")
    void testSerializarPrestamoResponseDTO() throws JsonProcessingException {
        PrestamoResponseDTO dto = new PrestamoResponseDTO();
        dto.setPrestamoId(10L);
        dto.setFechaSolicitud(LocalDateTime.of(2026, 10, 6, 10, 0, 0));

        String json = objectMapper.writeValueAsString(dto);

        assertNotNull(json);
        assertTrue(json.contains("\"fechaSolicitud\":\"2026-10-06T10:00:00\""));
    }
}
