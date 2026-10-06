package py.edu.une.politecnica.robogest.exception;

/**
 * Excepcion lanzada cuando se intenta realizar una operacion con un integrante en estado INACTIVO.
 */
public class IntegranteInactivoException extends RoboGestException {

    public IntegranteInactivoException(String message) {
        super(message);
    }
}
