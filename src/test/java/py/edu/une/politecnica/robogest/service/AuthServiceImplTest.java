package py.edu.une.politecnica.robogest.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import py.edu.une.politecnica.robogest.dao.IntegranteDAO;
import py.edu.une.politecnica.robogest.entity.Integrante;
import py.edu.une.politecnica.robogest.entity.enums.CarreraEnum;
import py.edu.une.politecnica.robogest.entity.enums.EstadoIntegranteEnum;
import py.edu.une.politecnica.robogest.entity.enums.RolEnum;
import py.edu.une.politecnica.robogest.exception.CredencialesInvalidasException;
import py.edu.une.politecnica.robogest.exception.IntegranteInactivoException;
import py.edu.une.politecnica.robogest.security.JwtTokenProvider;
import py.edu.une.politecnica.robogest.service.impl.AuthServiceImpl;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private IntegranteDAO integranteDAO;

    private JwtTokenProvider jwtTokenProvider;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        authService = new AuthServiceImpl(integranteDAO, jwtTokenProvider);
    }

    private Integrante crearIntegranteValido(String passwordPlana, EstadoIntegranteEnum estado) {
        String hash = BCrypt.hashpw(passwordPlana, BCrypt.gensalt());
        Integrante integrante = new Integrante(
                "Carlos",
                "Gomez",
                "3456789",
                "FP-2022-09",
                "0971234567",
                "carlos.gomez@fpune.edu.py",
                LocalDate.now(),
                estado,
                "AABBCCDD",
                CarreraEnum.INGENIERIA_DE_SISTEMAS,
                RolEnum.ADMIN,
                hash
        );
        integrante.setId(10L);
        return integrante;
    }

    @Test
    @DisplayName("Debe autenticar exitosamente y retornar token JWT valido con claims correctos")
    void testAutenticar_Exitoso() {
        String passwordPlana = "ClaveSecreta123!";
        Integrante integrante = crearIntegranteValido(passwordPlana, EstadoIntegranteEnum.ACTIVO);

        when(integranteDAO.findByEmail("carlos.gomez@fpune.edu.py")).thenReturn(Optional.of(integrante));

        String token = authService.autenticar("carlos.gomez@fpune.edu.py", passwordPlana);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("carlos.gomez@fpune.edu.py", jwtTokenProvider.getUsernameFromToken(token));
        assertEquals("ADMIN", jwtTokenProvider.getRolFromToken(token));
    }

    @Test
    @DisplayName("Debe permitir autenticarse utilizando el numero de cedula")
    void testAutenticar_PorCedula_Exitoso() {
        String passwordPlana = "ClaveSecreta123!";
        Integrante integrante = crearIntegranteValido(passwordPlana, EstadoIntegranteEnum.ACTIVO);

        when(integranteDAO.findByEmail("3456789")).thenReturn(Optional.empty());
        when(integranteDAO.findByCi("3456789")).thenReturn(Optional.of(integrante));

        String token = authService.autenticar("3456789", passwordPlana);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("carlos.gomez@fpune.edu.py", jwtTokenProvider.getUsernameFromToken(token));
    }

    @Test
    @DisplayName("Debe lanzar CredencialesInvalidasException cuando la contraseña es incorrecta")
    void testAutenticar_PasswordIncorrecta() {
        Integrante integrante = crearIntegranteValido("ClaveCorrecta123", EstadoIntegranteEnum.ACTIVO);
        when(integranteDAO.findByEmail("carlos.gomez@fpune.edu.py")).thenReturn(Optional.of(integrante));

        assertThrows(CredencialesInvalidasException.class, () ->
                authService.autenticar("carlos.gomez@fpune.edu.py", "PasswordEquivocada"));
    }

    @Test
    @DisplayName("Debe lanzar IntegranteInactivoException cuando el usuario esta inactivo")
    void testAutenticar_IntegranteInactivo() {
        Integrante integrante = crearIntegranteValido("Clave123", EstadoIntegranteEnum.INACTIVO);
        when(integranteDAO.findByEmail("carlos.gomez@fpune.edu.py")).thenReturn(Optional.of(integrante));

        assertThrows(IntegranteInactivoException.class, () ->
                authService.autenticar("carlos.gomez@fpune.edu.py", "Clave123"));
    }

    @Test
    @DisplayName("Debe lanzar CredencialesInvalidasException cuando no existe el usuario")
    void testAutenticar_UsuarioNoExiste() {
        when(integranteDAO.findByEmail("inexistente@fpune.edu.py")).thenReturn(Optional.empty());
        when(integranteDAO.findByCi("inexistente@fpune.edu.py")).thenReturn(Optional.empty());

        assertThrows(CredencialesInvalidasException.class, () ->
                authService.autenticar("inexistente@fpune.edu.py", "Cualquiera123"));
    }
}
