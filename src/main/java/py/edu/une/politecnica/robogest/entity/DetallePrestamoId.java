package py.edu.une.politecnica.robogest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Clave primaria compuesta para la entidad DetallePrestamo.
 */
@Embeddable
public class DetallePrestamoId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "prestamo_id")
    private Long prestamoId;

    @Column(name = "material_id")
    private Long materialId;

    public DetallePrestamoId() {
    }

    public DetallePrestamoId(Long prestamoId, Long materialId) {
        this.prestamoId = prestamoId;
        this.materialId = materialId;
    }

    public Long getPrestamoId() {
        return prestamoId;
    }

    public void setPrestamoId(Long prestamoId) {
        this.prestamoId = prestamoId;
    }

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetallePrestamoId that = (DetallePrestamoId) o;
        return Objects.equals(prestamoId, that.prestamoId) &&
               Objects.equals(materialId, that.materialId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prestamoId, materialId);
    }

    @Override
    public String toString() {
        return "DetallePrestamoId{" +
                "prestamoId=" + prestamoId +
                ", materialId=" + materialId +
                '}';
    }
}
