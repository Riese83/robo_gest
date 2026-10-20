package py.edu.une.politecnica.robogest.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilsTest {

    private final JwtTokenProvider tokenProvider = new JwtTokenProvider();

    @Test
    @DisplayName("Debe rechazar con SecurityException cuando authHeader es null o vacio")
    void testRechazarNullOVacio() {
        assertThrows(SecurityException.class, () -> SecurityUtils.validarRolAdministrativo(null));
        assertThrows(SecurityException.class, () -> SecurityUtils.validarRolAdministrativo(""));
        assertThrows(SecurityException.class, () -> SecurityUtils.validarRolAdministrativo("   "));
    }

    @Test
    @DisplayName("Debe permitir acceso para rol ADMIN")
    void testPermitirAdmin() {
        String token = tokenProvider.generateToken("admin@fpune.edu.py", "ADMIN");
        assertDoesNotThrow(() -> SecurityUtils.validarRolAdministrativo("Bearer " + token));
    }

    @Test
    @DisplayName("Debe permitir acceso para rol DIRECTIVA")
    void testPermitirDirectiva() {
        String token = tokenProvider.generateToken("directiva@fpune.edu.py", "DIRECTIVA");
        assertDoesNotThrow(() -> SecurityUtils.validarRolAdministrativo("Bearer " + token));
    }

    @Test
    @DisplayName("Debe denegar acceso con SecurityException para rol MIEMBRO")
    void testDenegarMiembro() {
        String token = tokenProvider.generateToken("miembro@fpune.edu.py", "MIEMBRO");
        SecurityException ex = assertThrows(SecurityException.class,
                () -> SecurityUtils.validarRolAdministrativo("Bearer " + token));
        assertTrue(ex.getMessage().contains("Acceso denegado"));
    }

    @Test
    @DisplayName("Debe lanzar SecurityException con token alterado o invalido")
    void testTokenInvalido() {
        assertThrows(SecurityException.class,
                () -> SecurityUtils.validarRolAdministrativo("Bearer token_invalido_xyz"));
        assertThrows(SecurityException.class,
                () -> SecurityUtils.validarRolAdministrativo("Basic abc1234"));
    }
}
