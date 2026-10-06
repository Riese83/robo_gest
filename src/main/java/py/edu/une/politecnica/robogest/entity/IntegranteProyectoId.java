package py.edu.une.politecnica.robogest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Clave primaria compuesta para la entidad IntegranteProyecto.
 */
@Embeddable
public class IntegranteProyectoId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "integrante_id")
    private Long integranteId;

    @Column(name = "proyecto_id")
    private Long proyectoId;

    public IntegranteProyectoId() {
    }

    public IntegranteProyectoId(Long integranteId, Long proyectoId) {
        this.integranteId = integranteId;
        this.proyectoId = proyectoId;
    }

    public Long getIntegranteId() {
        return integranteId;
    }

    public void setIntegranteId(Long integranteId) {
        this.integranteId = integranteId;
    }

    public Long getProyectoId() {
        return proyectoId;
    }

    public void setProyectoId(Long proyectoId) {
        this.proyectoId = proyectoId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntegranteProyectoId that = (IntegranteProyectoId) o;
        return Objects.equals(integranteId, that.integranteId) &&
               Objects.equals(proyectoId, that.proyectoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(integranteId, proyectoId);
    }

    @Override
    public String toString() {
        return "IntegranteProyectoId{" +
                "integranteId=" + integranteId +
                ", proyectoId=" + proyectoId +
                '}';
    }
}
