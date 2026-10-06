package py.edu.une.politecnica.robogest.entity;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Entidad asociativa (N:M) entre Integrante y Proyecto.
 */
@Entity
@Table(name = "integrante_proyecto", indexes = {
    @Index(name = "idx_ip_proyecto", columnList = "proyecto_id")
})
public class IntegranteProyecto implements Serializable {

    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private IntegranteProyectoId id = new IntegranteProyectoId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("integranteId")
    @JoinColumn(name = "integrante_id", nullable = false)
    private Integrante integrante;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("proyectoId")
    @JoinColumn(name = "proyecto_id", nullable = false)
    private Proyecto proyecto;

    @Column(name = "fecha_incorporacion", nullable = false)
    private LocalDate fechaIncorporacion;

    public IntegranteProyecto() {
    }

    public IntegranteProyecto(Integrante integrante, Proyecto proyecto, LocalDate fechaIncorporacion) {
        this.integrante = integrante;
        this.proyecto = proyecto;
        this.fechaIncorporacion = fechaIncorporacion;
        if (integrante != null && proyecto != null) {
            this.id = new IntegranteProyectoId(integrante.getId(), proyecto.getId());
        }
    }

    public IntegranteProyectoId getId() {
        return id;
    }

    public void setId(IntegranteProyectoId id) {
        this.id = id;
    }

    public Integrante getIntegrante() {
        return integrante;
    }

    public void setIntegrante(Integrante integrante) {
        this.integrante = integrante;
        if (integrante != null && this.id != null) {
            this.id.setIntegranteId(integrante.getId());
        }
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

    public LocalDate getFechaIncorporacion() {
        return fechaIncorporacion;
    }

    public void setFechaIncorporacion(LocalDate fechaIncorporacion) {
        this.fechaIncorporacion = fechaIncorporacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntegranteProyecto that = (IntegranteProyecto) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "IntegranteProyecto{" +
                "id=" + id +
                ", fechaIncorporacion=" + fechaIncorporacion +
                '}';
    }
}
