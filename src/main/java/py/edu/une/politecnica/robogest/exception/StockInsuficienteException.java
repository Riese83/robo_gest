package py.edu.une.politecnica.robogest.exception;

/**
 * Excepcion lanzada cuando el stock disponible de un material no cubre la cantidad solicitada.
 */
public class StockInsuficienteException extends RoboGestException {

    private final Long materialId;
    private final String nombreMaterial;
    private final int cantidadSolicitada;
    private final int cantidadDisponible;

    public StockInsuficienteException(String message) {
        super(message);
        this.materialId = null;
        this.nombreMaterial = null;
        this.cantidadSolicitada = 0;
        this.cantidadDisponible = 0;
    }

    public StockInsuficienteException(Long materialId, String nombreMaterial, int cantidadSolicitada, int cantidadDisponible) {
        super(String.format("Stock insuficiente para el material '%s' (ID: %d). Solicitado: %d, Disponible: %d",
                nombreMaterial, materialId, cantidadSolicitada, cantidadDisponible));
        this.materialId = materialId;
        this.nombreMaterial = nombreMaterial;
        this.cantidadSolicitada = cantidadSolicitada;
        this.cantidadDisponible = cantidadDisponible;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public String getNombreMaterial() {
        return nombreMaterial;
    }

    public int getCantidadSolicitada() {
        return cantidadSolicitada;
    }

    public int getCantidadDisponible() {
        return cantidadDisponible;
    }
}
