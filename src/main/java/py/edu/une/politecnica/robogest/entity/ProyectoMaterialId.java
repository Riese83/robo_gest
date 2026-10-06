package py.edu.une.politecnica.robogest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Clave primaria compuesta para la entidad ProyectoMaterial.
 */
@Embeddable
public class ProyectoMaterialId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "proyecto_id")
    private Long proyectoId;

    @Column(name = "material_id")
    private Long materialId;

    public ProyectoMaterialId() {
    }

    public ProyectoMaterialId(Long proyectoId, Long materialId) {
        this.proyectoId = proyectoId;
        this.materialId = materialId;
    }

    public Long getProyectoId() {
        return proyectoId;
    }

    public void setProyectoId(Long proyectoId) {
        this.proyectoId = proyectoId;
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
        ProyectoMaterialId that = (ProyectoMaterialId) o;
        return Objects.equals(proyectoId, that.proyectoId) &&
               Objects.equals(materialId, that.materialId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(proyectoId, materialId);
    }

    @Override
    public String toString() {
        return "ProyectoMaterialId{" +
                "proyectoId=" + proyectoId +
                ", materialId=" + materialId +
                '}';
    }
}
