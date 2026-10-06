package py.edu.une.politecnica.robogest.entity;

import jakarta.persistence.*;
import py.edu.une.politecnica.robogest.entity.enums.EstadoProyectoEnum;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entidad que representa un proyecto del Club de Robotica.
 */
@Entity
@Table(name = "proyecto")
public class Proyecto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "proyecto_id")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoProyectoEnum estado = EstadoProyectoEnum.PLANIFICADO;

    @OneToMany(mappedBy = "proyecto", fetch = FetchType.LAZY)
    private List<IntegranteProyecto> integranteProyectos = new ArrayList<>();

    @OneToMany(mappedBy = "proyecto", fetch = FetchType.LAZY)
    private List<Prestamo> prestamos = new ArrayList<>();

    @OneToMany(mappedBy = "proyecto", fetch = FetchType.LAZY)
    private List<ProyectoMaterial> proyectoMateriales = new ArrayList<>();

    public Proyecto() {
    }

    public Proyecto(String nombre, String descripcion, LocalDate fechaInicio,
                    LocalDate fechaFin, EstadoProyectoEnum estado) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.estado = estado;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public EstadoProyectoEnum getEstado() {
        return estado;
    }

    public void setEstado(EstadoProyectoEnum estado) {
        this.estado = estado;
    }

    public List<IntegranteProyecto> getIntegranteProyectos() {
        return integranteProyectos;
    }

    public void setIntegranteProyectos(List<IntegranteProyecto> integranteProyectos) {
        this.integranteProyectos = integranteProyectos;
    }

    public List<Prestamo> getPrestamos() {
        return prestamos;
    }

    public void setPrestamos(List<Prestamo> prestamos) {
        this.prestamos = prestamos;
    }

    public List<ProyectoMaterial> getProyectoMateriales() {
        return proyectoMateriales;
    }

    public void setProyectoMateriales(List<ProyectoMaterial> proyectoMateriales) {
        this.proyectoMateriales = proyectoMateriales;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Proyecto proyecto = (Proyecto) o;
        return Objects.equals(id, proyecto.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Proyecto{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", fechaInicio=" + fechaInicio +
                ", fechaFin=" + fechaFin +
                ", estado=" + estado +
                '}';
    }
}
