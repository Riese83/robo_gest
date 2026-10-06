package py.edu.une.politecnica.robogest.controller;

import io.jsonwebtoken.Claims;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import py.edu.une.politecnica.robogest.dao.impl.IntegranteDAOJpaImpl;
import py.edu.une.politecnica.robogest.dto.AuthResponseDTO;
import py.edu.une.politecnica.robogest.dto.LoginRequestDTO;
import py.edu.une.politecnica.robogest.security.JwtTokenProvider;
import py.edu.une.politecnica.robogest.service.AuthService;
import py.edu.une.politecnica.robogest.service.impl.AuthServiceImpl;
import py.edu.une.politecnica.robogest.util.JpaUtil;

import java.util.Objects;

/**
 * Controlador REST para el modulo de autenticacion de usuarios.
 */
@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Constructor por defecto para el contenedor JAX-RS / Servlets.
     */
    public AuthController() {
        this.jwtTokenProvider = new JwtTokenProvider();
        this.authService = new AuthServiceImpl(
                new IntegranteDAOJpaImpl(),
                this.jwtTokenProvider
        );
    }

    /**
     * Constructor para inyeccion de dependencias (pruebas unitarias / frameworks de DI).
     */
    public AuthController(AuthService authService, JwtTokenProvider jwtTokenProvider) {
        this.authService = Objects.requireNonNull(authService, "authService no puede ser null");
        this.jwtTokenProvider = Objects.requireNonNull(jwtTokenProvider, "jwtTokenProvider no puede ser null");
    }

    /**
     * Endpoint publico para inicio de sesion de integrantes.
     * POST /api/auth/login
     */
    @POST
    @Path("/login")
    public Response login(LoginRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("El cuerpo de la solicitud no puede estar vacio");
        }

        // Autenticar credenciales y obtener token JWT
        String token = authService.autenticar(request.getIdentificador(), request.getPassword());

        // Extraer claims para poblar la respuesta
        Claims claims = jwtTokenProvider.getClaimsFromToken(token);
        Number integranteIdNum = claims.get("integranteId", Number.class);
        Long integranteId = integranteIdNum != null ? integranteIdNum.longValue() : null;
        String ci = claims.get("ci", String.class);
        String nombreCompleto = claims.get("nombreCompleto", String.class);
        String rol = claims.get("rol", String.class);
        long expiresInMs = claims.getExpiration().getTime() - System.currentTimeMillis();

        AuthResponseDTO responseDTO = new AuthResponseDTO(
                token,
                integranteId,
                claims.getSubject(),
                ci,
                nombreCompleto,
                rol,
                Math.max(expiresInMs, 0)
        );

        return Response.ok(responseDTO).build();
    }
}
