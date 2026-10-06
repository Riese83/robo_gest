package py.edu.une.politecnica.robogest.exception;

/**
 * Excepcion lanzada cuando no se encuentra un prestamo por su identificador.
 */
public class PrestamoNoEncontradoException extends RoboGestException {

    public PrestamoNoEncontradoException(Long prestamoId) {
        super("No se encontro ningun prestamo con el ID: " + prestamoId);
    }

    public PrestamoNoEncontradoException(String message) {
        super(message);
    }
}
