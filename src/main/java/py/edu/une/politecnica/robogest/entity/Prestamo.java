package py.edu.une.politecnica.robogest.entity;

import jakarta.persistence.*;
import py.edu.une.politecnica.robogest.entity.enums.EstadoPrestamoEnum;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Entidad que representa la transaccion y ciclo de vida de un prestamo de materiales.
 */
@Entity
@Table(name = "prestamo", indexes = {
    @Index(name = "idx_prestamo_integrante", columnList = "integrante_id"),
    @Index(name = "idx_prestamo_proyecto", columnList = "proyecto_id"),
    @Index(name = "idx_prestamo_estado", columnList = "estado")
})
public class Prestamo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prestamo_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "integrante_id", nullable = false)
    private Integrante integrante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proyecto_id")
    private Proyecto proyecto;

    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud = LocalDateTime.now();

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @Column(name = "fecha_devolucion_prevista")
    private LocalDateTime fechaDevolucionPrevista;

    @Column(name = "fecha_devolucion_real")
    private LocalDateTime fechaDevolucionReal;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoPrestamoEnum estado = EstadoPrestamoEnum.SOLICITADO;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    @OneToMany(mappedBy = "prestamo", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetallePrestamo> detalles = new ArrayList<>();

    public Prestamo() {
    }

    public Prestamo(Integrante integrante, Proyecto proyecto, LocalDateTime fechaSolicitud,
                    LocalDateTime fechaEntrega, LocalDateTime fechaDevolucionPrevista,
                    LocalDateTime fechaDevolucionReal, EstadoPrestamoEnum estado, String descripcion) {
        this.integrante = integrante;
        this.proyecto = proyecto;
        this.fechaSolicitud = fechaSolicitud;
        this.fechaEntrega = fechaEntrega;
        this.fechaDevolucionPrevista = fechaDevolucionPrevista;
        this.fechaDevolucionReal = fechaDevolucionReal;
        this.estado = estado;
        this.descripcion = descripcion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integrante getIntegrante() {
        return integrante;
    }

    public void setIntegrante(Integrante integrante) {
        this.integrante = integrante;
    }

    public Proyecto getProyecto() {
        return proyecto;
    }

    public void setProyecto(Proyecto proyecto) {
        this.proyecto = proyecto;
    }

    public LocalDateTime getFechaSolicitud() {
        return fechaSolicitud;
    }

    public void setFechaSolicitud(LocalDateTime fechaSolicitud) {
        this.fechaSolicitud = fechaSolicitud;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public void setFechaEntrega(LocalDateTime fechaEntrega) {
        this.fechaEntrega = fechaEntrega;
    }

    public LocalDateTime getFechaDevolucionPrevista() {
        return fechaDevolucionPrevista;
    }

    public void setFechaDevolucionPrevista(LocalDateTime fechaDevolucionPrevista) {
        this.fechaDevolucionPrevista = fechaDevolucionPrevista;
    }

    public LocalDateTime getFechaDevolucionReal() {
        return fechaDevolucionReal;
    }

    public void setFechaDevolucionReal(LocalDateTime fechaDevolucionReal) {
        this.fechaDevolucionReal = fechaDevolucionReal;
    }

    public EstadoPrestamoEnum getEstado() {
        return estado;
    }

    public void setEstado(EstadoPrestamoEnum estado) {
        this.estado = estado;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public List<DetallePrestamo> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetallePrestamo> detalles) {
        this.detalles = detalles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Prestamo prestamo = (Prestamo) o;
        return Objects.equals(id, prestamo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Prestamo{" +
                "id=" + id +
                ", fechaSolicitud=" + fechaSolicitud +
                ", fechaEntrega=" + fechaEntrega +
                ", fechaDevolucionPrevista=" + fechaDevolucionPrevista +
                ", fechaDevolucionReal=" + fechaDevolucionReal +
                ", estado=" + estado +
                '}';
    }
}
