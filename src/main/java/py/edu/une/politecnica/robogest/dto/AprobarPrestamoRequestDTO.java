package py.edu.une.politecnica.robogest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO para la solicitud de aprobacion de prestamo con fecha de devolucion opcional.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AprobarPrestamoRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long prestamoId;
    private String observacion;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaDevolucionPrevista;

    public AprobarPrestamoRequestDTO() {
    }

    public AprobarPrestamoRequestDTO(LocalDateTime fechaDevolucionPrevista) {
        this.fechaDevolucionPrevista = fechaDevolucionPrevista;
    }

    public Long getPrestamoId() {
        return prestamoId;
    }

    public void setPrestamoId(Long prestamoId) {
        this.prestamoId = prestamoId;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public LocalDateTime getFechaDevolucionPrevista() {
        return fechaDevolucionPrevista;
    }

    public void setFechaDevolucionPrevista(LocalDateTime fechaDevolucionPrevista) {
        this.fechaDevolucionPrevista = fechaDevolucionPrevista;
    }
}
