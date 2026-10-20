package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import py.edu.une.politecnica.robogest.dto.ConsultaIaRequestDTO;
import py.edu.une.politecnica.robogest.dto.ConsultaIaResponseDTO;
import py.edu.une.politecnica.robogest.security.SecurityUtils;
import py.edu.une.politecnica.robogest.service.AsistenteIaService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsistenteControllerTest {

    @Mock
    private AsistenteIaService asistenteIaService;

    @Mock
    private HttpHeaders httpHeaders;

    private AsistenteController asistenteController;

    @BeforeEach
    void setUp() {
        asistenteController = new AsistenteController(asistenteIaService);
    }

    @Test
    @DisplayName("Debe consultar estado del servicio de IA retornando HTTP 200 con token valido")
    void testConsultarEstado() {
        when(httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(SecurityUtils.INTERNAL_ADMIN_TOKEN);

        Response response = asistenteController.consultarEstado(httpHeaders);
        assertEquals(200, response.getStatus());
        assertNotNull(response.getEntity());
    }

    @Test
    @DisplayName("Debe procesar consulta de chat exitosamente retornando HTTP 200")
    void testChatExitoso() {
        when(httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(SecurityUtils.INTERNAL_ADMIN_TOKEN);

        ConsultaIaRequestDTO request = new ConsultaIaRequestDTO();
        request.setMensaje("¿Qué proyectos puedo hacer con Arduino?");

        ConsultaIaResponseDTO mockResp = ConsultaIaResponseDTO.exito("Puedes crear un seguidor de línea...", "Google Gemini 1.5 Flash");
        when(asistenteIaService.consultar(any(ConsultaIaRequestDTO.class), any(), anyString(), anyString())).thenReturn(mockResp);

        Response response = asistenteController.chat(httpHeaders, request);
        assertEquals(200, response.getStatus());

        ConsultaIaResponseDTO entidad = (ConsultaIaResponseDTO) response.getEntity();
        assertTrue(entidad.isExito());
        assertEquals("Puedes crear un seguidor de línea...", entidad.getRespuesta());
    }

    @Test
    @DisplayName("Debe rechazar consulta sin cabecera Authorization")
    void testChatSinAuth() {
        when(httpHeaders.getHeaderString(HttpHeaders.AUTHORIZATION)).thenReturn(null);
        ConsultaIaRequestDTO request = new ConsultaIaRequestDTO();
        request.setMensaje("Hola");

        assertThrows(SecurityException.class, () -> asistenteController.chat(httpHeaders, request));
    }
}
