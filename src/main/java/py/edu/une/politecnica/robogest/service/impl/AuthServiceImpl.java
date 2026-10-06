package py.edu.une.politecnica.robogest.service.impl;

import org.mindrot.jbcrypt.BCrypt;
import py.edu.une.politecnica.robogest.dao.IntegranteDAO;
import py.edu.une.politecnica.robogest.entity.Integrante;
import py.edu.une.politecnica.robogest.entity.enums.EstadoIntegranteEnum;
import py.edu.une.politecnica.robogest.exception.CredencialesInvalidasException;
import py.edu.une.politecnica.robogest.exception.IntegranteInactivoException;
import py.edu.une.politecnica.robogest.security.JwtTokenProvider;
import py.edu.une.politecnica.robogest.service.AuthService;

import java.util.Objects;
import java.util.Optional;

/**
 * Implementacion del servicio de autenticacion con jBCrypt y JWT.
 */
public class AuthServiceImpl implements AuthService {

    private final IntegranteDAO integranteDAO;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(IntegranteDAO integranteDAO, JwtTokenProvider jwtTokenProvider) {
        this.integranteDAO = Objects.requireNonNull(integranteDAO, "integranteDAO no puede ser null");
        this.jwtTokenProvider = Objects.requireNonNull(jwtTokenProvider, "jwtTokenProvider no puede ser null");
    }

    @Override
    public String autenticar(String identificador, String passwordPlana) {
        if (identificador == null || identificador.isBlank() || passwordPlana == null || passwordPlana.isBlank()) {
            throw new CredencialesInvalidasException("El identificador y la contraseña no pueden estar vacios");
        }

        // Buscar primero por email; si no se encuentra, buscar por CI (cedula)
        Optional<Integrante> integranteOpt = integranteDAO.findByEmail(identificador.trim());
        if (integranteOpt.isEmpty()) {
            integranteOpt = integranteDAO.findByCi(identificador.trim());
        }

        Integrante integrante = integranteOpt.orElseThrow(() ->
                new CredencialesInvalidasException("Credenciales invalidas"));

        // Validar que el integrante se encuentre activo
        if (integrante.getEstado() == EstadoIntegranteEnum.INACTIVO) {
            throw new IntegranteInactivoException("El integrante se encuentra inactivo en el sistema");
        }

        // Validar hash de contraseña con jBCrypt
        String hashAlmacenado = integrante.getPasswordHash();
        if (hashAlmacenado == null || hashAlmacenado.isBlank() || !BCrypt.checkpw(passwordPlana, hashAlmacenado)) {
            throw new CredencialesInvalidasException("Credenciales invalidas");
        }

        // Generar token JWT con claims del integrante y su rol
        return jwtTokenProvider.generateToken(integrante);
    }
}
