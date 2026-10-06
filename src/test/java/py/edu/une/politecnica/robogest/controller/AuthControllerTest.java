package py.edu.une.politecnica.robogest.controller;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import py.edu.une.politecnica.robogest.dto.AuthResponseDTO;
import py.edu.une.politecnica.robogest.dto.LoginRequestDTO;
import py.edu.une.politecnica.robogest.entity.Integrante;
import py.edu.une.politecnica.robogest.entity.enums.CarreraEnum;
import py.edu.une.politecnica.robogest.entity.enums.EstadoIntegranteEnum;
import py.edu.une.politecnica.robogest.entity.enums.RolEnum;
import py.edu.une.politecnica.robogest.exception.CredencialesInvalidasException;
import py.edu.une.politecnica.robogest.security.JwtTokenProvider;
import py.edu.une.politecnica.robogest.service.AuthService;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private JwtTokenProvider jwtTokenProvider;
    private AuthController authController;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        authController = new AuthController(authService, jwtTokenProvider);
    }

    @Test
    @DisplayName("Debe autenticar y retornar HTTP 200 con AuthResponseDTO")
    void testLogin_Exitoso() {
        Integrante integrante = new Integrante(
                "Lucas", "Gimenez", "4111222", "FP-2023-01", "0981111222",
                "lucas@fpune.edu.py", LocalDate.now(), EstadoIntegranteEnum.ACTIVO,
                "A1B2C3", CarreraEnum.INGENIERIA_DE_SISTEMAS, RolEnum.MIEMBRO, "hash"
        );
        integrante.setId(5L);

        String token = jwtTokenProvider.generateToken(integrante);

        when(authService.autenticar("lucas@fpune.edu.py", "123456")).thenReturn(token);

        LoginRequestDTO request = new LoginRequestDTO("lucas@fpune.edu.py", "123456");
        Response response = authController.login(request);

        assertEquals(200, response.getStatus());
        assertNotNull(response.getEntity());
        assertInstanceOf(AuthResponseDTO.class, response.getEntity());

        AuthResponseDTO authDTO = (AuthResponseDTO) response.getEntity();
        assertEquals(token, authDTO.getToken());
        assertEquals("lucas@fpune.edu.py", authDTO.getEmail());
        assertEquals("MIEMBRO", authDTO.getRol());
        assertEquals(5L, authDTO.getIntegranteId());
    }

    @Test
    @DisplayName("Debe propagar CredencialesInvalidasException cuando el servicio falla")
    void testLogin_CredencialesInvalidas() {
        when(authService.autenticar("wrong@fpune.edu.py", "bad_pass"))
                .thenThrow(new CredencialesInvalidasException("Credenciales invalidas"));

        LoginRequestDTO request = new LoginRequestDTO("wrong@fpune.edu.py", "bad_pass");

        assertThrows(CredencialesInvalidasException.class, () -> authController.login(request));
    }
}
