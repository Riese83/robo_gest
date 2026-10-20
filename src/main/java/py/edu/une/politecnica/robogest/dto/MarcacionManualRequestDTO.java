package py.edu.une.politecnica.robogest.dto;

import java.io.Serializable;

/**
 * DTO para el registro manual de asistencia de integrantes o visitantes.
 */
public class MarcacionManualRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long integranteId;
    private String nfcUid;
    private String nombre;
    private String apellido;
    private String ci;
    private String carrera;
    private String tipoMarcacion; // "ENTRADA", "SALIDA", o "AUTO"
    private String dispositivo;   // ej. "REGISTRO_MANUAL_LAB"

    public MarcacionManualRequestDTO() {
    }

    public MarcacionManualRequestDTO(Long integranteId, String nfcUid, String nombre, String apellido,
                                     String ci, String carrera, String tipoMarcacion, String dispositivo) {
        this.integranteId = integranteId;
        this.nfcUid = nfcUid;
        this.nombre = nombre;
        this.apellido = apellido;
        this.ci = ci;
        this.carrera = carrera;
        this.tipoMarcacion = tipoMarcacion;
        this.dispositivo = dispositivo;
    }

    public Long getIntegranteId() {
        return integranteId;
    }

    public void setIntegranteId(Long integranteId) {
        this.integranteId = integranteId;
    }

    public String getNfcUid() {
        return nfcUid;
    }

    public void setNfcUid(String nfcUid) {
        this.nfcUid = nfcUid;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
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

    public String getTipoMarcacion() {
        return tipoMarcacion;
    }

    public void setTipoMarcacion(String tipoMarcacion) {
        this.tipoMarcacion = tipoMarcacion;
    }

    public String getDispositivo() {
        return dispositivo;
    }

    public void setDispositivo(String dispositivo) {
        this.dispositivo = dispositivo;
    }
}
