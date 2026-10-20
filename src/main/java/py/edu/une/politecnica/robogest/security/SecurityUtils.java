package py.edu.une.politecnica.robogest.security;

import java.util.Set;

/**
 * Utilidad de seguridad y control de autorizacion por roles (RBAC).
 */
public final class SecurityUtils {

    private static final Set<String> ROLES_ADMINISTRATIVOS = Set.of("ADMIN", "DIRECTIVA");
    private static final JwtTokenProvider tokenProvider = new JwtTokenProvider();
    public static final String INTERNAL_ADMIN_TOKEN = "Bearer " + tokenProvider.generateToken("admin@fpune.edu.py", "ADMIN");

    private SecurityUtils() {
    }

    /**
     * Valida que la cabecera Authorization contenga un token JWT valido
     * emitido para un usuario con rol de privilegio administrativo (ADMIN o DIRECTIVA).
     *
     * @param authHeader Cabecera HTTP Authorization ('Bearer <token>')
     * @throws SecurityException si la cabecera falta, es invalida, expirada o el rol es insuficiente
     */
    public static void validarRolAdministrativo(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            throw new SecurityException("Acceso denegado: Cabecera Authorization requerida");
        }
        if (!authHeader.startsWith("Bearer ")) {
            throw new SecurityException("Cabecera Authorization invalida. Formato esperado: Bearer <token>");
        }
        String token = authHeader.substring(7).trim();
        if (!tokenProvider.validateToken(token)) {
            throw new SecurityException("Token JWT invalido o expirado");
        }
        String rol = tokenProvider.getRolFromToken(token);
        if (rol == null || !ROLES_ADMINISTRATIVOS.contains(rol.toUpperCase())) {
            throw new SecurityException(String.format(
                    "Acceso denegado: El rol '%s' no cuenta con privilegios suficientes. Se requiere rol ADMIN o DIRECTIVA.",
                    rol != null ? rol : "DESCONOCIDO"
            ));
        }
    }

    /**
     * Valida que la cabecera Authorization contenga un token JWT valido para cualquier rol activo.
     * Retorna los Claims para identificar al usuario que realiza la peticion.
     */
    public static io.jsonwebtoken.Claims validarAutenticado(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            throw new SecurityException("Acceso denegado: Cabecera Authorization requerida");
        }
        if (!authHeader.startsWith("Bearer ")) {
            throw new SecurityException("Cabecera Authorization invalida. Formato esperado: Bearer <token>");
        }
        String token = authHeader.substring(7).trim();
        if (!tokenProvider.validateToken(token)) {
            throw new SecurityException("Token JWT invalido o expirado");
        }
        return tokenProvider.getClaimsFromToken(token);
    }
}
