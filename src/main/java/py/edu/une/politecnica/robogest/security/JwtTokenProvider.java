package py.edu.une.politecnica.robogest.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import py.edu.une.politecnica.robogest.entity.Integrante;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Objects;

/**
 * Proveedor y validador de JSON Web Tokens (JWT) para autenticacion y autorizacion.
 */
public class JwtTokenProvider {

    // Clave secreta fija por defecto (minimo 256 bits para HMAC-SHA256)
    private static final String DEFAULT_SECRET = "RoboGestSecretKeyForJwtTokenGenerationSecurityFPUNE2026ClubRobotica!";
    private static final long DEFAULT_EXPIRATION_MS = 86400000L; // 24 horas

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider() {
        this(DEFAULT_SECRET, DEFAULT_EXPIRATION_MS);
    }

    public JwtTokenProvider(String secret, long expirationMs) {
        Objects.requireNonNull(secret, "El secreto JWT no puede ser null");
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /**
     * Genera un token JWT a partir de la entidad Integrante incluyendo claims de rol y datos clave.
     */
    public String generateToken(Integrante integrante) {
        Objects.requireNonNull(integrante, "El integrante no puede ser null");
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        String rol = integrante.getRol() != null ? integrante.getRol().name() : "MIEMBRO";

        return Jwts.builder()
                .subject(integrante.getEmail())
                .claim("integranteId", integrante.getId())
                .claim("ci", integrante.getCi())
                .claim("nombreCompleto", integrante.getNombre() + " " + integrante.getApellido())
                .claim("rol", rol)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Genera un token JWT indicando directamente sujeto y rol.
     */
    public String generateToken(String username, String rol) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("rol", rol)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * Valida la firma y expiracion del token JWT.
     */
    public boolean validateToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Obtiene los claims decodificados del token.
     */
    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extrae el sujeto (username o email) del token.
     */
    public String getUsernameFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * Extrae el rol asignado al integrante desde el token.
     */
    public String getRolFromToken(String token) {
        return getClaimsFromToken(token).get("rol", String.class);
    }

    /**
     * Extrae el ID del integrante del token.
     */
    public Long getIntegranteIdFromToken(String token) {
        Number id = getClaimsFromToken(token).get("integranteId", Number.class);
        return id != null ? id.longValue() : null;
    }
}
