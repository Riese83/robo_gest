package py.edu.une.politecnica.robogest.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DTO de respuesta para confirmacion de marcacion IoT retornada al ESP32.
 */
public class MarcacionResponseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long asistenciaId;
    private Long integranteId;
    private String nombreIntegrante;
    private String carnetUniversitario;
    private String tipoMarcacion; // "ENTRADA" o "SALIDA"

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaHora;

    private String dispositivo;
    private String mensaje;

    public MarcacionResponseDTO() {
    }

    public MarcacionResponseDTO(Long asistenciaId, Long integranteId, String nombreIntegrante,
                                String carnetUniversitario, String tipoMarcacion,
                                LocalDateTime fechaHora, String dispositivo, String mensaje) {
        this.asistenciaId = asistenciaId;
        this.integranteId = integranteId;
        this.nombreIntegrante = nombreIntegrante;
        this.carnetUniversitario = carnetUniversitario;
        this.tipoMarcacion = tipoMarcacion;
        this.fechaHora = fechaHora;
        this.dispositivo = dispositivo;
        this.mensaje = mensaje;
    }

    public Long getAsistenciaId() {
        return asistenciaId;
    }

    public void setAsistenciaId(Long asistenciaId) {
        this.asistenciaId = asistenciaId;
    }

    public Long getIntegranteId() {
        return integranteId;
    }

    public void setIntegranteId(Long integranteId) {
        this.integranteId = integranteId;
    }

    public String getNombreIntegrante() {
        return nombreIntegrante;
    }

    public void setNombreIntegrante(String nombreIntegrante) {
        this.nombreIntegrante = nombreIntegrante;
    }

    public String getCarnetUniversitario() {
        return carnetUniversitario;
    }

    public void setCarnetUniversitario(String carnetUniversitario) {
        this.carnetUniversitario = carnetUniversitario;
    }

    public String getTipoMarcacion() {
        return tipoMarcacion;
    }

    public void setTipoMarcacion(String tipoMarcacion) {
        this.tipoMarcacion = tipoMarcacion;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getDispositivo() {
        return dispositivo;
    }

    public void setDispositivo(String dispositivo) {
        this.dispositivo = dispositivo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
