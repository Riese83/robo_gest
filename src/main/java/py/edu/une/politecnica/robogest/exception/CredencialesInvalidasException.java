package py.edu.une.politecnica.robogest.exception;

/**
 * Excepcion lanzada ante credenciales de autenticacion erroneas o invalidas.
 */
public class CredencialesInvalidasException extends RoboGestException {

    public CredencialesInvalidasException(String message) {
        super(message);
    }
}
