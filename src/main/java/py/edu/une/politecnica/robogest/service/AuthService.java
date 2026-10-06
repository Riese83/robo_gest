package py.edu.une.politecnica.robogest.service;

/**
 * Servicio encargado de la autenticacion de usuarios y expedicion de tokens JWT.
 */
public interface AuthService {

    /**
     * Autentica al integrante utilizando su identificador (email o numero de cedula)
     * y su contraseña en texto plano, validando el hash con jBCrypt.
     *
     * @param identificador Email o numero de cedula de identidad
     * @param passwordPlana Contraseña enviada por el usuario
     * @return Token JWT firmado si las credenciales son validas
     */
    String autenticar(String identificador, String passwordPlana);
}
