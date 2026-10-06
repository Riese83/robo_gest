package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import py.edu.une.politecnica.robogest.dto.AprobarPrestamoRequestDTO;
import py.edu.une.politecnica.robogest.dto.PrestamoResponseDTO;
import py.edu.une.politecnica.robogest.entity.Integrante;
import py.edu.une.politecnica.robogest.entity.Prestamo;
import py.edu.une.politecnica.robogest.entity.enums.CarreraEnum;
import py.edu.une.politecnica.robogest.entity.enums.EstadoIntegranteEnum;
import py.edu.une.politecnica.robogest.entity.enums.EstadoPrestamoEnum;
import py.edu.une.politecnica.robogest.entity.enums.RolEnum;
import py.edu.une.politecnica.robogest.security.JwtTokenProvider;
import py.edu.une.politecnica.robogest.service.PrestamoService;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrestamoControllerTest {

    @Mock
    private PrestamoService prestamoService;

    private JwtTokenProvider jwtTokenProvider;
    private PrestamoController prestamoController;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        prestamoController = new PrestamoController(prestamoService, jwtTokenProvider);
    }

    private String generarTokenConRol(String email, RolEnum rol) {
        Integrante integrante = new Integrante(
                "Admin", "User", "1234567", "FP-2020-01", "0981000999",
                email, LocalDate.now(), EstadoIntegranteEnum.ACTIVO,
                "ADMIN_NFC", CarreraEnum.INGENIERIA_DE_SISTEMAS, rol, "hash"
        );
        integrante.setId(1L);
        return jwtTokenProvider.generateToken(integrante);
    }

    @Test
    @DisplayName("Debe permitir aprobacion a usuario con rol ADMIN y retornar HTTP 200 con PrestamoResponseDTO")
    void testAprobarPrestamo_AdminExitoso() {
        Long prestamoId = 200L;
        String tokenAdmin = generarTokenConRol("admin@fpune.edu.py", RolEnum.ADMIN);
        String authHeader = "Bearer " + tokenAdmin;

        Integrante integranteSolicitante = new Integrante(
                "Juan", "Perez", "4888999", "FP-2023-40", "0981444333",
                "juan@fpune.edu.py", LocalDate.now(), EstadoIntegranteEnum.ACTIVO,
                "JUAN_NFC", CarreraEnum.INGENIERIA_ELECTRICA, RolEnum.MIEMBRO, "hash"
        );
        integranteSolicitante.setId(2L);

        Prestamo prestamoAprobado = new Prestamo(
                integranteSolicitante, null, LocalDateTime.now(), null,
                LocalDateTime.now().plusDays(7), null, EstadoPrestamoEnum.APROBADO, "Para robot"
        );
        prestamoAprobado.setId(prestamoId);

        when(prestamoService.aprobarPrestamo(eq(prestamoId), any(LocalDateTime.class))).thenReturn(prestamoAprobado);

        LocalDateTime fechaDevolucion = LocalDateTime.now().plusDays(10);
        AprobarPrestamoRequestDTO request = new AprobarPrestamoRequestDTO(fechaDevolucion);

        Response response = prestamoController.aprobarPrestamo(prestamoId, authHeader, request);

        assertEquals(200, response.getStatus());
        assertNotNull(response.getEntity());
        assertInstanceOf(PrestamoResponseDTO.class, response.getEntity());

        PrestamoResponseDTO dto = (PrestamoResponseDTO) response.getEntity();
        assertEquals(prestamoId, dto.getPrestamoId());
        assertEquals("APROBADO", dto.getEstado());
        assertEquals("Juan Perez", dto.getNombreIntegrante());
    }

    @Test
    @DisplayName("Debe rechazar con SecurityException si falta la cabecera Authorization")
    void testAprobarPrestamo_SinCabecera() {
        assertThrows(SecurityException.class, () ->
                prestamoController.aprobarPrestamo(1L, null, null));
    }

    @Test
    @DisplayName("Debe rechazar con SecurityException si el rol es MIEMBRO (no autorizado)")
    void testAprobarPrestamo_RolNoAutorizado() {
        String tokenMiembro = generarTokenConRol("miembro@fpune.edu.py", RolEnum.MIEMBRO);
        String authHeader = "Bearer " + tokenMiembro;

        SecurityException ex = assertThrows(SecurityException.class, () ->
                prestamoController.aprobarPrestamo(1L, authHeader, null));

        assertTrue(ex.getMessage().contains("Acceso denegado"));
        verify(prestamoService, never()).aprobarPrestamo(any());
    }
}
