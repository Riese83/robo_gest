package py.edu.une.politecnica.robogest.exception;

/**
 * Excepcion lanzada cuando se intenta una transicion de estado invalida en un prestamo.
 */
public class EstadoPrestamoInvalidoException extends RoboGestException {

    public EstadoPrestamoInvalidoException(String message) {
        super(message);
    }
}
