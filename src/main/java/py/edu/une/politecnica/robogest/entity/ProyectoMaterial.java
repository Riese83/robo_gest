package py.edu.une.politecnica.robogest.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * Entidad que asocia materiales planificados para el desarrollo de un proyecto.
 */
@Entity
@Table(name = "proyecto_material", indexes = {
    @Index(name = "idx_pm_material", columnList = "material_id")
})
public class ProyectoMaterial implements Serializable {

    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private ProyectoMaterialId id = new ProyectoMaterialId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("proyectoId")
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("materialId")
    @JoinColumn(name = "material_id", nullable = false)
    private Material material;

    @Column(name = "cantidad_planificada")
    private Integer cantidadPlanificada;

    public ProyectoMaterial() {
    }

    public ProyectoMaterial(Proyecto proyecto, Material material, Integer cantidadPlanificada) {
        this.proyecto = proyecto;
        this.material = material;
        this.cantidadPlanificada = cantidadPlanificada;
        if (proyecto != null && material != null) {
            this.id = new ProyectoMaterialId(proyecto.getId(), material.getId());
        }
    }

    public ProyectoMaterialId getId() {
        return id;
    }

    public void setId(ProyectoMaterialId id) {
        this.id = id;
    }

    public Proyecto getProyecto() {
        return proyecto;
    }

    public void setProyecto(Proyecto proyecto) {
        this.proyecto = proyecto;
        if (proyecto != null && this.id != null) {
            this.id.setProyectoId(proyecto.getId());
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

    public Integer getCantidadPlanificada() {
        return cantidadPlanificada;
    }

    public void setCantidadPlanificada(Integer cantidadPlanificada) {
        this.cantidadPlanificada = cantidadPlanificada;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProyectoMaterial that = (ProyectoMaterial) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ProyectoMaterial{" +
                "id=" + id +
                ", cantidadPlanificada=" + cantidadPlanificada +
                '}';
    }
}
