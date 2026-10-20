package py.edu.une.politecnica.robogest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * DTO para representar un participante vinculado a un proyecto.
 */
public class ProyectoParticipanteDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long integranteId;
    private String nombreCompleto;
    private String ci;
    private String carrera;
    private String email;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate fechaIncorporacion;

    public ProyectoParticipanteDTO() {
    }

    public ProyectoParticipanteDTO(Long integranteId, String nombreCompleto, String ci, String carrera, String email, LocalDate fechaIncorporacion) {
        this.integranteId = integranteId;
        this.nombreCompleto = nombreCompleto;
        this.ci = ci;
        this.carrera = carrera;
        this.email = email;
        this.fechaIncorporacion = fechaIncorporacion;
    }

    public Long getIntegranteId() {
        return integranteId;
    }

    public void setIntegranteId(Long integranteId) {
        this.integranteId = integranteId;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getCi() {
        return ci;
    }

    public void setCi(String ci) {
        this.ci = ci;
    }

    public String getCarrera() {
        return carrera;
    }

    public void setCarrera(String carrera) {
        this.carrera = carrera;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getFechaIncorporacion() {
        return fechaIncorporacion;
    }

    public void setFechaIncorporacion(LocalDate fechaIncorporacion) {
        this.fechaIncorporacion = fechaIncorporacion;
    }
}
