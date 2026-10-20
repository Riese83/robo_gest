package py.edu.une.politecnica.robogest.dto;

import java.io.Serializable;

/**
 * Item individual de material y cantidad solicitado en un prestamo.
 */
public class DetallePrestamoItemDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long materialId;
    private Integer cantidad;

    public DetallePrestamoItemDTO() {
    }

    public DetallePrestamoItemDTO(Long materialId, Integer cantidad) {
        this.materialId = materialId;
        this.cantidad = cantidad;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }
}
