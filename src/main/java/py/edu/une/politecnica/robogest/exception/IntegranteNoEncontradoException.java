package py.edu.une.politecnica.robogest.exception;

/**
 * Excepcion lanzada cuando no se localiza un integrante por sus identificadores (ID, CI, NFC, Email).
 */
public class IntegranteNoEncontradoException extends RoboGestException {

    public IntegranteNoEncontradoException(String message) {
        super(message);
    }
}
