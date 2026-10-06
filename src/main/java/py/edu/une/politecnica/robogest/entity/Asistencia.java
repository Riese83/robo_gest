package py.edu.une.politecnica.robogest.entity;

import jakarta.persistence.*;
import py.edu.une.politecnica.robogest.entity.enums.TipoMarcacionEnum;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad que registra el control de asistencia IoT (ESP32 con lector RFID).
 */
@Entity
@Table(name = "asistencia", indexes = {
    @Index(name = "idx_asistencia_integrante_fecha", columnList = "integrante_id, fecha_hora")
})
public class Asistencia implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asistencia_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "integrante_id", nullable = false)
    private Integrante integrante;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_marcacion", nullable = false, length = 10)
    private TipoMarcacionEnum tipoMarcacion;

    @Column(name = "dispositivo_utilizado", length = 100)
    private String dispositivoUtilizado;

    public Asistencia() {
    }

    public Asistencia(Integrante integrante, LocalDateTime fechaHora,
                      TipoMarcacionEnum tipoMarcacion, String dispositivoUtilizado) {
        this.integrante = integrante;
        this.fechaHora = fechaHora;
        this.tipoMarcacion = tipoMarcacion;
        this.dispositivoUtilizado = dispositivoUtilizado;
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

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public TipoMarcacionEnum getTipoMarcacion() {
        return tipoMarcacion;
    }

    public void setTipoMarcacion(TipoMarcacionEnum tipoMarcacion) {
        this.tipoMarcacion = tipoMarcacion;
    }

    public String getDispositivoUtilizado() {
        return dispositivoUtilizado;
    }

    public void setDispositivoUtilizado(String dispositivoUtilizado) {
        this.dispositivoUtilizado = dispositivoUtilizado;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Asistencia that = (Asistencia) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Asistencia{" +
                "id=" + id +
                ", fechaHora=" + fechaHora +
                ", tipoMarcacion=" + tipoMarcacion +
                ", dispositivoUtilizado='" + dispositivoUtilizado + '\'' +
                '}';
    }
}
