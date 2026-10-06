package py.edu.une.politecnica.robogest.exception;

/**
 * Excepcion base para errores del dominio y reglas de negocio de Robo_Gest.
 */
public class RoboGestException extends RuntimeException {

    public RoboGestException(String message) {
        super(message);
    }

    public RoboGestException(String message, Throwable cause) {
        super(message, cause);
    }
}
