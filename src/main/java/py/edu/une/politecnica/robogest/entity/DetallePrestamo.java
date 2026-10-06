package py.edu.une.politecnica.robogest.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * Entidad que representa cada item o material asociado a un prestamo.
 */
@Entity
@Table(name = "detalle_prestamo", indexes = {
    @Index(name = "idx_dp_material", columnList = "material_id")
})
public class DetallePrestamo implements Serializable {

    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private DetallePrestamoId id = new DetallePrestamoId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("prestamoId")
    @JoinColumn(name = "prestamo_id", nullable = false)
    private Prestamo prestamo;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("materialId")
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    public DetallePrestamo() {
    }

    public DetallePrestamo(Prestamo prestamo, Material material, Integer cantidad) {
        this.prestamo = prestamo;
        this.material = material;
        this.cantidad = cantidad;
        if (prestamo != null && material != null) {
            this.id = new DetallePrestamoId(prestamo.getId(), material.getId());
        }
    }

    public DetallePrestamoId getId() {
        return id;
    }

    public void setId(DetallePrestamoId id) {
        this.id = id;
    }

    public Prestamo getPrestamo() {
        return prestamo;
    }

    public void setPrestamo(Prestamo prestamo) {
        this.prestamo = prestamo;
        if (prestamo != null && this.id != null) {
            this.id.setPrestamoId(prestamo.getId());
        }
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
        if (material != null && this.id != null) {
            this.id.setMaterialId(material.getId());
        }
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetallePrestamo that = (DetallePrestamo) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "DetallePrestamo{" +
                "id=" + id +
                ", cantidad=" + cantidad +
                '}';
    }
}
